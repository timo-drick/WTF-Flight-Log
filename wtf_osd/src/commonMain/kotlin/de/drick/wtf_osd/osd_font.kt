package de.drick.wtf_osd

import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import flightlog.wtf_osd.generated.resources.Res


/**

msp-osd source: osd_dji_overlay_udp.c

#define FONT_VARIANT_GENERIC 0
#define FONT_VARIANT_BETAFLIGHT 1
#define FONT_VARIANT_INAV 2
#define FONT_VARIANT_ARDUPILOT 3
#define FONT_VARIANT_KISS_ULTRA 4

switch (font_variant)
{
    case FONT_VARIANT_BETAFLIGHT:
        snprintf(name_buf, len, "%s_bf", font_path);
        break;
    case FONT_VARIANT_INAV:
        snprintf(name_buf, len, "%s_inav", font_path);
        break;
    case FONT_VARIANT_ARDUPILOT:
        snprintf(name_buf, len, "%s_ardu", font_path);
        break;
    case FONT_VARIANT_KISS_ULTRA:
        snprintf(name_buf, len, "%s_ultra", font_path);
        break;
    default:
        snprintf(name_buf, len, "%s", font_path);
}
 */

enum class FontVariant(val variant: Int, val suffix: String, val logoOffset: LogoOffset? = null) {
    GENERIC(0, "", LogoOffset(24, 4, 160)),
    BETAFLIGHT(1, "_btfl", LogoOffset(24, 4, 160)),
    INAV(2, "_inav", LogoOffset(10, 4, 259)),
    ARDUPILOT(3, "_ardu", LogoOffset(6, 4, 257)),
    KISS_ULTRA(4, "_ultr"),
    QUIC(5, "_ultr", LogoOffset(24, 4, 300));


    companion object {
        fun fromVariant(variant: Int) = entries[variant.coerceIn(0, entries.size - 1)]
    }
    fun fileName() = "font$suffix.png"

    data class LogoOffset(
        val cols: Int,
        val rows: Int,
        val offset: Int
    )
}

//private const val FONT_WIDTH = 36
//private const val FONT_HEIGHT = 54

suspend fun loadOsdFont(variant: FontVariant) = loadOsdFont(variant, Res.readBytes("files/${variant.fileName()}"))

suspend fun loadOsdFont(variant: FontVariant, byteArray: ByteArray): OsdFont {
    return OsdFont(variant, byteArray.decodeToImageBitmap(), 36, 54)
}

fun OsdFont.offset(c: Short): IntOffset {
    val fontRows = (image.height / height)
    val column = c / fontRows
    val row = c % fontRows
    val xOffset = width * column
    val yOffset = height * row
    return IntOffset(xOffset, yOffset)
}

fun DrawScope.drawCharacter(font: OsdFont, c: Short, x: Int, y: Int) {
    val size = IntSize(font.width, font.height)
    drawImage(
        image = font.image,
        srcOffset = font.offset(c),
        srcSize = size,
        dstOffset = IntOffset(x, y),
        dstSize = size,
        blendMode = BlendMode.SrcOver
    )
}
fun Canvas.drawCharacter(font: OsdFont, c: Short, x: Int, y: Int) {
    val size = IntSize(font.width, font.height)
    drawImageRect(
        image = font.image,
        srcOffset = font.offset(c),
        srcSize = size,
        dstOffset = IntOffset(x, y),
        dstSize = size,
        paint = Paint()
    )
}

fun DrawScope.drawString(font: OsdFont, text: String, xOffset: Int, yOffset: Int) {
    text.forEachIndexed { i, c ->
        val x = i * font.width + xOffset
        val y = yOffset
        drawCharacter(font, c.code.toShort(), x, y)
    }
}

fun OsdFont.extractSample(): ImageBitmap {
    val cols = 10
    val rows = 5
    val offset = 0
    val image = ImageBitmap(cols * width, rows * height)
    val canvas = Canvas(image)
    for (y in 0 until rows) {
        for (x in 0 until cols) {
            val char = x + y * cols + offset
            canvas.drawCharacter(this, char.toShort(), x * width, y * height)
        }
    }
    return image
}

fun OsdFont.extractLogo(): ImageBitmap? =
    variant.logoOffset?.let { extractLogo(it.cols, it.rows, it.offset) }

fun OsdFont.extractLogo(
    cols: Int,
    rows: Int,
    offset: Int
): ImageBitmap {
    val bitmap = ImageBitmap(cols * width, rows * height)
    val canvas = Canvas(bitmap)
    for (y in 0 until rows) {
        for (x in 0 until cols) {
            val char = x + y * cols + offset
            canvas.drawCharacter(this, char.toShort(), x * width, y * height)
        }
    }
    return bitmap
}
