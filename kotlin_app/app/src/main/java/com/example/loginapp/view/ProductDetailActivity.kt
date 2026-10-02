package com.example.loginapp.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.loginapp.R
import com.example.loginapp.controller.ProductDetailController
import com.example.loginapp.databinding.ActivityProductDetailBinding
import com.example.loginapp.model.Product
import com.example.loginapp.model.UiState
import com.example.loginapp.util.CategoryTranslator
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

/**
 * View del detalle de producto (US05).
 *
 * La interfaz es dinámica: los controles de gestión (Editar/Eliminar)
 * se construyen en tiempo de ejecución ÚNICAMENTE si el rol guardado
 * en la sesión local es Administrador.
 */
class ProductDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductDetailBinding
    private lateinit var controller: ProductDetailController
    private var productId: Int = 0
    private var currentProduct: Product? = null

    /**
     * Recibe de vuelta los datos ya actualizados desde EditProductActivity
     * (US07) y refresca la pantalla SIN volver a llamar a la API: por la
     * nota de negocio de US07, un nuevo GET no reflejaría el cambio de
     * todas formas, porque Fake Store API no persiste el PUT.
     */
    private val editProductLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        if (activityResult.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val data = activityResult.data ?: return@registerForActivityResult
        val base = currentProduct ?: return@registerForActivityResult

        val updated = base.copy(
            title = data.getStringExtra(EditProductActivity.EXTRA_TITLE) ?: base.title,
            price = data.getDoubleExtra(EditProductActivity.EXTRA_PRICE, base.price),
            description = data.getStringExtra(EditProductActivity.EXTRA_DESCRIPTION) ?: base.description,
            imageUrl = data.getStringExtra(EditProductActivity.EXTRA_IMAGE) ?: base.imageUrl,
            category = data.getStringExtra(EditProductActivity.EXTRA_CATEGORY) ?: base.category
        )
        renderProduct(updated)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        controller = ProductDetailController(applicationContext)
        productId = intent.getIntExtra(EXTRA_PRODUCT_ID, 0)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        loadProduct()
    }

    private fun loadProduct() {
        showLoading(true)
        lifecycleScope.launch {
            when (val state = controller.loadProduct(productId)) {
                is UiState.Success -> {
                    showLoading(false)
                    renderProduct(state.data)
                }
                is UiState.Error -> {
                    showLoading(false)
                    // US05 Escenario 3: alerta y retorno automático al catálogo.
                    showUnavailableAndGoBack(state.message)
                }
                UiState.Loading -> showLoading(true)
            }
        }
    }

    /**
     * US05 Escenario 1: todos los roles ven imagen, título, precio,
     * descripción completa y categoría.
     */
    private fun renderProduct(product: Product) {
        currentProduct = product
        binding.scrollContent.visibility = View.VISIBLE

        binding.txtTitle.text = product.title
        binding.txtPrice.text = product.formattedPrice
        binding.txtCategory.text = CategoryTranslator.displayName(this, product.category)
        binding.txtDescription.text = product.description
        binding.txtRating.text = getString(
            R.string.detail_rating,
            product.ratingRate.toString(),
            product.ratingCount
        )

        Glide.with(this)
            .load(product.imageUrl)
            .centerInside()
            .into(binding.imgProduct)

        // US05 Escenario 2: interfaz de gestión solo para Administrador.
        renderRoleBasedActions(product)
    }

    /**
     * Construye los botones de gestión SOLO si el rol local lo permite.
     *
     * Nota de negocio US05: los componentes de edición/eliminación no
     * deben instanciarse ocultos; por eso si el rol no es Administrador
     * simplemente no se agregan a la jerarquía de vistas (el contenedor
     * queda vacío y no ocupa espacio).
     */
    private fun renderRoleBasedActions(product: Product) {
        binding.containerAdminActions.removeAllViews()

        if (!controller.canManageProducts()) return

        val editButton = MaterialButton(
            this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = getString(R.string.detail_edit)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener { openEditScreen(product) }
        }

        val deleteButton = MaterialButton(this).apply {
            text = getString(R.string.detail_delete)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 14 }
            setOnClickListener { confirmDelete(product) }
        }

        binding.containerAdminActions.addView(editButton)
        binding.containerAdminActions.addView(deleteButton)
    }

    /**
     * US07 - Escenario 2: manda los datos YA CARGADOS del detalle,
     * para que el formulario de edición abra con todo pre-llenado
     * sin necesidad de una segunda petición de red.
     */
    private fun openEditScreen(product: Product) {
        val intent = Intent(this, EditProductActivity::class.java).apply {
            putExtra(EditProductActivity.EXTRA_ID, product.id)
            putExtra(EditProductActivity.EXTRA_TITLE, product.title)
            putExtra(EditProductActivity.EXTRA_PRICE, product.price)
            putExtra(EditProductActivity.EXTRA_DESCRIPTION, product.description)
            putExtra(EditProductActivity.EXTRA_IMAGE, product.imageUrl)
            putExtra(EditProductActivity.EXTRA_CATEGORY, product.category)
        }
        editProductLauncher.launch(intent)
    }

    private fun confirmDelete(product: Product) {
        AlertDialog.Builder(this)
            .setTitle(R.string.detail_delete_confirm_title)
            .setMessage(getString(R.string.detail_delete_confirm_message, product.title))
            .setNegativeButton(R.string.common_cancel, null)
            .setPositiveButton(R.string.common_delete) { _, _ -> deleteProduct(product) }
            .show()
    }

    private fun deleteProduct(product: Product) {
        showLoading(true)
        lifecycleScope.launch {
            when (val state = controller.deleteProduct(product.id)) {
                is UiState.Success -> {
                    showLoading(false)
                    Toast.makeText(
                        this@ProductDetailActivity,
                        getString(R.string.detail_delete_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
                is UiState.Error -> {
                    showLoading(false)
                    binding.scrollContent.visibility = View.VISIBLE
                    AlertDialog.Builder(this@ProductDetailActivity)
                        .setMessage(state.message)
                        .setPositiveButton(R.string.common_understood, null)
                        .show()
                }
                UiState.Loading -> showLoading(true)
            }
        }
    }

    private fun showUnavailableAndGoBack(message: String) {
        AlertDialog.Builder(this)
            .setTitle(R.string.detail_unavailable_title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(R.string.detail_back_to_catalog) { _, _ -> finish() }
            .show()
    }

    private fun showLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        if (loading) binding.scrollContent.visibility = View.GONE
    }

    companion object {
        const val EXTRA_PRODUCT_ID = "extra_product_id"
    }
}
