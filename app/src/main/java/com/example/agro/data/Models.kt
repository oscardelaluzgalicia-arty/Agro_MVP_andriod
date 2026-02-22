package com.example.agro.data

import com.google.gson.annotations.SerializedName
import androidx.room.Entity
import androidx.room.PrimaryKey

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val token: String
)

@Entity(tableName = "auth_token")
data class TokenEntity(
    @PrimaryKey val id: Int = 0,
    val token: String
)
