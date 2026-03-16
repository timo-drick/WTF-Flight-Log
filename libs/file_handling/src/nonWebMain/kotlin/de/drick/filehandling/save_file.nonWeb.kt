package de.drick.filehandling

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.compose.SaverResultLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.launch

class FileSaverImpl: FileSaver {
    private val settings = FileKitDialogSettings(title = "Test title")
    private var launcher: SaverResultLauncher? = null
    private var data: ByteArray? = null

    @Composable
    fun init() {
        val scope = rememberCoroutineScope()
        launcher = rememberFileSaverLauncher(settings) {
            it?.let { file ->
                scope.launch {
                    file.write(requireNotNull(data))
                }
            }
        }
    }
    override fun saveToFile(byteArray: ByteArray, name: String) {
        data = byteArray
        requireNotNull(launcher).launch(name)
    }
}

@Composable
actual fun rememberFileSaver(): FileSaver {
    val fileSaver =  remember { FileSaverImpl() }
    fileSaver.init()
    return fileSaver
}