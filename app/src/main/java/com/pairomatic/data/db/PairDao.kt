package com.pairomatic.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PairDao {

    @Query("SELECT * FROM pairs ORDER BY letters ASC")
    fun observeAll(): Flow<List<PairEntity>>

    @Query("SELECT * FROM pairs")
    suspend fun getAll(): List<PairEntity>

    @Query("SELECT * FROM pairs WHERE id = :id")
    suspend fun getById(id: Long): PairEntity?

    @Query("SELECT * FROM pairs WHERE letters = :letters LIMIT 1")
    suspend fun getByLetters(letters: String): PairEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(pair: PairEntity): Long

    @Update
    suspend fun update(pair: PairEntity)

    @Delete
    suspend fun delete(pair: PairEntity)

    @Query("DELETE FROM pairs")
    suspend fun deleteAll()

    /** Zapisuje ocenę (poziom + czas) bez naruszania pozostałych pól. */
    @Query("UPDATE pairs SET level = :level, lastSeen = :lastSeen WHERE id = :id")
    suspend fun grade(id: Long, level: Int, lastSeen: Long)
}
