package com.oadultradeepfield.starseek.data.mapper

import com.oadultradeepfield.starseek.data.local.ObjectDetailEntity
import com.oadultradeepfield.starseek.data.local.SolveEntity
import com.oadultradeepfield.starseek.data.remote.dto.CelestialObjectDto
import com.oadultradeepfield.starseek.data.remote.dto.ObjectDetailResponse
import com.oadultradeepfield.starseek.data.remote.dto.SolveResult
import com.oadultradeepfield.starseek.domain.model.CelestialObject
import com.oadultradeepfield.starseek.domain.model.ObjectDetail
import com.oadultradeepfield.starseek.domain.model.ObjectType
import com.oadultradeepfield.starseek.domain.model.Solve
import kotlinx.serialization.json.Json

private val json = Json

fun SolveEntity.toDomain(): Solve =
    Solve(
        id = id,
        imageUri = imageUri,
        imageHash = imageHash,
        objects = json.decodeFromString(objectsJson),
        timestamp = timestamp,
    )

fun Solve.toEntity(): SolveEntity =
    SolveEntity(
        imageUri = imageUri,
        imageHash = imageHash,
        objectsJson = json.encodeToString(objects),
        timestamp = timestamp,
    )

fun SolveResult.toDomain(): Solve =
    Solve(
        imageUri = "",
        imageHash = "",
        objects = objects.map { it.toDomain() },
        timestamp = System.currentTimeMillis(),
    )

fun CelestialObjectDto.toDomain(): CelestialObject =
    CelestialObject(
        name = name,
        type = type.toObjectType(),
        constellation = constellation,
        pixelX = pixelX,
        pixelY = pixelY,
    )

fun ObjectDetailEntity.toDomain(): ObjectDetail =
    ObjectDetail(
        name = name,
        type = type.toObjectType(),
        constellation = constellation,
        funFact = funFact,
    )

fun ObjectDetailResponse.toDomain(): ObjectDetail =
    ObjectDetail(
        name = name,
        type = type.toObjectType(),
        constellation = constellation,
        funFact = funFact,
    )

fun ObjectDetailResponse.toEntity(): ObjectDetailEntity =
    ObjectDetailEntity(
        name = name,
        type = type,
        constellation = constellation,
        funFact = funFact,
    )

fun String.toObjectType(): ObjectType =
    when (lowercase()) {
      "star" -> ObjectType.STAR
      "nebula" -> ObjectType.NEBULA
      "galaxy" -> ObjectType.GALAXY
      "cluster" -> ObjectType.CLUSTER
      else -> ObjectType.STAR
    }
