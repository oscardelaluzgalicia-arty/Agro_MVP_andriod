package com.example.agro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agro.data.AgroRepository
import com.example.agro.data.ScientificNameResponse
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(repository: AgroRepository, onBack: () -> Unit) {
    // Estados de la UI
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<ScientificNameResponse>>(emptyList()) }
    val selectedScientificNames = remember { mutableStateListOf<ScientificNameResponse>() }
    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Colores Profesionales (Fijos para evitar problemas con Modo Oscuro)
    val primaryGreen = Color(0xFF2E7D32)
    val secondaryGreen = Color(0xFFE8F5E9)
    val fixedDarkText = Color(0xFF1C1B1F)
    val fixedGrayText = Color(0xFF49454F)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Búsqueda Semántica", fontWeight = FontWeight.ExtraBold, color = primaryGreen)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = primaryGreen)
                    }
                },
                actions = {
                    // ICONO DE LIMPIEZA (Mejorado para entender que limpia la pantalla)
                    IconButton(onClick = {
                        query = ""
                        results = emptyList()
                        selectedScientificNames.clear()
                    }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Limpiar Todo", tint = Color.Gray)
                    }

                    if (selectedScientificNames.isNotEmpty()) {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                repository.saveScientificNames(selectedScientificNames.toList())
                                snackbarHostState.showSnackbar("Guardado localmente: ${selectedScientificNames.size} nombres")
                                selectedScientificNames.clear()
                            }
                        }) {
                            Icon(Icons.Default.Save, contentDescription = "Guardar", tint = primaryGreen)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
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

                // BARRA DE BÚSQUEDA PROFESIONAL
                Card(
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = primaryGreen)
                        TextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text("Ej: Maíz, Papa, Tomate...", color = Color.LightGray) },
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(color = fixedDarkText, fontSize = 16.sp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = primaryGreen,
                                focusedTextColor = fixedDarkText,
                                unfocusedTextColor = fixedDarkText
                            ),
                            singleLine = true
                        )

                        if (query.isNotBlank()) {
                            Button(
                                onClick = {
                                    isLoading = true
                                    coroutineScope.launch {
                                        val result = repository.resolveCommonName(query.trim())
                                        if (result.isSuccess) {
                                            results = result.getOrNull()?.scientificNames ?: emptyList()
                                            if (results.isEmpty()) snackbarHostState.showSnackbar("Sin resultados")
                                        } else {
                                            snackbarHostState.showSnackbar("Error en la conexión")
                                        }
                                        isLoading = false
                                    }
                                },
                                enabled = !isLoading,
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = primaryGreen),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Text("BUSCAR", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // RESULTADOS O ESTADO VACÍO
                if (results.isEmpty() && !isLoading) {
                    EmptySearchState(fixedGrayText)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        items(results) { scientific ->
                            SemanticResultCard(
                                item = scientific,
                                isSelected = selectedScientificNames.contains(scientific),
                                primaryGreen = primaryGreen,
                                darkText = fixedDarkText,
                                grayText = fixedGrayText,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) selectedScientificNames.add(scientific)
                                    else selectedScientificNames.remove(scientific)
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
fun SemanticResultCard(
    item: ScientificNameResponse,
    isSelected: Boolean,
    primaryGreen: Color,
    darkText: Color,
    grayText: Color,
    onCheckedChange: (Boolean) -> Unit
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
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(checkedColor = primaryGreen)
            )

            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = item.scientificName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = darkText
                )
                Text(
                    text = "Canónico: ${item.canonicalName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = grayText
                )

                Row(modifier = Modifier.padding(top = 8.dp)) {
                    // Badge de Confianza
                    Surface(
                        color = primaryGreen.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Confianza: ${item.confidence}%",
                            color = primaryGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // Badge de Estatus
                    Surface(
                        color = Color.LightGray.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = item.status.uppercase(),
                            color = grayText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptySearchState(color: Color) {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.TravelExplore,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = Color.LightGray.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Escribe el nombre de una fruta o verdura\npara descubrir su identidad científica.",
            textAlign = TextAlign.Center,
            color = color,
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 22.sp
        )
    }
}