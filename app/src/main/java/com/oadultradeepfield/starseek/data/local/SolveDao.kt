package com.oadultradeepfield.starseek.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SolveDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(solve: SolveEntity): Long

  @Query("SELECT * FROM solves ORDER BY timestamp DESC") fun getAllSolves(): Flow<List<SolveEntity>>

  @Query("SELECT * FROM solves WHERE id = :id") suspend fun getSolveById(id: Long): SolveEntity?

  @Query("SELECT * FROM solves WHERE imageHash = :hash")
  suspend fun getSolveByHash(hash: String): SolveEntity?

  @Query("DELETE FROM solves WHERE id = :id") suspend fun deleteById(id: Long)
}
