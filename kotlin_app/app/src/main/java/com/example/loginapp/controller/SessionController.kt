package com.example.loginapp.controller

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.loginapp.model.Role
import com.example.loginapp.model.UserModel
import com.example.loginapp.util.CartState

/**
 * Controller de sesión (patrón MVC): responsable de persistir, leer y
 * borrar la sesión del usuario usando EncryptedSharedPreferences
 * (almacenamiento seguro nativo de Android, equivalente a Keychain/
 * flutter_secure_storage), y de orquestar el reinicio de estados
 * globales en el logout (US02).
 */
class SessionController(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context.applicationContext,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveSession(user: UserModel) {
        prefs.edit()
            .putInt(KEY_ID, user.id)
            .putString(KEY_USERNAME, user.username)
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_ROLE, user.role.name)
            .putString(KEY_TOKEN, user.token)
            .apply()
    }

    fun readSession(): UserModel? {
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        return UserModel(
            id = prefs.getInt(KEY_ID, 0),
            username = prefs.getString(KEY_USERNAME, "") ?: "",
            email = prefs.getString(KEY_EMAIL, "") ?: "",
            role = Role.fromStorageValue(prefs.getString(KEY_ROLE, null)),
            token = token
        )
    }

    fun isAuthenticated(): Boolean = readSession() != null

    /**
     * Limpieza profunda exigida por US02:
     *  1. Borra el token/rol/id del almacenamiento persistente del
     *     dispositivo (no solo variables en memoria).
     *  2. Resetea cualquier estado global relacionado al usuario
     *     (carrito de compras).
     */
    fun logout() {
        prefs.edit().clear().apply()
        CartState.reset()
    }

    companion object {
        private const val PREFS_FILE_NAME = "secure_session_prefs"
        private const val KEY_ID = "session_id"
        private const val KEY_USERNAME = "session_username"
        private const val KEY_EMAIL = "session_email"
        private const val KEY_ROLE = "session_role"
        private const val KEY_TOKEN = "session_token"
    }
}
