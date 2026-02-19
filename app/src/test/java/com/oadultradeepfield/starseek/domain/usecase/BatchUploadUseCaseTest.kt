package com.oadultradeepfield.starseek.domain.usecase

import android.net.Uri
import com.oadultradeepfield.starseek.domain.model.BatchUploadResult
import com.oadultradeepfield.starseek.domain.model.BatchUploadState
import com.oadultradeepfield.starseek.domain.model.ImageUploadStatus
import com.oadultradeepfield.starseek.domain.model.UploadResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BatchUploadUseCaseTest {
  private lateinit var processAndUploadImage: ProcessAndUploadImageUseCase
  private lateinit var useCase: BatchUploadUseCase

  @Before
  fun setup() {
    processAndUploadImage = mockk()
    useCase = BatchUploadUseCase(processAndUploadImage)
  }

  private fun createUri(path: String = "file:///test/image.jpg"): Uri {
    val uri = mockk<Uri>()
    every { uri.toString() } returns path
    return uri
  }

  @Test
  fun `emits initial pending state for all uris`() =
      runTest(UnconfinedTestDispatcher()) {
        val uri1 = createUri("file:///test/image1.jpg")
        val uri2 = createUri("file:///test/image2.jpg")
        coEvery { processAndUploadImage(any(), any()) } returns UploadResult.Success(1L)

        val states = useCase(listOf(uri1, uri2)).toList()
        val first = states.first() as BatchUploadState.InProgress
        assertEquals(2, first.items.size)
        assertTrue(first.items.all { it is ImageUploadStatus.Pending })
      }

  @Test
  fun `emits success result when all uploads succeed`() =
      runTest(UnconfinedTestDispatcher()) {
        val uri1 = createUri("file:///test/image1.jpg")
        val uri2 = createUri("file:///test/image2.jpg")
        coEvery { processAndUploadImage(uri1, any()) } returns UploadResult.Success(1L)
        coEvery { processAndUploadImage(uri2, any()) } returns UploadResult.CacheHit(2L)

        val states = useCase(listOf(uri1, uri2)).toList()
        val lastState = states.last() as BatchUploadState.Completed
        val result = lastState.result as BatchUploadResult.Success
        assertEquals(2, result.solveIds.size)
        assertTrue(result.solveIds.containsAll(listOf(1L, 2L)))
      }

  @Test
  fun `emits AllFailed when all uploads fail`() =
      runTest(UnconfinedTestDispatcher()) {
        val uri1 = createUri("file:///test/image1.jpg")
        val uri2 = createUri("file:///test/image2.jpg")
        coEvery { processAndUploadImage(uri1, any()) } returns UploadResult.Failure("Error 1")
        coEvery { processAndUploadImage(uri2, any()) } returns UploadResult.Failure("Error 2")

        val states = useCase(listOf(uri1, uri2)).toList()
        val lastState = states.last() as BatchUploadState.Completed
        val result = lastState.result as BatchUploadResult.AllFailed
        assertTrue(result.firstError == "Error 1" || result.firstError == "Error 2")
      }

  @Test
  fun `emits Success with partial results when some uploads fail`() =
      runTest(UnconfinedTestDispatcher()) {
        val uri1 = createUri("file:///test/image1.jpg")
        val uri2 = createUri("file:///test/image2.jpg")
        coEvery { processAndUploadImage(uri1, any()) } returns UploadResult.Success(1L)
        coEvery { processAndUploadImage(uri2, any()) } returns UploadResult.Failure("Failed")

        val states = useCase(listOf(uri1, uri2)).toList()
        val lastState = states.last() as BatchUploadState.Completed
        val result = lastState.result as BatchUploadResult.Success
        assertEquals(listOf(1L), result.solveIds)
      }

  @Test
  fun `emits in-progress states during upload`() =
      runTest(UnconfinedTestDispatcher()) {
        val uri = createUri()
        coEvery { processAndUploadImage(uri, any()) } returns UploadResult.Success(1L)

        val states = useCase(listOf(uri)).toList()
        val inProgressStates = states.filterIsInstance<BatchUploadState.InProgress>()
        assertTrue("Should have at least one InProgress state", inProgressStates.isNotEmpty())
        assertTrue("Should end with Completed", states.last() is BatchUploadState.Completed)
      }

  @Test
  fun `handles single uri upload`() =
      runTest(UnconfinedTestDispatcher()) {
        val uri = createUri()
        coEvery { processAndUploadImage(uri, any()) } returns UploadResult.Success(42L)

        val states = useCase(listOf(uri)).toList()
        val lastState = states.last() as BatchUploadState.Completed
        val result = lastState.result as BatchUploadResult.Success
        assertEquals(listOf(42L), result.solveIds)
      }
}
