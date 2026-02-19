package com.oadultradeepfield.starseek.ui.upload

import android.net.Uri
import app.cash.turbine.test
import com.oadultradeepfield.starseek.domain.model.PollResult
import com.oadultradeepfield.starseek.domain.model.UploadImageResult
import com.oadultradeepfield.starseek.domain.usecase.PollJobStatusUseCase
import com.oadultradeepfield.starseek.domain.usecase.SaveSolveUseCase
import com.oadultradeepfield.starseek.domain.usecase.UploadImageUseCase
import com.oadultradeepfield.starseek.testutil.TestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
  private val backgroundDispatcher = UnconfinedTestDispatcher()
  private lateinit var uploadImage: UploadImageUseCase
  private lateinit var pollJobStatus: PollJobStatusUseCase
  private lateinit var saveSolve: SaveSolveUseCase
  private lateinit var viewModel: UploadViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    uploadImage = mockk()
    pollJobStatus = mockk()
    saveSolve = mockk()
    viewModel = UploadViewModel(uploadImage, pollJobStatus, saveSolve, backgroundDispatcher)
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
  fun `onUploadClick emits Success when upload completes with cache hit`() = runTest {
    val uri = createUri()
    coEvery { uploadImage(uri) } returns UploadImageResult.CacheHit(42L)

    viewModel.onImagesSelected(listOf(uri))
    viewModel.onUploadClick()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value as UploadUiState.Success
    assertEquals(listOf(42L), state.solveIds)
  }

  @Test
  fun `onUploadClick emits Success after full upload flow`() = runTest {
    val uri = createUri()
    val internalUri = createUri("file:///internal/image.jpg")
    val solve = TestData.createSolve(id = 0)
    coEvery { uploadImage(uri) } returns
        UploadImageResult.Uploaded("job-123", internalUri, "hash123")
    coEvery { pollJobStatus("job-123") } returns PollResult.Success(solve)
    coEvery { saveSolve(any()) } returns 99L

    viewModel.onImagesSelected(listOf(uri))
    viewModel.onUploadClick()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value as UploadUiState.Success
    assertEquals(listOf(99L), state.solveIds)
  }

  @Test
  fun `upload failure emits Error with message and lastUris`() = runTest {
    val uri = createUri()
    coEvery { uploadImage(uri) } returns UploadImageResult.Failure("Network error")

    viewModel.onImagesSelected(listOf(uri))
    viewModel.onUploadClick()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value as UploadUiState.Error
    assertEquals("Network error", state.message)
    assertEquals(listOf(uri), state.lastUris)
  }

  @Test
  fun `poll failure emits Error`() = runTest {
    val uri = createUri()
    val internalUri = createUri("file:///internal/image.jpg")
    coEvery { uploadImage(uri) } returns
        UploadImageResult.Uploaded("job-123", internalUri, "hash123")
    coEvery { pollJobStatus("job-123") } returns PollResult.Failure("Analysis failed")

    viewModel.onImagesSelected(listOf(uri))
    viewModel.onUploadClick()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value as UploadUiState.Error
    assertEquals("Analysis failed", state.message)
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
    coEvery { uploadImage(uri) } answers
        {
          callCount++
          if (callCount == 1) UploadImageResult.Failure("Network error")
          else UploadImageResult.CacheHit(42L)
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
    coEvery { uploadImage(uri1) } returns UploadImageResult.CacheHit(1L)
    coEvery { uploadImage(uri2) } returns UploadImageResult.CacheHit(2L)

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
    coEvery { uploadImage(uri1) } returns UploadImageResult.CacheHit(1L)
    coEvery { uploadImage(uri2) } returns UploadImageResult.Failure("Failed")

    viewModel.onImagesSelected(listOf(uri1, uri2))
    viewModel.onUploadClick()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value as UploadUiState.Success
    assertEquals(listOf(1L), state.solveIds)
  }
}
