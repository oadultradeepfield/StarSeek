package com.oadultradeepfield.starseek.domain.usecase

import app.cash.turbine.test
import com.oadultradeepfield.starseek.domain.model.BatchUploadProgress
import com.oadultradeepfield.starseek.domain.model.ImageUploadStatus
import com.oadultradeepfield.starseek.domain.model.UploadStep
import com.oadultradeepfield.starseek.domain.repository.ImageProcessor
import com.oadultradeepfield.starseek.testutil.mockUri
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BatchUploadUseCaseTest {
  private val testDispatcher = UnconfinedTestDispatcher()
  private lateinit var processSingleUpload: ProcessSingleUploadUseCase
  private lateinit var imageProcessor: ImageProcessor
  private lateinit var useCase: BatchUploadUseCase

  @Before
  fun setup() {
    processSingleUpload = mockk()
    imageProcessor = mockk()
    justRun { imageProcessor.logBenchmarkSummary() }
    useCase = BatchUploadUseCase(processSingleUpload, imageProcessor, testDispatcher)
  }

  @Test
  fun `emits initial pending state`() = runTest {
    val uri = mockUri()
    every { processSingleUpload(uri) } returns flowOf(ImageUploadStatus.Completed(42L))
    useCase(listOf(uri)).test {
      val initial = awaitItem()
      assertEquals(1, initial.items.size)
      assertEquals(ImageUploadStatus.Pending, initial.items[0].status)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `single upload completes`() = runTest {
    val uri = mockUri()
    every { processSingleUpload(uri) } returns
        flowOf(
            ImageUploadStatus.InProgress(UploadStep.Uploading),
            ImageUploadStatus.Completed(42L),
        )
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
    val uri = mockUri()
    every { processSingleUpload(uri) } returns
        flowOf(
            ImageUploadStatus.InProgress(UploadStep.Uploading),
            ImageUploadStatus.Failed("Network error"),
        )
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
    val uri = mockUri()
    every { processSingleUpload(uri) } returns
        flowOf(
            ImageUploadStatus.InProgress(UploadStep.Uploading),
            ImageUploadStatus.InProgress(UploadStep.Analyzing),
            ImageUploadStatus.InProgress(UploadStep.Saving),
            ImageUploadStatus.Completed(99L),
        )
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
  fun `multiple images upload in parallel`() = runTest {
    val uri1 = mockUri("file:///test/image1.jpg")
    val uri2 = mockUri("file:///test/image2.jpg")
    every { processSingleUpload(uri1) } returns
        flowOf(
            ImageUploadStatus.InProgress(UploadStep.Uploading),
            ImageUploadStatus.Completed(1L),
        )
    every { processSingleUpload(uri2) } returns
        flowOf(
            ImageUploadStatus.InProgress(UploadStep.Uploading),
            ImageUploadStatus.Completed(2L),
        )
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
    val uri1 = mockUri("file:///test/image1.jpg")
    val uri2 = mockUri("file:///test/image2.jpg")
    every { processSingleUpload(uri1) } returns
        flowOf(
            ImageUploadStatus.InProgress(UploadStep.Uploading),
            ImageUploadStatus.Completed(1L),
        )
    every { processSingleUpload(uri2) } returns
        flowOf(
            ImageUploadStatus.InProgress(UploadStep.Uploading),
            ImageUploadStatus.Failed("Failed"),
        )
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
