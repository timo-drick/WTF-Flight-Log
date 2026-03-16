package de.drick.filehandling

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.download
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class FileSaverImpl(
    private val scope: CoroutineScope
): FileSaver {
    override fun saveToFile(byteArray: ByteArray, name: String) {
        scope.launch {
            FileKit.download(byteArray, name)
        }
    }
}

@Composable
actual fun rememberFileSaver(): FileSaver = FileSaverImpl(rememberCoroutineScope())