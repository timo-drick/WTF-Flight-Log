package de.drick.wtf_osd

import androidx.compose.ui.graphics.ImageBitmap

data class OsdFont(
    val variant: FontVariant,
    val image: ImageBitmap,
    val width: Int,
    val height: Int
) {
    val characters: Int get() = (image.height / height) * (image.width / width)
}

data class OsdData(
    val font: OsdFont,
    val record: OsdRecord,
    val srtFrames: List<SrtFrame>
)

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

data class SrtFrame(
    val index: Int,
    val startTimeMs: Int,
    val endTimeMs: Int,
    val delayMs: Int,
    val bitrateMbps: Float
)
