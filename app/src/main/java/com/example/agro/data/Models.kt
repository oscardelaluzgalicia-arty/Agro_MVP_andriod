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
