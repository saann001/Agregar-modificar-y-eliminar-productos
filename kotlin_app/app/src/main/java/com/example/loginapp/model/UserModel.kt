package com.example.loginapp.model

import com.example.loginapp.model.dto.ApiUser

/**
 * Entidad de dominio (Model, POO) que representa al usuario autenticado.
 * No conoce nada de Retrofit, Activities ni almacenamiento.
 */
data class UserModel(
    val id: Int,
    val username: String,
    val email: String,
    val role: Role,
    val token: String
) {
    companion object {
        /**
         * Construye el modelo combinando el usuario obtenido de /users
         * con el token de /auth/login, aplicando el mapeo local de rol.
         */
        fun fromApi(apiUser: ApiUser, token: String): UserModel {
            return UserModel(
                id = apiUser.id,
                username = apiUser.username.orEmpty(),
                email = apiUser.email.orEmpty(),
                role = Role.fromUserId(apiUser.id),
                token = token
            )
        }
    }
}
