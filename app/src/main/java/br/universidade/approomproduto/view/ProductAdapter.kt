package br.universidade.approomproduto.view

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.universidade.approomproduto.databinding.ItemProductBinding
import br.universidade.approomproduto.model.Product

/** Adapter do RecyclerView que mostra a lista de produtos (A3 - desafio extra). */
class ProductAdapter(
    private val onExcluir: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ViewHolder>() {

    private var items: List<Product> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun setProducts(list: List<Product>) {
        items = list
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ItemProductBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = items[position]
        val descricao = product.description?.takeIf { it.isNotBlank() } ?: "sem descrição"
        holder.binding.txtProduto.text =
            "#${product.id} - ${product.name}\n" +
                "${formatarMoeda(product.price)} | Qtd: ${product.quantity}\n" +
                descricao
        holder.binding.btnExcluirItem.setOnClickListener { onExcluir(product) }
    }
}
