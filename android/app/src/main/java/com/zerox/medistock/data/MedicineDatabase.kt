package com.zerox.medistock.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ShopEntity::class, MedicineEntity::class, StockMovementEntity::class],
    version = 3,
    exportSchema = false
)
abstract class MedicineDatabase : RoomDatabase() {
    abstract fun dao(): MedicineDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS stock_movements (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        medicineId INTEGER NOT NULL,
                        medicineName TEXT NOT NULL,
                        movementType TEXT NOT NULL,
                        quantity INTEGER NOT NULL,
                        unitPrice REAL NOT NULL,
                        totalAmount REAL NOT NULL,
                        occurredAt INTEGER NOT NULL
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_movements_occurredAt ON stock_movements(occurredAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_movements_medicineId ON stock_movements(medicineId)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS shops (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        ownerName TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )"""
                )
                db.execSQL("INSERT INTO shops (id, name, ownerName, createdAt) VALUES (1, 'My Shop', 'Owner', ${System.currentTimeMillis()})")
                db.execSQL("ALTER TABLE medicines ADD COLUMN shopId INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE stock_movements ADD COLUMN shopId INTEGER NOT NULL DEFAULT 1")
            }
        }

        @Volatile private var instance: MedicineDatabase? = null

        fun get(context: Context): MedicineDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MedicineDatabase::class.java,
                    "medi_stock.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
            }
    }
}