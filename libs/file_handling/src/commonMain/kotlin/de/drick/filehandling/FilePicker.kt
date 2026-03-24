package de.drick.filehandling

import androidx.compose.runtime.Composable
import io.github.vinceglb.filekit.PlatformFile

interface FilePicker {
    suspend fun pickDirectory(): List<PlatformFile>?
    suspend fun pickFiles(): List<PlatformFile>?
}

@Composable
expect fun rememberFilePicker(): FilePicker
