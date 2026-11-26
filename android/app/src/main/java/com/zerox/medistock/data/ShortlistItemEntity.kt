package com.zerox.medistock.data
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "shortlist_items")
data class ShortlistItemEntity(
 @PrimaryKey(autoGenerate = true) val id: Long = 0,
 val shopId: Long,
 val medicineName: String,
 val currentStockBoxes: Int,
 val buyBoxes: Int,
 val boxPrice: Double,
 val dateKey: String,
 val purchased: Boolean = false,
 val createdAt: Long = System.currentTimeMillis()
)
