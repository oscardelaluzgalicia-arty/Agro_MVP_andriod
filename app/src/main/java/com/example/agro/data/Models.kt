package com.example.agro.data

import com.google.gson.annotations.SerializedName
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

// --- Autenticación ---
data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val modulos: List<ModuleResponse>
)

data class ModuleResponse(
    @SerializedName("id_module") val idModule: Int,
    val name: String,
    val description: String,
    @SerializedName("access_level") val accessLevel: String,
    @SerializedName("granted_at") val grantedAt: String
)

@Entity(tableName = "auth_token")
data class TokenEntity(
    @PrimaryKey val id: Int = 0,
    val token: String
)

@Entity(tableName = "user_modules")
data class ModuleEntity(
    @PrimaryKey val idModule: Int,
    val name: String,
    val description: String,
    val accessLevel: String,
    val grantedAt: String
)

// --- Búsqueda Semántica ---
data class ResolveNameRequest(
    val name: String
)

data class ResolveNameBatchRequest(
    val names: List<String>
)

data class ResolveNameResponse(
    val commonName: String,
    val scientificNames: List<ScientificNameResponse>,
    val totalFound: Int
)

@Entity(tableName = "scientific_names")
data class ScientificNameResponse(
    @PrimaryKey(autoGenerate = true) val localId: Int = 0,
    val inputName: String,
    val scientificName: String,
    val canonicalName: String,
    val taxonKey: Long,
    val rank: String,
    val status: String,
    val confidence: Int,
    val matchType: String,
    val phylum: String
)

// --- Importación de Datos (GBIF) ---
data class ImportRequest(
    val name: String,
    val country: String = "Mexico",
    @SerializedName("state_province") val stateProvince: String
)

data class ImportResponse(
    val query: String,
    val taxonKey: Long,
    @SerializedName("scientific_name") val scientificName: String,
    @SerializedName("species_import") val speciesImport: SpeciesImport,
    @SerializedName("ecological_zones_import") val ecologicalZonesImport: EcologicalZonesImport,
    @SerializedName("zones_source") val zonesSource: String
)

data class SpeciesImport(
    val status: String,
    @SerializedName("id_species") val idSpecies: Int
)

data class EcologicalZonesImport(
    @SerializedName("zones_inserted") val zonesInserted: Int,
    @SerializedName("zones_skipped") val zonesSkipped: Int,
    @SerializedName("species_zones_linked") val speciesZonesLinked: Int,
    @SerializedName("occurrences_inserted") val occurrencesInserted: Int,
    @SerializedName("occurrences_duplicated") val occurrencesDuplicated: Int,
    @SerializedName("occurrences_errors") val occurrencesErrors: Int,
    val errors: Int
)

@Entity(tableName = "successful_imports")
data class SuccessfulImportEntity(
    @PrimaryKey val taxonKey: Long,
    val query: String,
    val commonName: String,
    val idSpecies: Int
)

// --- CRUD y Ocurrencias ---
data class CrudRequest(
    val action: String = "read",
    val table: String,
    val where: Map<String, Any>? = null
)

@Entity(tableName = "occurrences")
data class OccurrenceEntity(
    @PrimaryKey 
    @SerializedName("id_occurrence") 
    @ColumnInfo(name = "id_occurrence")
    val idOccurrence: Int,
    
    @SerializedName("gbif_occurrence_id") 
    @ColumnInfo(name = "gbif_occurrence_id")
    val gbifOccurrenceId: Long,
    
    @SerializedName("id_species") 
    @ColumnInfo(name = "id_species")
    val idSpecies: Int,
    
    @SerializedName("decimal_latitude") 
    @ColumnInfo(name = "decimal_latitude")
    val latitude: Double,
    
    @SerializedName("decimal_longitude") 
    @ColumnInfo(name = "decimal_longitude")
    val longitude: Double,
    
    @SerializedName("coordinate_uncertainty_meters") 
    @ColumnInfo(name = "coordinate_uncertainty_meters")
    val uncertainty: Double?,
    
    val country: String,
    
    @SerializedName("state_province") 
    @ColumnInfo(name = "state_province")
    val stateProvince: String?,
    
    val municipality: String?,
    val locality: String?,
    
    @SerializedName("event_date") 
    @ColumnInfo(name = "event_date")
    val eventDate: String?,
    
    val year: Int?,
    val month: Int?,
    val day: Int?,
    val habitat: String?,
    val elevation: Double?,
    
    @SerializedName("basis_of_record") 
    @ColumnInfo(name = "basis_of_record")
    val basisOfRecord: String?,
    
    @SerializedName("dataset_key") 
    @ColumnInfo(name = "dataset_key")
    val datasetKey: String?,
    
    @SerializedName("institution_code") 
    @ColumnInfo(name = "institution_code")
    val institutionCode: String?,
    
    @SerializedName("recorded_by") 
    @ColumnInfo(name = "recorded_by")
    val recordedBy: String?,
    
    @SerializedName("identified_by") 
    @ColumnInfo(name = "identified_by")
    val identifiedBy: String?,
    
    @SerializedName("created_at") 
    @ColumnInfo(name = "created_at")
    val createdAt: String?
)

@Entity(tableName = "species")
data class SpeciesEntity(
    @PrimaryKey 
    @SerializedName("id_species") 
    @ColumnInfo(name = "id_species")
    val idSpecies: Int,
    
    @SerializedName("taxonKey") 
    @ColumnInfo(name = "taxonKey")
    val taxonKey: Long,
    
    @SerializedName("scientific_name") 
    @ColumnInfo(name = "scientific_name")
    val scientificName: String,
    
    val kingdom: String,
    val phylum: String,
    
    @SerializedName("class_name") 
    @ColumnInfo(name = "class_name")
    val className: String,
    
    @SerializedName("order_name") 
    @ColumnInfo(name = "order_name")
    val orderName: String,
    
    val family: String,
    val genus: String,
    val species: String,
    
    @SerializedName("taxonomic_status") 
    @ColumnInfo(name = "taxonomic_status")
    val taxonomicStatus: String,
    
    @SerializedName("created_at") 
    @ColumnInfo(name = "created_at")
    val createdAt: String
)

// --- Nicho Climático ---
@Entity(tableName = "species_niches")
data class SpeciesNicheEntity(
    @PrimaryKey 
    @ColumnInfo(name = "id_species")
    val idSpecies: Int,
    
    val temp_min: Double,
    val temp_opt_min: Double,
    val temp_opt_max: Double,
    val temp_max: Double,
    
    val rainfall_min: Double,
    val rainfall_opt_min: Double,
    val rainfall_opt_max: Double,
    val rainfall_max: Double,
    
    val altitude_min: Double,
    val altitude_max: Double,
    
    val points_sampled: Int,
    val points_with_climate: Int,
    val created_at: Long = System.currentTimeMillis()
)

data class NicheResponse(
    val success: Boolean,
    val id_species: Int,
    val operation: String,
    val niche_data: NicheData
)

data class NicheData(
    val id_species: Int,
    val temp_min: Double,
    val temp_opt_min: Double,
    val temp_opt_max: Double,
    val temp_max: Double,
    val rainfall_min: Double,
    val rainfall_opt_min: Double,
    val rainfall_opt_max: Double,
    val rainfall_max: Double,
    val altitude_min: Double,
    val altitude_max: Double,
    val points_sampled: Int,
    val points_with_climate: Int
)

data class CalculateNicheRequest(
    @SerializedName("id_species") val idSpecies: Int,
    @SerializedName("sample_size") val sampleSize: Int = 30
)

// --- Requisitos Climáticos (CRUD legacy) ---
@Entity(tableName = "climate_requirements")
data class ClimateRequirementEntity(
    @PrimaryKey 
    @SerializedName("id_requirement") 
    @ColumnInfo(name = "id_requirement")
    val idRequirement: Int,
    
    @SerializedName("id_species") 
    @ColumnInfo(name = "id_species")
    val idSpecies: Int,
    
    @SerializedName("variable_name") 
    @ColumnInfo(name = "variable_name")
    val variableName: String,
    
    @SerializedName("min_value") 
    @ColumnInfo(name = "min_value")
    val minValue: Double,
    
    @SerializedName("max_value") 
    @ColumnInfo(name = "max_value")
    val maxValue: Double,
    
    val units: String,
    
    @SerializedName("created_at") 
    @ColumnInfo(name = "created_at")
    val createdAt: String?
)
