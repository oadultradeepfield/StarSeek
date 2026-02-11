package com.oadultradeepfield.starseek.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SolveEntity::class, ObjectDetailEntity::class], version = 3)
abstract class StarSeekDatabase : RoomDatabase() {
  abstract fun solveDao(): SolveDao

  abstract fun objectDetailDao(): ObjectDetailDao
}
