package com.example.loginapp.view

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.loginapp.R
import com.example.loginapp.controller.CatalogController
import com.example.loginapp.controller.SessionController
import com.example.loginapp.databinding.ActivityHomeBinding
import com.example.loginapp.model.Product
import com.example.loginapp.model.Role
import com.example.loginapp.model.UiState
import com.example.loginapp.util.CategoryTranslator
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

/**
 * View del catálogo (patrón MVC). Cubre:
 *  - US03: renderiza el catálogo con RecyclerView, maneja loading y error.
 *  - US04: chips de categorías para filtrar y opción "Ver todos".
 *  - US02: botón de cerrar sesión en el menú del toolbar.
 *
 * Toda la lógica de red vive en [CatalogController]; esta clase solo
 * traduce el [UiState] recibido a cambios visuales.
 */
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var sessionController: SessionController
    private lateinit var catalogController: CatalogController
    private lateinit var adapter: ProductAdapter

    /** Etiqueta del chip que representa "sin filtro" (US04 escenario 3). */
    private val allCategoriesLabel: String
        get() = getString(R.string.catalog_all_categories)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionController = SessionController(applicationContext)
        catalogController = CatalogController(applicationContext)

        val user = sessionController.readSession()
        if (user == null) {
            goToLogin()
            return
        }

        setSupportActionBar(binding.toolbar)
        binding.txtWelcome.text = getString(R.string.catalog_welcome, user.username)
        binding.txtRole.text = user.role.displayName

        setupRecyclerView()
        setupListeners()
        setupAddProductFab(user.role)

        // US04 Escenario 1: las categorías se piden al inicializar la vista.
        loadCategories()
        // US03: carga inicial del catálogo completo.
        loadProducts(null)

        // US02 Escenario 2: bloquear "Atrás" en pantalla protegida.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                moveTaskToBack(true)
            }
        })
    }

    private fun setupRecyclerView() {
        adapter = ProductAdapter { product -> openDetail(product) }
        binding.recyclerProducts.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerProducts.adapter = adapter
        binding.recyclerProducts.setHasFixedSize(true)
    }

    private fun setupListeners() {
        // US03 Escenario 3: botón "Reintentar" relanza la misma petición.
        binding.btnRetry.setOnClickListener {
            loadProducts(catalogController.selectedCategory)
        }

        binding.swipeRefresh.setOnRefreshListener {
            loadProducts(catalogController.selectedCategory)
        }
    }

    // ---------- US04: categorías ----------

    private fun loadCategories() {
        lifecycleScope.launch {
            when (val state = catalogController.loadCategories()) {
                is UiState.Success -> renderCategoryChips(state.data)
                is UiState.Error -> {
                    // Si fallan las categorías, el catálogo general
                    // sigue siendo usable: solo mostramos "Ver todos".
                    renderCategoryChips(emptyList())
                }
                UiState.Loading -> Unit
            }
        }
    }

    private fun renderCategoryChips(categories: List<String>) {
        binding.chipGroupCategories.removeAllViews()

        // Primer chip: "Ver todos" (sin filtro). El resto son las
        // categorías de la API. El valor `apiValue` viaja a la URL en
        // inglés; el texto visible se traduce al español.
        addCategoryChip(label = allCategoriesLabel, apiValue = null, checked = true)

        categories.forEach { apiCategory ->
            addCategoryChip(
                label = CategoryTranslator.displayName(this, apiCategory),
                apiValue = apiCategory,
                checked = false
            )
        }
    }

    private fun addCategoryChip(label: String, apiValue: String?, checked: Boolean) {
        val chip = Chip(this).apply {
            text = label
            isCheckable = true
            isClickable = true
            isChecked = checked
            setChipBackgroundColorResource(R.color.surface)
            setOnClickListener {
                // US04 Escenario 2 y 3: null = sin filtro (todos).
                loadProducts(apiValue)
            }
        }
        binding.chipGroupCategories.addView(chip)
    }

    // ---------- US03: catálogo ----------

    private fun loadProducts(category: String?) {
        // US04 nota de negocio "Consistencia de carga": cualquier cambio
        // de categoría dispara el mismo indicador de carga.
        showLoading()

        lifecycleScope.launch {
            when (val state = catalogController.loadProducts(category)) {
                is UiState.Success -> showProducts(state.data)
                is UiState.Error -> showError(state.message)
                UiState.Loading -> showLoading()
            }
        }
    }

    private fun showLoading() {
        // "Gestión de memoria" US04: se vacía la lista visible antes de
        // que llegue la nueva respuesta, para no mezclar categorías.
        adapter.submitList(emptyList())
        binding.progressBar.visibility = View.VISIBLE
        binding.layoutError.visibility = View.GONE
        binding.recyclerProducts.visibility = View.GONE
        binding.txtProductCount.visibility = View.GONE
    }

    private fun showProducts(products: List<Product>) {
        binding.progressBar.visibility = View.GONE
        binding.layoutError.visibility = View.GONE
        binding.recyclerProducts.visibility = View.VISIBLE
        binding.swipeRefresh.isRefreshing = false
        binding.txtProductCount.visibility = View.VISIBLE
        binding.txtProductCount.text =
            getString(R.string.catalog_product_count, products.size)
        adapter.submitList(products)
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.recyclerProducts.visibility = View.GONE
        binding.swipeRefresh.isRefreshing = false
        binding.layoutError.visibility = View.VISIBLE
        binding.txtProductCount.visibility = View.GONE
        binding.txtError.text = message
    }

    // ---------- US06: alta de producto (solo Administrador) ----------

    /**
     * El FAB se instancia por código SOLO si el rol es Administrador
     * (mismo criterio que los botones de gestión de US05): para otros
     * roles, el contenedor queda vacío y el botón nunca existe.
     */
    private fun setupAddProductFab(role: Role) {
        binding.containerFab.removeAllViews()
        if (role != Role.ADMINISTRADOR) return

        val fab = FloatingActionButton(this).apply {
            setImageResource(android.R.drawable.ic_input_add)
            contentDescription = getString(R.string.catalog_fab_add)
            backgroundTintList = ColorStateList.valueOf(getColor(R.color.primary_variant))
            imageTintList = ColorStateList.valueOf(getColor(R.color.text_on_primary))
            setOnClickListener { openCreateProduct() }
        }
        binding.containerFab.addView(fab)
    }

    private fun openCreateProduct() {
        startActivity(Intent(this, CreateProductActivity::class.java))
    }

    // ---------- US05: navegación al detalle ----------

    private fun openDetail(product: Product) {
        val intent = Intent(this, ProductDetailActivity::class.java)
        intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.id)
        startActivity(intent)
    }

    // ---------- US02: logout ----------

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.action_logout) {
            sessionController.logout()
            goToLogin()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
