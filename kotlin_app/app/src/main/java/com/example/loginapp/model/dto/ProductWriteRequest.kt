package com.example.loginapp.model.dto

import com.google.gson.annotations.SerializedName

/**
 * Cuerpo que se envía en POST /products (US06) y PUT /products/{id}
 * (US07). Es distinto de [ProductDto] porque este NO lleva id (la API
 * lo asigna) ni rating (no aplica al crear/editar).
 */
data class ProductWriteRequest(
    @SerializedName("title") val title: String,
    @SerializedName("price") val price: Double,
    @SerializedName("description") val description: String,
    @SerializedName("image") val image: String,
    @SerializedName("category") val category: String
)
