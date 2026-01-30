package com.oadultradeepfield.starseek.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SolveResponse(
    val jobId: String,
    val status: String,
)
