package com.oadultradeepfield.starseek.data.repository

import com.oadultradeepfield.starseek.data.local.SolveDao
import com.oadultradeepfield.starseek.data.mapper.SolveMapper
import com.oadultradeepfield.starseek.data.remote.StarSeekApi
import com.oadultradeepfield.starseek.data.remote.dto.JobStatusResponse
import com.oadultradeepfield.starseek.data.remote.dto.JobStatusType
import com.oadultradeepfield.starseek.data.remote.dto.ObjectDetailResponse
import com.oadultradeepfield.starseek.data.remote.dto.SolveResponse
import com.oadultradeepfield.starseek.domain.model.ObjectType
import com.oadultradeepfield.starseek.testutil.TestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SolveRepositoryImplTest {
  private lateinit var api: StarSeekApi
  private lateinit var dao: SolveDao
  private lateinit var mapper: SolveMapper
  private lateinit var repository: SolveRepositoryImpl

  @Before
  fun setup() {
    api = mockk()
    dao = mockk()
    mapper = mockk()
    repository = SolveRepositoryImpl(api, dao, mapper)
  }

  @Test
  fun `getCachedSolve returns mapped solve when DAO finds entity`() = runTest {
    val entity = TestData.createSolveEntity()
    val solve = TestData.createSolve()

    coEvery { dao.getSolveByHash("hash123") } returns entity
    every { mapper.mapToDomain(entity) } returns solve

    val result = repository.getCachedSolve("hash123")
    assertEquals(solve, result)
  }

  @Test
  fun `getCachedSolve returns null when DAO returns null`() = runTest {
    coEvery { dao.getSolveByHash("unknown") } returns null
    val result = repository.getCachedSolve("unknown")
    assertNull(result)
  }

  @Test
  fun `uploadImage returns jobId on success`() = runTest {
    val response = SolveResponse(jobId = "job-123", status = "processing")

    coEvery { api.uploadImage(any()) } returns response

    val result = repository.uploadImage(byteArrayOf(1, 2, 3), "test.jpg")

    assertTrue(result.isSuccess)
    assertEquals("job-123", result.getOrNull())
  }

  @Test
  fun `uploadImage returns failure on network exception`() = runTest {
    coEvery { api.uploadImage(any()) } throws RuntimeException("Network error")
    val result = repository.uploadImage(byteArrayOf(1, 2, 3), "test.jpg")
    assertTrue(result.isFailure)
  }

  @Test
  fun `getJobStatus returns Processing when status is processing`() = runTest {
    val response = JobStatusResponse(status = JobStatusType.PROCESSING)
    coEvery { api.getJobStatus("job-1") } returns response

    val result = repository.getJobStatus("job-1")
    assertTrue(result.isSuccess)
    assertEquals(JobStatus.Processing, result.getOrNull())
  }

  @Test
  fun `getJobStatus returns Success with mapped solve when status is success`() = runTest {
    val solveResult = TestData.createSolveResult()
    val solve = TestData.createSolve()
    val response = JobStatusResponse(status = JobStatusType.SUCCESS, result = solveResult)

    coEvery { api.getJobStatus("job-2") } returns response
    every { mapper.mapToDomain(solveResult) } returns solve

    val result = repository.getJobStatus("job-2")

    assertTrue(result.isSuccess)

    val status = result.getOrNull() as JobStatus.Success
    assertEquals(solve, status.solve)
    assertEquals("https://api.example.com/annotated.jpg", status.annotatedImageUrl)
  }

  @Test
  fun `getJobStatus returns Failed with error message when status is failed`() = runTest {
    val response = JobStatusResponse(status = JobStatusType.FAILED, error = "Image too dark")
    coEvery { api.getJobStatus("job-3") } returns response

    val result = repository.getJobStatus("job-3")
    assertTrue(result.isSuccess)

    val status = result.getOrNull() as JobStatus.Failed
    assertEquals("Image too dark", status.error)
  }

  @Test
  fun `getJobStatus returns Unknown error when error is null`() = runTest {
    val response = JobStatusResponse(status = JobStatusType.FAILED, error = null)
    coEvery { api.getJobStatus("job-4") } returns response

    val result = repository.getJobStatus("job-4")
    assertTrue(result.isSuccess)

    val status = result.getOrNull() as JobStatus.Failed
    assertEquals("Unknown error", status.error)
  }

  @Test
  fun `saveSolve maps to entity and returns inserted ID`() = runTest {
    val solve = TestData.createSolve()
    val entity = TestData.createSolveEntity()

    every { mapper.mapToEntity(solve) } returns entity
    coEvery { dao.insert(entity) } returns 42L

    val result = repository.saveSolve(solve)
    assertEquals(42L, result)
  }

  @Test
  fun `getAllSolves returns flow of mapped solves`() = runTest {
    val entity1 = TestData.createSolveEntity(id = 1)
    val entity2 = TestData.createSolveEntity(id = 2)
    val solve1 = TestData.createSolve(id = 1)
    val solve2 = TestData.createSolve(id = 2)

    every { dao.getAllSolves() } returns flowOf(listOf(entity1, entity2))
    every { mapper.mapToDomain(entity1) } returns solve1
    every { mapper.mapToDomain(entity2) } returns solve2

    val result = repository.getAllSolves().first()

    assertEquals(2, result.size)
    assertEquals(solve1, result[0])
    assertEquals(solve2, result[1])
  }

  @Test
  fun `getSolveById returns mapped solve when found`() = runTest {
    val entity = TestData.createSolveEntity()
    val solve = TestData.createSolve()

    coEvery { dao.getSolveById(1L) } returns entity
    every { mapper.mapToDomain(entity) } returns solve

    val result = repository.getSolveById(1L)
    assertEquals(solve, result)
  }

  @Test
  fun `getSolveById returns null when not found`() = runTest {
    coEvery { dao.getSolveById(999L) } returns null
    val result = repository.getSolveById(999L)
    assertNull(result)
  }

  @Test
  fun `deleteSolve calls DAO deleteById`() = runTest {
    coEvery { dao.deleteById(5L) } returns Unit
    repository.deleteSolve(5L)
    coVerify { dao.deleteById(5L) }
  }

  @Test
  fun `getObjectDetail returns mapped ObjectDetail on success`() = runTest {
    val response = ObjectDetailResponse("Sirius", "star", "Canis Major", "Brightest star")

    coEvery { api.getObjectDetail("Sirius") } returns response
    every { mapper.mapToObjectType("star") } returns ObjectType.STAR

    val result = repository.getObjectDetail("Sirius")

    assertTrue(result.isSuccess)

    val detail = result.getOrNull()!!

    assertEquals("Sirius", detail.name)
    assertEquals(ObjectType.STAR, detail.type)
    assertEquals("Canis Major", detail.constellation)
    assertEquals("Brightest star", detail.funFact)
  }

  @Test
  fun `getObjectDetail returns failure on exception`() = runTest {
    coEvery { api.getObjectDetail("Unknown") } throws RuntimeException("Not found")
    val result = repository.getObjectDetail("Unknown")
    assertTrue(result.isFailure)
  }
}
