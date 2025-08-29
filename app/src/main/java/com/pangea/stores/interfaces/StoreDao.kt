package com.pangea.stores.interfaces

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.pangea.stores.models.StoreEntity

@Dao
interface StoreDao {
    @Query("SELECT * FROM StoreEntity")
    suspend fun getAllStores() : MutableList<StoreEntity>

    @Query("SELECT * FROM StoreEntity WHERE  id = :id")
    suspend fun getStoreById(id: Long): StoreEntity

    @Insert
    suspend fun addStore(storeEntity: StoreEntity): Long

    @Update
    suspend  fun updateStore(storeEntity: StoreEntity)

    @Delete
    suspend fun deleteStore(storeEntity: StoreEntity)
}