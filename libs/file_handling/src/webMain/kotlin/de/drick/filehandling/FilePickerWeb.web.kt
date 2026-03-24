package de.drick.filehandling

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType

class FilePickerWeb: FilePicker {

    override suspend fun pickDirectory(): List<PlatformFile>? {
        val chromePicker = DirectoryPickerChrome()
        return if (isShowDirectoryPickerSupported()) {
            chromePicker.pickDirectory()?.let { dir ->
                readDirectoryRecursively(dir)
            }
        } else {
            openFilePicker(
                type = FileKitType.File(),
                multipleMode = true,
                directoryMode = true
            )
        }
    }

    override suspend fun pickFiles(): List<PlatformFile>? {
        val result = openFilePicker(
            type = FileKitType.File(),
            multipleMode = true,
            directoryMode = false
        )
        return result
    }

}

@Composable
actual fun rememberFilePicker(): FilePicker {
    val picker = remember { FilePickerWeb() }
    return picker
}

