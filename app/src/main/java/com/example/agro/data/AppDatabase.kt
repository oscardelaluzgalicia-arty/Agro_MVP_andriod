package com.example.agro.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TokenEntity::class, ModuleEntity::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tokenDao(): TokenDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "agro_database"
                )
                .fallbackToDestructiveMigration() // Para simplificar durante el desarrollo si cambias el esquema
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
