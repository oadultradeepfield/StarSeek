package com.oadultradeepfield.starseek.domain.usecase

import com.oadultradeepfield.starseek.domain.model.JobStatus
import com.oadultradeepfield.starseek.domain.model.PollResult
import com.oadultradeepfield.starseek.domain.repository.SolveRepository
import com.oadultradeepfield.starseek.testutil.TestData
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PollJobStatusUseCaseTest {
  private lateinit var repository: SolveRepository
  private lateinit var useCase: PollJobStatusUseCase

  @Before
  fun setup() {
    repository = mockk()
    useCase = PollJobStatusUseCase(repository)
  }

  @Test
  fun `returns Success when job completes successfully`() = runTest {
    val solve = TestData.createSolve()
    coEvery { repository.getJobStatus("job-123") } returns Result.success(JobStatus.Success(solve))

    val result = useCase("job-123")

    advanceTimeBy(6000)
    assertTrue(result is PollResult.Success)
    assertEquals(solve, (result as PollResult.Success).solve)
  }

  @Test
  fun `returns Failure when job fails`() = runTest {
    coEvery { repository.getJobStatus("job-123") } returns
        Result.success(JobStatus.Failed("Image too dark"))

    val result = useCase("job-123")

    advanceTimeBy(6000)
    assertTrue(result is PollResult.Failure)
    assertEquals("Image too dark", (result as PollResult.Failure).error)
  }

  @Test
  fun `returns Failure when polling throws exception`() = runTest {
    coEvery { repository.getJobStatus("job-123") } returns
        Result.failure(RuntimeException("Network error"))

    val result = useCase("job-123")

    advanceTimeBy(6000)
    assertTrue(result is PollResult.Failure)
    assertEquals("Network error", (result as PollResult.Failure).error)
  }

  @Test
  fun `polls until job completes`() = runTest {
    var callCount = 0
    val solve = TestData.createSolve()
    coEvery { repository.getJobStatus("job-123") } answers
        {
          callCount++
          if (callCount < 3) Result.success(JobStatus.Processing)
          else Result.success(JobStatus.Success(solve))
        }

    val result = useCase("job-123")

    advanceTimeBy(20000)
    assertTrue(result is PollResult.Success)
    assertEquals(3, callCount)
  }
}
