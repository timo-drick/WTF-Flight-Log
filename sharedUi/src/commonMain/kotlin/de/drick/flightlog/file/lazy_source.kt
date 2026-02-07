package de.drick.flightlog.file

import kotlinx.io.Buffer
import kotlinx.io.RawSource

class LazyByteArraySource(
    private val producer: () -> ByteArray
) : RawSource {

    // Backing data, created only on first read
    private var bytes: ByteArray? = null
    private var position: Int = 0
    private var closed: Boolean = false

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        check(!closed) { "Source is closed" }

        // Initialize on first read
        val data = bytes ?: producer().apply {
            bytes = this
            position = 0
        }

        if (position >= data.size) return -1L  // EOF

        val toCopy = minOf(byteCount.toInt(), data.size - position)
        sink.write(data, position, toCopy)
        position += toCopy
        return toCopy.toLong()
    }

    override fun close() {
        closed = true
        // Optionally free array reference
        bytes = null
    }
}