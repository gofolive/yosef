package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [StudentEntity::class, ScanLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MtuMealDatabase : RoomDatabase() {
    abstract fun mealCardDao(): MealCardDao

    companion object {
        @Volatile
        private var INSTANCE: MtuMealDatabase? = null

        fun getInstance(context: Context): MtuMealDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MtuMealDatabase::class.java,
                    "mtu_meal_card_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
