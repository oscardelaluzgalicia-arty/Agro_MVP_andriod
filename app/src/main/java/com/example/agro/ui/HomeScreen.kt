package com.example.agro.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel, onNavigateToMap: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val results by viewModel.results.collectAsState()
    val showDialog by viewModel.showDialog.collectAsState()
    val importResult by viewModel.importResult.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Búsqueda Semántica", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        // Búsqueda
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = viewModel.commonNameSearch,
                onValueChange = { viewModel.commonNameSearch = it },
                label = { Text("Nombre común (ej: uva)") },
                modifier = Modifier.weight(1.0f),
                enabled = uiState !is HomeUiState.Loading
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { viewModel.search() },
                enabled = uiState !is HomeUiState.Loading
            ) {
                Text("Buscar")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selector de Estado
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            TextField(
                value = viewModel.selectedState,
                onValueChange = {},
                readOnly = true,
                label = { Text("Estado") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                viewModel.statesOfMexico.forEach { state ->
                    DropdownMenuItem(
                        text = { Text(state) },
                        onClick = {
                            viewModel.selectedState = state
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState is HomeUiState.Loading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        // Resultados
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(results) { scientificName ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = scientificName.scientificName, style = MaterialTheme.typography.titleMedium)
                            Text(text = "Rango: ${scientificName.rank}", style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = { viewModel.import(scientificName.scientificName) }) {
                            Text("Importar")
                        }
                    }
                }
            }
        }

        if (uiState is HomeUiState.Error) {
            Text(
                text = (uiState as HomeUiState.Error).message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }

    // Ventana Emergente (Diálogo)
    if (showDialog && importResult != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialog() },
            title = { Text(text = "Resultado de Importación") },
            text = {
                val zones = importResult?.ecologicalZonesImport
                Column {
                    Text(text = "Consulta: ${importResult?.query}")
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (zones != null) {
                        if (zones.occurrencesInserted > 0) {
                            Text(text = "¡Éxito! Se insertaron ${zones.occurrencesInserted} nuevas ocurrencias.")
                        } else if (zones.occurrencesDuplicated > 0) {
                            Text(text = "Información ya existente. Se detectaron ${zones.occurrencesDuplicated} ocurrencias duplicadas.")
                        } else {
                            Text(text = "No se encontraron nuevas ocurrencias para esta región.")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { 
                    viewModel.dismissDialog()
                    onNavigateToMap() 
                }) {
                    Text("Ir al Mapa")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDialog() }) {
                    Text("OK")
                }
            }
        )
    }
}
