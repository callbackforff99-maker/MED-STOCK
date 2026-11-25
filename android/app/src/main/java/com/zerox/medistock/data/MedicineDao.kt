package com.zerox.medistock.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {
    @Query("SELECT * FROM shops ORDER BY id ASC")
    fun observeShops(): Flow<List<ShopEntity>>

    @Query("SELECT * FROM shops ORDER BY id ASC")
    suspend fun getShops(): List<ShopEntity>

    @Insert
    suspend fun insertShop(shop: ShopEntity): Long

    @Query("SELECT * FROM medicines WHERE shopId=:shopId ORDER BY name COLLATE NOCASE")
    fun observeAll(shopId: Long): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicines WHERE shopId=:shopId ORDER BY name COLLATE NOCASE")
    suspend fun getAllForExport(shopId: Long): List<MedicineEntity>

    @Query("SELECT * FROM stock_movements WHERE shopId=:shopId ORDER BY occurredAt ASC")
    suspend fun getAllMovementsForExport(shopId: Long): List<StockMovementEntity>

    @Insert
    suspend fun insert(item: MedicineEntity)

    @Query("UPDATE medicines SET quantity=:qty WHERE id=:id")
    suspend fun setQty(id: Long, qty: Int)

    @Insert
    suspend fun insertMovement(item: StockMovementEntity)

    @Transaction
    suspend fun saveMovementAndQty(item: StockMovementEntity, newQty: Int) {
        insertMovement(item)
        setQty(item.medicineId, newQty)
    }

    @Query("SELECT * FROM stock_movements WHERE shopId=:shopId AND occurredAt >= :fromInclusive AND occurredAt < :toExclusive ORDER BY occurredAt ASC")
    fun observeMovements(shopId: Long, fromInclusive: Long, toExclusive: Long): Flow<List<StockMovementEntity>>
}