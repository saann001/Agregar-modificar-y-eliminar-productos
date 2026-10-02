package com.example.loginapp.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.loginapp.R
import com.example.loginapp.controller.CatalogController
import com.example.loginapp.controller.ProductFormController
import com.example.loginapp.databinding.ActivityProductFormBinding
import com.example.loginapp.model.ProductFormErrors
import com.example.loginapp.model.ProductFormResult
import com.example.loginapp.model.UiState
import com.example.loginapp.util.CategoryTranslator
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Clase base (herencia, POO) para las pantallas de formulario de
 * producto. [CreateProductActivity] (US06) y [EditProductActivity]
 * (US07) piden EXACTAMENTE los mismos campos y las mismas
 * validaciones, así que toda esa lógica común vive aquí una sola vez.
 *
 * Responsabilidades de esta clase:
 *  - Bloquear el acceso si el rol local no es Administrador (US06/US07
 *    escenario 3), incluso si alguien intenta entrar por enlace
 *    profundo o manipulación de estado.
 *  - Cargar categorías (reutiliza [CatalogController] de US04) y
 *    traducirlas al español sin perder el valor que viaja a la API.
 *  - Deshabilitar el botón "Guardar" y mostrar el indicador de carga
 *    mientras la petición está en curso (evita envíos duplicados).
 *  - Pintar en rojo los campos inválidos que devuelva el Controller.
 */
abstract class ProductFormActivity : AppCompatActivity() {

    protected lateinit var binding: ActivityProductFormBinding
    protected lateinit var formController: ProductFormController

    /** Categorías (valor real de la API) en el mismo orden que el dropdown. */
    private var categoryApiValues: List<String> = emptyList()
    private var selectedCategoryApiValue: String? = null

    @get:StringRes
    protected abstract val screenTitleRes: Int

    @get:StringRes
    protected abstract val saveButtonTextRes: Int

    /** Categoría a preseleccionar (solo la usa [EditProductActivity]). */
    protected open fun initialCategoryApiValue(): String? = null

    /** Prellenado de campos (solo lo sobreescribe [EditProductActivity]). */
    protected open fun prefill() {}

    /** Cada subclase decide si esto es un POST o un PUT. */
    protected abstract suspend fun performSave(
        title: String,
        priceText: String,
        description: String,
        imageUrl: String,
        category: String
    ): ProductFormResult

    /** Cada subclase decide qué hacer tras un guardado exitoso. */
    protected abstract fun onSaveSuccess(result: ProductFormResult.Success)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        formController = ProductFormController(applicationContext)

        // US06/US07 - Escenario 3: bloqueo de acceso a nivel de código.
        // Si el rol local no es Administrador, ni siquiera se infla el
        // formulario: se redirige de inmediato al catálogo.
        if (!formController.canManageProducts()) {
            redirectToCatalogForbidden()
            return
        }

        binding = ActivityProductFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.title = getString(screenTitleRes)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnSave.text = getString(saveButtonTextRes)
        binding.btnSave.setOnClickListener { onSaveClicked() }

        lifecycleScope.launch { setupCategoryDropdown(initialCategoryApiValue()) }
        prefill()
    }

    private suspend fun setupCategoryDropdown(preselectApiValue: String?) {
        // Reutiliza el mismo controller/endpoint de US04, no se duplica lógica.
        when (val state = CatalogController(applicationContext).loadCategories()) {
            is UiState.Success -> {
                categoryApiValues = state.data
                val labels = state.data.map { CategoryTranslator.displayName(this, it) }
                binding.dropdownCategory.setAdapter(
                    ArrayAdapter(this, android.R.layout.simple_list_item_1, labels)
                )
                binding.dropdownCategory.setOnItemClickListener { _, _, position, _ ->
                    selectedCategoryApiValue = categoryApiValues.getOrNull(position)
                    binding.layoutCategory.error = null
                }

                preselectApiValue?.let { apiValue ->
                    val index = categoryApiValues.indexOf(apiValue)
                    if (index >= 0) {
                        binding.dropdownCategory.setText(labels[index], false)
                        selectedCategoryApiValue = apiValue
                    }
                }
            }
            is UiState.Error -> Unit // el usuario simplemente no verá opciones; la validación lo cubre
            UiState.Loading -> Unit
        }
    }

    private fun onSaveClicked() {
        clearFieldErrors()

        val title = binding.editTitle.text?.toString().orEmpty()
        val priceText = binding.editPrice.text?.toString().orEmpty()
        val description = binding.editDescription.text?.toString().orEmpty()
        val imageUrl = binding.editImageUrl.text?.toString().orEmpty()
        val category = selectedCategoryApiValue.orEmpty()

        setSaving(true)
        lifecycleScope.launch {
            val result = performSave(title, priceText, description, imageUrl, category)
            setSaving(false)

            when (result) {
                is ProductFormResult.Success -> onSaveSuccess(result)
                is ProductFormResult.ValidationError -> renderErrors(result.errors)
                is ProductFormResult.NetworkError ->
                    showSnackbar(result.message.ifBlank { getString(R.string.form_network_error) })
                ProductFormResult.Forbidden -> redirectToCatalogForbidden()
            }
        }
    }

    private fun renderErrors(errors: ProductFormErrors) {
        binding.layoutTitle.error = errors.title
        binding.layoutPrice.error = errors.price
        binding.layoutDescription.error = errors.description
        binding.layoutImageUrl.error = errors.imageUrl
        binding.layoutCategory.error = errors.category
    }

    private fun clearFieldErrors() {
        binding.layoutTitle.error = null
        binding.layoutPrice.error = null
        binding.layoutDescription.error = null
        binding.layoutImageUrl.error = null
        binding.layoutCategory.error = null
    }

    /** US07 nota de negocio: deshabilita el botón + spinner mientras viaja la petición. */
    private fun setSaving(saving: Boolean) {
        binding.btnSave.isEnabled = !saving
        binding.progressBar.visibility = if (saving) View.VISIBLE else View.GONE
    }

    /** US06 escenario 1: "limpia el formulario" tras un guardado exitoso. */
    protected fun clearForm() {
        binding.editTitle.text?.clear()
        binding.editPrice.text?.clear()
        binding.editDescription.text?.clear()
        binding.editImageUrl.text?.clear()
        binding.dropdownCategory.text?.clear()
        selectedCategoryApiValue = null
        clearFieldErrors()
    }

    protected fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun redirectToCatalogForbidden() {
        showToastAndGoHome()
    }

    private fun showToastAndGoHome() {
        Toast.makeText(this, R.string.form_access_denied, Toast.LENGTH_LONG).show()
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
