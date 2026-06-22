package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDao {
    @Query("SELECT * FROM catalog_items ORDER BY timestamp DESC")
    fun getAllItems(): Flow<List<CatalogItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: CatalogItem): Long

    @Delete
    suspend fun deleteItem(item: CatalogItem)

    @Query("DELETE FROM catalog_items WHERE id = :id")
    suspend fun deleteItemById(id: Int)

    @Query("DELETE FROM catalog_items")
    suspend fun clearCatalog()
}
