package de.drick.flightlog.file

import de.drick.core.log
import de.drick.wtf_osd.ParseResult
import de.drick.wtf_osd.extractGps
import de.drick.wtf_osd.parseOsdFile
import de.drick.wtf_osd.parseSrtFile
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

fun List<FileItem>.analyzeFlow() = flow {
    groupBy { it.name }
        .forEach { (name, fileList) ->
            val items = fileList.mapNotNull { fileItem ->
                when (fileItem.extension.lowercase()) {
                    "osd" -> {
                        val osd = withContext(Dispatchers.Default) {
                            parseOsdFile(fileItem.source())
                        }
                        when(osd) {
                            is ParseResult.Success -> {
                                val duration = osd.record.frames.last().millis.milliseconds
                                val gps = extractGps(osd.record)
                                OSDFile(
                                    file = fileItem,
                                    fontVariant = osd.record.fontVariant,
                                    duration = duration,
                                    hasGpsData = gps.wayPoints.isNotEmpty()
                                )
                            }
                            is ParseResult.Error -> ErrorFile(fileItem, osd.type.name)
                        }
                    }
                    "srt" -> {
                        val srt = withContext(Dispatchers.Default) {
                            parseSrtFile(fileItem.source())
                        }
                        when(srt) {
                            is ParseResult.Success -> {
                                val duration = srt.record.frames.last().endTimeMs.milliseconds
                                SRTFile(
                                    file = fileItem,
                                    duration = duration
                                )
                            }
                            is ParseResult.Error -> ErrorFile(fileItem, srt.type.name)
                        }
                    }
                    "mov", "mp4" -> {
                        VideoFile(fileItem)
                    }
                    else -> null
                }
            }
            emit(LogItem(name, items.toImmutableSet()))
        }
    }
