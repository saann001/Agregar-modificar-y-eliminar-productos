package com.example.loginapp.controller

import android.content.Context
import com.example.loginapp.R
import com.example.loginapp.model.Product
import com.example.loginapp.model.UiState
import com.example.loginapp.network.ApiClient
import com.example.loginapp.network.FakeStoreApiService
import com.example.loginapp.util.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Controller del catálogo (patrón MVC). Cubre:
 *  - US03: descarga del catálogo general desde GET /products.
 *  - US04: descarga de categorías y filtrado por categoría.
 *
 * La Activity no sabe nada de Retrofit: solo llama a estos métodos
 * y recibe un [UiState] (Loading / Success / Error) que sabe pintar.
 */
class CatalogController(
    private val appContext: Context,
    private val api: FakeStoreApiService = ApiClient.service
) {

    /**
     * Categoría actualmente seleccionada.
     * `null` significa "Ver todos" (sin filtro) — US04 escenario 3.
     */
    var selectedCategory: String? = null
        private set

    /** Caché en memoria de los productos que se están mostrando. */
    private val currentProducts = mutableListOf<Product>()

    fun products(): List<Product> = currentProducts.toList()

    /**
     * US04 - Escenario 1: obtiene las categorías disponibles al
     * inicializar la vista del catálogo (GET /products/categories).
     */
    suspend fun loadCategories(): UiState<List<String>> {
        if (!NetworkUtils.hasInternetConnection(appContext)) {
            return UiState.Error(appContext.getString(R.string.catalog_error_no_connection))
        }
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getCategories()
                if (response.isSuccessful) {
                    UiState.Success(response.body().orEmpty())
                } else {
                    UiState.Error(appContext.getString(R.string.catalog_error_generic))
                }
            } catch (e: Exception) {
                UiState.Error(appContext.getString(R.string.catalog_error_generic))
            }
        }
    }

    /**
     * Carga el catálogo. Si [category] es null consume GET /products
     * (US03); si trae valor consume GET /products/category/{category}
     * (US04 escenario 2).
     *
     * Nota de negocio US04 - "Gestión de memoria": antes de lanzar la
     * nueva petición se limpia el arreglo anterior, para que no se
     * muestren datos de la categoría previa mientras carga la nueva.
     */
    suspend fun loadProducts(category: String? = null): UiState<List<Product>> {
        selectedCategory = category
        currentProducts.clear()

        if (!NetworkUtils.hasInternetConnection(appContext)) {
            return UiState.Error(appContext.getString(R.string.catalog_error_no_connection))
        }

        return withContext(Dispatchers.IO) {
            try {
                val response = if (category.isNullOrBlank()) {
                    api.getProducts()
                } else {
                    api.getProductsByCategory(category)
                }

                if (!response.isSuccessful) {
                    return@withContext UiState.Error(appContext.getString(R.string.catalog_error_generic))
                }

                val products = response.body().orEmpty().map { Product.fromDto(it) }
                currentProducts.addAll(products)

                if (products.isEmpty()) {
                    UiState.Error(appContext.getString(R.string.catalog_empty))
                } else {
                    UiState.Success(products)
                }
            } catch (e: Exception) {
                UiState.Error(appContext.getString(R.string.catalog_error_generic))
            }
        }
    }

    /** US04 - Escenario 3: quita el filtro y recarga el catálogo completo. */
    suspend fun clearFilter(): UiState<List<Product>> = loadProducts(null)
}
