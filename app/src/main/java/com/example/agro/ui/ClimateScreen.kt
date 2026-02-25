package com.example.agro.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agro.data.AgroRepository
import com.example.agro.data.SpeciesNicheEntity
import com.example.agro.data.SuccessfulImportEntity
import kotlinx.coroutines.launch

private const val TAG = "AgroDebug"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClimateScreen(repository: AgroRepository) {
    var importedSpecies by remember { mutableStateOf<List<SuccessfulImportEntity>>(emptyList()) }
    var selectedSpecies by remember { mutableStateOf<SuccessfulImportEntity?>(null) }
    var nicheData by remember { mutableStateOf<SpeciesNicheEntity?>(null) }
    
    var expanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        Log.d(TAG, "Iniciando ClimateScreen: Cargando especies importadas")
        importedSpecies = repository.getSuccessfulImports()
        Log.d(TAG, "Especies cargadas: ${importedSpecies.size}")
    }

    LaunchedEffect(selectedSpecies) {
        selectedSpecies?.let {
            Log.d(TAG, "Especie seleccionada: ${it.commonName} (ID: ${it.idSpecies})")
            nicheData = repository.getSavedNiche(it.idSpecies)
            if (nicheData != null) {
                Log.d(TAG, "Nicho cargado desde base de datos local")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Gestión de Nicho Climático", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Genera y analiza el nicho basado en datos de GBIF", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                
                Spacer(modifier = Modifier.height(16.dp))

                // Selector de Especies
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    TextField(
                        value = selectedSpecies?.commonName ?: "Seleccionar especie...",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Especies Importadas") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        importedSpecies.forEach { species ->
                            DropdownMenuItem(
                                text = { Text("${species.commonName} (${species.query})") },
                                onClick = {
                                    selectedSpecies = species
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        selectedSpecies?.let {
                            Log.d(TAG, "Botón clickeado: Generando nicho para ID: ${it.idSpecies}, sample_size: 30")
                            isLoading = true
                            coroutineScope.launch {
                                try {
                                    val result = repository.calculateAndSaveNiche(it.idSpecies, 30)
                                    if (result.isSuccess) {
                                        Log.d(TAG, "Éxito: Nicho generado y guardado")
                                        nicheData = repository.getSavedNiche(it.idSpecies)
                                        snackbarHostState.showSnackbar("Nicho generado correctamente")
                                    } else {
                                        val error = result.exceptionOrNull()?.message ?: "Error desconocido"
                                        Log.e(TAG, "Error en el repositorio: $error")
                                        snackbarHostState.showSnackbar("Error: $error")
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Excepción atrapada en UI: ${e.message}", e)
                                    snackbarHostState.showSnackbar("Excepción: ${e.message}")
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    },
                    enabled = selectedSpecies != null && !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Generar Nicho Inteligente")
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (nicheData != null) {
                    NicheDetailView(nicheData!!)
                } else if (selectedSpecies != null && !isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No hay datos de nicho calculados para esta especie.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Indicador de carga a pantalla completa
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Calculando nicho climático...", fontWeight = FontWeight.Bold)
                            Text("Consultando datos satelitales", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NicheDetailView(niche: SpeciesNicheEntity) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            ClimateSummaryCard(niche)
        }
        item {
            NicheFactorCard(
                title = "Temperatura",
                icon = Icons.Default.Thermostat,
                color = Color(0xFFFF5722),
                min = niche.temp_min,
                optMin = niche.temp_opt_min,
                optMax = niche.temp_opt_max,
                max = niche.temp_max,
                unit = "°C",
                description = "Se encuentra principalmente entre ${niche.temp_opt_min}° y ${niche.temp_opt_max}°C"
            )
        }
        item {
            NicheFactorCard(
                title = "Precipitación",
                icon = Icons.Default.WaterDrop,
                color = Color(0xFF2196F3),
                min = niche.rainfall_min,
                optMin = niche.rainfall_opt_min,
                optMax = niche.rainfall_opt_max,
                max = niche.rainfall_max,
                unit = "mm",
                description = "Rango más favorable: ${niche.rainfall_opt_min} - ${niche.rainfall_opt_max} mm"
            )
        }
        item {
            NicheFactorCard(
                title = "Altitud",
                icon = Icons.Default.Landscape,
                color = Color(0xFF4CAF50),
                min = niche.altitude_min,
                optMin = niche.altitude_min,
                optMax = niche.altitude_max,
                max = niche.altitude_max,
                unit = "msnm",
                description = "Rango de elevación observado: ${niche.altitude_min.toInt()} - ${niche.altitude_max.toInt()} msnm"
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Metadatos del cálculo", style = MaterialTheme.typography.titleSmall)
                    Text("Puntos muestreados: ${niche.points_sampled}", fontSize = 12.sp)
                    Text("Puntos con datos climáticos: ${niche.points_with_climate}", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ClimateSummaryCard(niche: SpeciesNicheEntity) {
    val climateType = when {
        niche.temp_opt_max < 15 -> "Frío / Boreal"
        niche.temp_opt_max in 15.0..25.0 -> "Templado"
        else -> "Cálido / Tropical"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("🌤 Tipo de Clima Probable", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(climateType, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Basado en el rango óptimo de crecimiento.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun NicheFactorCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    min: Double,
    optMin: Double,
    optMax: Double,
    max: Double,
    unit: String,
    description: String
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color)
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(description, style = MaterialTheme.typography.bodyMedium)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Barra Visual Simplificada
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${min.toInt()}$unit", fontSize = 10.sp)
                    Text("${max.toInt()}$unit", fontSize = 10.sp)
                }
                LinearProgressIndicator(
                    progress = { 0.5f },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = color,
                    trackColor = color.copy(alpha = 0.2f)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Text("Rango Ideal", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("🔵 Rango favorable: $optMin - $optMax $unit", fontSize = 12.sp)
                Text("🟡 Rango tolerado: $min - $max $unit", fontSize = 12.sp)
                Text("⚠ Fuera de este rango: Estrés biótico", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
