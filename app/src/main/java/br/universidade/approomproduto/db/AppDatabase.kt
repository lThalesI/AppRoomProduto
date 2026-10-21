package br.universidade.approomproduto.db

import androidx.room.Database
import androidx.room.RoomDatabase
import br.universidade.approomproduto.dao.ProductDao
import br.universidade.approomproduto.model.Product

// A8 - version = 2 porque a entidade Product ganhou o campo "quantity"
@Database(entities = [Product::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
}
