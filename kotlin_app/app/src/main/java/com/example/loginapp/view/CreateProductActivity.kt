package com.example.loginapp.view

import androidx.appcompat.app.AlertDialog
import com.example.loginapp.R
import com.example.loginapp.model.ProductFormResult

/**
 * View de creación de producto (US06). Toda la lógica común de
 * formulario vive en [ProductFormActivity]; aquí solo se define QUÉ
 * endpoint se llama y QUÉ pasa tras un guardado exitoso.
 */
class CreateProductActivity : ProductFormActivity() {

    override val screenTitleRes = R.string.form_create_title
    override val saveButtonTextRes = R.string.form_save_create

    override suspend fun performSave(
        title: String,
        priceText: String,
        description: String,
        imageUrl: String,
        category: String
    ): ProductFormResult {
        return formController.createProduct(title, priceText, description, imageUrl, category)
    }

    /**
     * US06 - Escenario 1: alerta de confirmación con el nuevo ID
     * generado + el formulario se limpia. Se deja al Administrador
     * decidir si cierra la pantalla o sigue registrando otro producto.
     */
    override fun onSaveSuccess(result: ProductFormResult.Success) {
        AlertDialog.Builder(this)
            .setTitle(R.string.form_create_success_title)
            .setMessage(getString(R.string.form_create_success_message, result.product.id))
            .setCancelable(false)
            .setPositiveButton(R.string.form_continue_editing) { _, _ -> clearForm() }
            .setNegativeButton(R.string.form_close) { _, _ -> finish() }
            .show()
    }
}
