package com.example.loginapp.model.dto

/** Cuerpo enviado a POST /auth/login. */
data class LoginRequest(
    val username: String,
    val password: String
)

/** Respuesta de POST /auth/login. */
data class LoginResponse(
    val token: String
)

/** Representa un registro de GET /users tal como lo expone Fake Store API. */
data class ApiUser(
    val id: Int,
    val email: String? = null,
    val username: String? = null,
    val password: String? = null,
    val name: ApiUserName? = null
)

data class ApiUserName(
    val firstname: String? = null,
    val lastname: String? = null
)
