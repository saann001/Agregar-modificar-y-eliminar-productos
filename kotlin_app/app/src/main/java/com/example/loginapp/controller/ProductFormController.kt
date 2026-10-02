package com.example.loginapp.controller

import android.content.Context
import android.util.Patterns
import com.example.loginapp.model.Product
import com.example.loginapp.model.ProductFormErrors
import com.example.loginapp.model.ProductFormResult
import com.example.loginapp.model.Role
import com.example.loginapp.model.dto.ProductWriteRequest
import com.example.loginapp.network.ApiClient
import com.example.loginapp.network.FakeStoreApiService
import com.example.loginapp.util.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Controller del formulario de producto (patrón MVC). Cubre:
 *  - US06: creación (POST /products).
 *  - US07: edición (PUT /products/{id}).
 *
 * Ambas historias piden EXACTAMENTE las mismas validaciones locales,
 * así que viven en un único controller para no duplicar reglas.
 */
class ProductFormController(
    private val appContext: Context,
    private val sessionController: SessionController = SessionController(appContext),
    private val api: FakeStoreApiService = ApiClient.service
) {

    /** Único punto de verdad para saber si el usuario puede gestionar productos. */
    fun canManageProducts(): Boolean =
        sessionController.readSession()?.role == Role.ADMINISTRADOR

    /**
     * Validación 100% local, sin tocar la red (US06 escenario 2 y nota
     * de negocio "Validaciones Frontend"). Se ejecuta ANTES de intentar
     * cualquier petición HTTP.
     */
    fun validate(
        title: String,
        priceText: String,
        description: String,
        imageUrl: String,
        category: String
    ): ProductFormErrors {
        val titleError = if (title.isBlank()) "El título es obligatorio." else null

        val price = priceText.trim().toDoubleOrNull()
        val priceError = when {
            priceText.isBlank() -> "El precio es obligatorio."
            price == null -> "Ingresa solo números (usa punto para decimales)."
            price <= 0.0 -> "El precio debe ser mayor a 0."
            else -> null
        }

        val descriptionError =
            if (description.isBlank()) "La descripción es obligatoria." else null

        val imageError = when {
            imageUrl.isBlank() -> "La URL de la imagen es obligatoria."
            !Patterns.WEB_URL.matcher(imageUrl).matches() -> "Ingresa una URL válida (https://...)."
            else -> null
        }

        val categoryError = if (category.isBlank()) "Selecciona una categoría." else null

        return ProductFormErrors(
            title = titleError,
            price = priceError,
            description = descriptionError,
            imageUrl = imageError,
            category = categoryError
        )
    }

    /** US06 - creación de producto. */
    suspend fun createProduct(
        title: String,
        priceText: String,
        description: String,
        imageUrl: String,
        category: String
    ): ProductFormResult {
        if (!canManageProducts()) return ProductFormResult.Forbidden

        val errors = validate(title, priceText, description, imageUrl, category)
        if (errors.hasErrors) return ProductFormResult.ValidationError(errors)

        if (!NetworkUtils.hasInternetConnection(appContext)) {
            return ProductFormResult.NetworkError(
                "No hay conexión a internet. Verifica tu red e inténtalo de nuevo."
            )
        }

        val request = ProductWriteRequest(
            title = title.trim(),
            price = priceText.trim().toDouble(),
            description = description.trim(),
            image = imageUrl.trim(),
            category = category
        )

        return withContext(Dispatchers.IO) {
            try {
                val response = api.createProduct(request)
                val dto = response.body()
                if (response.isSuccessful && dto != null) {
                    ProductFormResult.Success(Product.fromDto(dto))
                } else {
                    ProductFormResult.NetworkError(
                        "No se pudo crear el producto (${response.code()})."
                    )
                }
            } catch (e: Exception) {
                ProductFormResult.NetworkError(
                    e.message ?: "Ocurrió un problema al crear el producto."
                )
            }
        }
    }

    /** US07 - edición de producto existente. */
    suspend fun updateProduct(
        productId: Int,
        title: String,
        priceText: String,
        description: String,
        imageUrl: String,
        category: String
    ): ProductFormResult {
        if (!canManageProducts()) return ProductFormResult.Forbidden

        val errors = validate(title, priceText, description, imageUrl, category)
        if (errors.hasErrors) return ProductFormResult.ValidationError(errors)

        if (!NetworkUtils.hasInternetConnection(appContext)) {
            return ProductFormResult.NetworkError(
                "No hay conexión a internet. Verifica tu red e inténtalo de nuevo."
            )
        }

        val request = ProductWriteRequest(
            title = title.trim(),
            price = priceText.trim().toDouble(),
            description = description.trim(),
            image = imageUrl.trim(),
            category = category
        )

        return withContext(Dispatchers.IO) {
            try {
                val response = api.updateProduct(productId, request)
                val dto = response.body()
                if (response.isSuccessful && dto != null) {
                    // Fake Store API a veces no devuelve el id en el PUT;
                    // lo forzamos al id original para no perderlo en pantalla.
                    ProductFormResult.Success(Product.fromDto(dto).copy(id = productId))
                } else {
                    ProductFormResult.NetworkError(
                        "No se pudo actualizar el producto (${response.code()})."
                    )
                }
            } catch (e: Exception) {
                ProductFormResult.NetworkError(
                    e.message ?: "Ocurrió un problema al actualizar el producto."
                )
            }
        }
    }
}
