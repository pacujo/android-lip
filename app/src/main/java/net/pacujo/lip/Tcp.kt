package net.pacujo.lip

import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val TRACE_DEBUG = false

private fun <T> asyncHandler() =
    object : CompletionHandler<T, Continuation<T>> {
        override fun completed(result: T, cont: Continuation<T>) {
            cont.resume(result)
        }
        override fun failed(exc: Throwable, cont: Continuation<T>) {
            cont.resumeWithException(exc)
        }
    }

private val voidAsyncHandler = asyncHandler<Void>()
private val intAsyncHandler = asyncHandler<Int>()

class TcpConnection(
    private val socket: AsynchronousSocketChannel,
) : Connection {
    suspend fun connect(address: SocketAddress) =
        suspendCoroutine {
            socket.connect(address, it, voidAsyncHandler)
        }

    suspend fun connect(hostname: String, port: Int) =
        connect(InetSocketAddress(hostname, port))

    override suspend fun read(buf: ByteBuffer) =
        monitor(TRACE_DEBUG, "TcpConnection(${objId(this)}).read") {
            suspendCoroutine {
                socket.read(buf, it, intAsyncHandler)
            }
        }

    override suspend fun write(buf: ByteBuffer) =
        monitor(TRACE_DEBUG, "TcpConnection(${objId(this)}).write") {
            suspendCoroutine {
                socket.write(buf, it, intAsyncHandler)
            }
        }

    override suspend fun close() = socket.close()

    companion object Factory {
        suspend fun connect(address: SocketAddress): TcpConnection {
            val conn = TcpConnection(AsynchronousSocketChannel.open())
            conn.connect(address)
            return conn
        }

        suspend fun connect(hostname: String, port: Int): TcpConnection =
            connect(InetSocketAddress(hostname, port))
    }
}
