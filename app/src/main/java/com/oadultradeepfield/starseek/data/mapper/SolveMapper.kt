package com.oadultradeepfield.starseek.data.mapper

import com.oadultradeepfield.starseek.data.local.SolveEntity
import com.oadultradeepfield.starseek.data.remote.dto.CelestialObjectDto
import com.oadultradeepfield.starseek.data.remote.dto.SolveResult
import com.oadultradeepfield.starseek.domain.model.CelestialObject
import com.oadultradeepfield.starseek.domain.model.ObjectType
import com.oadultradeepfield.starseek.domain.model.Solve
import kotlinx.serialization.json.Json
import javax.inject.Inject

class SolveMapper @Inject constructor(private val json: Json) {
  fun mapToDomain(entity: SolveEntity): Solve =
      Solve(
          id = entity.id,
          imageUri = entity.imageUri,
          imageHash = entity.imageHash,
          objects = json.decodeFromString(entity.objectsJson),
          timestamp = entity.timestamp,
      )

  fun mapToEntity(solve: Solve): SolveEntity =
      SolveEntity(
          imageUri = solve.imageUri,
          imageHash = solve.imageHash,
          objectsJson = json.encodeToString(solve.objects),
          objectCount = solve.objects.size,
          timestamp = solve.timestamp,
      )

  fun mapToDomain(result: SolveResult): Solve =
      Solve(
          imageUri = "",
          imageHash = "",
          objects = result.objects.map { mapToDomain(it) },
          timestamp = System.currentTimeMillis(),
      )

  fun mapToDomain(dto: CelestialObjectDto): CelestialObject =
      CelestialObject(
          name = dto.name,
          type = mapToObjectType(dto.type),
          constellation = dto.constellation,
          pixelX = dto.pixelX,
          pixelY = dto.pixelY,
      )

  fun mapToObjectType(type: String): ObjectType =
      when (type.lowercase()) {
        "star" -> ObjectType.STAR
        "nebula" -> ObjectType.NEBULA
        "galaxy" -> ObjectType.GALAXY
        "cluster" -> ObjectType.CLUSTER
        else -> ObjectType.STAR
      }
}
