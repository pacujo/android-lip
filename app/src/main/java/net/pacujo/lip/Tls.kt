package net.pacujo.lip

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.ByteBuffer
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngineResult

private val Empty = ByteBuffer.allocate(0)

class TlsConnection private constructor(
    private val transport: Connection,
    clientMode: Boolean = true,
) : Connection {
    private val engine = SSLContext.getDefault().createSSLEngine()
    private val mutex = Mutex()
    private val rxMutex = Mutex()
    private val txMutex = Mutex()
    private val inputWire = ByteBuffer.allocate(10000)
    private val inputPlain = ByteBuffer.allocate(10000)

    init {
        engine.useClientMode = clientMode
        inputWire.flip()
        inputPlain.flip()
    }

    private suspend fun handshake(): Boolean {
        while (true)
            when (engine.handshakeStatus) {
                SSLEngineResult.HandshakeStatus.NEED_UNWRAP -> {
                    if (rxMutex.isLocked) {
                        mutex.withoutLock { rxMutex.withLock {} }
                        continue
                    }
                    while (true) {
                        val remaining = inputWire.remaining()
                        @Suppress("BlockingMethodInNonBlockingContext")
                        engine.unwrap(inputWire, Empty)
                        if (remaining != inputWire.remaining())
                            break
                        inputWire.compact()
                        assert(inputWire.hasRemaining())
                        val count = transport.read(inputWire)
                        inputWire.flip()
                        if (count < 0)
                            return false
                    }
                }

                SSLEngineResult.HandshakeStatus.NEED_WRAP -> {
                    if (txMutex.isLocked) {
                        mutex.withoutLock { txMutex.withLock {} }
                        continue
                    }
                    val outputWire = ByteBuffer.allocate(10000)
                    @Suppress("BlockingMethodInNonBlockingContext")
                    engine.wrap(Empty, outputWire)
                    outputWire.flip()
                    writeAll(outputWire, transport::write)
                }

                SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING -> break

                else -> {
                    val status = engine.handshakeStatus
                    throw IOException(
                        "Unexpected TLS handshake status: $status",
                    )
                }
            }
        return true
    }

    override suspend fun read(buf: ByteBuffer): Int {
        val origPos = buf.position()
        if (!inputPlain.hasRemaining()) {
            inputPlain.compact()
            mutex.withLock {
                while (true) {
                    if (!handshake())
                        return -1
                    val result = engine.unwrap(inputWire, inputPlain)
                    when (result.status) {
                        SSLEngineResult.Status.OK ->
                            if (result.bytesProduced() > 0)
                                break

                        SSLEngineResult.Status.BUFFER_UNDERFLOW -> {
                            inputWire.compact()
                            val count = rxMutex.withLock {
                                mutex.withoutLock {
                                    transport.read(inputWire)
                                }
                            }
                            inputWire.flip()
                            if (count < 0) // dirty EOF
                                return -1
                        }

                        SSLEngineResult.Status.BUFFER_OVERFLOW ->
                            throw IOException("TLS unwrap overflow")

                        else ->
                            return -1
                    }
                }
            }
            inputPlain.flip()
        }
        val amount = minOf(inputPlain.remaining(), buf.remaining())
        val bytes = ByteArray(amount)
        inputPlain.get(bytes)
        buf.put(bytes)
        return buf.position() - origPos
    }

    override suspend fun write(buf: ByteBuffer): Int {
        val count = buf.remaining()
        var outputCapacity = 1000 + count
        mutex.withLock {
            while (buf.remaining() == count) {
                if (!handshake())
                    return -1
                val outputWire = ByteBuffer.allocate(outputCapacity)
                val result = engine.wrap(buf, outputWire)
                when (result.status) {
                    SSLEngineResult.Status.OK -> {
                        outputWire.flip()
                        txMutex.withLock {
                            mutex.withoutLock {
                                if (!writeAll(outputWire, transport::write))
                                    return -1
                            }
                        }
                    }

                    SSLEngineResult.Status.BUFFER_OVERFLOW ->
                        outputCapacity *= 2

                    SSLEngineResult.Status.BUFFER_UNDERFLOW ->
                        throw IOException("TLS wrap underflow")

                    else ->
                        return -1
                }
            }
        }
        return count - buf.remaining()
    }

    override suspend fun close() = transport.close() // TODO properly

    companion object {
        @Suppress("MemberVisibilityCanBePrivate")
        suspend fun connect(address: SocketAddress): TlsConnection {
            val tcpConn = TcpConnection.connect(address)
            return TlsConnection(tcpConn, true)
        }

        suspend fun connect(hostname: String, port: Int) =
            connect(InetSocketAddress(hostname, port))
    }
}

private suspend inline fun <T> Mutex.withoutLock(action: () -> T): T {
    this.unlock()
    try {
        return action()
    } finally {
        this.lock()
    }
}
