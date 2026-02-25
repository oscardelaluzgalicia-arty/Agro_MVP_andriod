package com.example.agro.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.agro.data.AgroRepository
import com.example.agro.data.ScientificNameResponse
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(repository: AgroRepository, onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<ScientificNameResponse>>(emptyList()) }
    val selectedScientificNames = remember { mutableStateListOf<ScientificNameResponse>() }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Búsqueda Semántica") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        query = ""
                        results = emptyList()
                        selectedScientificNames.clear()
                    }) {
                        Icon(Icons.Default.ClearAll, contentDescription = "Limpiar pantalla")
                    }
                    if (selectedScientificNames.isNotEmpty()) {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                repository.saveScientificNames(selectedScientificNames.toList())
                                snackbarHostState.showSnackbar("Guardado localmente: ${selectedScientificNames.size} nombres")
                                selectedScientificNames.clear()
                            }
                        }) {
                            Icon(Icons.Default.Save, contentDescription = "Guardar selección")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Fruta o verdura") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("maiz") },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (query.isNotBlank()) {
                                isLoading = true
                                coroutineScope.launch {
                                    val result = repository.resolveCommonName(query.trim())
                                    if (result.isSuccess) {
                                        results = result.getOrNull()?.scientificNames ?: emptyList()
                                        if (results.isEmpty()) {
                                            snackbarHostState.showSnackbar("No se encontraron resultados")
                                        }
                                    } else {
                                        snackbarHostState.showSnackbar("Error al buscar")
                                    }
                                    isLoading = false
                                }
                            }
                        },
                        enabled = query.isNotBlank() && !isLoading
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Buscar")
                    }
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(results) { scientific ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = selectedScientificNames.contains(scientific),
                                    onCheckedChange = { isChecked ->
                                        if (isChecked) {
                                            selectedScientificNames.add(scientific)
                                        } else {
                                            selectedScientificNames.remove(scientific)
                                        }
                                    }
                                )
                                Column {
                                    Text(
                                        text = scientific.scientificName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Nombre canónico: ${scientific.canonicalName}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Confianza: ${scientific.confidence}% | Status: ${scientific.status}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
