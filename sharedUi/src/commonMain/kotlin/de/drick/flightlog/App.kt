package de.drick.flightlog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.drick.core.log
import de.drick.flightlog.file.BaseFile
import de.drick.flightlog.file.FileItem
import de.drick.flightlog.file.LogItem
import de.drick.flightlog.file.analyzeFileList
import de.drick.flightlog.ui.LogItemListView
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.openFilePicker
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch


fun FileItem.fromPlatformFile(file: PlatformFile) = BaseFile(file)
fun PlatformFile.toFileItem() = BaseFile(this)

@Composable
fun App() {
    val scope = rememberCoroutineScope()

    FlightLogTheme {
        var logList: ImmutableList<LogItem> by remember {
            mutableStateOf(persistentListOf())
        }
        var selectedLogItem by remember { mutableStateOf<LogItem?>(null) }
        Scaffold {
            Column(
                modifier = Modifier
                    .safeDrawingPadding()
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (selectedLogItem != null) {
                    LogItemDetailView(
                        logItem = selectedLogItem!!,
                        onBackClick = { selectedLogItem = null }
                    )
                } else {
                    Button(onClick = {
                        scope.launch {
                            FileKit.openFilePicker(mode = FileKitMode.Multiple())
                                ?.let { selectedFiles ->
                                    val fileItemList = selectedFiles
                                        .map { it.toFileItem() }
                                    log("Analyzing files: $fileItemList")
                                    val result = analyzeFileList(fileItemList)
                                    logList = result.toImmutableList()
                                }
                        }
                    }) {
                        Text("Import files")
                    }
                    AnimatedVisibility(logList.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "Selected files",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            LogItemListView(
                                logList = logList,
                                onLogItemClick = { selectedLogItem = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}