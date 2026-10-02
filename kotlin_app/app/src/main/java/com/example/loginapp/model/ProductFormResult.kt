package com.example.loginapp.model

/**
 * Errores de validación LOCAL del formulario de producto.
 * Un campo con valor `null` significa "sin error". Se usa tanto en
 * creación (US06) como en edición (US07), porque piden exactamente
 * las mismas validaciones.
 */
data class ProductFormErrors(
    val title: String? = null,
    val price: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val category: String? = null
) {
    val hasErrors: Boolean
        get() = listOfNotNull(title, price, description, imageUrl, category).isNotEmpty()
}

/**
 * Desenlaces posibles de guardar un producto (crear o editar).
 *
 *  - [ValidationError]: la petición NUNCA llegó a viajar por red
 *    (US06 escenario 2 / US07 nota "validación estrictamente local").
 *  - [Success]: la API respondió bien; el producto trae el ID nuevo
 *    (creación) o los datos actualizados (edición).
 *  - [NetworkError]: fallo de conexión o respuesta no exitosa de la API.
 *  - [Forbidden]: el usuario no tiene rol de Administrador (defensa en
 *    profundidad además de ocultar el botón/pantalla en la View).
 */
sealed class ProductFormResult {
    data class Success(val product: Product) : ProductFormResult()
    data class ValidationError(val errors: ProductFormErrors) : ProductFormResult()
    data class NetworkError(val message: String) : ProductFormResult()
    data object Forbidden : ProductFormResult()
}
