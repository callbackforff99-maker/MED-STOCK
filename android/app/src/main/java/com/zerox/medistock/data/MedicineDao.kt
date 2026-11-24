package com.zerox.medistock.data
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
@Dao interface MedicineDao {
 @Query("SELECT * FROM medicines ORDER BY name COLLATE NOCASE") fun observeAll():Flow<List<MedicineEntity>>
 @Insert suspend fun insert(item:MedicineEntity)
 @Query("UPDATE medicines SET quantity=:qty WHERE id=:id") suspend fun setQty(id:Long,qty:Int)
}
