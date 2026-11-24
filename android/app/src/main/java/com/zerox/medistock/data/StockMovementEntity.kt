package com.zerox.medistock.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_movements",
    indices = [Index(value = ["occurredAt"]), Index(value = ["medicineId"])]
)
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicineId: Long,
    val medicineName: String,
    val movementType: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalAmount: Double,
    val occurredAt: Long = System.currentTimeMillis()
)
