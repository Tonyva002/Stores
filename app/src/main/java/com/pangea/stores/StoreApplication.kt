package com.pangea.stores

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pangea.stores.models.StoreDatabase

class StoreApplication : Application() {

    // Migración de la versión 1 a la 2
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE StoreEntity ADD COLUMN photoUrl TEXT NOT NULL DEFAULT ''")
        }
    }

    // Instancia de la base de datos
    val database: StoreDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            StoreDatabase::class.java,
            "StoreDataBase"

        )
            .addMigrations(MIGRATION_1_2)
            .build()
    }
}
