package net.pacujo.lip

import java.nio.ByteBuffer

interface Connection {
    suspend fun read(buf: ByteBuffer): Int

    suspend fun write(buf: ByteBuffer): Int

    suspend fun close()
}