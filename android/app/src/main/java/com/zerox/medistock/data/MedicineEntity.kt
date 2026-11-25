package com.zerox.medistock.data
import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
@Entity(tableName="medicines")
data class MedicineEntity(@PrimaryKey(autoGenerate=true) val id:Long=0,val name:String,val strength:String="",val company:String="",val unitPrice:Double=0.0,val quantity:Int=0,val minimumStock:Int=10,@ColumnInfo(defaultValue = "1") val shopId:Long=1)
