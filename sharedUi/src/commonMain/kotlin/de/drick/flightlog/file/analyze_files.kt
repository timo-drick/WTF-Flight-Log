package de.drick.flightlog.file

import de.drick.wtf_osd.ParseResult
import de.drick.wtf_osd.parseOsdFile
import kotlinx.collections.immutable.toImmutableSet

suspend fun analyzeFileList(fileList: List<FileItem>): List<LogItem> = fileList
    .groupBy { it.name }
        .map { (name, fileList) ->
            val items = fileList.mapNotNull { fileItem ->
                when (fileItem.extension.lowercase()) {
                    "osd" -> {
                        when(val osd = parseOsdFile(fileItem.source())) {
                            is ParseResult.Success -> OSDFile(fileItem, osd.record.fontVariant)
                            is ParseResult.Error -> ErrorFile(fileItem, osd.type.name)
                        }
                    }
                    "mov", "mp4" -> {
                        VideoFile(fileItem)
                    }
                    else -> null
                }
            }
            LogItem(name, items.toImmutableSet())
        }

    /*val fontFiles = FontVariant.entries.mapNotNull { variant ->
        fileList.find { it.name == variant.fileName() }
            ?.let { fileItem ->
                FontFile(fileItem, variant)
            }
    }*/
