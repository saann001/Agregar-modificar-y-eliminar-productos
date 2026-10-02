package com.example.loginapp.controller

import android.content.Context
import com.example.loginapp.R
import com.example.loginapp.model.Product
import com.example.loginapp.model.Role
import com.example.loginapp.model.UiState
import com.example.loginapp.network.ApiClient
import com.example.loginapp.network.FakeStoreApiService
import com.example.loginapp.util.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Controller de la vista de detalle (US05).
 *
 * Además de descargar GET /products/{id}, decide si el usuario tiene
 * permisos de gestión. Regla de negocio de US05: el rol se lee
 * ESTRICTAMENTE de la sesión local ([SessionController]), nunca se le
 * pregunta a Fake Store API.
 */
class ProductDetailController(
    private val appContext: Context,
    private val sessionController: SessionController = SessionController(appContext),
    private val api: FakeStoreApiService = ApiClient.service
) {

    /**
     * True solo si el rol guardado localmente es ADMINISTRADOR.
     * La View usa esto para decidir si siquiera INSTANCIA los botones
     * de editar/eliminar (no los crea ocultos — nota de negocio US05).
     */
    fun canManageProducts(): Boolean {
        val user = sessionController.readSession() ?: return false
        return user.role == Role.ADMINISTRADOR
    }

    fun currentRole(): Role? = sessionController.readSession()?.role

    /**
     * US05 - Escenarios 1 y 3: descarga el detalle del producto.
     * Si la API falla o el producto no existe, devuelve [UiState.Error]
     * y la View mostrará "Producto no disponible" y volverá al catálogo.
     */
    suspend fun loadProduct(productId: Int): UiState<Product> {
        if (!NetworkUtils.hasInternetConnection(appContext)) {
            return UiState.Error(appContext.getString(R.string.detail_unavailable_message))
        }

        return withContext(Dispatchers.IO) {
            try {
                val response = api.getProductById(productId)

                if (!response.isSuccessful) {
                    return@withContext UiState.Error(appContext.getString(R.string.detail_unavailable_message))
                }

                val dto = response.body()
                // Fake Store API puede responder 200 con cuerpo vacío/null
                // cuando el id no existe, por eso también se valida aquí.
                if (dto == null || dto.id == 0) {
                    return@withContext UiState.Error(appContext.getString(R.string.detail_unavailable_message))
                }

                UiState.Success(Product.fromDto(dto))
            } catch (e: Exception) {
                UiState.Error(appContext.getString(R.string.detail_unavailable_message))
            }
        }
    }

    /**
     * Elimina un producto (solo Administrador). Se revalida el permiso
     * aquí en el Controller y no solo en la UI, para que ocultar un
     * botón no sea la única barrera de seguridad.
     */
    suspend fun deleteProduct(productId: Int): UiState<Product> {
        if (!canManageProducts()) {
            return UiState.Error(appContext.getString(R.string.detail_no_permission))
        }

        return withContext(Dispatchers.IO) {
            try {
                val response = api.deleteProduct(productId)
                val dto = response.body()
                if (response.isSuccessful && dto != null) {
                    UiState.Success(Product.fromDto(dto))
                } else {
                    UiState.Error(appContext.getString(R.string.catalog_error_generic))
                }
            } catch (e: Exception) {
                UiState.Error(appContext.getString(R.string.catalog_error_generic))
            }
        }
    }
}
