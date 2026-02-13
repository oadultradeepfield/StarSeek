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

fun List<CelestialObject>.groupByConstellation():
    Map<String, Map<ObjectType, List<CelestialObject>>> =
    groupBy { it.constellation }.mapValues { (_, objects) -> objects.groupBy { it.type } }
