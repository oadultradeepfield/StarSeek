package com.oadultradeepfield.starseek.domain.model

/**
 * Result of a "plate solve" - an astronomy technique that identifies celestial objects in a star
 * field image by matching star patterns against a catalog database.
 */
data class Solve(
    val id: Long = 0,
    val imageUri: String,
    val annotatedImageUri: String,
    val imageHash: String,
    val objects: List<CelestialObject>,
    val timestamp: Long,
)
