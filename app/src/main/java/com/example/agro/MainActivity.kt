package com.example.agro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agro.data.AgroRepository
import com.example.agro.data.AppDatabase
import com.example.agro.ui.ClimateScreen
import com.example.agro.ui.HomeScreen
import com.example.agro.ui.LoginScreen
import com.example.agro.ui.LoginViewModel
import com.example.agro.ui.LoginViewModelFactory
import com.example.agro.ui.MapScreen
import com.example.agro.ui.SearchScreen
import com.example.agro.ui.SpeciesScreen
import com.example.agro.ui.WelcomeScreen
import com.example.agro.ui.theme.AgroTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = AppDatabase.getDatabase(this)
        val repository = AgroRepository(database.tokenDao())
        val loginViewModel: LoginViewModel by viewModels { LoginViewModelFactory(repository) }

        enableEdgeToEdge()
        setContent {
            AgroTheme {
                val isLoggedIn by loginViewModel.isLoggedIn.collectAsState()

                when (isLoggedIn) {
                    null -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    false -> {
                        LoginScreen(viewModel = loginViewModel)
                    }
                    true -> {
                        AgroApp(
                            repository = repository,
                            onLogout = { loginViewModel.logout() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AgroApp(repository: AgroRepository, onLogout: () -> Unit) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.WELCOME) }

    if (currentDestination == AppDestinations.SEARCH_SCIENTIFIC) {
        SearchScreen(
            repository = repository,
            onBack = { currentDestination = AppDestinations.SPECIES }
        )
    } else if (currentDestination == AppDestinations.WELCOME) {
        WelcomeScreen(
            onStartInvestigating = { currentDestination = AppDestinations.SEARCH_SCIENTIFIC }
        )
    } else {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                AppDestinations.entries.filter { !it.isSearch && it != AppDestinations.WELCOME }.forEach {
                    item(
                        icon = {
                            Icon(
                                it.icon,
                                contentDescription = it.label
                            )
                        },
                        label = { Text(it.label) },
                        selected = it == currentDestination,
                        onClick = { currentDestination = it }
                    )
                }
            }
        ) {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    when (currentDestination) {
                        AppDestinations.BUSQUEDA -> HomeScreen(
                            repository = repository,
                            onNavigateToMap = { currentDestination = AppDestinations.MAPA }
                        )
                        AppDestinations.MAPA -> MapScreen(repository = repository)
                        AppDestinations.CLIMATE -> ClimateScreen(repository = repository)
                        AppDestinations.SPECIES -> SpeciesScreen(
                            repository = repository,
                            onNavigateToSearch = { currentDestination = AppDestinations.SEARCH_SCIENTIFIC }
                        )
                        AppDestinations.EXIT -> ExitScreen(onLogout = onLogout)
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
fun ExitScreen(onLogout: () -> Unit) {
    val primaryGreen = Color(0xFF2E7D32)
    val secondaryGreen = Color(0xFFE8F5E9)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(secondaryGreen, Color.White)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Spa,
                contentDescription = null,
                tint = primaryGreen,
                modifier = Modifier.size(100.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "¡Gracias por usar Agro!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = primaryGreen,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Esperamos que la información recopilada sea de gran utilidad para tus proyectos agrícolas. Vuelve pronto para seguir investigando.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF49454F),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Cerrar Sesión",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
    val isSearch: Boolean = false
) {
    WELCOME("Inicio", Icons.Default.Agriculture),
    BUSQUEDA("Búsqueda", Icons.Default.Search),
    MAPA("Mapa", Icons.Default.Public),
    CLIMATE("Nichos", Icons.Filled.Thermostat),
    SPECIES("Especies", Icons.Default.Spa),
    SEARCH_SCIENTIFIC("Buscar", Icons.Default.Search, true),
    EXIT("Cuenta", Icons.Default.AccountCircle),
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AgroTheme {
        Greeting("Android")
    }
}
