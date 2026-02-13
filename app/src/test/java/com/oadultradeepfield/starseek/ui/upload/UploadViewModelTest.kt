package com.oadultradeepfield.starseek.ui.upload

import android.net.Uri
import app.cash.turbine.test
import com.oadultradeepfield.starseek.domain.usecase.ProcessAndUploadImageUseCase
import com.oadultradeepfield.starseek.domain.usecase.model.UploadProgress
import com.oadultradeepfield.starseek.domain.usecase.model.UploadResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
  private lateinit var processAndUploadImage: ProcessAndUploadImageUseCase
  private lateinit var viewModel: UploadViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    processAndUploadImage = mockk()
    viewModel = UploadViewModel(processAndUploadImage)
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
  fun `onUploadClick returns cached solve immediately when CacheHit`() = runTest {
    val uri = createUri()

    coEvery { processAndUploadImage(uri, any()) } returns UploadResult.CacheHit(42L)

    viewModel.onImagesSelected(listOf(uri))
    viewModel.onUploadClick()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value as UploadUiState.Success

    assertEquals(listOf(42L), state.solveIds)
  }

  @Test
  fun `onUploadClick shows Processing states during upload`() = runTest {
    val uri = createUri()

    coEvery { processAndUploadImage(uri, any()) } coAnswers
        {
          val callback = secondArg<(UploadProgress) -> Unit>()
          callback(UploadProgress.CheckingCache)
          callback(UploadProgress.Uploading)
          UploadResult.Success(1L)
        }

    viewModel.onImagesSelected(listOf(uri))
    viewModel.onUploadClick()
    testDispatcher.scheduler.runCurrent()

    val state = viewModel.uiState.value

    assertTrue(state is UploadUiState.Processing)
  }

  @Test
  fun `upload failure emits Error with message and lastUris`() = runTest {
    val uri = createUri()

    coEvery { processAndUploadImage(uri, any()) } returns UploadResult.Failure("Network error")

    viewModel.onImagesSelected(listOf(uri))
    viewModel.onUploadClick()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value as UploadUiState.Error

    assertEquals("Network error", state.message)
    assertEquals(listOf(uri), state.lastUris)
  }

  @Test
  fun `successful upload emits Success with solveId`() = runTest {
    val uri = createUri()

    coEvery { processAndUploadImage(uri, any()) } returns UploadResult.Success(42L)

    viewModel.onImagesSelected(listOf(uri))
    viewModel.onUploadClick()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value as UploadUiState.Success

    assertEquals(listOf(42L), state.solveIds)
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

    coEvery { processAndUploadImage(uri, any()) } returns
        UploadResult.Failure("Network error") andThen
        UploadResult.CacheHit(42L)

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
  fun `multiple images upload processes in parallel`() = runTest {
    val uri1 = createUri("file:///test/image1.jpg")
    val uri2 = createUri("file:///test/image2.jpg")

    coEvery { processAndUploadImage(uri1, any()) } returns UploadResult.CacheHit(1L)
    coEvery { processAndUploadImage(uri2, any()) } returns UploadResult.CacheHit(2L)

    viewModel.onImagesSelected(listOf(uri1, uri2))
    viewModel.onUploadClick()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value as UploadUiState.Success

    assertEquals(2, state.solveIds.size)
    assertTrue(state.solveIds.contains(1L))
    assertTrue(state.solveIds.contains(2L))
  }

  @Test
  fun `partial success navigates with successful solveIds`() = runTest {
    val uri1 = createUri("file:///test/image1.jpg")
    val uri2 = createUri("file:///test/image2.jpg")

    coEvery { processAndUploadImage(uri1, any()) } returns UploadResult.CacheHit(1L)
    coEvery { processAndUploadImage(uri2, any()) } returns UploadResult.Failure("Failed to read")

    viewModel.onImagesSelected(listOf(uri1, uri2))
    viewModel.onUploadClick()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value as UploadUiState.Success

    assertEquals(listOf(1L), state.solveIds)
  }
}
