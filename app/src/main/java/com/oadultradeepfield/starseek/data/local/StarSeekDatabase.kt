package com.oadultradeepfield.starseek.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SolveEntity::class], version = 1)
abstract class StarSeekDatabase : RoomDatabase() {
  abstract fun solveDao(): SolveDao
}
