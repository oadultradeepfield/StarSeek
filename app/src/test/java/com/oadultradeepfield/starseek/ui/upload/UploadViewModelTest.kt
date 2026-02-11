package com.oadultradeepfield.starseek.ui.upload

import android.net.Uri
import app.cash.turbine.test
import com.oadultradeepfield.starseek.domain.model.JobStatus
import com.oadultradeepfield.starseek.domain.repository.ImageProcessor
import com.oadultradeepfield.starseek.domain.repository.SolveRepository
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
  private lateinit var imageProcessor: ImageProcessor
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
  fun `onUploadClick returns cached solve immediately when hash matches`() = runTest {
    val uri = createUri()
    val cachedSolve = TestData.createSolve()
    val imageBytes = byteArrayOf(1, 2, 3)

    coEvery { imageProcessor.readBytes(uri) } returns imageBytes
    every { imageProcessor.computeHash(imageBytes) } returns "hash123"
    coEvery { repository.getCachedSolve("hash123") } returns cachedSolve

    viewModel.onImagesSelected(listOf(uri))
    viewModel.onUploadClick()
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.uiState.test {
      val state = awaitItem() as UploadUiState.Success
      assertEquals(listOf(cachedSolve.id), state.solveIds)
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

    viewModel.onImagesSelected(listOf(uri))
    viewModel.onUploadClick()
    testDispatcher.scheduler.runCurrent()

    val state = viewModel.uiState.value
    assertTrue(state is UploadUiState.Processing)
  }

  @Test
  fun `upload failure emits Error with message and lastUris`() = runTest {
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

    viewModel.onImagesSelected(listOf(uri))
    viewModel.onUploadClick()
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.uiState.test {
      val state = awaitItem() as UploadUiState.Error
      assertEquals("Network error", state.message)
      assertEquals(listOf(uri), state.lastUris)
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

    viewModel.onImagesSelected(listOf(uri))
    viewModel.onUploadClick()
    testDispatcher.scheduler.advanceUntilIdle()
    advanceTimeBy(6000)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.uiState.test {
      val state = awaitItem() as UploadUiState.Success
      assertEquals(listOf(42L), state.solveIds)
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

    viewModel.onImagesSelected(listOf(uri))
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

    viewModel.onImagesSelected(listOf(uri))
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
  fun `retry restarts upload from lastUris when in Error state`() = runTest {
    val uri = createUri()
    val cachedSolve = TestData.createSolve()
    val imageBytes = byteArrayOf(1, 2, 3)

    coEvery { imageProcessor.readBytes(uri) } throws
        RuntimeException("Network error") andThen
        imageBytes
    every { imageProcessor.computeHash(imageBytes) } returns "hash123"
    coEvery { repository.getCachedSolve("hash123") } returns cachedSolve

    viewModel.onImagesSelected(listOf(uri))
    viewModel.onUploadClick()
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.uiState.test { assertTrue(awaitItem() is UploadUiState.Error) }

    viewModel.retry()
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.uiState.test {
      val state = awaitItem() as UploadUiState.Success
      assertEquals(listOf(cachedSolve.id), state.solveIds)
    }
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
    val cachedSolve1 = TestData.createSolve(id = 1)
    val cachedSolve2 = TestData.createSolve(id = 2)
    val imageBytes = byteArrayOf(1, 2, 3)

    coEvery { imageProcessor.readBytes(uri1) } returns imageBytes
    coEvery { imageProcessor.readBytes(uri2) } returns imageBytes
    every { imageProcessor.computeHash(imageBytes) } returns "hash1" andThen "hash2"
    coEvery { repository.getCachedSolve("hash1") } returns cachedSolve1
    coEvery { repository.getCachedSolve("hash2") } returns cachedSolve2

    viewModel.onImagesSelected(listOf(uri1, uri2))
    viewModel.onUploadClick()
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.uiState.test {
      val state = awaitItem() as UploadUiState.Success
      assertEquals(2, state.solveIds.size)
      assertTrue(state.solveIds.contains(1L))
      assertTrue(state.solveIds.contains(2L))
    }
  }

  @Test
  fun `partial success navigates with successful solveIds`() = runTest {
    val uri1 = createUri("file:///test/image1.jpg")
    val uri2 = createUri("file:///test/image2.jpg")
    val cachedSolve1 = TestData.createSolve(id = 1)
    val imageBytes = byteArrayOf(1, 2, 3)

    coEvery { imageProcessor.readBytes(uri1) } returns imageBytes
    coEvery { imageProcessor.readBytes(uri2) } throws RuntimeException("Failed to read")
    every { imageProcessor.computeHash(imageBytes) } returns "hash1"
    coEvery { repository.getCachedSolve("hash1") } returns cachedSolve1

    viewModel.onImagesSelected(listOf(uri1, uri2))
    viewModel.onUploadClick()
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.uiState.test {
      val state = awaitItem() as UploadUiState.Success
      assertEquals(listOf(1L), state.solveIds)
    }
  }

  @Test
  fun `concurrency is limited to MAX_CONCURRENT_UPLOADS`() = runTest {
    val uris = (1..4).map { createUri("file:///test/image$it.jpg") }
    val imageBytes = byteArrayOf(1, 2, 3)

    uris.forEach { uri -> coEvery { imageProcessor.readBytes(uri) } returns imageBytes }
    every { imageProcessor.computeHash(imageBytes) } returns "hash"
    coEvery { repository.getCachedSolve("hash") } returns TestData.createSolve(id = 1)

    viewModel.onImagesSelected(uris)
    viewModel.onUploadClick()
    testDispatcher.scheduler.runCurrent()

    val state = viewModel.uiState.value as UploadUiState.Processing
    val processingCount = state.items.count { it.status is ImageStatus.Processing }
    val pendingCount = state.items.count { it.status is ImageStatus.Pending }
    assertTrue(
        "Should have at most ${UploadViewModel.MAX_CONCURRENT_UPLOADS} processing",
        processingCount <= UploadViewModel.MAX_CONCURRENT_UPLOADS,
    )
    assertTrue("Should have some pending items", pendingCount > 0 || processingCount < uris.size)
  }
}
