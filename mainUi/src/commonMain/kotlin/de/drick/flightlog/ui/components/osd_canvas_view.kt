package de.drick.flightlog.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import de.drick.core.log
import de.drick.wtf_osd.MspFrame
import de.drick.wtf_osd.OsdFont
import de.drick.wtf_osd.OsdRecord
import de.drick.wtf_osd.drawCharacter
import kotlinx.coroutines.isActive
import kotlin.math.max
import kotlin.math.min

@Composable
fun OsdCanvasView(
    osdRecord: OsdRecord,
    osdFont: OsdFont,
    positionProvider: () -> Long,
    modifier: Modifier = Modifier
) {
    var frame: MspFrame? by remember { mutableStateOf(null) }
    LaunchedEffect(osdRecord) {
        val frameIterator = osdRecord.frames.listIterator()
        var currentFrame = frameIterator.next()
        while (isActive) {
            withFrameMillis {
                val videoPositionMillis = positionProvider()
                val currentOsdMillis = currentFrame.millis
                val deltaMillis = currentOsdMillis - videoPositionMillis
                when {
                    deltaMillis < 0 -> {
                        //Seek forward in osd frames
                        while (currentFrame.millis < videoPositionMillis && frameIterator.hasNext()) {
                            val newFrame = frameIterator.next()
                            if (newFrame.millis > videoPositionMillis) {
                                frameIterator.previous()
                                break
                            }
                            currentFrame = newFrame
                        }
                    }
                    deltaMillis > 100 -> {
                        //Seek backward in osd frames
                        while (currentFrame.millis > videoPositionMillis && frameIterator.hasPrevious()) {
                            val newFrame = frameIterator.previous()
                            if (newFrame.millis < videoPositionMillis) {
                                frameIterator.next()
                                break
                            }
                            currentFrame = newFrame
                        }
                    }
                }
                if (currentFrame != frame) {
                    frame = currentFrame
                    //log("Millis video: $videoPositionMillis  current frame: ${currentFrame.millis}")
                }
            }
        }
    }
    Spacer(modifier.drawWithCache {
        val scaleX = size.width / (osdRecord.charWidth * osdFont.width)
        val scaleY = size.height / (osdRecord.charHeight * osdFont.height)
        val scale = min(scaleX, scaleY)
        log("record       size: ${osdRecord.charWidth}x${osdRecord.charHeight}")
        log("record pixel size: ${osdRecord.charWidth*osdFont.width}x${osdRecord.charHeight*osdFont.height}")
        log("canvas       size: ${size.width}x${size.height}")
        log("scale            : $scaleX,$scaleY")
        val osdWidth = (osdFont.width * osdRecord.charWidth).toFloat()
        val osdHeight = (osdFont.height * osdRecord.charHeight).toFloat()
        val leftOffset = max(0f, (size.width - osdWidth * scale) / 2f)
        val topOffset = max(0f, (size.height - osdHeight  * scale) / 2f)
        onDrawBehind {
            translate(left = leftOffset, top = topOffset) {
                scale(scale = scale, pivot = Offset.Zero) {
                    frame?.let { frame ->
                        for (y in 0 until osdRecord.charHeight) {
                            for (x in 0 until osdRecord.charWidth) {
                                val xOffset = x * osdFont.width
                                val yOffset = y * osdFont.height
                                val index = y * osdRecord.charWidth + x
                                if (index >= frame.data.size) {
                                    log("Index out of bounds: $index")
                                } else {
                                    val char = frame.data[index]
                                    drawCharacter(osdFont, char, xOffset, yOffset)
                                }
                            }
                        }
                    }
                }
            }
        }
    })
}
