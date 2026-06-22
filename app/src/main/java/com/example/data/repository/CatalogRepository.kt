package com.example.data.repository

import com.example.data.local.CatalogDao
import com.example.data.local.CatalogItem
import kotlinx.coroutines.flow.Flow

class CatalogRepository(private val catalogDao: CatalogDao) {

    val allItems: Flow<List<CatalogItem>> = catalogDao.getAllItems()

    suspend fun insert(item: CatalogItem): Long {
        return catalogDao.insertItem(item)
    }

    suspend fun delete(item: CatalogItem) {
        catalogDao.deleteItem(item)
    }

    suspend fun deleteById(id: Int) {
        catalogDao.deleteItemById(id)
    }

    suspend fun clear() {
        catalogDao.clearCatalog()
    }
}
