package com.example.agro.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AgroRepository(private val tokenDao: TokenDao) {

    private val apiService = Retrofit.Builder()
        .baseUrl("https://agro-c2g6z3l4e-oscardelaluzgalicia-artys-projects.vercel.app/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)

    suspend fun login(username: String, password: String): Result<String> {
        return try {
            val response = apiService.login(LoginRequest(username, password))
            tokenDao.insertToken(TokenEntity(token = response.token))
            Result.success(response.token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getToken(): String? {
        return tokenDao.getToken()?.token
    }

    suspend fun logout() {
        tokenDao.deleteToken()
    }
}
