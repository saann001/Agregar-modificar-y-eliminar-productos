package com.example.loginapp.model.dto

import com.google.gson.annotations.SerializedName

/**
 * Mapea EXACTAMENTE la estructura que devuelve Fake Store API en
 * /products, /products/category/{c} y /products/{id} (US03 - nota de
 * negocio "Mapeo del JSON").
 *
 * Ejemplo de respuesta:
 * {
 *   "id": 1,
 *   "title": "Fjallraven - Foldsack No. 1 Backpack",
 *   "price": 109.95,
 *   "description": "Your perfect pack for everyday use...",
 *   "category": "men's clothing",
 *   "image": "https://fakestoreapi.com/img/81fPKd-2AYL._AC_SL1500_.jpg",
 *   "rating": { "rate": 3.9, "count": 120 }
 * }
 */
data class ProductDto(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String? = null,
    @SerializedName("price") val price: Double? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("image") val image: String? = null,
    @SerializedName("rating") val rating: RatingDto? = null
)

data class RatingDto(
    @SerializedName("rate") val rate: Double? = null,
    @SerializedName("count") val count: Int? = null
)
