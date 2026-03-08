package de.drick.flightlog.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.drick.flightlog.localStorage.AircraftIdentifier
import de.drick.flightlog.ui.icons.MaterialIconsClose

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AircraftIdentifierPane(
    state: FlightLogState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val aircraftList = state.aircraftIdentifierList
    var newAircraftName by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Aircraft Identifiers") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(MaterialIconsClose, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newAircraftName,
                    onValueChange = { newAircraftName = it },
                    label = { Text("New Aircraft Name") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Button(
                    onClick = {
                        if (newAircraftName.isNotBlank()) {
                            val newAircraft = AircraftIdentifier(newAircraftName.trim())
                            state.addAircraft(newAircraft)
                            newAircraftName = ""
                        }
                    },
                    enabled = newAircraftName.isNotBlank()
                ) {
                    Text("Add")
                }
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(aircraftList) { aircraft ->
                    AircraftListItem(
                        aircraft = aircraft,
                        onDelete = {
                            state.removeAircraft(aircraft)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AircraftListItem(
    aircraft: AircraftIdentifier,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = aircraft.name,
                style = MaterialTheme.typography.bodyLarge
            )
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = MaterialIconsClose,
                    contentDescription = "Delete aircraft",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
