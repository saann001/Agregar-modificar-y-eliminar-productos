package com.example.loginapp.controller

import android.content.Context
import com.example.loginapp.R
import com.example.loginapp.model.LoginResult
import com.example.loginapp.model.UserModel
import com.example.loginapp.model.dto.LoginRequest
import com.example.loginapp.network.ApiClient
import com.example.loginapp.network.FakeStoreApiService
import com.example.loginapp.util.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Controller de la pantalla de Login (patrón MVC).
 *
 * Orquesta: 1) verificación de red, 2) POST /auth/login,
 * 3) verificación del usuario contra GET /users, 4) mapeo local de rol.
 * La Activity solo llama a [login] y reacciona al [LoginResult]; no
 * conoce detalles de Retrofit ni de HTTP.
 */
class LoginController(
    private val appContext: Context,
    private val api: FakeStoreApiService = ApiClient.service
) {

    suspend fun login(username: String, password: String): LoginResult {
        if (username.isBlank() || password.isBlank()) {
            return LoginResult.UnknownError(appContext.getString(R.string.login_error_empty))
        }

        // Escenario 3 (US01): sin conexión, se detiene ANTES de llamar a la API.
        if (!NetworkUtils.hasInternetConnection(appContext)) {
            return LoginResult.NoConnection
        }

        return withContext(Dispatchers.IO) {
            try {
                // 1) Autenticación real contra /auth/login
                val loginResponse = api.login(LoginRequest(username, password))

                if (loginResponse.code() == 401 || loginResponse.code() == 400) {
                    return@withContext LoginResult.InvalidCredentials
                }
                if (!loginResponse.isSuccessful) {
                    return@withContext LoginResult.UnknownError(
                        "Error del servidor (${loginResponse.code()})."
                    )
                }
                val token = loginResponse.body()?.token
                    ?: return@withContext LoginResult.UnknownError("La API no devolvió un token válido.")

                // 2) Verificación/descarga de datos del usuario contra /users
                val usersResponse = api.getUsers()
                if (!usersResponse.isSuccessful) {
                    return@withContext LoginResult.UnknownError(
                        "No se pudo obtener la lista de usuarios (${usersResponse.code()})."
                    )
                }
                val apiUser = usersResponse.body()?.firstOrNull {
                    it.username?.equals(username, ignoreCase = true) == true
                } ?: return@withContext LoginResult.InvalidCredentials

                // 3) Mapeo local de rol + construcción del modelo de dominio
                val user = UserModel.fromApi(apiUser, token)
                LoginResult.Success(user)
            } catch (e: Exception) {
                LoginResult.UnknownError(e.message ?: "Error desconocido")
            }
        }
    }
}
