package com.example.agro.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AgroRepository(private val tokenDao: TokenDao) {

    private val apiService = Retrofit.Builder()
        .baseUrl("https://agro-f60paf8il-oscardelaluzgalicia-artys-projects.vercel.app/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)

    private val semanticApiService = Retrofit.Builder()
        .baseUrl("https://agro-qc6bigywm-oscardelaluzgalicia-artys-projects.vercel.app/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val crudApiService = Retrofit.Builder()
        .baseUrl("https://agro-nnb2lnjio-oscardelaluzgalicia-artys-projects.vercel.app/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val semanticService = semanticApiService.create(SemanticApiService::class.java)
    private val importService = semanticApiService.create(ImportApiService::class.java)
    private val crudService = crudApiService.create(CrudApiService::class.java)

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

    suspend fun resolveCommonName(name: String): Result<ResolveNameResponse> {
        return try {
            val token = tokenDao.getToken()?.token ?: return Result.failure(Exception("No token"))
            val response = semanticService.resolveCommonName("Bearer $token", ResolveNameRequest(name))
            
            tokenDao.deleteAllScientificNames()
            tokenDao.insertScientificNames(response.scientificNames)
            
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun import(scientificName: String, state: String, commonName: String): Result<ImportResponse> {
        return try {
            val token = tokenDao.getToken()?.token ?: return Result.failure(Exception("No token"))
            val response = importService.importData("Bearer $token", ImportRequest(name = scientificName, stateProvince = state))
            
            if (response.ecologicalZonesImport.occurrencesInserted > 0 || response.ecologicalZonesImport.occurrencesDuplicated > 0) {
                tokenDao.insertSuccessfulImport(
                    SuccessfulImportEntity(
                        taxonKey = response.taxonKey,
                        query = response.query,
                        commonName = commonName,
                        idSpecies = response.speciesImport.idSpecies // Guardar el id_species
                    )
                )
            }
            
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchOccurrences(idSpecies: Int): Result<List<OccurrenceEntity>> {
        return try {
            val token = tokenDao.getToken()?.token ?: return Result.failure(Exception("No token"))
            val request = CrudRequest(where = mapOf("id_species" to idSpecies))
            val response = crudService.crud("Bearer $token", request)
            
            tokenDao.deleteAllOccurrences()
            tokenDao.insertOccurrences(response)
            
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
