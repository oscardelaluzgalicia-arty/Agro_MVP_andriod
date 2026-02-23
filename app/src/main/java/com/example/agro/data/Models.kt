package com.example.agro.data

import com.google.gson.annotations.SerializedName
import androidx.room.Entity
import androidx.room.PrimaryKey

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

// Nuevos modelos para Búsqueda Semántica e Importación
data class ResolveNameRequest(
    val name: String
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

data class ImportRequest(
    val name: String,
    val country: String = "Mexico",
    @SerializedName("state_province") val stateProvince: String
)

// Modelos para la respuesta de importación detallada
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

// Entidad para guardar importaciones exitosas
@Entity(tableName = "successful_imports")
data class SuccessfulImportEntity(
    @PrimaryKey val taxonKey: Long,
    val query: String,
    val commonName: String,
    val idSpecies: Int // Añadido idSpecies
)

// Nuevos modelos para CRUD de ocurrencias
data class CrudRequest(
    val action: String = "read",
    val table: String = "occurrences",
    val where: Map<String, Int>
)

@Entity(tableName = "occurrences")
data class OccurrenceEntity(
    @PrimaryKey @SerializedName("id_occurrence") val idOccurrence: Int,
    @SerializedName("gbif_occurrence_id") val gbifOccurrenceId: Long,
    @SerializedName("id_species") val idSpecies: Int,
    @SerializedName("decimal_latitude") val latitude: Double,
    @SerializedName("decimal_longitude") val longitude: Double,
    @SerializedName("coordinate_uncertainty_meters") val uncertainty: Double?,
    val country: String,
    @SerializedName("state_province") val stateProvince: String?,
    val municipality: String?,
    val locality: String?,
    @SerializedName("event_date") val eventDate: String?,
    val year: Int?,
    val month: Int?,
    val day: Int?,
    val habitat: String?,
    val elevation: Double?,
    @SerializedName("basis_of_record") val basisOfRecord: String?,
    @SerializedName("dataset_key") val datasetKey: String?,
    @SerializedName("institution_code") val institutionCode: String?,
    @SerializedName("recorded_by") val recordedBy: String?,
    @SerializedName("identified_by") val identifiedBy: String?,
    @SerializedName("created_at") val createdAt: String?
)
