package com.oadultradeepfield.starseek.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ObjectDetailResponse(
    val name: String,
    val type: String,
    val constellation: String,
    val funFact: String,
)
