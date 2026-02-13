package com.oadultradeepfield.starseek.ui.history

import app.cash.turbine.test
import com.oadultradeepfield.starseek.domain.usecase.DeleteSolveWithImageUseCase
import com.oadultradeepfield.starseek.domain.usecase.ObserveSolvesUseCase
import com.oadultradeepfield.starseek.testutil.TestData
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
  private val testDispatcher = StandardTestDispatcher()
  private lateinit var observeSolves: ObserveSolvesUseCase
  private lateinit var deleteSolveWithImage: DeleteSolveWithImageUseCase

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    observeSolves = mockk()
    deleteSolveWithImage = mockk()
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `emits Content when solves exist`() = runTest {
    val solves = listOf(TestData.createSolve(id = 1), TestData.createSolve(id = 2))
    every { observeSolves() } returns flowOf(solves)
    val viewModel = HistoryViewModel(observeSolves, deleteSolveWithImage)
    viewModel.uiState.test {
      assertEquals(HistoryUiState.Loading, awaitItem())
      assertEquals(HistoryUiState.Content(solves), awaitItem())
    }
  }

  @Test
  fun `emits Empty when no solves exist`() = runTest {
    every { observeSolves() } returns flowOf(emptyList())
    val viewModel = HistoryViewModel(observeSolves, deleteSolveWithImage)
    viewModel.uiState.test {
      assertEquals(HistoryUiState.Loading, awaitItem())
      assertEquals(HistoryUiState.Empty, awaitItem())
    }
  }

  @Test
  fun `onDeleteClick sets deleteConfirmId`() = runTest {
    every { observeSolves() } returns flowOf(emptyList())
    val viewModel = HistoryViewModel(observeSolves, deleteSolveWithImage)
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.onDeleteClick(42L)
    viewModel.deleteConfirmId.test { assertEquals(42L, awaitItem()) }
  }

  @Test
  fun `onDeleteDismiss clears deleteConfirmId`() = runTest {
    every { observeSolves() } returns flowOf(emptyList())
    val viewModel = HistoryViewModel(observeSolves, deleteSolveWithImage)
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.onDeleteClick(42L)
    viewModel.onDeleteDismiss()
    viewModel.deleteConfirmId.test { assertNull(awaitItem()) }
  }

  @Test
  fun `onDeleteConfirm calls use case and clears deleteConfirmId`() = runTest {
    val solve = TestData.createSolve(id = 1, imageUri = "file:///image.jpg")
    every { observeSolves() } returns flowOf(listOf(solve))
    coEvery { deleteSolveWithImage(1L) } returns Unit
    val viewModel = HistoryViewModel(observeSolves, deleteSolveWithImage)
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.onDeleteClick(1L)
    viewModel.onDeleteConfirm()
    testDispatcher.scheduler.advanceUntilIdle()
    coVerify { deleteSolveWithImage(1L) }
    viewModel.deleteConfirmId.test { assertNull(awaitItem()) }
  }

  @Test
  fun `onDeleteConfirm does nothing when deleteConfirmId is null`() = runTest {
    every { observeSolves() } returns flowOf(emptyList())
    val viewModel = HistoryViewModel(observeSolves, deleteSolveWithImage)
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.onDeleteConfirm()
    testDispatcher.scheduler.advanceUntilIdle()
    coVerify(exactly = 0) { deleteSolveWithImage(any()) }
  }
}
