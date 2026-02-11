package com.oadultradeepfield.starseek.ui.upload

import android.net.Uri
import app.cash.turbine.test
import com.oadultradeepfield.starseek.data.ImageProcessorImpl
import com.oadultradeepfield.starseek.data.repository.JobStatus
import com.oadultradeepfield.starseek.data.repository.SolveRepository
import com.oadultradeepfield.starseek.testutil.TestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
  private lateinit var repository: SolveRepository
  private lateinit var imageProcessor: ImageProcessorImpl
  private lateinit var viewModel: UploadViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    repository = mockk()
    imageProcessor = mockk()
    viewModel = UploadViewModel(repository, imageProcessor)
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
  fun `onImageSelected updates state to ImageSelected`() = runTest {
    val uri = createUri()
    viewModel.onImageSelected(uri)

    viewModel.uiState.test {
      val state = awaitItem() as UploadUiState.ImageSelected
      assertEquals(uri, state.uri)
    }
  }

  @Test
  fun `onUploadClick does nothing when state is not ImageSelected`() = runTest {
    viewModel.onUploadClick()
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.uiState.test { assertEquals(UploadUiState.Empty, awaitItem()) }
  }

  @Test
  fun `onUploadClick returns cached solve immediately when hash matches`() = runTest {
    val uri = createUri()
    val cachedSolve = TestData.createSolve()
    val imageBytes = byteArrayOf(1, 2, 3)

    coEvery { imageProcessor.readBytes(uri) } returns imageBytes
    every { imageProcessor.computeHash(imageBytes) } returns "hash123"
    coEvery { repository.getCachedSolve("hash123") } returns cachedSolve

    viewModel.onImageSelected(uri)
    viewModel.onUploadClick()
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.uiState.test {
      val state = awaitItem() as UploadUiState.Success
      assertEquals(cachedSolve, state.solve)
    }
  }

  @Test
  fun `onUploadClick shows Processing states during upload`() = runTest {
    val uri = createUri()
    val internalUri = createUri("file:///internal/image.jpg")
    val imageBytes = byteArrayOf(1, 2, 3)
    val solve = TestData.createSolve(id = 0)

    coEvery { imageProcessor.readBytes(uri) } returns imageBytes
    every { imageProcessor.computeHash(imageBytes) } returns "hash123"
    coEvery { repository.getCachedSolve("hash123") } returns null
    coEvery { imageProcessor.copyToInternalStorage(imageBytes) } returns internalUri
    coEvery { imageProcessor.compressForUpload(imageBytes) } returns imageBytes
    coEvery { repository.uploadImage(imageBytes, "image.jpg") } returns Result.success("job-123")
    coEvery { repository.getJobStatus("job-123") } returns Result.success(JobStatus.Success(solve))
    coEvery { repository.saveSolve(any()) } returns 1L

    viewModel.onImageSelected(uri)
    viewModel.onUploadClick()
    testDispatcher.scheduler.runCurrent()

    val state = viewModel.uiState.value
    assertTrue(state is UploadUiState.Processing)
  }

  @Test
  fun `upload failure emits Error with message and lastUri`() = runTest {
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

    viewModel.onImageSelected(uri)
    viewModel.onUploadClick()
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.uiState.test {
      val state = awaitItem() as UploadUiState.Error
      assertEquals("Network error", state.message)
      assertEquals(uri, state.lastUri)
    }
  }

  @Test
  fun `polling on JobStatus Success saves solve`() = runTest {
    val uri = createUri()
    val internalUri = createUri("file:///internal/image.jpg")
    val imageBytes = byteArrayOf(1, 2, 3)
    val solve = TestData.createSolve(id = 0)

    coEvery { imageProcessor.readBytes(uri) } returns imageBytes
    every { imageProcessor.computeHash(imageBytes) } returns "hash123"
    coEvery { repository.getCachedSolve("hash123") } returns null
    coEvery { imageProcessor.copyToInternalStorage(imageBytes) } returns internalUri
    coEvery { imageProcessor.compressForUpload(imageBytes) } returns imageBytes
    coEvery { repository.uploadImage(imageBytes, "image.jpg") } returns Result.success("job-123")
    coEvery { repository.getJobStatus("job-123") } returns Result.success(JobStatus.Success(solve))
    coEvery { repository.saveSolve(any()) } returns 42L

    viewModel.onImageSelected(uri)
    viewModel.onUploadClick()
    testDispatcher.scheduler.advanceUntilIdle()
    advanceTimeBy(6000)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.uiState.test {
      val state = awaitItem() as UploadUiState.Success
      assertEquals(42L, state.solve.id)
    }
  }

  @Test
  fun `polling on JobStatus Failed emits Error with message`() = runTest {
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

    viewModel.onImageSelected(uri)
    viewModel.onUploadClick()
    testDispatcher.scheduler.advanceUntilIdle()
    advanceTimeBy(6000)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.uiState.test {
      val state = awaitItem() as UploadUiState.Error
      assertEquals("Image too dark", state.message)
    }
  }

  @Test
  fun `image processing exception emits Error`() = runTest {
    val uri = createUri()

    coEvery { imageProcessor.readBytes(uri) } throws RuntimeException("Cannot read image")

    viewModel.onImageSelected(uri)
    viewModel.onUploadClick()
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.uiState.test {
      val state = awaitItem() as UploadUiState.Error
      assertEquals("Cannot read image", state.message)
    }
  }

  @Test
  fun `retry does nothing when state is not Error`() = runTest {
    viewModel.retry()
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.uiState.test { assertEquals(UploadUiState.Empty, awaitItem()) }
  }

  @Test
  fun `retry restarts upload from lastUri when in Error state`() = runTest {
    val uri = createUri()
    val cachedSolve = TestData.createSolve()
    val imageBytes = byteArrayOf(1, 2, 3)

    coEvery { imageProcessor.readBytes(uri) } throws
        RuntimeException("Network error") andThen
        imageBytes
    every { imageProcessor.computeHash(imageBytes) } returns "hash123"
    coEvery { repository.getCachedSolve("hash123") } returns cachedSolve

    viewModel.onImageSelected(uri)
    viewModel.onUploadClick()
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.uiState.test { assertTrue(awaitItem() is UploadUiState.Error) }

    viewModel.retry()
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.uiState.test {
      val state = awaitItem() as UploadUiState.Success
      assertEquals(cachedSolve, state.solve)
    }
  }

  @Test
  fun `reset sets state to Empty`() = runTest {
    val uri = createUri()
    viewModel.onImageSelected(uri)

    viewModel.reset()

    viewModel.uiState.test { assertEquals(UploadUiState.Empty, awaitItem()) }
  }
}
