package com.example.agro.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
    var occurrences by remember { mutableStateOf<List<OccurrenceEntity>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Cargar especies importadas al iniciar
    LaunchedEffect(Unit) {
        importedSpecies = repository.getSuccessfulImports()
    }

    // Cargar ocurrencias cuando cambia la especie seleccionada
    LaunchedEffect(selectedSpecies) {
        selectedSpecies?.let { species ->
            isLoading = true
            val result = repository.fetchOccurrences(species.idSpecies)
            if (result.isSuccess) {
                occurrences = result.getOrDefault(emptyList())
            }
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // El mapa va al fondo
        GoogleMapContainer(repository, occurrences)

        // Buscador superpuesto con fondo para visibilidad
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), MaterialTheme.shapes.medium)
                .padding(8.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                TextField(
                    value = selectedSpecies?.query ?: "Seleccionar especie...",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Especies Importadas") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    if (importedSpecies.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No hay especies importadas") },
                            onClick = { expanded = false }
                        )
                    } else {
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
                .snippet("Registrado por: ${occurrence.recordedBy ?: "N/A"}")
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
