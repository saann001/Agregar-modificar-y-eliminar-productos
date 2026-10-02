package com.example.loginapp.model

/**
 * Estado genérico de una pantalla que depende de una petición de red.
 *
 * Cubre los escenarios 2 y 3 de US03:
 *  - [Loading] -> se muestra el spinner centrado.
 *  - [Success] -> se pinta la lista/grid de productos.
 *  - [Error]   -> se oculta el spinner y se muestra el mensaje + botón "Reintentar".
 *
 * Es genérico (<T>) para poder reutilizarlo tanto en el catálogo
 * (List<Product>) como en el detalle (Product).
 */
sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
