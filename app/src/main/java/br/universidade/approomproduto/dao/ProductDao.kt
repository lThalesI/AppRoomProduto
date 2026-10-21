package br.universidade.approomproduto.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import br.universidade.approomproduto.model.Product

@Dao
interface ProductDao {

    // suspend: o Room não permite consultas na main thread
    @Query("SELECT * FROM product")
    suspend fun getAll(): List<Product>

    @Insert
    suspend fun insertAll(vararg products: Product)

    // A5 - busca por nome (use "%texto%" para encontrar nomes parecidos)
    @Query("SELECT * FROM product WHERE name LIKE :nome")
    suspend fun buscarPorNome(nome: String): List<Product>

    // A6 - excluir todos os produtos
    @Query("DELETE FROM product")
    suspend fun deleteAll()

    // A6 (desafio) - excluir um produto específico
    @Delete
    suspend fun delete(product: Product)

    // A7 - atualizar um produto já cadastrado
    @Update
    suspend fun update(product: Product)

    @Query("SELECT * FROM product WHERE id = :id")
    suspend fun getById(id: Int): Product?
}
