package de.drick.flightlog.file

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.lastModified
import io.github.vinceglb.filekit.path
import kotlin.time.Instant

actual fun PlatformFile.lastModifiedTime(): Instant? = lastModified()
actual fun PlatformFile.path(): String = this.path
