package com.example.loginapp.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.loginapp.databinding.ItemProductBinding
import com.example.loginapp.model.Product
import com.example.loginapp.util.CategoryTranslator

/**
 * Adapter del catálogo (US03).
 *
 * Usa [ListAdapter] + [DiffUtil], que además de reciclar las vistas
 * calcula automáticamente qué elementos cambiaron al filtrar por
 * categoría (US04), evitando redibujar toda la lista.
 *
 * Las imágenes se descargan en segundo plano con Glide, así la
 * interfaz nunca se congela esperando la red (nota de negocio US03).
 */
class ProductAdapter(
    private val onProductClick: (Product) -> Unit
) : ListAdapter<Product, ProductAdapter.ProductViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProductViewHolder(
        private val binding: ItemProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            // Escenario 1 de US03: cada item muestra imagen, título y precio.
            binding.txtTitle.text = product.title
            binding.txtPrice.text = product.formattedPrice
            binding.txtCategory.text =
                CategoryTranslator.displayName(binding.root.context, product.category)

            Glide.with(binding.imgProduct.context)
                .load(product.imageUrl)
                .centerInside()
                .into(binding.imgProduct)

            binding.root.setOnClickListener { onProductClick(product) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Product>() {
            override fun areItemsTheSame(oldItem: Product, newItem: Product) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Product, newItem: Product) =
                oldItem == newItem
        }
    }
}
