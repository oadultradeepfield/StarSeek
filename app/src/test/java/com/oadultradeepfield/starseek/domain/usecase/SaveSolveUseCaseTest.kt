package com.oadultradeepfield.starseek.domain.usecase

import com.oadultradeepfield.starseek.domain.repository.SolveRepository
import com.oadultradeepfield.starseek.testutil.TestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SaveSolveUseCaseTest {
  private lateinit var repository: SolveRepository
  private lateinit var useCase: SaveSolveUseCase

  @Before
  fun setup() {
    repository = mockk()
    useCase = SaveSolveUseCase(repository)
  }

  @Test
  fun `saves solve and returns id`() = runTest {
    val solve = TestData.createSolve(id = 0)
    coEvery { repository.saveSolve(solve) } returns 42L

    val result = useCase(solve)

    assertEquals(42L, result)
    coVerify { repository.saveSolve(solve) }
  }
}
