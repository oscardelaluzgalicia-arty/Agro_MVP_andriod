package com.example.agro.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.agro.data.AgroRepository
import com.example.agro.data.ImportResponse
import com.example.agro.data.ScientificNameResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: AgroRepository) : ViewModel() {

    var commonNameSearch by mutableStateOf("")
    
    private val _results = MutableStateFlow<List<ScientificNameResponse>>(emptyList())
    val results: StateFlow<List<ScientificNameResponse>> = _results.asStateFlow()

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _importResult = MutableStateFlow<ImportResponse?>(null)
    val importResult: StateFlow<ImportResponse?> = _importResult.asStateFlow()

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()

    val statesOfMexico = listOf(
        "Aguascalientes", "Baja California", "Baja California Sur", "Campeche", "Chiapas",
        "Chihuahua", "Coahuila", "Colima", "Ciudad de México", "Durango", "Guanajuato",
        "Guerrero", "Hidalgo", "Jalisco", "México", "Michoacán", "Morelos", "Nayarit",
        "Nuevo León", "Oaxaca", "Puebla", "Querétaro", "Quintana Roo", "San Luis Potosí",
        "Sinaloa", "Sonora", "Tabasco", "Tamaulipas", "Tlaxcala", "Veracruz", "Yucatán", "Zacatecas"
    )

    var selectedState by mutableStateOf("Baja California")

    fun search() {
        if (commonNameSearch.isBlank()) return
        
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch {
            val result = repository.resolveCommonName(commonNameSearch)
            if (result.isSuccess) {
                _results.value = result.getOrNull()?.scientificNames ?: emptyList()
                _uiState.value = HomeUiState.Idle
            } else {
                _uiState.value = HomeUiState.Error(result.exceptionOrNull()?.message ?: "Error al buscar")
            }
        }
    }

    fun import(scientificName: String) {
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch {
            val result = repository.import(scientificName, selectedState, commonNameSearch)
            if (result.isSuccess) {
                _importResult.value = result.getOrNull()
                _showDialog.value = true
                _uiState.value = HomeUiState.Idle
            } else {
                _uiState.value = HomeUiState.Error(result.exceptionOrNull()?.message ?: "Error al importar")
            }
        }
    }

    fun fetchOccurrencesAndNavigate(onNavigate: () -> Unit) {
        val idSpecies = _importResult.value?.speciesImport?.idSpecies ?: return
        
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch {
            val result = repository.fetchOccurrences(idSpecies)
            if (result.isSuccess) {
                _uiState.value = HomeUiState.Idle
                dismissDialog()
                onNavigate()
            } else {
                _uiState.value = HomeUiState.Error(result.exceptionOrNull()?.message ?: "Error al cargar ocurrencias")
            }
        }
    }

    fun dismissDialog() {
        _showDialog.value = false
        _importResult.value = null
    }
}

sealed class HomeUiState {
    object Idle : HomeUiState()
    object Loading : HomeUiState()
    data class Success(val message: String) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModelFactory(private val repository: AgroRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
