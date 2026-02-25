package com.example.agro.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.agro.data.AgroRepository
import com.example.agro.data.ImportResponse
import com.example.agro.data.ScientificNameResponse
import com.example.agro.data.SpeciesEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(repository: AgroRepository, onNavigateToMap: () -> Unit) {
    var speciesList by remember { mutableStateOf<List<SpeciesEntity>>(emptyList()) }
    var savedScientificNames by remember { mutableStateOf<List<ScientificNameResponse>>(emptyList()) }
    var referenceStates by remember { mutableStateOf<List<String>>(emptyList()) }
    
    var selectedReferenceState by remember { mutableStateOf<String?>(null) }
    var selectedSpecies by remember { mutableStateOf<SpeciesEntity?>(null) }
    var selectedScientificName by remember { mutableStateOf<ScientificNameResponse?>(null) }
    var selectedStateForQuery by remember { mutableStateOf<String?>(null) }
    
    var expandedRefStates by remember { mutableStateOf(false) }
    var expandedSpecies by remember { mutableStateOf(false) }
    var expandedScientific by remember { mutableStateOf(false) }
    var expandedQueryStates by remember { mutableStateOf(false) }
    
    var isLoading by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importResult by remember { mutableStateOf<ImportResponse?>(null) }
    var occurrencesCount by remember { mutableStateOf(0) }
    
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val statesOfMexico = listOf(
        "Aguascalientes", "Baja California", "Baja California Sur", "Campeche", "Chiapas",
        "Chihuahua", "Ciudad de México", "Coahuila", "Colima", "Durango", "Estado de México",
        "Guanajuato", "Guerrero", "Hidalgo", "Jalisco", "Michoacán", "Morelos", "Nayarit",
        "Nuevo León", "Oaxaca", "Puebla", "Querétaro", "Quintana Roo", "San Luis Potosí",
        "Sinaloa", "Sonora", "Tabasco", "Tamaulipas", "Tlaxcala", "Veracruz", "Yucatán", "Zacatecas"
    )

    LaunchedEffect(Unit) {
        speciesList = repository.getSavedSpecies()
        savedScientificNames = repository.getSavedScientificNames()
        referenceStates = repository.getDistinctStates()
    }

    // Filtrar listas basadas en el estado de referencia seleccionado
    val filteredSpecies = remember(selectedReferenceState, speciesList) {
        if (selectedReferenceState == null) speciesList 
        else {
            // Aquí idealmente filtraríamos por especies que existen en ese estado
            // Por ahora mostramos la lista completa, pero podrías añadir lógica de filtrado por ID
            speciesList 
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text(
                "Búsqueda de Ocurrencias",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // FILTRO DE REFERENCIA (NUEVO)
            Text("Filtrar especies por estado (Referencia)", style = MaterialTheme.typography.titleSmall)
            ExposedDropdownMenuBox(
                expanded = expandedRefStates,
                onExpandedChange = { expandedRefStates = !expandedRefStates }
            ) {
                OutlinedTextField(
                    value = selectedReferenceState ?: "Todos los estados con registros...",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        if (selectedReferenceState != null) {
                            IconButton(onClick = { selectedReferenceState = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar")
                            }
                        } else {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRefStates)
                        }
                    },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedRefStates,
                    onDismissRequest = { expandedRefStates = false }
                ) {
                    referenceStates.forEach { state ->
                        DropdownMenuItem(
                            text = { Text(state) },
                            onClick = {
                                selectedReferenceState = state
                                expandedRefStates = false
                            }
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Selector 1: Especies desde tabla 'species' (Catálogo)
            if (selectedScientificName == null) {
                Text("Especies desde catálogo", style = MaterialTheme.typography.titleSmall)
                ExposedDropdownMenuBox(
                    expanded = expandedSpecies,
                    onExpandedChange = { expandedSpecies = !expandedSpecies }
                ) {
                    OutlinedTextField(
                        value = selectedSpecies?.scientificName ?: "Seleccionar de catálogo...",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            if (selectedSpecies != null) {
                                IconButton(onClick = { selectedSpecies = null; selectedStateForQuery = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Limpiar")
                                }
                            } else {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSpecies)
                            }
                        },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedSpecies,
                        onDismissRequest = { expandedSpecies = false }
                    ) {
                        filteredSpecies.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.scientificName) },
                                onClick = {
                                    selectedSpecies = item
                                    selectedScientificName = null
                                    expandedSpecies = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Selector 2: Nombres desde 'searchscreen'
            if (selectedSpecies == null) {
                Text("Nombres desde búsqueda semántica", style = MaterialTheme.typography.titleSmall)
                ExposedDropdownMenuBox(
                    expanded = expandedScientific,
                    onExpandedChange = { expandedScientific = !expandedScientific }
                ) {
                    OutlinedTextField(
                        value = selectedScientificName?.scientificName ?: "Seleccionar de búsqueda...",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            if (selectedScientificName != null) {
                                IconButton(onClick = { selectedScientificName = null; selectedStateForQuery = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Limpiar")
                                }
                            } else {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedScientific)
                            }
                        },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedScientific,
                        onDismissRequest = { expandedScientific = false }
                    ) {
                        savedScientificNames.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.scientificName) },
                                onClick = {
                                    selectedScientificName = item
                                    selectedSpecies = null
                                    expandedScientific = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Selector de Estado para la Consulta
            if (selectedSpecies != null || selectedScientificName != null) {
                Text("Seleccionar Estado para búsqueda", style = MaterialTheme.typography.titleSmall)
                ExposedDropdownMenuBox(
                    expanded = expandedQueryStates,
                    onExpandedChange = { expandedQueryStates = !expandedQueryStates }
                ) {
                    OutlinedTextField(
                        value = selectedStateForQuery ?: "Todos los estados (Opcional)",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedQueryStates) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedQueryStates,
                        onDismissRequest = { expandedQueryStates = false }
                    ) {
                        statesOfMexico.forEach { state ->
                            DropdownMenuItem(
                                text = { Text(state) },
                                onClick = {
                                    selectedStateForQuery = state
                                    expandedQueryStates = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            Button(
                onClick = {
                    val scientificName = selectedSpecies?.scientificName ?: selectedScientificName?.scientificName
                    val state = selectedStateForQuery ?: ""
                    if (scientificName != null) {
                        isLoading = true
                        coroutineScope.launch {
                            val result = repository.import(scientificName, state, scientificName)
                            if (result.isSuccess) {
                                importResult = result.getOrNull()
                                showImportDialog = true
                            } else {
                                snackbarHostState.showSnackbar("Error al importar datos")
                            }
                            isLoading = false
                        }
                    }
                },
                enabled = (selectedSpecies != null || selectedScientificName != null) && !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buscar")
                }
            }
        }

        if (showImportDialog && importResult != null) {
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                title = { Text("Resultados de Importación") },
                text = {
                    Column {
                        val displayName = selectedSpecies?.scientificName ?: selectedScientificName?.scientificName ?: ""
                        Text("Especie: $displayName")
                        Text("Ocurrencias nuevas: ${importResult!!.ecologicalZonesImport.occurrencesInserted}")
                        Text("Ocurrencias duplicadas: ${importResult!!.ecologicalZonesImport.occurrencesDuplicated}")
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showImportDialog = false
                            val idSpecies = importResult!!.speciesImport.idSpecies
                            val totalOccurrences = importResult!!.ecologicalZonesImport.occurrencesInserted + 
                                                 importResult!!.ecologicalZonesImport.occurrencesDuplicated
                            
                            if (totalOccurrences > 0) {
                                isLoading = true
                                occurrencesCount = totalOccurrences
                                coroutineScope.launch {
                                    val result = repository.fetchOccurrences(idSpecies, selectedStateForQuery)
                                    if (result.isSuccess) {
                                        onNavigateToMap()
                                    } else {
                                        snackbarHostState.showSnackbar("Error al cargar ocurrencias")
                                    }
                                    isLoading = false
                                }
                            }
                        }
                    ) {
                        Text("Visualizar en Mapa")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportDialog = false }) {
                        Text("Cerrar")
                    }
                }
            )
        }

        if (isLoading && occurrencesCount > 0) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Cargando $occurrencesCount ocurrencias...")
                    }
                }
            }
        }
    }
}
