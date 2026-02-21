package de.drick.wtf_osd

import androidx.compose.ui.graphics.ImageBitmap


enum class ErrorType {
    Parsing, IO
}
sealed interface ParseResult<T> {
    data class Success<T>(val record: T) : ParseResult<T>
    data class Error<T>(val error: Throwable, val type: ErrorType) : ParseResult<T>
}

data class OsdFont(
    val variant: FontVariant,
    val image: ImageBitmap,
    val width: Int,
    val height: Int
) {
    val characters: Int get() = (image.height / height) * (image.width / width)
}

data class OsdRecord(
    val header: String,
    val version: Int,
    val charWidth: Int,
    val charHeight: Int,
    val fontWidth: Int,
    val fontHeight: Int,
    val fontVariant: FontVariant,
    val xOffset: Int,
    val yOffset: Int,
    val frames: List<MspFrame>
)

class MspFrame(
    val millis: Long,
    val data: ShortArray
)

data class SrtData(
    val frames: List<SrtFrame>
)

data class SrtFrame(
    val index: Int,
    val startTimeMs: Int,
    val endTimeMs: Int,
    val delayMs: Int?,
    val bitrateMbps: Float?,
    val glsBat: String?
)
