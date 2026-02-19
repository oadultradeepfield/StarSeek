package com.oadultradeepfield.starseek.domain.usecase

import android.net.Uri
import app.cash.turbine.test
import com.oadultradeepfield.starseek.domain.model.BatchUploadProgress
import com.oadultradeepfield.starseek.domain.model.ImageUploadStatus
import com.oadultradeepfield.starseek.domain.model.PollResult
import com.oadultradeepfield.starseek.domain.model.UploadImageResult
import com.oadultradeepfield.starseek.domain.model.UploadStep
import com.oadultradeepfield.starseek.testutil.TestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BatchUploadUseCaseTest {
  private val testDispatcher = UnconfinedTestDispatcher()
  private lateinit var uploadImage: UploadImageUseCase
  private lateinit var pollJobStatus: PollJobStatusUseCase
  private lateinit var saveSolve: SaveSolveUseCase
  private lateinit var useCase: BatchUploadUseCase

  @Before
  fun setup() {
    uploadImage = mockk()
    pollJobStatus = mockk()
    saveSolve = mockk()
    useCase = BatchUploadUseCase(uploadImage, pollJobStatus, saveSolve, testDispatcher)
  }

  private fun createUri(path: String = "file:///test/image.jpg"): Uri {
    val uri = mockk<Uri>()
    every { uri.toString() } returns path
    return uri
  }

  @Test
  fun `emits initial pending state`() = runTest {
    val uri = createUri()
    coEvery { uploadImage(uri) } returns UploadImageResult.CacheHit(42L)

    useCase(listOf(uri)).test {
      val initial = awaitItem()
      assertEquals(1, initial.items.size)
      assertEquals(ImageUploadStatus.Pending, initial.items[0].status)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `cache hit completes immediately`() = runTest {
    val uri = createUri()
    coEvery { uploadImage(uri) } returns UploadImageResult.CacheHit(42L)

    useCase(listOf(uri)).test {
      var lastStatus: ImageUploadStatus?
      while (true) {
        val item = awaitItem()
        lastStatus = item.items[0].status
        if (lastStatus is ImageUploadStatus.Completed) break
      }
      assertEquals(42L, lastStatus.solveId)
      awaitComplete()
    }
  }

  @Test
  fun `upload failure emits failed status`() = runTest {
    val uri = createUri()
    coEvery { uploadImage(uri) } returns UploadImageResult.Failure("Network error")

    useCase(listOf(uri)).test {
      var lastStatus: ImageUploadStatus?
      while (true) {
        val item = awaitItem()
        lastStatus = item.items[0].status
        if (lastStatus is ImageUploadStatus.Failed) break
      }
      assertEquals("Network error", lastStatus.error)
      awaitComplete()
    }
  }

  @Test
  fun `full upload flow emits all steps`() = runTest {
    val uri = createUri()
    val internalUri = createUri("file:///internal/image.jpg")
    val solve = TestData.createSolve(id = 0)
    coEvery { uploadImage(uri) } returns
        UploadImageResult.Uploaded("job-123", internalUri, "hash123")
    coEvery { pollJobStatus("job-123") } returns PollResult.Success(solve)
    coEvery { saveSolve(any()) } returns 99L

    val steps = mutableListOf<ImageUploadStatus>()
    useCase(listOf(uri)).test {
      while (true) {
        val item = awaitItem()
        steps.add(item.items[0].status)
        if (item.items[0].status is ImageUploadStatus.Completed) break
      }
      awaitComplete()
    }

    assertTrue(steps.any { it == ImageUploadStatus.Pending })
    assertTrue(steps.any { it == ImageUploadStatus.InProgress(UploadStep.Uploading) })
    assertTrue(steps.any { it == ImageUploadStatus.InProgress(UploadStep.Analyzing) })
    assertTrue(steps.any { it == ImageUploadStatus.InProgress(UploadStep.Saving) })
    assertTrue(steps.any { it is ImageUploadStatus.Completed && it.solveId == 99L })
  }

  @Test
  fun `poll failure emits failed status`() = runTest {
    val uri = createUri()
    val internalUri = createUri("file:///internal/image.jpg")
    coEvery { uploadImage(uri) } returns
        UploadImageResult.Uploaded("job-123", internalUri, "hash123")
    coEvery { pollJobStatus("job-123") } returns PollResult.Failure("Analysis failed")

    useCase(listOf(uri)).test {
      var lastStatus: ImageUploadStatus?
      while (true) {
        val item = awaitItem()
        lastStatus = item.items[0].status
        if (lastStatus is ImageUploadStatus.Failed) break
      }
      assertEquals("Analysis failed", lastStatus.error)
      awaitComplete()
    }
  }

  @Test
  fun `multiple images upload in parallel`() = runTest {
    val uri1 = createUri("file:///test/image1.jpg")
    val uri2 = createUri("file:///test/image2.jpg")
    coEvery { uploadImage(uri1) } returns UploadImageResult.CacheHit(1L)
    coEvery { uploadImage(uri2) } returns UploadImageResult.CacheHit(2L)

    useCase(listOf(uri1, uri2)).test {
      var lastItem: BatchUploadProgress?
      while (true) {
        lastItem = awaitItem()
        val completedCount = lastItem.items.count { it.status is ImageUploadStatus.Completed }
        if (completedCount == 2) break
      }
      val solveIds =
          lastItem.items.mapNotNull { (it.status as? ImageUploadStatus.Completed)?.solveId }
      assertEquals(2, solveIds.size)
      assertTrue(solveIds.contains(1L))
      assertTrue(solveIds.contains(2L))
      awaitComplete()
    }
  }

  @Test
  fun `partial failure with multiple images`() = runTest {
    val uri1 = createUri("file:///test/image1.jpg")
    val uri2 = createUri("file:///test/image2.jpg")
    coEvery { uploadImage(uri1) } returns UploadImageResult.CacheHit(1L)
    coEvery { uploadImage(uri2) } returns UploadImageResult.Failure("Failed")

    useCase(listOf(uri1, uri2)).test {
      var lastItem: BatchUploadProgress?
      while (true) {
        lastItem = awaitItem()
        val completed = lastItem.items.count { it.status is ImageUploadStatus.Completed }
        val failed = lastItem.items.count { it.status is ImageUploadStatus.Failed }
        if (completed + failed == 2) break
      }

      val successCount = lastItem.items.count { it.status is ImageUploadStatus.Completed }
      val failCount = lastItem.items.count { it.status is ImageUploadStatus.Failed }
      assertEquals(1, successCount)
      assertEquals(1, failCount)
      awaitComplete()
    }
  }
}
