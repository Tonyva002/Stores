package com.pangea.stores.models

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pangea.stores.interfaces.StoreDao

@Database(entities = [StoreEntity::class], version = 2)
abstract class StoreDatabase : RoomDatabase(){
    abstract fun storeDao(): StoreDao

}