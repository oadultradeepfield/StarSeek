package com.oadultradeepfield.starseek.domain.usecase

import android.net.Uri
import com.oadultradeepfield.starseek.domain.repository.ImageProcessor
import com.oadultradeepfield.starseek.domain.repository.SolveRepository
import com.oadultradeepfield.starseek.testutil.TestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class DeleteSolveWithImageUseCaseTest {
  private lateinit var repository: SolveRepository
  private lateinit var imageProcessor: ImageProcessor
  private lateinit var useCase: DeleteSolveWithImageUseCase

  @Before
  fun setup() {
    repository = mockk()
    imageProcessor = mockk()

    useCase = DeleteSolveWithImageUseCase(repository, imageProcessor)

    mockkStatic(Uri::class)
  }

  @After
  fun tearDown() {
    unmockkStatic(Uri::class)
  }

  @Test
  fun `deletes image and solve when solve exists`() = runTest {
    val imageUri = mockk<Uri>()
    val solve = TestData.createSolve(id = 1, imageUri = "file:///image.jpg")

    coEvery { repository.getSolveById(1L) } returns solve

    every { Uri.parse("file:///image.jpg") } returns imageUri
    every { imageProcessor.deleteImage(imageUri) } returns Unit

    coEvery { repository.deleteSolve(1L) } returns Unit

    useCase(1L)

    coVerify { imageProcessor.deleteImage(imageUri) }
    coVerify { repository.deleteSolve(1L) }
  }

  @Test
  fun `does nothing when solve not found`() = runTest {
    coEvery { repository.getSolveById(999L) } returns null

    useCase(999L)

    coVerify(exactly = 0) { imageProcessor.deleteImage(any()) }
    coVerify(exactly = 0) { repository.deleteSolve(any()) }
  }
}
