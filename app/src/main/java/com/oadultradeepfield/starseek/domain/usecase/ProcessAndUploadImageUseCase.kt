package com.oadultradeepfield.starseek.domain.usecase

import android.net.Uri
import com.oadultradeepfield.starseek.domain.model.JobStatus
import com.oadultradeepfield.starseek.domain.model.UploadProgress
import com.oadultradeepfield.starseek.domain.model.UploadResult
import com.oadultradeepfield.starseek.domain.repository.ImageProcessor
import com.oadultradeepfield.starseek.domain.repository.SolveRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import javax.inject.Inject

class ProcessAndUploadImageUseCase
@Inject
constructor(
    private val repository: SolveRepository,
    private val imageProcessor: ImageProcessor,
) {
  suspend operator fun invoke(
      uri: Uri,
      onProgress: (UploadProgress) -> Unit,
  ): UploadResult {
    return try {
      onProgress(UploadProgress.CheckingCache)

      val imageBytes = imageProcessor.readBytes(uri)
      val imageHash = imageProcessor.computeHash(imageBytes)
      val cached = repository.getCachedSolve(imageHash)

      if (cached != null) return UploadResult.CacheHit(cached.id)

      val internalUri = imageProcessor.copyToInternalStorage(imageBytes)

      onProgress(UploadProgress.Uploading)

      val compressedBytes = imageProcessor.compressForUpload(imageBytes)
      val uploadResult = repository.uploadImage(compressedBytes, "image.jpg")

      uploadResult.fold(
          onSuccess = { jobId ->
            onProgress(UploadProgress.Analyzing)
            pollJobStatus(jobId, internalUri, imageHash)
          },
          onFailure = { e -> UploadResult.Failure(e.message ?: "Upload failed") },
      )
    } catch (e: Exception) {
      UploadResult.Failure(e.message ?: "Failed to process image")
    }
  }

  private suspend fun pollJobStatus(
      jobId: String,
      imageUri: Uri,
      imageHash: String,
  ): UploadResult {
    while (currentCoroutineContext().isActive) {
      delay(POLL_INTERVAL_MS)

      currentCoroutineContext().ensureActive()

      val result = repository.getJobStatus(jobId)

      result.fold(
          onSuccess = { status ->
            when (status) {
              is JobStatus.Processing -> {}
              is JobStatus.Success -> {
                val solve =
                    status.solve.copy(
                        imageUri = imageUri.toString(),
                        imageHash = imageHash,
                    )

                val id = repository.saveSolve(solve)
                return UploadResult.Success(id)
              }
              is JobStatus.Failed -> return UploadResult.Failure(status.error)
            }
          },
          onFailure = { e ->
            return UploadResult.Failure(e.message ?: "Polling failed")
          },
      )
    }

    return UploadResult.Failure("Cancelled")
  }

  companion object {
    private const val POLL_INTERVAL_MS = 5000L
  }
}
