package de.drick.flightlog.file

import de.drick.flightlog.ui.components.toGeoPoint
import de.drick.wtf_osd.ParseResult
import de.drick.wtf_osd.Symbols
import de.drick.wtf_osd.extractFlightData
import de.drick.wtf_osd.extractGps
import de.drick.wtf_osd.parseOsdFile
import de.drick.wtf_osd.parseSrtFile
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

fun List<FileItem>.analyzeFlow() = flow {
    groupBy { it.name }
        .forEach { (name, fileList) ->
            val items = fileList.mapNotNull { fileItem ->
                when (fileItem.extension.lowercase()) {
                    "osd" -> {
                        when(val osd = parseOsdFile(fileItem.source())) {
                            is ParseResult.Success -> {
                                val symbols = Symbols(osd.record)
                                val duration = osd.record.frames.last().millis.milliseconds
                                val gps = extractGps(osd.record)
                                val data = extractFlightData(symbols)
                                val maxSpeed = data.mapNotNull { it.speed }.maxByOrNull { it.value }
                                val maxHeight = data.mapNotNull { it.height }.maxByOrNull { it.value }
                                OSDFile(
                                    file = fileItem,
                                    fontVariant = osd.record.fontVariant,
                                    duration = duration,
                                    hasGpsData = gps.wayPoints.isNotEmpty(),
                                    startPosition = gps.wayPoints.firstOrNull()?.position?.toGeoPoint(),
                                    maxSpeed = maxSpeed,
                                    maxHeight = maxHeight
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
    }.flowOn(Dispatchers.Default)
