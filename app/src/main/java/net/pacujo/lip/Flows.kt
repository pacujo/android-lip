package net.pacujo.lip

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.nio.ByteBuffer

suspend fun writeAll(
    buf: ByteBuffer,
    write: suspend (buf: ByteBuffer) -> Int,
): Boolean {
    while (buf.hasRemaining())
        if (write(buf) < 0)
            return false
    return true
}

suspend fun writeAll(
    chunk: ByteArray,
    write: suspend (buf: ByteBuffer) -> Int,
): Boolean =
    writeAll(ByteBuffer.wrap(chunk), write)

fun readFlow(read: suspend (buf: ByteBuffer) -> Int) = flow {
    while (true) {
        val inb = ByteBuffer.allocate(1000)
        if (read(inb) <= 0)
            break
        inb.flip()
        val bytes = ByteArray(inb.remaining())
        inb.get(bytes)
        emit(bytes)
    }
}

fun chunksToLines(chunkFlow: Flow<ByteArray>) = flow {
    val lf = '\n'.code.toByte()
    val cr = '\r'.code.toByte()
    var head = ByteArray(0)
    chunkFlow.collect { chunk ->
        head += chunk
        while (true) {
            val newlineIndex = head.indexOf(lf)
            if (newlineIndex < 0)
                break
            val eolnIndex =
                if (newlineIndex > 0 && head[newlineIndex - 1] == cr)
                    newlineIndex - 1
                else
                    newlineIndex
            emit(value = head.copyOfRange(0, eolnIndex) to true)
            head = head.copyOfRange(newlineIndex + 1, head.size)
        }
    }
    emit(value = head to false)
}

fun chunksToRecords(chunkFlow: Flow<ByteArray>, terminator: Byte) = flow {
    var head = ByteArray(0)
    chunkFlow.collect { chunk ->
        head += chunk
        while (true) {
            val terminatorIndex = head.indexOf(terminator)
            if (terminatorIndex < 0)
                break
            emit(value = head.copyOfRange(0, terminatorIndex) to true)
            head = head.copyOfRange(terminatorIndex + 1, head.size)
        }
    }
    emit(value = head to false)
}

fun stringifyChunks(lineFlow: Flow<Pair<ByteArray, Boolean>>) =
    lineFlow.map { it.first.toString(Charsets.UTF_8) to it.second }
