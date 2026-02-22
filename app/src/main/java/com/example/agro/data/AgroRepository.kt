package com.example.agro.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AgroRepository(private val tokenDao: TokenDao) {

    private val apiService = Retrofit.Builder()
        .baseUrl("https://agro-c2g6z3l4e-oscardelaluzgalicia-artys-projects.vercel.app/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)

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
