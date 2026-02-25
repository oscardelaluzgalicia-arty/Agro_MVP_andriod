package com.example.agro.data

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {
    @POST("login")
    suspend fun login(@Body request: LoginRequest): LoginResponse
}

interface SemanticApiService {
    @POST("api/v1/semantic/resolve-common-name")
    suspend fun resolveCommonName(
        @Header("Authorization") token: String,
        @Body request: ResolveNameRequest
    ): ResolveNameResponse

    @POST("api/v1/semantic/resolve-common-name-batch")
    suspend fun resolveCommonNameBatch(
        @Header("Authorization") token: String,
        @Body request: ResolveNameBatchRequest
    ): List<ResolveNameResponse>
}

interface ImportApiService {
    @POST("api/v1/gbif/import")
    suspend fun importData(
        @Header("Authorization") token: String,
        @Body request: ImportRequest
    ): ImportResponse
}

interface CrudApiService {
    @POST("api/v1/crud")
    suspend fun crud(
        @Header("Authorization") token: String,
        @Body request: CrudRequest
    ): List<OccurrenceEntity>

    @POST("api/v1/crud")
    suspend fun getClimateRequirements(
        @Header("Authorization") token: String,
        @Body request: CrudRequest
    ): List<ClimateRequirementEntity>

    @POST("api/v1/crud")
    suspend fun getAllSpecies(
        @Header("Authorization") token: String,
        @Body request: CrudRequest
    ): List<SpeciesEntity>
}

interface ClimaticApiService {
    @POST("api/v1/climatic/calculate-and-save")
    suspend fun calculateAndSaveNiche(
        @Header("Authorization") token: String,
        @Body request: CalculateNicheRequest
    ): NicheResponse
}
