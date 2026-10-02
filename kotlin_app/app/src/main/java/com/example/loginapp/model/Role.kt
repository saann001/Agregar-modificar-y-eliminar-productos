package com.example.loginapp.model

/**
 * Roles soportados por la aplicación.
 *
 * Regla de negocio (US01):
 *  - id 1 y 2  -> ADMINISTRADOR
 *  - id 3      -> AUDITOR
 *  - resto     -> CLIENTE
 */
enum class Role(val displayName: String) {
    ADMINISTRADOR("Administrador"),
    AUDITOR("Auditor"),
    CLIENTE("Cliente");

    companion object {
        /** Única fuente de verdad para el mapeo local de roles. */
        fun fromUserId(id: Int): Role = when (id) {
            1, 2 -> ADMINISTRADOR
            3 -> AUDITOR
            else -> CLIENTE
        }

        fun fromStorageValue(value: String?): Role =
            entries.firstOrNull { it.name == value } ?: CLIENTE
    }
}
