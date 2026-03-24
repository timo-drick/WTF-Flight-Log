package de.drick.filehandling

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.list

class FilePickerNonWeb: FilePicker {
    override suspend fun pickDirectory(): List<PlatformFile>? {
        val dir = FileKit.openDirectoryPicker()
        return dir?.let { collectRecursively(it) }
    }

    override suspend fun pickFiles(): List<PlatformFile>? =
        FileKit.openFilePicker(
            type = FileKitType.File(),
            mode = FileKitMode.Multiple()
        )
}

@Composable
actual fun rememberFilePicker(): FilePicker {
    val picker = remember { FilePickerNonWeb() }
    return picker
}


fun collectRecursively(dir: PlatformFile): List<PlatformFile> = dir.list().flatMap {
    if (it.isDirectory()) collectRecursively(it) else listOf(it)
}
