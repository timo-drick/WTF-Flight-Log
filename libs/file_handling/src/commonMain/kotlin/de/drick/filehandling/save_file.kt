package de.drick.filehandling

import androidx.compose.runtime.Composable

interface FileSaver {
    fun saveToFile(byteArray: ByteArray, name: String)
}

@Composable
expect fun rememberFileSaver(): FileSaver
