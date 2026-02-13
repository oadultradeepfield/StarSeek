package com.oadultradeepfield.starseek.domain.usecase

import android.net.Uri
import com.oadultradeepfield.starseek.domain.model.JobStatus
import com.oadultradeepfield.starseek.domain.repository.ImageProcessor
import com.oadultradeepfield.starseek.domain.repository.SolveRepository
import com.oadultradeepfield.starseek.domain.usecase.model.UploadProgress
import com.oadultradeepfield.starseek.domain.usecase.model.UploadResult
import com.oadultradeepfield.starseek.testutil.TestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProcessAndUploadImageUseCaseTest {
  private lateinit var repository: SolveRepository
  private lateinit var imageProcessor: ImageProcessor
  private lateinit var useCase: ProcessAndUploadImageUseCase

  @Before
  fun setup() {
    repository = mockk()
    imageProcessor = mockk()
    useCase = ProcessAndUploadImageUseCase(repository, imageProcessor)
  }

  private fun createUri(path: String = "file:///test/image.jpg"): Uri {
    val uri = mockk<Uri>()
    every { uri.toString() } returns path
    return uri
  }

  @Test
  fun `returns CacheHit when image hash matches cached solve`() = runTest {
    val uri = createUri()
    val cachedSolve = TestData.createSolve(id = 42)
    val imageBytes = byteArrayOf(1, 2, 3)
    val progressUpdates = mutableListOf<UploadProgress>()

    coEvery { imageProcessor.readBytes(uri) } returns imageBytes
    every { imageProcessor.computeHash(imageBytes) } returns "hash123"
    coEvery { repository.getCachedSolve("hash123") } returns cachedSolve

    val result = useCase(uri) { progressUpdates.add(it) }

    assertTrue(result is UploadResult.CacheHit)
    assertEquals(42L, (result as UploadResult.CacheHit).solveId)
    assertTrue(progressUpdates.contains(UploadProgress.CheckingCache))
  }

  @Test
  fun `returns Success after successful upload and polling`() = runTest {
    val uri = createUri()
    val internalUri = createUri("file:///internal/image.jpg")
    val imageBytes = byteArrayOf(1, 2, 3)
    val solve = TestData.createSolve(id = 0)
    val progressUpdates = mutableListOf<UploadProgress>()

    coEvery { imageProcessor.readBytes(uri) } returns imageBytes
    every { imageProcessor.computeHash(imageBytes) } returns "hash123"
    coEvery { repository.getCachedSolve("hash123") } returns null
    coEvery { imageProcessor.copyToInternalStorage(imageBytes) } returns internalUri
    coEvery { imageProcessor.compressForUpload(imageBytes) } returns imageBytes
    coEvery { repository.uploadImage(imageBytes, "image.jpg") } returns Result.success("job-123")
    coEvery { repository.getJobStatus("job-123") } returns Result.success(JobStatus.Success(solve))
    coEvery { repository.saveSolve(any()) } returns 99L

    val result = useCase(uri) { progressUpdates.add(it) }

    advanceTimeBy(6000)

    assertTrue(result is UploadResult.Success)
    assertEquals(99L, (result as UploadResult.Success).solveId)
    assertTrue(progressUpdates.contains(UploadProgress.CheckingCache))
    assertTrue(progressUpdates.contains(UploadProgress.Uploading))
    assertTrue(progressUpdates.contains(UploadProgress.Analyzing))
  }

  @Test
  fun `returns Failure when upload fails`() = runTest {
    val uri = createUri()
    val internalUri = createUri("file:///internal/image.jpg")
    val imageBytes = byteArrayOf(1, 2, 3)

    coEvery { imageProcessor.readBytes(uri) } returns imageBytes
    every { imageProcessor.computeHash(imageBytes) } returns "hash123"
    coEvery { repository.getCachedSolve("hash123") } returns null
    coEvery { imageProcessor.copyToInternalStorage(imageBytes) } returns internalUri
    coEvery { imageProcessor.compressForUpload(imageBytes) } returns imageBytes
    coEvery { repository.uploadImage(imageBytes, "image.jpg") } returns
        Result.failure(RuntimeException("Network error"))

    val result = useCase(uri) {}

    assertTrue(result is UploadResult.Failure)
    assertEquals("Network error", (result as UploadResult.Failure).error)
  }

  @Test
  fun `returns Failure when job status is Failed`() = runTest {
    val uri = createUri()
    val internalUri = createUri("file:///internal/image.jpg")
    val imageBytes = byteArrayOf(1, 2, 3)

    coEvery { imageProcessor.readBytes(uri) } returns imageBytes
    every { imageProcessor.computeHash(imageBytes) } returns "hash123"
    coEvery { repository.getCachedSolve("hash123") } returns null
    coEvery { imageProcessor.copyToInternalStorage(imageBytes) } returns internalUri
    coEvery { imageProcessor.compressForUpload(imageBytes) } returns imageBytes
    coEvery { repository.uploadImage(imageBytes, "image.jpg") } returns Result.success("job-123")
    coEvery { repository.getJobStatus("job-123") } returns
        Result.success(JobStatus.Failed("Image too dark"))

    val result = useCase(uri) {}

    advanceTimeBy(6000)

    assertTrue(result is UploadResult.Failure)
    assertEquals("Image too dark", (result as UploadResult.Failure).error)
  }

  @Test
  fun `returns Failure when image read throws exception`() = runTest {
    val uri = createUri()

    coEvery { imageProcessor.readBytes(uri) } throws RuntimeException("Cannot read image")

    val result = useCase(uri) {}

    assertTrue(result is UploadResult.Failure)
    assertEquals("Cannot read image", (result as UploadResult.Failure).error)
  }

  @Test
  fun `saves solve with correct imageUri and imageHash`() = runTest {
    val uri = createUri()
    val internalUri = createUri("file:///internal/image.jpg")
    val imageBytes = byteArrayOf(1, 2, 3)
    val solve = TestData.createSolve(id = 0, imageUri = "", imageHash = "")

    coEvery { imageProcessor.readBytes(uri) } returns imageBytes
    every { imageProcessor.computeHash(imageBytes) } returns "hash123"
    coEvery { repository.getCachedSolve("hash123") } returns null
    coEvery { imageProcessor.copyToInternalStorage(imageBytes) } returns internalUri
    coEvery { imageProcessor.compressForUpload(imageBytes) } returns imageBytes
    coEvery { repository.uploadImage(imageBytes, "image.jpg") } returns Result.success("job-123")
    coEvery { repository.getJobStatus("job-123") } returns Result.success(JobStatus.Success(solve))
    coEvery { repository.saveSolve(any()) } returns 1L

    useCase(uri) {}

    advanceTimeBy(6000)

    coVerify {
      repository.saveSolve(
          match { it.imageUri == "file:///internal/image.jpg" && it.imageHash == "hash123" }
      )
    }
  }
}
