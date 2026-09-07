package com.jarvis.master.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Repair::class,
        Part::class,
        RepairPart::class,
        Transaction::class,
        Client::class,
        RepairPhoto::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun repairDao(): RepairDao
    abstract fun partDao(): PartDao
    abstract fun transactionDao(): TransactionDao
    abstract fun clientDao(): ClientDao
    abstract fun repairPhotoDao(): RepairPhotoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jarvis.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
