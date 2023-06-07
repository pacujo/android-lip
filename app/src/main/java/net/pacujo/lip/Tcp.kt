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

class TcpConnection private constructor(
    private val socket: AsynchronousSocketChannel,
) : Connection {
    private suspend fun connect(address: SocketAddress) =
        suspendCoroutine {
            socket.connect(address, it, voidAsyncHandler)
        }

    override suspend fun read(buf: ByteBuffer) =
        suspendCoroutine {
            socket.read(buf, it, intAsyncHandler)
        }

    override suspend fun write(buf: ByteBuffer) =
        suspendCoroutine {
            socket.write(buf, it, intAsyncHandler)
        }

    override suspend fun close() = socket.close()

    companion object {
        suspend fun connect(address: SocketAddress) =
            TcpConnection(AsynchronousSocketChannel.open()).also {
                it.connect(address)
            }

        suspend fun connect(hostname: String, port: Int) =
            connect(InetSocketAddress(hostname, port))
    }
}
