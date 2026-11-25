package com.zerox.medistock.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shops")
data class ShopEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val ownerName: String,
    val createdAt: Long = System.currentTimeMillis()
)
