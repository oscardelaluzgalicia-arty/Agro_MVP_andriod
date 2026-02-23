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
}
