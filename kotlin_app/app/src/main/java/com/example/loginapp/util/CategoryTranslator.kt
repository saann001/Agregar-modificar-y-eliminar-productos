package com.example.loginapp.util

import android.content.Context
import com.example.loginapp.R

/**
 * Fake Store API devuelve las categorías en inglés
 * ("electronics", "jewelery", "men's clothing", "women's clothing").
 *
 * Esta clase traduce SOLO la etiqueta que ve el usuario. El valor
 * original en inglés se conserva intacto porque es el que viaja en la
 * URL de GET /products/category/{category}; si tradujéramos el valor,
 * la petición fallaría.
 */
object CategoryTranslator {

    /** Devuelve el nombre en español para mostrar en pantalla. */
    fun displayName(context: Context, apiCategory: String): String {
        return when (apiCategory.trim().lowercase()) {
            "electronics" -> context.getString(R.string.category_electronics)
            "jewelery", "jewellery" -> context.getString(R.string.category_jewelery)
            "men's clothing", "mens clothing" ->
                context.getString(R.string.category_mens_clothing)
            "women's clothing", "womens clothing" ->
                context.getString(R.string.category_womens_clothing)
            // Si la API agrega una categoría nueva, al menos se muestra
            // capitalizada en vez de romper la pantalla.
            else -> apiCategory.replaceFirstChar { it.uppercase() }
        }
    }
}
