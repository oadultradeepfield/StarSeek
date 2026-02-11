package com.oadultradeepfield.starseek.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "object_details")
data class ObjectDetailEntity(
    @PrimaryKey val name: String,
    val type: String,
    val constellation: String,
    val funFact: String,
)
