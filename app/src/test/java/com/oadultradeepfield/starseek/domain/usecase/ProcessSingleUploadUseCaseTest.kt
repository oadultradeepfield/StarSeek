package com.oadultradeepfield.starseek.domain.usecase

import android.net.Uri
import app.cash.turbine.test
import com.oadultradeepfield.starseek.domain.model.ImageUploadStatus
import com.oadultradeepfield.starseek.domain.model.PollResult
import com.oadultradeepfield.starseek.domain.model.UploadImageResult
import com.oadultradeepfield.starseek.domain.model.UploadStep
import com.oadultradeepfield.starseek.testutil.TestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ProcessSingleUploadUseCaseTest {
  private lateinit var uploadImage: UploadImageUseCase
  private lateinit var pollJobStatus: PollJobStatusUseCase
  private lateinit var saveSolve: SaveSolveUseCase
  private lateinit var useCase: ProcessSingleUploadUseCase

  @Before
  fun setup() {
    uploadImage = mockk()
    pollJobStatus = mockk()
    saveSolve = mockk()
    useCase = ProcessSingleUploadUseCase(uploadImage, pollJobStatus, saveSolve)
  }

  private fun createUri(path: String = "file:///test/image.jpg"): Uri {
    val uri = mockk<Uri>()
    every { uri.toString() } returns path
    return uri
  }

  @Test
  fun `cache hit skips poll and save`() = runTest {
    val uri = createUri()
    coEvery { uploadImage(uri) } returns UploadImageResult.CacheHit(42L)

    val statuses = mutableListOf<ImageUploadStatus>()
    useCase(uri).test {
      while (true) {
        val status = awaitItem()
        statuses.add(status)
        if (status is ImageUploadStatus.Completed) break
      }
      awaitComplete()
    }

    assertEquals(
        listOf(
            ImageUploadStatus.InProgress(UploadStep.Uploading),
            ImageUploadStatus.Completed(42L),
        ),
        statuses,
    )
    coVerify(exactly = 0) { pollJobStatus(any()) }
    coVerify(exactly = 0) { saveSolve(any()) }
  }

  @Test
  fun `upload failure emits failed status`() = runTest {
    val uri = createUri()
    coEvery { uploadImage(uri) } returns UploadImageResult.Failure("Network error")

    val statuses = mutableListOf<ImageUploadStatus>()
    useCase(uri).test {
      while (true) {
        val status = awaitItem()
        statuses.add(status)
        if (status is ImageUploadStatus.Failed) break
      }
      awaitComplete()
    }

    assertEquals(
        listOf(
            ImageUploadStatus.InProgress(UploadStep.Uploading),
            ImageUploadStatus.Failed("Network error"),
        ),
        statuses,
    )
  }

  @Test
  fun `poll failure emits failed status`() = runTest {
    val uri = createUri()
    val internalUri = createUri("file:///internal/image.jpg")
    coEvery { uploadImage(uri) } returns
        UploadImageResult.Uploaded("job-123", internalUri, "hash123")
    coEvery { pollJobStatus("job-123") } returns PollResult.Failure("Analysis failed")

    val statuses = mutableListOf<ImageUploadStatus>()
    useCase(uri).test {
      while (true) {
        val status = awaitItem()
        statuses.add(status)
        if (status is ImageUploadStatus.Failed) break
      }
      awaitComplete()
    }

    assertEquals(
        listOf(
            ImageUploadStatus.InProgress(UploadStep.Uploading),
            ImageUploadStatus.InProgress(UploadStep.Analyzing),
            ImageUploadStatus.Failed("Analysis failed"),
        ),
        statuses,
    )
    coVerify(exactly = 0) { saveSolve(any()) }
  }

  @Test
  fun `full upload flow emits all steps and enriches solve`() = runTest {
    val uri = createUri()
    val internalUri = createUri("file:///internal/image.jpg")
    val solve = TestData.createSolve(id = 0)
    coEvery { uploadImage(uri) } returns
        UploadImageResult.Uploaded("job-123", internalUri, "hash123")
    coEvery { pollJobStatus("job-123") } returns PollResult.Success(solve)
    coEvery { saveSolve(any()) } returns 99L

    val statuses = mutableListOf<ImageUploadStatus>()
    useCase(uri).test {
      while (true) {
        val status = awaitItem()
        statuses.add(status)
        if (status is ImageUploadStatus.Completed) break
      }
      awaitComplete()
    }

    assertEquals(
        listOf(
            ImageUploadStatus.InProgress(UploadStep.Uploading),
            ImageUploadStatus.InProgress(UploadStep.Analyzing),
            ImageUploadStatus.InProgress(UploadStep.Saving),
            ImageUploadStatus.Completed(99L),
        ),
        statuses,
    )
    coVerify {
      saveSolve(match { it.imageUri == "file:///internal/image.jpg" && it.imageHash == "hash123" })
    }
  }
}
