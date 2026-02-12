package com.oadultradeepfield.starseek.testutil

import com.oadultradeepfield.starseek.data.local.SolveEntity
import com.oadultradeepfield.starseek.data.remote.dto.CelestialObjectDto
import com.oadultradeepfield.starseek.data.remote.dto.SolveResult
import com.oadultradeepfield.starseek.domain.model.CelestialObject
import com.oadultradeepfield.starseek.domain.model.ObjectType
import com.oadultradeepfield.starseek.domain.model.Solve

object TestData {
  fun createSolveEntity(
      id: Long = 1L,
      imageUri: String = "file:///images/image.jpg",
      imageHash: String = "abc123",
      objectsJson: String = "[]",
      timestamp: Long = 1700000000000L,
  ) =
      SolveEntity(
          id = id,
          imageUri = imageUri,
          imageHash = imageHash,
          objectsJson = objectsJson,
          timestamp = timestamp,
      )

  fun createSolve(
      id: Long = 1L,
      imageUri: String = "file:///images/image.jpg",
      imageHash: String = "abc123",
      objects: List<CelestialObject> = listOf(createCelestialObject()),
      timestamp: Long = 1700000000000L,
  ) =
      Solve(
          id = id,
          imageUri = imageUri,
          imageHash = imageHash,
          objects = objects,
          timestamp = timestamp,
      )

  fun createCelestialObject(
      name: String = "Sirius",
      type: ObjectType = ObjectType.STAR,
      constellation: String = "Canis Major",
      pixelX: Double? = null,
      pixelY: Double? = null,
  ) =
      CelestialObject(
          name = name,
          type = type,
          constellation = constellation,
          pixelX = pixelX,
          pixelY = pixelY,
      )

  fun createCelestialObjectDto(
      name: String = "Sirius",
      type: String = "star",
      constellation: String = "Canis Major",
      pixelX: Double? = null,
      pixelY: Double? = null,
  ) =
      CelestialObjectDto(
          name = name,
          type = type,
          constellation = constellation,
          pixelX = pixelX,
          pixelY = pixelY,
      )

  fun createSolveResult(objects: List<CelestialObjectDto> = listOf(createCelestialObjectDto())) =
      SolveResult(objects = objects)
}
