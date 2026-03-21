package de.drick.flightlog.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.drick.flightlog.FlightLogTheme
import de.drick.flightlog.file.ByteSize
import de.drick.flightlog.file.FileItem
import de.drick.flightlog.file.LogItem
import de.drick.flightlog.file.OSDFile
import de.drick.flightlog.file.SRTFile
import de.drick.flightlog.file.VideoFile
import de.drick.flightlog.file.megabytes
import de.drick.wtf_osd.FontVariant
import de.drick.compose.tilemap.GeoPoint
import de.drick.flightlog.localStorage.AircraftIdentifier
import de.drick.wtf_osd.Height
import de.drick.wtf_osd.Speed
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.collections.immutable.toPersistentList
import kotlinx.io.Source
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

@Composable
fun BasePreview(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    FlightLogTheme {
        Scaffold(modifier) {
            content()
        }
    }
}

fun mockFlightLogState(
    isWorking: Boolean = false,
    vararg logItem: LogItem = arrayOf(
        mockLogItem("Test entry 1", FontVariant.BETAFLIGHT),
        mockLogItem("Test entry 2", FontVariant.ARDUPILOT),
        mockLogItem("Test entry 3", FontVariant.INAV),
        mockLogItem("Test entry 4", FontVariant.GENERIC)
    )
) = object : FlightLogState {
    override val isWorking = isWorking
    override val lazyListState = LazyListState()
    override val list = logItem.toList()
    override val groups = list.group()
    override val aircraftIdentifierList = emptyList<AircraftIdentifier>()
    override fun importFiles(files: List<PlatformFile>) { /* only mock */ }
    override fun rescanLogItems() { /* only mock */ }
    override fun addAircraft(aircraftIdentifier: AircraftIdentifier) { /* only mock */ }
    override fun removeAircraft(aircraftIdentifier: AircraftIdentifier) { /* only mock */ }
}

fun mockLogItem(name: String, vararg file: FileItem) = LogItem(name, file.toPersistentList())

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
    return LogItem(name, files.toPersistentList())
}

private const val MOCK_TIME_STAMP = 1770542159025

fun mockBaseFile(
    fileName: String,
    extension: String,
    size: ByteSize = 5.megabytes,
) = object : FileItem {
    override val name = fileName
    override val extension = extension
    override val size = size
    override val lastModified: Instant = Instant.fromEpochMilliseconds(MOCK_TIME_STAMP)
    override suspend fun source(): Source = TODO("Not yet implemented for mock files")
    override fun platformFile(): PlatformFile = TODO("Not yet implemented")
}

fun mockVideoFile(previewFileName: String) = VideoFile(
    file = mockBaseFile(previewFileName, "mov")
)
fun mockOsdFile(
    font: FontVariant,
    duration: Duration = 400140.milliseconds,
    hasGpsData: Boolean = false,
    startPosition: GeoPoint? = null,
    aircraftIdentifier: String? = null,
    maxSpeed: Speed? = null,
    maxHeight: Height? = null,
    distanceTotal: Double? = null,
    maxDistanceHome: Double? = null
) = OSDFile(
    file = mockBaseFile(font.fileName(), "osd"),
    fontVariant = font,
    duration = duration,
    hasGpsData = hasGpsData,
    startPosition = startPosition,
    aircraftIdentifier = aircraftIdentifier,
    maxSpeed = maxSpeed,
    maxHeight = maxHeight,
    distanceTotal = distanceTotal,
    maxDistanceHome = maxDistanceHome
)
fun mockSrtFile(name: String = "srtfile", duration: Duration) = SRTFile(
    file = mockBaseFile(name, "srt"),
    duration = duration
)
