package de.drick.flightlog.ui

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import de.drick.flightlog.FlightLogTheme
import de.drick.flightlog.file.ByteSize
import de.drick.flightlog.file.FileItem
import de.drick.flightlog.file.LogItem
import de.drick.flightlog.file.VideoFile
import de.drick.flightlog.file.megabytes
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.io.Source
import kotlin.time.Instant

@Composable
fun BasePreview(content: @Composable () -> Unit) {
    FlightLogTheme {
        Scaffold {
            content()
        }
    }
}

fun mockLogItem(name: String): LogItem {
    val files = persistentSetOf(
        mockVideoFile(name)
    )
    return LogItem(name, files)
}

fun mockBaseFile(
    fileName: String,
    extension: String,
    size: ByteSize = 5.megabytes,
) = object : FileItem {
    override val name
        get() = fileName
    override val extension
        get() = extension
    override val size
        get() = size
    override val lastModified: Instant?
        get() = Instant.fromEpochMilliseconds(1770542159025)
    override suspend fun source(): Source = TODO("Not yet implemented for mock files")
    override fun platformFile(): PlatformFile = TODO("Not yet implemented")
}

fun mockVideoFile(previewFileName: String) = VideoFile(
    file = mockBaseFile(previewFileName, "mov")
)