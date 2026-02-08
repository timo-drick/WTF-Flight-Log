package de.drick.flightlog.file

import io.github.vinceglb.filekit.PlatformFile
import kotlin.time.Instant

expect fun PlatformFile.lastModifiedTime(): Instant?
