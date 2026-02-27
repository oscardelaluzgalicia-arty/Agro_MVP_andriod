package com.example.agro.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.agro.data.AgroRepository
import com.example.agro.data.OccurrenceEntity
import com.example.agro.data.SuccessfulImportEntity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions

@Composable
fun MapScreen(repository: AgroRepository) {
    val context = LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        }
    )

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val primaryGreen = Color(0xFF2E7D32)
    val secondaryGreen = Color(0xFFE8F5E9)

    if (hasLocationPermission) {
        MapWithSearch(repository, primaryGreen, secondaryGreen)
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(secondaryGreen, Color.White))),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(64.dp), tint = primaryGreen)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Se requieren permisos de ubicación para ver el mapa.",
                    color = Color(0xFF1C1B1F),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        launcher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryGreen)
                ) {
                    Text("Conceder Permisos")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapWithSearch(repository: AgroRepository, primaryGreen: Color, secondaryGreen: Color) {
    var importedSpecies by remember { mutableStateOf<List<SuccessfulImportEntity>>(emptyList()) }
    var selectedSpecies by remember { mutableStateOf<SuccessfulImportEntity?>(null) }
    var allOccurrences by remember { mutableStateOf<List<OccurrenceEntity>>(emptyList()) }
    var filteredOccurrences by remember { mutableStateOf<List<OccurrenceEntity>>(emptyList()) }
    
    var availableStates by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedState by remember { mutableStateOf<String?>(null) }
    
    var expandedSpecies by remember { mutableStateOf(false) }
    var expandedStates by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val fixedDarkText = Color(0xFF1C1B1F)

    LaunchedEffect(Unit) {
        importedSpecies = repository.getSuccessfulImports()
    }

    LaunchedEffect(selectedSpecies) {
        selectedSpecies?.let { species ->
            isLoading = true
            val result = repository.fetchOccurrences(species.idSpecies)
            if (result.isSuccess) {
                allOccurrences = result.getOrDefault(emptyList())
                availableStates = allOccurrences.mapNotNull { it.stateProvince }.distinct().sorted()
                selectedState = null
                filteredOccurrences = allOccurrences
            }
            isLoading = false
        }
    }

    LaunchedEffect(selectedState, allOccurrences) {
        filteredOccurrences = if (selectedState == null) {
            allOccurrences
        } else {
            allOccurrences.filter { it.stateProvince == selectedState }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMapContainer(repository, filteredOccurrences, selectedSpecies?.commonName)

        // Panel de Control Flotante
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Filtros de Mapa",
                    style = MaterialTheme.typography.titleSmall,
                    color = primaryGreen,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Selector de Especies
                ExposedDropdownMenuBox(
                    expanded = expandedSpecies,
                    onExpandedChange = { expandedSpecies = it }
                ) {
                    OutlinedTextField(
                        value = selectedSpecies?.commonName ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Especie", color = primaryGreen) },
                        placeholder = { Text("Seleccionar especie...", color = Color.Gray) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSpecies) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryGreen,
                            unfocusedBorderColor = Color.LightGray,
                            focusedContainerColor = Color(0xFFF9F9F9),
                            unfocusedContainerColor = Color(0xFFF9F9F9),
                            focusedTextColor = fixedDarkText,
                            unfocusedTextColor = fixedDarkText
                        )
                    )
                    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(surface = Color.White)) {
                        ExposedDropdownMenu(
                            expanded = expandedSpecies,
                            onDismissRequest = { expandedSpecies = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            importedSpecies.forEach { species ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            text = "${species.commonName} (${species.query})", 
                                            color = fixedDarkText,
                                            fontWeight = FontWeight.Medium
                                        ) 
                                    },
                                    onClick = {
                                        selectedSpecies = species
                                        expandedSpecies = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (selectedSpecies != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    ExposedDropdownMenuBox(
                        expanded = expandedStates,
                        onExpandedChange = { expandedStates = it }
                    ) {
                        OutlinedTextField(
                            value = selectedState ?: "Todos los estados",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Estado", color = primaryGreen) },
                            trailingIcon = {
                                if (selectedState != null) {
                                    IconButton(onClick = { selectedState = null }) {
                                        Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = Color.Red)
                                    }
                                } else {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStates)
                                }
                            },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryGreen,
                                unfocusedBorderColor = Color.LightGray,
                                focusedContainerColor = Color(0xFFF9F9F9),
                                unfocusedContainerColor = Color(0xFFF9F9F9),
                                focusedTextColor = fixedDarkText,
                                unfocusedTextColor = fixedDarkText
                            )
                        )
                        MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(surface = Color.White)) {
                            ExposedDropdownMenu(
                                expanded = expandedStates,
                                onDismissRequest = { expandedStates = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Todos los estados", color = fixedDarkText, fontWeight = FontWeight.Medium) },
                                    onClick = {
                                        selectedState = null
                                        expandedStates = false
                                    }
                                )
                                availableStates.forEach { state ->
                                    DropdownMenuItem(
                                        text = { Text(state, color = fixedDarkText, fontWeight = FontWeight.Medium) },
                                        onClick = {
                                            selectedState = state
                                            expandedStates = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                if (isLoading) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = primaryGreen,
                        trackColor = secondaryGreen
                    )
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun GoogleMapContainer(repository: AgroRepository, occurrences: List<OccurrenceEntity>, speciesName: String?) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }

    AndroidView(
        factory = {
            mapView.apply {
                onCreate(null)
                getMapAsync { googleMap ->
                    googleMap.isMyLocationEnabled = true
                    googleMap.uiSettings.isMyLocationButtonEnabled = true
                    googleMap.uiSettings.isZoomControlsEnabled = true
                    
                    // Aplicar estilo de mapa "Retro/Nature" mediante JSON
                    val mapStyleJson = """
                        [
                          {
                            "featureType": "landscape",
                            "stylers": [{ "color": "#e8f5e9" }]
                          },
                          {
                            "featureType": "water",
                            "stylers": [{ "color": "#81d4fa" }]
                          },
                          {
                            "featureType": "road",
                            "stylers": [{ "visibility": "simplified" }]
                          },
                          {
                            "featureType": "poi.park",
                            "stylers": [{ "color": "#c8e6c9" }]
                          }
                        ]
                    """.trimIndent()
                    
                    googleMap.setMapStyle(MapStyleOptions(mapStyleJson))
                    
                    updateMarkers(googleMap, occurrences, speciesName)
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = {
            it.getMapAsync { googleMap ->
                updateMarkers(googleMap, occurrences, speciesName)
            }
            it.onResume()
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            mapView.onPause()
            mapView.onDestroy()
        }
    }
}

private fun updateMarkers(googleMap: GoogleMap, occurrences: List<OccurrenceEntity>, speciesName: String?) {
    googleMap.clear()
    if (occurrences.isEmpty()) {
        val mexicoCity = LatLng(23.6345, -102.5528) // Centro de México
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mexicoCity, 5f))
        return
    }

    val builder = LatLngBounds.Builder()
    occurrences.forEach { occurrence ->
        val pos = LatLng(occurrence.latitude, occurrence.longitude)
        googleMap.addMarker(
            MarkerOptions()
                .position(pos)
                .title("Registro de ${speciesName ?: "Especie"}")
                .snippet("Estado: ${occurrence.stateProvince ?: "N/A"}")
        )
        builder.include(pos)
    }

    try {
        val bounds = builder.build()
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150))
    } catch (e: Exception) {
        if (occurrences.isNotEmpty()) {
            val first = LatLng(occurrences[0].latitude, occurrences[0].longitude)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(first, 8f))
        }
    }
}
