package de.drick.wtf_osd

import de.drick.core.log
import kotlinx.io.IOException
import kotlinx.io.Source
import kotlinx.io.readByteArray
import kotlinx.io.readIntLe
import kotlinx.io.readShortLe
import kotlin.math.roundToLong

const val MSP_OSD_VERSION = "0.12.4" // msp-osd
const val O3_OSD_VERSION = "2.1.1"   // o3-multipage-osd

private const val REC_MAGIC = "MSPOSD"

enum class ErrorType {
    Parsing, IO
}
sealed interface ParseResult {
    data class Success(val record: OsdRecord) : ParseResult
    data class Error(val error: Throwable, val type: ErrorType) : ParseResult
}

/**
 * typedef struct rec_file_header_t
 * {
 *     char magic[7];
 *     uint16_t version;
 *     rec_config_t config;
 * } __attribute__((packed)) rec_file_header_t;
 */
fun parseOsdFile(src: Source): ParseResult {
    try {
        // Read the first 6 bytes
        val headerBytes = src.peek().readByteArray(7)
        val header = headerBytes.toText()
        log("Header: $header")
        val osdRecord = if (header == REC_MAGIC) {
            parseOldMspOsdFile(src)
        } else {
            log("O3 file header detected")
            parseO3OsdFile(src)
        }
        return ParseResult.Success(osdRecord)
    } catch (err: IOException) {
        return ParseResult.Error(err, ErrorType.IO)
    } catch (err: Exception) {
        return ParseResult.Error(err, ErrorType.Parsing)
    }
}

//fun ByteArray.toText() = this.map { Char(it.toInt()) }.joinToString("")
fun ByteArray.toText() = buildString {
    for (byte in this@toText) {
        if (byte == 0.toByte()) break
        append(Char(byte.toInt()))
    }
}

private fun fontVariant(shortName: String) = when(shortName) {
    "BTFL" -> FontVariant.BETAFLIGHT
    "ARDU" -> FontVariant.ARDUPILOT
    "INAV" -> FontVariant.INAV
    "ULTR" -> FontVariant.KISS_ULTRA
    "QUIC" -> FontVariant.QUIC
    else -> FontVariant.GENERIC
}

private fun parseO3OsdFile(src: Source): OsdRecord {
    // Read the first 40 bytes
    val header = src.readByteArray(40)
    val firmwarePart = header.sliceArray(0 until 4).toText()
    val fontVariant = fontVariant(firmwarePart)
    println("Firmware part: $firmwarePart")
    val signature = header.sliceArray(36 until 40).toText()
    println("Signature: $signature")

    val numCols: Int
    val numRows: Int

    if (signature == "DJ03") {
        numCols = 53
        numRows = 20
    } else {
        numCols = header[36].toInt()
        numRows = header[38].toInt()
    }

    val frameSize = numCols * numRows
    val frameSizeByte = 4 + frameSize * 2
    println("Cols: $numCols Rows: $numRows")
    val frames = mutableListOf<MspFrame>()
    while (src.request(frameSizeByte.toLong())) {
        val timeMs = src.readIntLe()
        //byteBuffer.asShortBuffer().get(frameData)
        val frameData = ShortArray(frameSize)
        for (i in 0 until frameSize) {
            frameData[i] = src.readShortLe()
        }
        frames.add(MspFrame(timeMs.toLong(), frameData))
    }

    return OsdRecord(
        header = "",
        version = 99,
        charWidth = numCols,
        charHeight = numRows,
        fontWidth = 1,
        fontHeight = 1,
        fontVariant = fontVariant,
        xOffset = 0,
        yOffset = 0,
        frames = frames
    )
}

private fun parseOldMspOsdFile(src: Source) : OsdRecord {
    //Read header
    val header = src.readByteArray(7).toText()
    val version = src.readShortLe().toUShort()
    val charWidth = src.readByte().toInt()
    val charHeight = src.readByte().toInt()
    val fontWidth = src.readByte().toInt()
    val fontHeight = src.readByte().toInt()
    val xOffset = src.readShortLe().toUShort().toInt()
    val yOffset = src.readShortLe().toUShort().toInt()

    val fontVariant = when (version.toInt()) {
        1 -> when (src.readByte().toInt()) {
            0 -> FontVariant.GENERIC
            1 -> FontVariant.BETAFLIGHT
            2 -> FontVariant.INAV
            3 -> FontVariant.ARDUPILOT
            4 -> FontVariant.KISS_ULTRA
            else -> FontVariant.GENERIC
        }
        2 -> {
            val variant = src.readByteArray(5).toText()
            log("Font variant: $variant")
            fontVariant(variant)
        }
        else -> {
            log("Unsupported version: $version")
            throw IllegalStateException("Unsupported version! $version")
        }
    }

    log("file header : $header")
    log("file version: $version")
    log("char width  : $charWidth")
    log("char height : $charHeight")
    log("font width  : $fontWidth")
    log("font height : $fontHeight")
    log("x offset    : $xOffset")
    log("y offset    : $yOffset")
    log("font variant: $fontVariant")
    val frames = mutableListOf<MspFrame>()
    var isFirstFrame = true
    while (src.request(8)) {
        var frameIdx = src.readIntLe()
        if (isFirstFrame && frameIdx > 100) frameIdx = 1
        isFirstFrame = false
        val frameSize = src.readIntLe()
        // Check frame size
        val maxY = charHeight - 1
        val maxX = charWidth - 1
        val expectedSize = maxY + maxX * 22
        if (frameSize < expectedSize) {
            log("Frame size: $frameSize expected: $expectedSize", Throwable())
            break
        }
        val frameByteSize = frameSize * 2
        if (src.request(frameByteSize.toLong())) {
            val rawData = ShortArray(frameSize)
            for (i in 0 until frameSize) {
                rawData[i] = src.readShortLe()
            }
            val millis = (frameIdx.toFloat() * (1000f/60f)).roundToLong()
            // Rearrange frame data
            val frameData = ShortArray(charWidth * charHeight)
            for (y in 0 until charHeight) {
                for (x in 0 until charWidth) {
                    val indexOld = y + x * 22
                    val indexNew = y * charWidth + x
                    frameData[indexNew] = rawData[indexOld]
                }
            }
            frames.add(MspFrame(millis, frameData))
        }
    }
    println("Frames: ${frames.size}")
    return OsdRecord(
        header = header,
        version = version.toInt(),
        charWidth = charWidth,
        charHeight = charHeight,
        fontWidth = fontWidth,
        fontHeight = fontHeight,
        fontVariant = fontVariant,
        xOffset = xOffset,
        yOffset = yOffset,
        frames = frames
    )
}