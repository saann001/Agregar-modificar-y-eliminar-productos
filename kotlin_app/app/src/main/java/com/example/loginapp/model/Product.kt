package com.example.loginapp.model

import com.example.loginapp.model.dto.ProductDto
import java.text.NumberFormat
import java.util.Locale

/**
 * Entidad de dominio (Model, POO) que representa un producto ya
 * "limpio" para la app: sin nulls y con el precio ya formateado.
 * La View nunca toca el DTO crudo, solo este objeto.
 */
data class Product(
    val id: Int,
    val title: String,
    val price: Double,
    val description: String,
    val category: String,
    val imageUrl: String,
    val ratingRate: Double,
    val ratingCount: Int
) {
    /** Precio ya formateado como moneda, listo para pintar en pantalla. */
    val formattedPrice: String
        get() = NumberFormat.getCurrencyInstance(Locale.US).format(price)

    companion object {
        fun fromDto(dto: ProductDto): Product = Product(
            id = dto.id,
            title = dto.title.orEmpty(),
            price = dto.price ?: 0.0,
            description = dto.description.orEmpty(),
            category = dto.category.orEmpty(),
            imageUrl = dto.image.orEmpty(),
            ratingRate = dto.rating?.rate ?: 0.0,
            ratingCount = dto.rating?.count ?: 0
        )
    }
}
