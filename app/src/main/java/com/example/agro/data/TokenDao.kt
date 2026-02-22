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
    }
}
