package com.oadultradeepfield.starseek.domain.usecase

import com.oadultradeepfield.starseek.domain.model.UploadImageResult
import com.oadultradeepfield.starseek.domain.repository.ImageProcessor
import com.oadultradeepfield.starseek.domain.repository.SolveRepository
import com.oadultradeepfield.starseek.testutil.TestData
import com.oadultradeepfield.starseek.testutil.mockUri
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UploadImageUseCaseTest {
  private lateinit var repository: SolveRepository
  private lateinit var imageProcessor: ImageProcessor
  private lateinit var useCase: UploadImageUseCase

  @Before
  fun setup() {
    repository = mockk()
    imageProcessor = mockk()
    useCase = UploadImageUseCase(repository, imageProcessor)
  }

  @Test
  fun `returns CacheHit when image hash matches cached solve`() = runTest {
    val uri = mockUri()
    val cachedSolve = TestData.createSolve(id = 42)
    val imageBytes = byteArrayOf(1, 2, 3)
    coEvery { imageProcessor.readBytes(uri) } returns imageBytes
    every { imageProcessor.computeHash(imageBytes) } returns "hash123"
    coEvery { repository.getCachedSolve("hash123") } returns cachedSolve
    val result = useCase(uri)
    assertTrue(result is UploadImageResult.CacheHit)
    assertEquals(42L, (result as UploadImageResult.CacheHit).solveId)
  }

  @Test
  fun `returns Uploaded after successful upload`() = runTest {
    val uri = mockUri()
    val internalUri = mockUri("file:///internal/image.jpg")
    val imageBytes = byteArrayOf(1, 2, 3)
    coEvery { imageProcessor.readBytes(uri) } returns imageBytes
    every { imageProcessor.computeHash(imageBytes) } returns "hash123"
    coEvery { repository.getCachedSolve("hash123") } returns null
    coEvery { imageProcessor.copyToInternalStorage(imageBytes) } returns internalUri
    coEvery { imageProcessor.compressForUpload(imageBytes) } returns imageBytes
    coEvery { repository.uploadImage(imageBytes, "image.jpg") } returns Result.success("job-123")
    val result = useCase(uri)
    assertTrue(result is UploadImageResult.Uploaded)
    val uploaded = result as UploadImageResult.Uploaded
    assertEquals("job-123", uploaded.jobId)
    assertEquals(internalUri, uploaded.imageUri)
    assertEquals("hash123", uploaded.imageHash)
  }

  @Test
  fun `returns Failure when upload fails`() = runTest {
    val uri = mockUri()
    val internalUri = mockUri("file:///internal/image.jpg")
    val imageBytes = byteArrayOf(1, 2, 3)
    coEvery { imageProcessor.readBytes(uri) } returns imageBytes
    every { imageProcessor.computeHash(imageBytes) } returns "hash123"
    coEvery { repository.getCachedSolve("hash123") } returns null
    coEvery { imageProcessor.copyToInternalStorage(imageBytes) } returns internalUri
    coEvery { imageProcessor.compressForUpload(imageBytes) } returns imageBytes
    coEvery { repository.uploadImage(imageBytes, "image.jpg") } returns
        Result.failure(RuntimeException("Network error"))
    val result = useCase(uri)
    assertTrue(result is UploadImageResult.Failure)
    assertEquals("Network error", (result as UploadImageResult.Failure).error)
  }

  @Test
  fun `returns Failure when image read throws exception`() = runTest {
    val uri = mockUri()
    coEvery { imageProcessor.readBytes(uri) } throws RuntimeException("Cannot read image")
    val result = useCase(uri)
    assertTrue(result is UploadImageResult.Failure)
    assertEquals("Cannot read image", (result as UploadImageResult.Failure).error)
  }
}
