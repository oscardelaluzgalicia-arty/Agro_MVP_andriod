package com.example.agro.data

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class AgroRepository(private val tokenDao: TokenDao) {

    // Cliente con timeouts extendidos (60 segundos) para todas las peticiones
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiService = Retrofit.Builder()
        .baseUrl("https://agro-f60paf8il-oscardelaluzgalicia-artys-projects.vercel.app/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)

    private val semanticApiService = Retrofit.Builder()
        .baseUrl("https://agro-qc6bigywm-oscardelaluzgalicia-artys-projects.vercel.app/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val crudApiService = Retrofit.Builder()
        .baseUrl("https://agro-q169ig13g-oscardelaluzgalicia-artys-projects.vercel.app/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val climaticApiService = Retrofit.Builder()
        .baseUrl("https://agro-q169ig13g-oscardelaluzgalicia-artys-projects.vercel.app/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val semanticService = semanticApiService.create(SemanticApiService::class.java)
    private val importService = semanticApiService.create(ImportApiService::class.java)
    private val crudService = crudApiService.create(CrudApiService::class.java)
    private val climaticService = climaticApiService.create(ClimaticApiService::class.java)

    suspend fun login(username: String, password: String): Result<LoginResponse> {
        return try {
            val response = apiService.login(LoginRequest(username, password))
            
            val tokenEntity = TokenEntity(token = response.token)
            val moduleEntities = response.modulos.map { 
                ModuleEntity(
                    idModule = it.idModule,
                    name = it.name,
                    description = it.description,
                    accessLevel = it.accessLevel,
                    grantedAt = it.grantedAt
                )
            }
            
            tokenDao.clearAllAndInsert(tokenEntity, moduleEntities)
            
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchAllSpecies(): Result<List<SpeciesEntity>> {
        return try {
            val token = tokenDao.getToken()?.token ?: return Result.failure(Exception("No token"))
            val request = CrudRequest(table = "species")
            val response = crudService.getAllSpecies("Bearer $token", request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveSpeciesLocally(species: List<SpeciesEntity>) {
        tokenDao.insertSpecies(species)
    }

    suspend fun getSavedSpecies(): List<SpeciesEntity> {
        return tokenDao.getSavedSpecies()
    }

    suspend fun fetchClimateRequirements(idSpecies: Int): Result<List<ClimateRequirementEntity>> {
        return try {
            val token = tokenDao.getToken()?.token ?: return Result.failure(Exception("No token"))
            val request = CrudRequest(table = "species_climate_requirements", where = mapOf("id_species" to idSpecies))
            val response = crudService.getClimateRequirements("Bearer $token", request)
            
            tokenDao.deleteAllClimateRequirements()
            tokenDao.insertClimateRequirements(response)
            
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun calculateAndSaveNiche(idSpecies: Int, sampleSize: Int): Result<NicheResponse> {
        return try {
            val token = tokenDao.getToken()?.token ?: return Result.failure(Exception("No token"))
            val response = climaticService.calculateAndSaveNiche("Bearer $token", CalculateNicheRequest(idSpecies, sampleSize))
            
            if (response.success) {
                val nicheEntity = SpeciesNicheEntity(
                    idSpecies = response.niche_data.id_species,
                    temp_min = response.niche_data.temp_min,
                    temp_opt_min = response.niche_data.temp_opt_min,
                    temp_opt_max = response.niche_data.temp_opt_max,
                    temp_max = response.niche_data.temp_max,
                    rainfall_min = response.niche_data.rainfall_min,
                    rainfall_opt_min = response.niche_data.rainfall_opt_min,
                    rainfall_opt_max = response.niche_data.rainfall_opt_max,
                    rainfall_max = response.niche_data.rainfall_max,
                    altitude_min = response.niche_data.altitude_min,
                    altitude_max = response.niche_data.altitude_max,
                    points_sampled = response.niche_data.points_sampled,
                    points_with_climate = response.niche_data.points_with_climate
                )
                tokenDao.insertNiche(nicheEntity)
            }
            
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSavedNiche(idSpecies: Int): SpeciesNicheEntity? {
        return tokenDao.getNicheForSpecies(idSpecies)
    }

    suspend fun resolveCommonName(name: String): Result<ResolveNameResponse> {
        return try {
            val token = tokenDao.getToken()?.token ?: return Result.failure(Exception("No token"))
            val response = semanticService.resolveCommonName("Bearer $token", ResolveNameRequest(name))
            
            tokenDao.insertScientificNames(response.scientificNames)
            
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resolveCommonNameBatch(names: List<String>): Result<List<ResolveNameResponse>> {
        return try {
            val token = tokenDao.getToken()?.token ?: return Result.failure(Exception("No token"))
            val response = semanticService.resolveCommonNameBatch("Bearer $token", ResolveNameBatchRequest(names))
            
            val allScientificNames = response.flatMap { it.scientificNames }
            tokenDao.insertScientificNames(allScientificNames)
            
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveScientificNames(names: List<ScientificNameResponse>) {
        tokenDao.insertScientificNames(names)
    }

    suspend fun import(scientificName: String, state: String, commonName: String): Result<ImportResponse> {
        return try {
            val token = tokenDao.getToken()?.token ?: return Result.failure(Exception("No token"))
            val response = importService.importData("Bearer $token", ImportRequest(name = scientificName, stateProvince = state))
            
            tokenDao.insertSuccessfulImport(
                SuccessfulImportEntity(
                    taxonKey = response.taxonKey,
                    query = response.query,
                    commonName = commonName,
                    idSpecies = response.speciesImport.idSpecies
                )
            )
            
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchOccurrences(idSpecies: Int, stateProvince: String? = null): Result<List<OccurrenceEntity>> {
        return try {
            val token = tokenDao.getToken()?.token ?: return Result.failure(Exception("No token"))
            val whereMap = mutableMapOf<String, Any>("id_species" to idSpecies)
            if (!stateProvince.isNullOrBlank()) {
                whereMap["state_province"] = stateProvince
            }
            val request = CrudRequest(table = "occurrences", where = whereMap)
            val response = crudService.crud("Bearer $token", request)
            
            tokenDao.deleteAllOccurrences()
            tokenDao.insertOccurrences(response)
            
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDistinctStates(): List<String> {
        return tokenDao.getDistinctStates()
    }

    suspend fun getSpeciesIdsByState(state: String): List<Int> {
        return tokenDao.getSpeciesIdsByState(state)
    }

    suspend fun getSavedOccurrences(): List<OccurrenceEntity> {
        return tokenDao.getOccurrences()
    }

    suspend fun getSuccessfulImports(): List<SuccessfulImportEntity> {
        return tokenDao.getSuccessfulImports()
    }

    suspend fun getSavedScientificNames(): List<ScientificNameResponse> {
        return tokenDao.getScientificNames()
    }

    suspend fun getToken(): String? {
        return tokenDao.getToken()?.token
    }

    suspend fun getModules(): List<ModuleEntity> {
        return tokenDao.getModules()
    }

    suspend fun logout() {
        tokenDao.logout()
    }
}
