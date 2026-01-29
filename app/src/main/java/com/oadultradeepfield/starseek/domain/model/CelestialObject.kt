package com.oadultradeepfield.starseek.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CelestialObject(
    val name: String,
    val type: ObjectType,
    val constellation: String,
    val pixelX: Double? = null,
    val pixelY: Double? = null,
)
