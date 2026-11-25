package com.zerox.medistock.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {
    @Query("SELECT * FROM medicines ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicines ORDER BY name COLLATE NOCASE")
    suspend fun getAllForExport(): List<MedicineEntity>

    @Query("SELECT * FROM stock_movements ORDER BY occurredAt ASC")
    suspend fun getAllMovementsForExport(): List<StockMovementEntity>

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

    @Query("SELECT * FROM stock_movements WHERE occurredAt >= :fromInclusive AND occurredAt < :toExclusive ORDER BY occurredAt ASC")
    fun observeMovements(fromInclusive: Long, toExclusive: Long): Flow<List<StockMovementEntity>>
}