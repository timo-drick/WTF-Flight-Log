package de.drick.flightlog.file

import de.drick.wtf_osd.FontVariant
import de.drick.compose.tilemap.GeoPoint
import de.drick.wtf_osd.Height
import de.drick.wtf_osd.Speed
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.nameWithoutExtension
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.size
import kotlinx.collections.immutable.ImmutableList
import kotlin.time.Instant
import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration

data class LogItem(
    val name: String,
    val files: ImmutableList<FileItem>
) {
    val lastModified: Instant? = files.mapNotNull { it.lastModified }.maxOrNull()
}

interface FileItem {
    val name: String
    val extension: String
    val size: ByteSize
    val lastModified: Instant?
    suspend fun source(): Source
    fun platformFile(): PlatformFile
}

fun FileItem.fromPlatformFile(file: PlatformFile) = BaseFile(file)

suspend fun PlatformFile.toSource() = Buffer().apply {
    write(readBytes())
}


data class BaseFile(
    val file: PlatformFile
) : FileItem {
    override val name: String
        get() = file.nameWithoutExtension
    override val extension: String
        get() = file.extension
    override val size: ByteSize
        get() = file.size().bytes
    override val lastModified: Instant?
        get() = file.lastModifiedTime()

    override suspend fun source() = file.toSource()
    override fun platformFile() = file
}

data class ErrorFile(
    val file: FileItem,
    val message: String
) : FileItem by file

data class VideoFile(
    val file: FileItem,
    //TODO maybe add duration
) : FileItem by file

@Serializable
@SerialName("OSDSummery")
data class OSDSummeryData(
    val fontVariant: FontVariant,
    val duration: Duration,
    val hasGpsData: Boolean,
    val startPosition: GeoPoint?,
    val aircraftIdentifier: String?,
    val maxSpeed: Speed?,
    val maxHeight: Height?,
    val distanceTotal: Double?,
    val maxDistanceHome: Double?
)

data class OSDFile(
    val file: FileItem,
    val fontVariant: FontVariant,
    val duration: Duration,
    val hasGpsData: Boolean,
    val startPosition: GeoPoint?,
    val aircraftIdentifier: String?,
    val maxSpeed: Speed?,
    val maxHeight: Height?,
    val distanceTotal: Double?,
    val maxDistanceHome: Double?
) : FileItem by file

@Serializable
@SerialName("SrtSummery")
data class SrtSummeryData(
    val duration: Duration
)

data class SRTFile(
    val file: FileItem,
    val duration: Duration
) : FileItem by file

data class FontFile(
    val file: FileItem,
    val fontVariant: FontVariant
) : FileItem by file
