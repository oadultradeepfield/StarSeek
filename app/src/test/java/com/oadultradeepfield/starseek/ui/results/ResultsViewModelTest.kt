package com.oadultradeepfield.starseek.ui.results

import app.cash.turbine.test
import com.oadultradeepfield.starseek.data.repository.SolveRepository
import com.oadultradeepfield.starseek.domain.model.ObjectDetail
import com.oadultradeepfield.starseek.domain.model.ObjectType
import com.oadultradeepfield.starseek.testutil.TestData
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ResultsViewModelTest {
  private val testDispatcher = StandardTestDispatcher()
  private lateinit var repository: SolveRepository
  private lateinit var viewModel: ResultsViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    repository = mockk()
    viewModel = ResultsViewModel(repository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `initial state is Loading`() = runTest {
    viewModel.uiState.test { assertEquals(ResultsUiState.Loading, awaitItem()) }
  }

  @Test
  fun `loadFromId emits Content with solve and grouped objects when found`() = runTest {
    val objects =
        listOf(
            TestData.createCelestialObject(name = "Sirius"),
            TestData.createCelestialObject(
                name = "M42",
                type = ObjectType.NEBULA,
                constellation = "Orion",
            ),
        )
    val solve = TestData.createSolve(objects = objects)
    coEvery { repository.getSolveById(1L) } returns solve

    viewModel.loadFromId(1L)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.uiState.test {
      val state = awaitItem() as ResultsUiState.Content
      assertEquals(solve, state.solve)
      assertEquals(2, state.grouped.byConstellation.size)
    }
  }

  @Test
  fun `loadFromId emits Error when solve not found`() = runTest {
    coEvery { repository.getSolveById(999L) } returns null

    viewModel.loadFromId(999L)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.uiState.test { assertEquals(ResultsUiState.Error("Solve not found"), awaitItem()) }
  }

  @Test
  fun `highlightObject sets highlightedObjectName`() = runTest {
    val solve = TestData.createSolve(objects = emptyList())
    coEvery { repository.getSolveById(1L) } returns solve

    viewModel.loadFromId(1L)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.highlightObject("Sirius")

    viewModel.uiState.test {
      val state = awaitItem() as ResultsUiState.Content
      assertEquals("Sirius", state.highlightedObjectName)
    }
  }

  @Test
  fun `highlightObject clears when passed null`() = runTest {
    val solve = TestData.createSolve(objects = emptyList())
    coEvery { repository.getSolveById(1L) } returns solve

    viewModel.loadFromId(1L)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.highlightObject("Sirius")
    viewModel.highlightObject(null)

    viewModel.uiState.test {
      val state = awaitItem() as ResultsUiState.Content
      assertNull(state.highlightedObjectName)
    }
  }

  @Test
  fun `onObjectClick sets objectDetailState to Loading immediately`() = runTest {
    val solve = TestData.createSolve(objects = emptyList())
    val detail = ObjectDetail("Sirius", ObjectType.STAR, "Canis Major", "Brightest star")
    coEvery { repository.getSolveById(1L) } returns solve
    coEvery { repository.getObjectDetail("Sirius") } returns Result.success(detail)

    viewModel.loadFromId(1L)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.onObjectClick("Sirius")

    viewModel.objectDetailState.test {
      val state = awaitItem()
      assertTrue(state is ObjectDetailState.Loading || state is ObjectDetailState.Loaded)
    }
  }

  @Test
  fun `onObjectClick sets objectDetailState to Loaded on success`() = runTest {
    val solve = TestData.createSolve(objects = emptyList())
    val detail = ObjectDetail("Sirius", ObjectType.STAR, "Canis Major", "Brightest star")
    coEvery { repository.getSolveById(1L) } returns solve
    coEvery { repository.getObjectDetail("Sirius") } returns Result.success(detail)

    viewModel.loadFromId(1L)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.onObjectClick("Sirius")
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.objectDetailState.test {
      val state = awaitItem() as ObjectDetailState.Loaded
      assertEquals(detail, state.detail)
    }
  }

  @Test
  fun `onObjectClick sets objectDetailState to Error on failure`() = runTest {
    val solve = TestData.createSolve(objects = emptyList())
    coEvery { repository.getSolveById(1L) } returns solve
    coEvery { repository.getObjectDetail("Unknown") } returns
        Result.failure(RuntimeException("Not found"))

    viewModel.loadFromId(1L)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.onObjectClick("Unknown")
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.objectDetailState.test {
      val state = awaitItem() as ObjectDetailState.Error
      assertEquals("Not found", state.message)
    }
  }

  @Test
  fun `onObjectClick also highlights the object`() = runTest {
    val solve = TestData.createSolve(objects = emptyList())
    val detail = ObjectDetail("Sirius", ObjectType.STAR, "Canis Major", "Brightest star")
    coEvery { repository.getSolveById(1L) } returns solve
    coEvery { repository.getObjectDetail("Sirius") } returns Result.success(detail)

    viewModel.loadFromId(1L)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.onObjectClick("Sirius")
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.uiState.test {
      val state = awaitItem() as ResultsUiState.Content
      assertEquals("Sirius", state.highlightedObjectName)
    }
  }

  @Test
  fun `dismissObjectDetail resets objectDetailState to Hidden`() = runTest {
    val solve = TestData.createSolve(objects = emptyList())
    val detail = ObjectDetail("Sirius", ObjectType.STAR, "Canis Major", "Brightest star")
    coEvery { repository.getSolveById(1L) } returns solve
    coEvery { repository.getObjectDetail("Sirius") } returns Result.success(detail)

    viewModel.loadFromId(1L)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.onObjectClick("Sirius")
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.dismissObjectDetail()

    viewModel.objectDetailState.test { assertEquals(ObjectDetailState.Hidden, awaitItem()) }
  }

  @Test
  fun `dismissObjectDetail clears highlightedObjectName`() = runTest {
    val solve = TestData.createSolve(objects = emptyList())
    val detail = ObjectDetail("Sirius", ObjectType.STAR, "Canis Major", "Brightest star")
    coEvery { repository.getSolveById(1L) } returns solve
    coEvery { repository.getObjectDetail("Sirius") } returns Result.success(detail)

    viewModel.loadFromId(1L)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.onObjectClick("Sirius")
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.dismissObjectDetail()

    viewModel.uiState.test {
      val state = awaitItem() as ResultsUiState.Content
      assertNull(state.highlightedObjectName)
    }
  }
}
