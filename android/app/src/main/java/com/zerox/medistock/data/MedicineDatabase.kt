package com.zerox.medistock.data
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
@Database(entities=[MedicineEntity::class],version=1,exportSchema=false)
abstract class MedicineDatabase:RoomDatabase(){
 abstract fun dao():MedicineDao
 companion object {
 @Volatile private var instance:MedicineDatabase?=null
 fun get(context:Context):MedicineDatabase=instance?: synchronized(this){instance?:Room.databaseBuilder(context.applicationContext,MedicineDatabase::class.java,"medi_stock.db").build().also{instance=it}}
 }
}
