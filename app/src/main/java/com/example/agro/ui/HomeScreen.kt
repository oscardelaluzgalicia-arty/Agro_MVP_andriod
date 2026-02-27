package com.example.agro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agro.data.AgroRepository
import com.example.agro.data.ImportResponse
import com.example.agro.data.ScientificNameResponse
import com.example.agro.data.SpeciesEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(repository: AgroRepository, onNavigateToMap: () -> Unit) {
    // --- LÓGICA DE ESTADOS ---
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

    // Colores Profesionales (Fijos para evitar errores de modo oscuro)
    val primaryGreen = Color(0xFF2E7D32)
    val secondaryGreen = Color(0xFFE8F5E9)
    val fixedDarkText = Color(0xFF1C1B1F)
    val fixedGrayText = Color(0xFF49454F)

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

    val filteredSpecies = remember(selectedReferenceState, speciesList) {
        if (selectedReferenceState == null) speciesList else speciesList
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(secondaryGreen, Color.White)))
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Cabecera
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Agriculture, contentDescription = null, tint = primaryGreen, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Panel de Ocurrencias",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = primaryGreen
                    )
                }
                Text("Configura los parámetros de búsqueda", color = fixedGrayText, style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(24.dp))

                // TARJETA DE FILTROS
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {

                        SectionHeader(title = "1. Filtro de Referencia", icon = Icons.Default.FilterList, color = primaryGreen)
                        DropdownSelector(
                            label = "Estado con registros",
                            value = selectedReferenceState ?: "Todos los estados...",
                            expanded = expandedRefStates,
                            onExpandedChange = { expandedRefStates = it },
                            onClear = { selectedReferenceState = null },
                            isSet = selectedReferenceState != null
                        ) {
                            referenceStates.forEach { state ->
                                DropdownMenuItem(
                                    text = { Text(state, color = fixedDarkText) },
                                    onClick = { selectedReferenceState = state; expandedRefStates = false }
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp), thickness = 0.5.dp, color = Color.LightGray)

                        SectionHeader(title = "2. Selección de Especie", icon = Icons.Default.Spa, color = primaryGreen)

                        if (selectedScientificName == null) {
                            DropdownSelector(
                                label = "Desde Catálogo",
                                value = selectedSpecies?.scientificName ?: "Seleccionar especie...",
                                expanded = expandedSpecies,
                                onExpandedChange = { expandedSpecies = it },
                                onClear = { selectedSpecies = null; selectedStateForQuery = null },
                                isSet = selectedSpecies != null
                            ) {
                                filteredSpecies.forEach { item ->
                                    DropdownMenuItem(
                                        text = { Text(item.scientificName, color = fixedDarkText) },
                                        onClick = { selectedSpecies = item; selectedScientificName = null; expandedSpecies = false }
                                    )
                                }
                            }
                        }

                        if (selectedSpecies == null && selectedScientificName == null) {
                            Text("— O —", modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), textAlign = TextAlign.Center, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                        }

                        if (selectedSpecies == null) {
                            DropdownSelector(
                                label = "Desde Búsqueda Semántica",
                                value = selectedScientificName?.scientificName ?: "Seleccionar nombre...",
                                expanded = expandedScientific,
                                onExpandedChange = { expandedScientific = it },
                                onClear = { selectedScientificName = null; selectedStateForQuery = null },
                                isSet = selectedScientificName != null
                            ) {
                                savedScientificNames.forEach { item ->
                                    DropdownMenuItem(
                                        text = { Text(item.scientificName, color = fixedDarkText) },
                                        onClick = { selectedScientificName = item; selectedSpecies = null; expandedScientific = false }
                                    )
                                }
                            }
                        }

                        if (selectedSpecies != null || selectedScientificName != null) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp), thickness = 0.5.dp, color = Color.LightGray)
                            SectionHeader(title = "3. Ubicación de Búsqueda", icon = Icons.Default.Place, color = primaryGreen)
                            DropdownSelector(
                                label = "Estado para consulta",
                                value = selectedStateForQuery ?: "Todos los estados (Opcional)",
                                expanded = expandedQueryStates,
                                onExpandedChange = { expandedQueryStates = it },
                                onClear = null,
                                isSet = false
                            ) {
                                statesOfMexico.forEach { state ->
                                    DropdownMenuItem(
                                        text = { Text(state, color = fixedDarkText) },
                                        onClick = { selectedStateForQuery = state; expandedQueryStates = false }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

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
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryGreen, disabledContainerColor = Color.LightGray)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("BUSCAR OCURRENCIAS", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }

            // DIÁLOGOS
            if (showImportDialog && importResult != null) {
                AlertDialog(
                    onDismissRequest = { showImportDialog = false },
                    shape = RoundedCornerShape(28.dp),
                    containerColor = Color.White,
                    title = { Text("Resumen", color = primaryGreen, fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            val displayName = selectedSpecies?.scientificName ?: selectedScientificName?.scientificName ?: ""
                            Text("Especie: $displayName", color = fixedDarkText, fontWeight = FontWeight.Bold)
                            Text("Nuevas: ${importResult!!.ecologicalZonesImport.occurrencesInserted}", color = fixedDarkText)
                            Text("Duplicadas: ${importResult!!.ecologicalZonesImport.occurrencesDuplicated}", color = fixedDarkText)
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            showImportDialog = false
                            val idSpecies = importResult!!.speciesImport.idSpecies
                            val total = importResult!!.ecologicalZonesImport.occurrencesInserted + importResult!!.ecologicalZonesImport.occurrencesDuplicated
                            if (total > 0) {
                                isLoading = true
                                occurrencesCount = total
                                coroutineScope.launch {
                                    val result = repository.fetchOccurrences(idSpecies, selectedStateForQuery)
                                    if (result.isSuccess) onNavigateToMap()
                                    isLoading = false
                                }
                            }
                        }, colors = ButtonDefaults.buttonColors(containerColor = primaryGreen)) { Text("Ver Mapa") }
                    },
                    dismissButton = { TextButton(onClick = { showImportDialog = false }) { Text("Cerrar", color = fixedGrayText) } }
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, color = color, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelector(
    label: String,
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onClear: (() -> Unit)?,
    isSet: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, color = if (isSet) Color(0xFF2E7D32) else Color(0xFF49454F)) },
            textStyle = LocalTextStyle.current.copy(color = if (isSet) Color(0xFF2E7D32) else Color(0xFF1C1B1F)),
            trailingIcon = {
                if (isSet && onClear != null) {
                    IconButton(onClick = onClear) { Icon(Icons.Default.Close, tint = Color.Red, contentDescription = null) }
                } else {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2E7D32),
                unfocusedBorderColor = Color.LightGray,
                focusedLabelColor = Color(0xFF2E7D32),
                unfocusedLabelColor = Color(0xFF49454F),
                focusedContainerColor = Color(0xFFF9F9F9),
                unfocusedContainerColor = Color(0xFFF9F9F9)
            )
        )
        MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(surface = Color.White)) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier.background(Color.White),
                content = content
            )
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
}