package com.oadultradeepfield.starseek.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ObjectDetailDao {
  @Query("SELECT * FROM object_details WHERE name = :name")
  suspend fun getByName(name: String): ObjectDetailEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(entity: ObjectDetailEntity)
}
