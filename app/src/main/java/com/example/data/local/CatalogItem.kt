package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "catalog_items")
data class CatalogItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val code: String,
    val height: String,
    val width: String,
    val depth: String = "",
    val material: String,
    val finish: String,
    val presentation: String,
    val imagePath: String?, // Local file path of final isolated canvas PNG
    val driveUrl: String? = null,
    val sheetsUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
