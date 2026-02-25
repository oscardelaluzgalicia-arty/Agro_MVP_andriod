package com.example.agro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.BUSQUEDA) }

    if (currentDestination == AppDestinations.SEARCH_SCIENTIFIC) {
        SearchScreen(
            repository = repository,
            onBack = { currentDestination = AppDestinations.SPECIES }
        )
    } else {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                AppDestinations.entries.filter { !it.isSearch }.forEach {
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
                        AppDestinations.PROFILE -> ProfileScreen(onLogout = onLogout)
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(onLogout: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Text(text = "Profile")
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(16.dp))
        Button(onClick = onLogout) {
            Text("Logout")
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
    val isSearch: Boolean = false
) {
    BUSQUEDA("Búsqueda", Icons.Default.Search),
    MAPA("Mapa", Icons.Default.Public),
    CLIMATE("Nichos", Icons.Filled.Thermostat),
    SPECIES("Especies", Icons.Default.Spa),
    SEARCH_SCIENTIFIC("Buscar", Icons.Default.Search, true),
    PROFILE("Profile", Icons.Default.AccountBox),
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
