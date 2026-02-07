package de.drick.wtf_osd_player.osd

import de.drick.wtf_osd.SrtFrame
import kotlinx.datetime.LocalTime

private val srtBlockRegex = """^(\d+${'$'})\n(\d{2}:\d{2}:\d{2},\d{3})\s*-->\s*(\d{2}:\d{2}:\d{2},\d{3})\n.+delay:(\d+)ms.+bitrate:(\d+.\d)Mbps""".toRegex(RegexOption.MULTILINE)

fun parseSrtData(file: CharSequence): Sequence<SrtFrame> = srtBlockRegex.findAll(file).map { matchResult ->
    val (index, startTime, endTime, delay, bitrate) = matchResult.destructured
    val start = LocalTime.parse(startTime.replace(",", "."))
    val end = LocalTime.parse(endTime.replace(",", "."))
    SrtFrame(
        index = index.toInt(),
        startTimeMs = start.toMillisecondOfDay(),
        endTimeMs = end.toMillisecondOfDay(),
        delayMs = delay.toInt(),
        bitrateMbps = bitrate.toFloat()
    )
}
