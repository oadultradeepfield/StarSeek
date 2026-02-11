package com.oadultradeepfield.starseek.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class JobStatusType {
  @SerialName("processing") PROCESSING,
  @SerialName("success") SUCCESS,
  @SerialName("failed") FAILED,
}

@Serializable
data class JobStatusResponse(
    val status: JobStatusType,
    val result: SolveResult? = null,
    val error: String? = null,
)

@Serializable data class SolveResult(val objects: List<CelestialObjectDto>)

@Serializable
data class CelestialObjectDto(
    val name: String,
    val type: String,
    val constellation: String,
    val pixelX: Double? = null,
    val pixelY: Double? = null,
)
