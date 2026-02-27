package com.example.agro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

    // Colores Profesionales (Fijos para evitar errores de modo oscuro)
    val primaryGreen = Color(0xFF2E7D32)
    val secondaryGreen = Color(0xFFE8F5E9)
    val fixedDarkText = Color(0xFF1C1B1F)
    val fixedGrayText = Color(0xFF49454F)

    LaunchedEffect(Unit) {
        isLoading = true
        val result = repository.fetchAllSpecies()
        if (result.isSuccess) {
            speciesList = result.getOrDefault(emptyList())
        }
        isLoading = false
    }

    Scaffold(
        containerColor = Color.Transparent, // Usaremos nuestro fondo degradado
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Explorar Especies", fontWeight = FontWeight.ExtraBold, color = primaryGreen)
                },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Buscar", tint = primaryGreen)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White.copy(alpha = 0.9f))
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (selectedSpecies.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            repository.saveSpeciesLocally(selectedSpecies.toList())
                            snackbarHostState.showSnackbar("Guardado localmente: ${selectedSpecies.size} especies")
                            selectedSpecies.clear()
                        }
                    },
                    containerColor = primaryGreen,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    icon = { Icon(Icons.Default.Save, contentDescription = null) },
                    text = { Text("Guardar Selección (${selectedSpecies.size})") }
                )
            }
        }
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
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Selecciona las especies que deseas sincronizar para uso offline.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = fixedGrayText,
                    modifier = Modifier.padding(bottom = 16.dp),
                    textAlign = TextAlign.Start
                )

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = primaryGreen)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp), // Espacio para el FAB
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(speciesList) { species ->
                            SpeciesSelectionCard(
                                species = species,
                                isSelected = selectedSpecies.contains(species),
                                primaryGreen = primaryGreen,
                                darkText = fixedDarkText,
                                grayText = fixedGrayText,
                                onSelectionChange = { isSelected ->
                                    if (isSelected) selectedSpecies.add(species)
                                    else selectedSpecies.remove(species)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpeciesSelectionCard(
    species: SpeciesEntity,
    isSelected: Boolean,
    primaryGreen: Color,
    darkText: Color,
    grayText: Color,
    onSelectionChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) primaryGreen.copy(alpha = 0.05f) else Color.White
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, primaryGreen) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono decorativo según selección
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) primaryGreen else primaryGreen.copy(alpha = 0.1f)
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Spa,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else primaryGreen,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = species.scientificName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = darkText
                )

                Text(
                    text = "${species.family} • ${species.genus}",
                    style = MaterialTheme.typography.bodySmall,
                    color = grayText
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Badge de Estatus Taxonómico
                    Surface(
                        color = primaryGreen.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = species.taxonomicStatus.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ID: ${species.idSpecies}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray
                    )
                }
            }

            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelectionChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = primaryGreen,
                    uncheckedColor = Color.LightGray
                )
            )
        }
    }
}
