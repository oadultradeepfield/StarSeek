package com.oadultradeepfield.starseek.ui.upload

import android.net.Uri
import app.cash.turbine.test
import com.oadultradeepfield.starseek.domain.model.BatchUploadResult
import com.oadultradeepfield.starseek.domain.model.BatchUploadState
import com.oadultradeepfield.starseek.domain.model.ImageUploadStatus
import com.oadultradeepfield.starseek.domain.model.UploadProgress
import com.oadultradeepfield.starseek.domain.usecase.BatchUploadUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UploadViewModelTest {
  private val testDispatcher = StandardTestDispatcher()
  private lateinit var batchUpload: BatchUploadUseCase
  private lateinit var viewModel: UploadViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    batchUpload = mockk()
    viewModel = UploadViewModel(batchUpload)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun createUri(path: String = "file:///test/image.jpg"): Uri {
    val uri = mockk<Uri>()
    every { uri.toString() } returns path
    return uri
  }

  @Test
  fun `initial state is Empty`() = runTest {
    viewModel.uiState.test { assertEquals(UploadUiState.Empty, awaitItem()) }
  }

  @Test
  fun `onImagesSelected updates state to ImagesSelected`() = runTest {
    val uri = createUri()
    viewModel.onImagesSelected(listOf(uri))
    viewModel.uiState.test {
      val state = awaitItem() as UploadUiState.ImagesSelected
      assertEquals(listOf(uri), state.uris)
    }
  }

  @Test
  fun `onUploadClick does nothing when state is not ImagesSelected`() = runTest {
    viewModel.onUploadClick()
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.uiState.test { assertEquals(UploadUiState.Empty, awaitItem()) }
  }

  @Test
  fun `onUploadClick emits Success when batch completes successfully`() = runTest {
    val uri = createUri()
    every { batchUpload(listOf(uri)) } returns
        flowOf(
            BatchUploadState.InProgress(listOf(ImageUploadStatus.Pending(uri))),
            BatchUploadState.Completed(BatchUploadResult.Success(listOf(42L))),
        )

    viewModel.onImagesSelected(listOf(uri))
    viewModel.onUploadClick()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value as UploadUiState.Success
    assertEquals(listOf(42L), state.solveIds)
  }

  @Test
  fun `onUploadClick shows Processing states during upload`() = runTest {
    val uri = createUri()
    every { batchUpload(listOf(uri)) } returns
        flowOf(
            BatchUploadState.InProgress(listOf(ImageUploadStatus.Pending(uri))),
            BatchUploadState.InProgress(
                listOf(ImageUploadStatus.Processing(uri, UploadProgress.Uploading))
            ),
            BatchUploadState.Completed(BatchUploadResult.Success(listOf(1L))),
        )

    viewModel.uiState.test {
      assertEquals(UploadUiState.Empty, awaitItem())
      viewModel.onImagesSelected(listOf(uri))
      assertTrue(awaitItem() is UploadUiState.ImagesSelected)
      viewModel.onUploadClick()
      testDispatcher.scheduler.advanceUntilIdle()
      val states = mutableListOf<UploadUiState>()
      while (true) {
        val state = awaitItem()
        states.add(state)
        if (state is UploadUiState.Success) break
      }
      assertTrue(states.any { it is UploadUiState.Processing })
    }
  }

  @Test
  fun `upload failure emits Error with message and lastUris`() = runTest {
    val uri = createUri()
    every { batchUpload(listOf(uri)) } returns
        flowOf(
            BatchUploadState.InProgress(listOf(ImageUploadStatus.Pending(uri))),
            BatchUploadState.Completed(BatchUploadResult.AllFailed("Network error")),
        )

    viewModel.onImagesSelected(listOf(uri))
    viewModel.onUploadClick()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value as UploadUiState.Error
    assertEquals("Network error", state.message)
    assertEquals(listOf(uri), state.lastUris)
  }

  @Test
  fun `retry does nothing when state is not Error`() = runTest {
    viewModel.retry()
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.uiState.test { assertEquals(UploadUiState.Empty, awaitItem()) }
  }

  @Test
  fun `retry restarts upload from lastUris when in Error state`() = runTest {
    val uri = createUri()
    var callCount = 0
    every { batchUpload(listOf(uri)) } answers
        {
          callCount++
          if (callCount == 1) {
            flowOf(
                BatchUploadState.InProgress(listOf(ImageUploadStatus.Pending(uri))),
                BatchUploadState.Completed(BatchUploadResult.AllFailed("Network error")),
            )
          } else {
            flowOf(
                BatchUploadState.InProgress(listOf(ImageUploadStatus.Pending(uri))),
                BatchUploadState.Completed(BatchUploadResult.Success(listOf(42L))),
            )
          }
        }

    viewModel.onImagesSelected(listOf(uri))
    viewModel.onUploadClick()
    testDispatcher.scheduler.advanceUntilIdle()
    assertTrue(viewModel.uiState.value is UploadUiState.Error)

    viewModel.retry()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value as UploadUiState.Success
    assertEquals(listOf(42L), state.solveIds)
  }

  @Test
  fun `reset sets state to Empty`() = runTest {
    val uri = createUri()
    viewModel.onImagesSelected(listOf(uri))
    viewModel.reset()
    viewModel.uiState.test { assertEquals(UploadUiState.Empty, awaitItem()) }
  }

  @Test
  fun `multiple images upload returns all solveIds`() = runTest {
    val uri1 = createUri("file:///test/image1.jpg")
    val uri2 = createUri("file:///test/image2.jpg")
    every { batchUpload(listOf(uri1, uri2)) } returns
        flowOf(
            BatchUploadState.InProgress(
                listOf(ImageUploadStatus.Pending(uri1), ImageUploadStatus.Pending(uri2))
            ),
            BatchUploadState.Completed(BatchUploadResult.Success(listOf(1L, 2L))),
        )

    viewModel.onImagesSelected(listOf(uri1, uri2))
    viewModel.onUploadClick()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value as UploadUiState.Success
    assertEquals(2, state.solveIds.size)
    assertTrue(state.solveIds.contains(1L))
    assertTrue(state.solveIds.contains(2L))
  }

  @Test
  fun `partial success returns only successful solveIds`() = runTest {
    val uri1 = createUri("file:///test/image1.jpg")
    val uri2 = createUri("file:///test/image2.jpg")
    every { batchUpload(listOf(uri1, uri2)) } returns
        flowOf(
            BatchUploadState.InProgress(
                listOf(ImageUploadStatus.Pending(uri1), ImageUploadStatus.Pending(uri2))
            ),
            BatchUploadState.Completed(BatchUploadResult.Success(listOf(1L))),
        )

    viewModel.onImagesSelected(listOf(uri1, uri2))
    viewModel.onUploadClick()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value as UploadUiState.Success
    assertEquals(listOf(1L), state.solveIds)
  }

  @Test
  fun `progress message maps correctly for CheckingCache`() = runTest {
    val uri = createUri()
    every { batchUpload(listOf(uri)) } returns
        flowOf(
            BatchUploadState.InProgress(
                listOf(ImageUploadStatus.Processing(uri, UploadProgress.CheckingCache))
            ),
            BatchUploadState.Completed(BatchUploadResult.Success(listOf(1L))),
        )

    viewModel.uiState.test {
      assertEquals(UploadUiState.Empty, awaitItem())
      viewModel.onImagesSelected(listOf(uri))
      assertTrue(awaitItem() is UploadUiState.ImagesSelected)
      viewModel.onUploadClick()
      testDispatcher.scheduler.advanceUntilIdle()
      val processingState = awaitItem() as UploadUiState.Processing
      val item = processingState.items.first()
      assertEquals("Checking cache...", (item.status as ImageStatus.Processing).message)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `progress message maps correctly for Analyzing`() = runTest {
    val uri = createUri()
    every { batchUpload(listOf(uri)) } returns
        flowOf(
            BatchUploadState.InProgress(
                listOf(ImageUploadStatus.Processing(uri, UploadProgress.Analyzing))
            ),
            BatchUploadState.Completed(BatchUploadResult.Success(listOf(1L))),
        )

    viewModel.uiState.test {
      assertEquals(UploadUiState.Empty, awaitItem())
      viewModel.onImagesSelected(listOf(uri))
      assertTrue(awaitItem() is UploadUiState.ImagesSelected)
      viewModel.onUploadClick()
      testDispatcher.scheduler.advanceUntilIdle()
      val processingState = awaitItem() as UploadUiState.Processing
      val item = processingState.items.first()
      assertEquals("Analyzing stars...", (item.status as ImageStatus.Processing).message)
      cancelAndIgnoreRemainingEvents()
    }
  }
}
