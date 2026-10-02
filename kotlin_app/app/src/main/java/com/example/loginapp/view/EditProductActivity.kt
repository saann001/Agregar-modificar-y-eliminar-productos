package com.example.loginapp.view

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import com.example.loginapp.R
import com.example.loginapp.model.ProductFormResult

/**
 * View de edición de producto (US07).
 *
 * Recibe los datos actuales del producto por Intent (los mismos que ya
 * tenía cargados [ProductDetailActivity]), en vez de volver a pedirlos
 * a la API: así se cumple el escenario 2 ("todos los campos aparecen
 * pre-cargados") sin depender de una segunda llamada de red.
 */
class EditProductActivity : ProductFormActivity() {

    private var productId: Int = 0

    override val screenTitleRes = R.string.form_edit_title
    override val saveButtonTextRes = R.string.form_save_edit

    override fun initialCategoryApiValue(): String? = intent.getStringExtra(EXTRA_CATEGORY)

    override fun prefill() {
        productId = intent.getIntExtra(EXTRA_ID, 0)
        binding.editTitle.setText(intent.getStringExtra(EXTRA_TITLE))
        binding.editDescription.setText(intent.getStringExtra(EXTRA_DESCRIPTION))
        binding.editImageUrl.setText(intent.getStringExtra(EXTRA_IMAGE))

        val price = intent.getDoubleExtra(EXTRA_PRICE, 0.0)
        // Evita mostrar "109.95000000001" por precisión de Double.
        val priceText = if (price == price.toLong().toDouble()) {
            price.toLong().toString()
        } else {
            price.toString()
        }
        binding.editPrice.setText(priceText)
    }

    override suspend fun performSave(
        title: String,
        priceText: String,
        description: String,
        imageUrl: String,
        category: String
    ): ProductFormResult {
        return formController.updateProduct(productId, title, priceText, description, imageUrl, category)
    }

    /**
     * US07 - Escenario 1: mensaje "Producto actualizado (Simulación)",
     * cierra el formulario y devuelve los datos actualizados para que
     * [ProductDetailActivity] los pinte sin necesidad de volver a
     * llamar a GET /products/{id} (que, por la nota de negocio, no
     * reflejaría el cambio de todas formas).
     */
    override fun onSaveSuccess(result: ProductFormResult.Success) {
        Toast.makeText(this, R.string.form_edit_success, Toast.LENGTH_SHORT).show()

        val resultIntent = Intent().apply {
            putExtra(EXTRA_ID, result.product.id)
            putExtra(EXTRA_TITLE, result.product.title)
            putExtra(EXTRA_PRICE, result.product.price)
            putExtra(EXTRA_DESCRIPTION, result.product.description)
            putExtra(EXTRA_IMAGE, result.product.imageUrl)
            putExtra(EXTRA_CATEGORY, result.product.category)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    companion object {
        const val EXTRA_ID = "extra_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_PRICE = "extra_price"
        const val EXTRA_DESCRIPTION = "extra_description"
        const val EXTRA_IMAGE = "extra_image"
        const val EXTRA_CATEGORY = "extra_category"
    }
}
