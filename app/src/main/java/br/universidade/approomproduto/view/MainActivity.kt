package br.universidade.approomproduto.view

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import br.universidade.approomproduto.dao.ProductDao
import br.universidade.approomproduto.databinding.ActivityMainBinding
import br.universidade.approomproduto.db.AppDatabase
import br.universidade.approomproduto.model.Product
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: AppDatabase
    private lateinit var productDao: ProductDao
    private lateinit var adapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa o ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializa o banco de dados Room (sem usar allowMainThreadQueries)
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "database-name"
        )
            .fallbackToDestructiveMigration() // recria o DB se o esquema mudar (só em desenvolvimento)
            .build()
        productDao = db.productDao()

        // RecyclerView com a lista de produtos (A3 - desafio extra)
        adapter = ProductAdapter { product -> excluirProduto(product) }
        binding.recyclerProdutos.layoutManager = LinearLayoutManager(this)
        binding.recyclerProdutos.adapter = adapter

        binding.btnInserirProduto.setOnClickListener { inserirProduto() }
        binding.btnListarProdutos.setOnClickListener { listarProdutos() }
        binding.btnBuscar.setOnClickListener { buscarProdutos() }
        binding.btnExcluirTodos.setOnClickListener { confirmarExcluirTodos() }
        binding.btnAtualizarPreco.setOnClickListener { atualizarPreco() }
    }

    /** Converte o texto em preço. Retorna null se for vazio, inválido, zero ou negativo (A2). */
    private fun lerPreco(texto: String): Double? =
        texto.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }

    // ---------------------------------------------------------------- INSERIR (A1, A2, A8)
    private fun inserirProduto() {
        val name = binding.editProductName.text.toString().trim()
        val price = lerPreco(binding.editProductPrice.text.toString())
        val description = binding.editProductDescription.text.toString().trim()
        val quantity = binding.editProductQuantity.text.toString().toIntOrNull() ?: 0

        // A2 - validação: mostra o erro no próprio campo
        var valido = true
        if (name.isEmpty()) {
            binding.editProductName.error = "Informe o nome do produto"
            valido = false
        }
        if (price == null) {
            binding.editProductPrice.error = "Informe um preço válido"
            valido = false
        }
        if (!valido || price == null) {
            Toast.makeText(this, "Preencha os campos obrigatórios!", Toast.LENGTH_SHORT).show()
            return
        }

        val product = Product(
            name = name,
            price = price,
            description = description.ifEmpty { null },
            quantity = quantity
        )

        // Insere o produto no banco de forma assíncrona (corrotina)
        lifecycleScope.launch {
            productDao.insertAll(product)
            Log.i("TAG", "Produto inserido: $name - R$$price")
            Toast.makeText(this@MainActivity, "Produto inserido!", Toast.LENGTH_SHORT).show()

            // A1 - limpa os campos e devolve o cursor para o campo Nome
            binding.editProductName.text.clear()
            binding.editProductPrice.text.clear()
            binding.editProductQuantity.text.clear()
            binding.editProductDescription.text.clear()
            binding.editProductName.requestFocus()
        }
    }

    // ---------------------------------------------------------------- LISTAR (A3, A4)
    private fun listarProdutos() {
        lifecycleScope.launch {
            val products = carregarLista()
            if (products.isNotEmpty()) {
                products.forEach {
                    Log.i(
                        "TAG",
                        "Produto = ${it.name}, Preço = R$${it.price}, " +
                            "Qtd = ${it.quantity}, Descrição = ${it.description}"
                    )
                }
            } else {
                Log.i("TAG", "Nenhum produto encontrado.")
            }
        }
    }

    private suspend fun carregarLista(): List<Product> {
        val products = productDao.getAll()
        mostrarProdutos(products)
        return products
    }

    /** Mostra a lista no RecyclerView e o resumo (quantidade + soma dos preços) no TextView. */
    private fun mostrarProdutos(products: List<Product>) {
        adapter.setProducts(products)
        val soma = products.sumOf { it.price }
        binding.txtResumo.text =
            "Produtos cadastrados: ${products.size}\nSoma dos preços: ${formatarMoeda(soma)}"
    }

    // ---------------------------------------------------------------- BUSCAR (A5)
    private fun buscarProdutos() {
        val texto = binding.editBuscar.text.toString().trim()
        lifecycleScope.launch {
            val products = if (texto.isEmpty()) {
                productDao.getAll()
            } else {
                productDao.buscarPorNome("%$texto%") // % encontra nomes parecidos
            }
            mostrarProdutos(products)
            if (products.isEmpty()) {
                Toast.makeText(this@MainActivity, "Nenhum produto encontrado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ---------------------------------------------------------------- EXCLUIR (A6)
    private fun confirmarExcluirTodos() {
        AlertDialog.Builder(this)
            .setTitle("Excluir todos")
            .setMessage("Deseja apagar TODOS os produtos cadastrados?")
            .setPositiveButton("Excluir") { _, _ ->
                lifecycleScope.launch {
                    productDao.deleteAll()
                    carregarLista()
                    Toast.makeText(this@MainActivity, "Todos os produtos foram excluídos", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun excluirProduto(product: Product) {
        lifecycleScope.launch {
            productDao.delete(product)
            carregarLista()
            Toast.makeText(this@MainActivity, "Produto excluído", Toast.LENGTH_SHORT).show()
        }
    }

    // ---------------------------------------------------------------- ATUALIZAR (A7)
    private fun atualizarPreco() {
        val id = binding.editUpdateId.text.toString().toIntOrNull()
        val novoPreco = lerPreco(binding.editUpdatePrice.text.toString())

        if (id == null) {
            binding.editUpdateId.error = "Informe o id do produto"
            return
        }
        if (novoPreco == null) {
            binding.editUpdatePrice.error = "Informe um preço válido"
            return
        }

        lifecycleScope.launch {
            val produto = productDao.getById(id)
            if (produto == null) {
                Toast.makeText(this@MainActivity, "Produto #$id não encontrado", Toast.LENGTH_SHORT).show()
            } else {
                productDao.update(produto.copy(price = novoPreco))
                Toast.makeText(this@MainActivity, "Preço atualizado!", Toast.LENGTH_SHORT).show()
                binding.editUpdateId.text.clear()
                binding.editUpdatePrice.text.clear()
                carregarLista()
            }
        }
    }
}
