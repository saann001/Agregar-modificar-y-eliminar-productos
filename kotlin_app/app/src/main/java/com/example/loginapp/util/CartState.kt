package com.example.loginapp.util

/**
 * Ejemplo de estado global (singleton) que la US02 exige resetear a cero
 * durante el cierre de sesión, para que el siguiente usuario no vea
 * productos del usuario anterior.
 */
object CartState {
    private val items = mutableListOf<String>()

    fun addItem(item: String) = items.add(item)

    fun currentItems(): List<String> = items.toList()

    fun reset() = items.clear()
}
