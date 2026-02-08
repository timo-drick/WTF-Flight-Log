package de.drick.flightlog.file

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.lastModified
import kotlin.time.Instant

actual fun PlatformFile.lastModifiedTime(): Instant? = lastModified()
