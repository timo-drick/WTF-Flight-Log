package de.drick.flightlog.ui

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import de.drick.flightlog.FlightLogTheme
import de.drick.flightlog.file.ByteSize
import de.drick.flightlog.file.FileItem
import de.drick.flightlog.file.LogItem
import de.drick.flightlog.file.OSDFile
import de.drick.flightlog.file.SRTFile
import de.drick.flightlog.file.VideoFile
import de.drick.flightlog.file.megabytes
import de.drick.wtf_osd.FontVariant
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.io.Source
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

@Composable
fun BasePreview(content: @Composable () -> Unit) {
    FlightLogTheme {
        Scaffold {
            content()
        }
    }
}

fun mockLogItem(
    name: String,
    variant: FontVariant?
) = mockLogItem(
    name = name,
    osdFile = variant?.let { mockOsdFile(variant) }
)

fun mockLogItem(
    name: String,
    osdFile: OSDFile? = null,
    srtFile: SRTFile? = null
): LogItem {
    val files = mutableSetOf<FileItem>()
    files.add(mockVideoFile("Video"))
    osdFile?.let { files.add(osdFile) }
    srtFile?.let { files.add(srtFile) }
    return LogItem(name, files.toPersistentSet())
}

fun mockBaseFile(
    fileName: String,
    extension: String,
    size: ByteSize = 5.megabytes,
) = object : FileItem {
    override val name = fileName
    override val extension = extension
    override val size = size
    override val lastModified: Instant = Instant.fromEpochMilliseconds(1770542159025)
    override suspend fun source(): Source = TODO("Not yet implemented for mock files")
    override fun platformFile(): PlatformFile = TODO("Not yet implemented")
}

fun mockVideoFile(previewFileName: String) = VideoFile(
    file = mockBaseFile(previewFileName, "mov")
)
fun mockOsdFile(
    font: FontVariant,
    duration: Duration = 400140.milliseconds,
    hasGpsData: Boolean = false
) = OSDFile(
    file = mockBaseFile(font.fileName(), "osd"),
    fontVariant = font,
    duration = duration,
    hasGpsData = hasGpsData
)
fun mockSrtFile(name: String = "srtfile", duration: Duration) = SRTFile(
    file = mockBaseFile(name, "srt"),
    duration = duration
)