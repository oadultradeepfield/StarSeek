package com.oadultradeepfield.starseek.ui.history

import android.net.Uri
import app.cash.turbine.test
import com.oadultradeepfield.starseek.domain.repository.ImageProcessor
import com.oadultradeepfield.starseek.domain.repository.SolveRepository
import com.oadultradeepfield.starseek.testutil.TestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
  private val testDispatcher = StandardTestDispatcher()
  private lateinit var repository: SolveRepository
  private lateinit var imageProcessor: ImageProcessor

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    repository = mockk()
    imageProcessor = mockk()
    mockkStatic(Uri::class)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkStatic(Uri::class)
  }

  @Test
  fun `emits Content when solves exist`() = runTest {
    val solves = listOf(TestData.createSolve(id = 1), TestData.createSolve(id = 2))
    every { repository.getAllSolves() } returns flowOf(solves)

    val viewModel = HistoryViewModel(repository, imageProcessor)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.uiState.test { assertEquals(HistoryUiState.Content(solves), awaitItem()) }
  }

  @Test
  fun `emits Empty when no solves exist`() = runTest {
    every { repository.getAllSolves() } returns flowOf(emptyList())

    val viewModel = HistoryViewModel(repository, imageProcessor)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.uiState.test { assertEquals(HistoryUiState.Empty, awaitItem()) }
  }

  @Test
  fun `onDeleteClick sets deleteConfirmId`() = runTest {
    every { repository.getAllSolves() } returns flowOf(emptyList())

    val viewModel = HistoryViewModel(repository, imageProcessor)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.onDeleteClick(42L)

    viewModel.deleteConfirmId.test { assertEquals(42L, awaitItem()) }
  }

  @Test
  fun `onDeleteDismiss clears deleteConfirmId`() = runTest {
    every { repository.getAllSolves() } returns flowOf(emptyList())

    val viewModel = HistoryViewModel(repository, imageProcessor)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.onDeleteClick(42L)
    viewModel.onDeleteDismiss()

    viewModel.deleteConfirmId.test { assertNull(awaitItem()) }
  }

  @Test
  fun `onDeleteConfirm deletes solve and image then clears deleteConfirmId`() = runTest {
    val imageUri = mockk<Uri>()
    val solve = TestData.createSolve(id = 1, imageUri = "file:///image.jpg")

    every { repository.getAllSolves() } returns flowOf(listOf(solve))
    coEvery { repository.getSolveById(1L) } returns solve
    every { Uri.parse("file:///image.jpg") } returns imageUri
    every { imageProcessor.deleteImage(imageUri) } returns Unit
    coEvery { repository.deleteSolve(1L) } returns Unit

    val viewModel = HistoryViewModel(repository, imageProcessor)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.onDeleteClick(1L)
    viewModel.onDeleteConfirm()
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify { imageProcessor.deleteImage(imageUri) }
    coVerify { repository.deleteSolve(1L) }

    viewModel.deleteConfirmId.test { assertNull(awaitItem()) }
  }

  @Test
  fun `onDeleteConfirm does nothing when deleteConfirmId is null`() = runTest {
    every { repository.getAllSolves() } returns flowOf(emptyList())

    val viewModel = HistoryViewModel(repository, imageProcessor)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.onDeleteConfirm()
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify(exactly = 0) { repository.getSolveById(any()) }
    coVerify(exactly = 0) { repository.deleteSolve(any()) }
  }
}
