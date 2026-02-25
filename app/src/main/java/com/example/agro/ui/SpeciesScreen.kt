package com.example.agro.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agro.data.AgroRepository
import com.example.agro.data.SpeciesEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeciesScreen(repository: AgroRepository, onNavigateToSearch: () -> Unit) {
    var speciesList by remember { mutableStateOf<List<SpeciesEntity>>(emptyList()) }
    val selectedSpecies = remember { mutableStateListOf<SpeciesEntity>() }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        isLoading = true
        val result = repository.fetchAllSpecies()
        if (result.isSuccess) {
            speciesList = result.getOrDefault(emptyList())
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Explorar Especies") },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Buscar semánticamente")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (selectedSpecies.isNotEmpty()) {
                FloatingActionButton(onClick = {
                    coroutineScope.launch {
                        repository.saveSpeciesLocally(selectedSpecies.toList())
                        snackbarHostState.showSnackbar("Guardado localmente: ${selectedSpecies.size} especies")
                        selectedSpecies.clear()
                    }
                }) {
                    Icon(Icons.Default.Save, contentDescription = "Guardar selección")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            Text(
                "Selecciona las especies que deseas guardar localmente",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(speciesList) { species ->
                        SpeciesCard(
                            species = species,
                            isSelected = selectedSpecies.contains(species),
                            onSelectionChange = { isSelected ->
                                if (isSelected) {
                                    selectedSpecies.add(species)
                                } else {
                                    selectedSpecies.remove(species)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SpeciesCard(
    species: SpeciesEntity,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = species.scientificName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${species.family} | ${species.genus}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                        Text(species.taxonomicStatus, fontSize = 10.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ID: ${species.idSpecies}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelectionChange
            )
        }
    }
}
