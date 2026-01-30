package com.oadultradeepfield.starseek.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "solves", indices = [Index(value = ["imageHash"], unique = true)])
data class SolveEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val imageUri: String,
    val annotatedImageUri: String,
    val imageHash: String,
    val objectsJson: String,
    val objectCount: Int,
    val timestamp: Long,
)
