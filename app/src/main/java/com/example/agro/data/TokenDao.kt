package com.example.agro.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface TokenDao {
    @Query("SELECT * FROM auth_token WHERE id = 0")
    suspend fun getToken(): TokenEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertToken(token: TokenEntity)

    @Query("DELETE FROM auth_token")
    suspend fun deleteToken()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModules(modules: List<ModuleEntity>)

    @Query("SELECT * FROM user_modules")
    suspend fun getModules(): List<ModuleEntity>

    @Query("DELETE FROM user_modules")
    suspend fun deleteModules()

    @Transaction
    suspend fun clearAllAndInsert(token: TokenEntity, modules: List<ModuleEntity>) {
        deleteToken()
        deleteModules()
        insertToken(token)
        insertModules(modules)
    }

    @Transaction
    suspend fun logout() {
        deleteToken()
        deleteModules()
        deleteAllScientificNames()
        deleteAllSuccessfulImports()
        deleteAllOccurrences()
        deleteAllClimateRequirements()
        deleteAllSpecies()
        deleteAllNiches()
    }

    // Para nombres científicos
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScientificNames(names: List<ScientificNameResponse>)

    @Query("SELECT * FROM scientific_names")
    suspend fun getScientificNames(): List<ScientificNameResponse>

    @Query("DELETE FROM scientific_names")
    suspend fun deleteAllScientificNames()

    // Para importaciones exitosas
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuccessfulImport(successfulImport: SuccessfulImportEntity)

    @Query("SELECT * FROM successful_imports")
    suspend fun getSuccessfulImports(): List<SuccessfulImportEntity>

    @Query("DELETE FROM successful_imports")
    suspend fun deleteAllSuccessfulImports()

    // Para ocurrencias
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOccurrences(occurrences: List<OccurrenceEntity>)

    @Query("SELECT * FROM occurrences")
    suspend fun getOccurrences(): List<OccurrenceEntity>

    @Query("DELETE FROM occurrences")
    suspend fun deleteAllOccurrences()

    @Query("SELECT DISTINCT state_province FROM occurrences WHERE state_province IS NOT NULL AND state_province != ''")
    suspend fun getDistinctStates(): List<String>

    @Query("SELECT DISTINCT id_species FROM occurrences WHERE state_province = :state")
    suspend fun getSpeciesIdsByState(state: String): List<Int>

    // Para requisitos climáticos (CRUD)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClimateRequirements(requirements: List<ClimateRequirementEntity>)

    @Query("SELECT * FROM climate_requirements")
    suspend fun getClimateRequirements(): List<ClimateRequirementEntity>

    @Query("DELETE FROM climate_requirements")
    suspend fun deleteAllClimateRequirements()

    // Para Especies
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpecies(species: List<SpeciesEntity>)

    @Query("SELECT * FROM species")
    suspend fun getSavedSpecies(): List<SpeciesEntity>

    @Query("DELETE FROM species")
    suspend fun deleteAllSpecies()

    // Para Nichos Climáticos (Calculados)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNiche(niche: SpeciesNicheEntity)

    @Query("SELECT * FROM species_niches WHERE id_species = :idSpecies")
    suspend fun getNicheForSpecies(idSpecies: Int): SpeciesNicheEntity?

    @Query("DELETE FROM species_niches")
    suspend fun deleteAllNiches()
}
