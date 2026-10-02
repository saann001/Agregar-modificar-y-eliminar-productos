package com.example.loginapp.network

import com.example.loginapp.model.dto.ApiUser
import com.example.loginapp.model.dto.LoginRequest
import com.example.loginapp.model.dto.LoginResponse
import com.example.loginapp.model.dto.ProductDto
import com.example.loginapp.model.dto.ProductWriteRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface FakeStoreApiService {

    // ---------- US01: autenticación ----------

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("users")
    suspend fun getUsers(): Response<List<ApiUser>>

    // ---------- US03: catálogo general ----------

    /** GET /products -> catálogo completo. */
    @GET("products")
    suspend fun getProducts(): Response<List<ProductDto>>

    // ---------- US04: filtrado por categoría ----------

    /** GET /products/categories -> lista de categorías disponibles. */
    @GET("products/categories")
    suspend fun getCategories(): Response<List<String>>

    /** GET /products/category/{category} -> productos de una categoría. */
    @GET("products/category/{category}")
    suspend fun getProductsByCategory(
        @Path("category") category: String
    ): Response<List<ProductDto>>

    // ---------- US05: detalle del producto ----------

    /** GET /products/{id} -> detalle de un producto. */
    @GET("products/{id}")
    suspend fun getProductById(@Path("id") id: Int): Response<ProductDto>

    /** DELETE /products/{id} -> solo disponible para el rol Administrador. */
    @DELETE("products/{id}")
    suspend fun deleteProduct(@Path("id") id: Int): Response<ProductDto>

    // ---------- US06: alta de producto ----------

    /**
     * POST /products -> crea un producto. Fake Store API devuelve el
     * objeto con un nuevo ID, pero NO lo persiste: GET /products no lo
     * mostrará después (nota de negocio de US06).
     */
    @POST("products")
    suspend fun createProduct(@Body request: ProductWriteRequest): Response<ProductDto>

    // ---------- US07: edición de producto ----------

    /**
     * PUT /products/{id} -> actualiza un producto. Fake Store API
     * responde 200 con el objeto actualizado, pero no persiste el
     * cambio en su servidor (nota de negocio de US07).
     */
    @PUT("products/{id}")
    suspend fun updateProduct(
        @Path("id") id: Int,
        @Body request: ProductWriteRequest
    ): Response<ProductDto>
}
