package com.example.loginapp.model

/**
 * Representa los desenlaces posibles del intento de login,
 * cubriendo los 3 escenarios de aceptación de US01.
 */
sealed class LoginResult {
    data class Success(val user: UserModel) : LoginResult()
    data object InvalidCredentials : LoginResult()
    data object NoConnection : LoginResult()
    data class UnknownError(val detail: String) : LoginResult()
}
