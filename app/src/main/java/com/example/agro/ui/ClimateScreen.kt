package com.example.agro.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

    // Colores fijos profesionales
    val primaryGreen = Color(0xFF2E7D32)
    val secondaryGreen = Color(0xFFE8F5E9)
    val fixedDarkText = Color(0xFF1C1B1F)
    val fixedGrayText = Color(0xFF49454F)

    LaunchedEffect(Unit) {
        importedSpecies = repository.getSuccessfulImports()
    }

    LaunchedEffect(selectedSpecies) {
        selectedSpecies?.let {
            nicheData = repository.getSavedNiche(it.idSpecies)
        }
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
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Cabecera Profesional
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudQueue, contentDescription = null, tint = primaryGreen, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Análisis de Nicho",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = primaryGreen
                    )
                }
                Text("Modelado climático basado en datos GBIF", color = fixedGrayText, style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(24.dp))

                // TARJETA DE CONFIGURACIÓN
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("1. Selecciona el Cultivo", style = MaterialTheme.typography.labelLarge, color = primaryGreen, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = selectedSpecies?.commonName ?: "Seleccionar especie...",
                                onValueChange = {},
                                readOnly = true,
                                textStyle = LocalTextStyle.current.copy(color = fixedDarkText),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryGreen,
                                    unfocusedBorderColor = Color.LightGray,
                                    focusedContainerColor = Color(0xFFF9F9F9),
                                    unfocusedContainerColor = Color(0xFFF9F9F9)
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                importedSpecies.forEach { species ->
                                    DropdownMenuItem(
                                        text = { Text("${species.commonName} (${species.query})", color = fixedDarkText) },
                                        onClick = {
                                            selectedSpecies = species
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                selectedSpecies?.let {
                                    isLoading = true
                                    coroutineScope.launch {
                                        val result = repository.calculateAndSaveNiche(it.idSpecies, 30)
                                        if (result.isSuccess) {
                                            nicheData = repository.getSavedNiche(it.idSpecies)
                                            snackbarHostState.showSnackbar("Nicho generado con éxito")
                                        }
                                        isLoading = false
                                    }
                                }
                            },
                            enabled = selectedSpecies != null && !isLoading,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryGreen)
                        ) {
                            Icon(Icons.Default.AutoGraph, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("GENERAR MODELO CLIMÁTICO", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // VISTA DE RESULTADOS
                if (nicheData != null) {
                    NicheDetailView(nicheData!!, primaryGreen, fixedDarkText, fixedGrayText)
                } else if (selectedSpecies != null && !isLoading) {
                    EmptyNicheState(fixedGrayText)
                }
            }

            // Overlay de Carga Profesional
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = primaryGreen)
                            Spacer(modifier = Modifier.height(20.dp))
                            Text("Analizando Nicho...", fontWeight = FontWeight.Bold, color = fixedDarkText)
                            Text("Procesando datos geoespaciales", style = MaterialTheme.typography.bodySmall, color = fixedGrayText)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NicheDetailView(niche: SpeciesNicheEntity, primaryGreen: Color, darkText: Color, grayText: Color) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item { ClimateSummaryCard(niche, primaryGreen) }

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
                darkText = darkText,
                grayText = grayText
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
                darkText = darkText,
                grayText = grayText
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
                darkText = darkText,
                grayText = grayText
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.5f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = grayText, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Muestreo: ${niche.points_sampled} puntos | Válidos: ${niche.points_with_climate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = grayText
                    )
                }
            }
        }
    }
}

@Composable
fun ClimateSummaryCard(niche: SpeciesNicheEntity, primaryGreen: Color) {
    val (climateType, icon) = when {
        niche.temp_opt_max < 15 -> "Frío / Boreal" to "❄"
        niche.temp_opt_max in 15.0..25.0 -> "Templado" to "🌤"
        else -> "Cálido / Tropical" to "🌴"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = primaryGreen),
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Tipo de Clima Probable", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
                Text(climateType, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
            Text(icon, fontSize = 40.sp)
        }
    }
}

@Composable
fun NicheFactorCard(
    title: String,
    icon: ImageVector,
    color: Color,
    min: Double,
    optMin: Double,
    optMax: Double,
    max: Double,
    unit: String,
    darkText: Color,
    grayText: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.padding(8.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = darkText)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Gráfico de Rango Visual
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Min: ${min.toInt()}$unit", fontSize = 11.sp, color = grayText)
                    Text("Max: ${max.toInt()}$unit", fontSize = 11.sp, color = grayText)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth().height(12.dp).background(color.copy(alpha = 0.1f), RoundedCornerShape(6.dp))) {
                    // Aquí se podría calcular la posición real del rango óptimo, por ahora visual:
                    Box(modifier = Modifier.fillMaxWidth(0.6f).fillMaxHeight().align(Alignment.Center).background(color, RoundedCornerShape(6.dp)))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip(label = "Ideal", value = "$optMin - $optMax $unit", color = color)
                InfoChip(label = "Tolerado", value = "${min.toInt()} - ${max.toInt()} $unit", color = Color.Gray)
            }
        }
    }
}

@Composable
fun InfoChip(label: String, value: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.05f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = color.copy(alpha = 0.7f))
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1B1F))
        }
    }
}

@Composable
fun EmptyNicheState(grayText: Color) {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Sin datos de nicho calculados.\nPresiona el botón superior para generar el análisis.",
            textAlign = TextAlign.Center,
            color = grayText,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}