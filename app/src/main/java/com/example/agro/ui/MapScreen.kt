package com.example.agro.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

    if (hasLocationPermission) {
        MapWithSearch(repository)
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Se requieren permisos de ubicación para ver el mapa.")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapWithSearch(repository: AgroRepository) {
    var importedSpecies by remember { mutableStateOf<List<SuccessfulImportEntity>>(emptyList()) }
    var selectedSpecies by remember { mutableStateOf<SuccessfulImportEntity?>(null) }
    var allOccurrences by remember { mutableStateOf<List<OccurrenceEntity>>(emptyList()) }
    var filteredOccurrences by remember { mutableStateOf<List<OccurrenceEntity>>(emptyList()) }
    
    var availableStates by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedState by remember { mutableStateOf<String?>(null) }
    
    var expandedSpecies by remember { mutableStateOf(false) }
    var expandedStates by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Cargar especies importadas
    LaunchedEffect(Unit) {
        importedSpecies = repository.getSuccessfulImports()
    }

    // Cargar todas las ocurrencias de la especie seleccionada
    LaunchedEffect(selectedSpecies) {
        selectedSpecies?.let { species ->
            isLoading = true
            val result = repository.fetchOccurrences(species.idSpecies)
            if (result.isSuccess) {
                allOccurrences = result.getOrDefault(emptyList())
                availableStates = allOccurrences.mapNotNull { it.stateProvince }.distinct().sorted()
                selectedState = null // Resetear filtro al cambiar de especie
                filteredOccurrences = allOccurrences
            }
            isLoading = false
        }
    }

    // Filtrar localmente cuando cambia el estado seleccionado
    LaunchedEffect(selectedState, allOccurrences) {
        filteredOccurrences = if (selectedState == null) {
            allOccurrences
        } else {
            allOccurrences.filter { it.stateProvince == selectedState }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMapContainer(repository, filteredOccurrences)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), MaterialTheme.shapes.medium)
                .padding(8.dp)
        ) {
            // Selector de Especies
            ExposedDropdownMenuBox(
                expanded = expandedSpecies,
                onExpandedChange = { expandedSpecies = !expandedSpecies }
            ) {
                TextField(
                    value = selectedSpecies?.commonName ?: "Seleccionar especie...",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Especie") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSpecies) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
                ExposedDropdownMenu(
                    expanded = expandedSpecies,
                    onDismissRequest = { expandedSpecies = false }
                ) {
                    importedSpecies.forEach { species ->
                        DropdownMenuItem(
                            text = { Text("${species.commonName} (${species.query})") },
                            onClick = {
                                selectedSpecies = species
                                expandedSpecies = false
                            }
                        )
                    }
                }
            }

            // Selector de Estados (Solo aparece si hay especie seleccionada)
            if (selectedSpecies != null) {
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = expandedStates,
                    onExpandedChange = { expandedStates = !expandedStates }
                ) {
                    TextField(
                        value = selectedState ?: "Todos los estados",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Filtrar por Estado") },
                        trailingIcon = {
                            if (selectedState != null) {
                                IconButton(onClick = { selectedState = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Limpiar")
                                }
                            } else {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStates)
                            }
                        },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expandedStates,
                        onDismissRequest = { expandedStates = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Todos los estados") },
                            onClick = {
                                selectedState = null
                                expandedStates = false
                            }
                        )
                        availableStates.forEach { state ->
                            DropdownMenuItem(
                                text = { Text(state) },
                                onClick = {
                                    selectedState = state
                                    expandedStates = false
                                }
                            )
                        }
                    }
                }
            }
            
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun GoogleMapContainer(repository: AgroRepository, occurrences: List<OccurrenceEntity>) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }

    AndroidView(
        factory = {
            mapView.apply {
                onCreate(null)
                getMapAsync { googleMap ->
                    googleMap.isMyLocationEnabled = true
                    googleMap.uiSettings.isMyLocationButtonEnabled = true
                    updateMarkers(googleMap, occurrences)
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = {
            it.getMapAsync { googleMap ->
                updateMarkers(googleMap, occurrences)
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

private fun updateMarkers(googleMap: GoogleMap, occurrences: List<OccurrenceEntity>) {
    googleMap.clear()
    if (occurrences.isEmpty()) {
        val mexicoCity = LatLng(19.4326, -99.1332)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mexicoCity, 5f))
        return
    }

    val builder = LatLngBounds.Builder()
    occurrences.forEach { occurrence ->
        val pos = LatLng(occurrence.latitude, occurrence.longitude)
        googleMap.addMarker(
            MarkerOptions()
                .position(pos)
                .title("Ocurrencia")
                .snippet("Estado: ${occurrence.stateProvince ?: "N/A"}")
        )
        builder.include(pos)
    }

    try {
        val bounds = builder.build()
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
    } catch (e: Exception) {
        if (occurrences.isNotEmpty()) {
            val first = LatLng(occurrences[0].latitude, occurrences[0].longitude)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(first, 10f))
        }
    }
}
