package de.drick.wtf_osd

import kotlinx.io.readString
import kotlinx.datetime.LocalTime
import kotlinx.io.IOException
import kotlinx.io.Source


fun parseSrtFile(src: Source): ParseResult<SrtData> {
    return try {
        val string = src.readString()
        val data = parseSrtData(string).toList()
        if (data.isEmpty()) ParseResult.Error(IllegalStateException(), ErrorType.Parsing)
        else ParseResult.Success(SrtData(data))
    } catch (err: IOException) {
        ParseResult.Error(err, ErrorType.IO)
    } catch (err: Exception) {
        ParseResult.Error(err, ErrorType.Parsing)
    }
}

private val srtBlockRegex =
    """^(\d+${'$'})\n(\d{2}:\d{2}:\d{2},\d{3})\s*-->\s*(\d{2}:\d{2}:\d{2},\d{3})\n(.+)\n"""
        .toRegex(RegexOption.MULTILINE)
/*private val srtBlockRegex =
    """^(\d+${'$'})\n(\d{2}:\d{2}:\d{2},\d{3})\s*-->\s*(\d{2}:\d{2}:\d{2},\d{3})\n.+glsBat:(\S+)\s+delay:(\d+)ms.+bitrate:(\d+.\d)Mbps"""
        .toRegex(RegexOption.MULTILINE)
*/
fun parseSrtData(file: CharSequence): Sequence<SrtFrame> = srtBlockRegex.findAll(file).map { matchResult ->
    val (index, startTime, endTime, secondLine) = matchResult.destructured
    val start = LocalTime.parse(startTime.replace(",", "."))
    val end = LocalTime.parse(endTime.replace(",", "."))
    val delay = """\s+delay:(\d+)ms""".toRegex().find(secondLine)?.groups[1]?.value
    val bitrate = """\s+bitrate:(\d+.\d)Mbps""".toRegex().find(secondLine)?.groups[1]?.value
    val glsBat = """\s+glsBat:(\S+)\s""".toRegex().find(secondLine)?.groups[1]?.value
    SrtFrame(
        index = index.toInt(),
        startTimeMs = start.toMillisecondOfDay(),
        endTimeMs = end.toMillisecondOfDay(),
        delayMs = delay?.toInt(),
        bitrateMbps = bitrate?.toFloat(),
        glsBat = glsBat
    )
}
