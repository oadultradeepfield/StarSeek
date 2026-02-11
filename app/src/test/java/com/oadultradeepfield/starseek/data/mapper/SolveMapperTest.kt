package com.oadultradeepfield.starseek.data.mapper

import com.oadultradeepfield.starseek.domain.model.ObjectType
import com.oadultradeepfield.starseek.testutil.TestData
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SolveMapperTest {
  private val json = Json { ignoreUnknownKeys = true }
  private lateinit var mapper: SolveMapper

  @Before
  fun setup() {
    mapper = SolveMapper(json)
  }

  @Test
  fun `mapToDomain from entity maps all fields correctly`() {
    val objects = listOf(TestData.createCelestialObject())
    val entity =
        TestData.createSolveEntity(
            id = 42,
            imageUri = "content://image",
            imageHash = "abc123",
            objectsJson = json.encodeToString(objects),
            objectCount = 1,
            timestamp = 1000L,
        )

    val result = mapper.mapToDomain(entity)

    assertEquals(42L, result.id)
    assertEquals("content://image", result.imageUri)
    assertEquals("abc123", result.imageHash)
    assertEquals(1000L, result.timestamp)
    assertEquals(1, result.objects.size)
    assertEquals("Sirius", result.objects[0].name)
  }

  @Test
  fun `mapToEntity from solve maps all fields correctly`() {
    val objects =
        listOf(
            TestData.createCelestialObject(name = "Betelgeuse", constellation = "Orion"),
            TestData.createCelestialObject(
                name = "M42",
                type = ObjectType.NEBULA,
                constellation = "Orion",
            ),
        )

    val solve =
        TestData.createSolve(
            id = 1,
            imageUri = "uri1",
            imageHash = "hash123",
            objects = objects,
            timestamp = 2000L,
        )

    val result = mapper.mapToEntity(solve)

    assertEquals("uri1", result.imageUri)
    assertEquals("hash123", result.imageHash)
    assertEquals(2000L, result.timestamp)
    assertEquals(2, result.objectCount)
    assertTrue(result.objectsJson.contains("Betelgeuse"))
    assertTrue(result.objectsJson.contains("M42"))
  }

  @Test
  fun `mapToDomain from SolveResult creates solve with empty URI and hash`() {
    val solveResult =
        TestData.createSolveResult(
            objects =
                listOf(
                    TestData.createCelestialObjectDto(
                        name = "Polaris",
                        constellation = "Ursa Minor",
                        pixelX = 100.0,
                        pixelY = 200.0,
                    )
                )
        )

    val result = mapper.mapToDomain(solveResult)

    assertEquals("", result.imageUri)
    assertEquals("", result.imageHash)
    assertEquals(1, result.objects.size)
    assertEquals("Polaris", result.objects[0].name)
    assertTrue(result.timestamp > 0)
  }

  @Test
  fun `mapToDomain from CelestialObjectDto maps all fields including nullable pixelX and pixelY`() {
    val dto =
        TestData.createCelestialObjectDto(
            name = "Vega",
            constellation = "Lyra",
            pixelX = 150.5,
            pixelY = 250.5,
        )
    val result = mapper.mapToDomain(dto)

    assertEquals("Vega", result.name)
    assertEquals(ObjectType.STAR, result.type)
    assertEquals("Lyra", result.constellation)
    assertEquals(150.5, result.pixelX)
    assertEquals(250.5, result.pixelY)
  }

  @Test
  fun `mapToDomain from CelestialObjectDto handles null pixel coordinates`() {
    val dto = TestData.createCelestialObjectDto(name = "Rigel", constellation = "Orion")
    val result = mapper.mapToDomain(dto)

    assertNull(result.pixelX)
    assertNull(result.pixelY)
  }

  @Test
  fun `mapToObjectType is case insensitive for star`() {
    assertEquals(ObjectType.STAR, mapper.mapToObjectType("star"))
    assertEquals(ObjectType.STAR, mapper.mapToObjectType("STAR"))
    assertEquals(ObjectType.STAR, mapper.mapToObjectType("Star"))
  }

  @Test
  fun `mapToObjectType maps all known types correctly`() {
    assertEquals(ObjectType.STAR, mapper.mapToObjectType("star"))
    assertEquals(ObjectType.NEBULA, mapper.mapToObjectType("nebula"))
    assertEquals(ObjectType.GALAXY, mapper.mapToObjectType("galaxy"))
    assertEquals(ObjectType.CLUSTER, mapper.mapToObjectType("cluster"))
  }

  @Test
  fun `mapToObjectType defaults to STAR for unknown types`() {
    assertEquals(ObjectType.STAR, mapper.mapToObjectType("unknown"))
    assertEquals(ObjectType.STAR, mapper.mapToObjectType("planet"))
    assertEquals(ObjectType.STAR, mapper.mapToObjectType(""))
  }
}
