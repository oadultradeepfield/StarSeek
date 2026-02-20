package com.oadultradeepfield.starseek.domain.usecase

import android.net.Uri
import com.oadultradeepfield.starseek.domain.model.UploadImageResult
import com.oadultradeepfield.starseek.domain.repository.ImageProcessor
import com.oadultradeepfield.starseek.domain.repository.SolveRepository
import javax.inject.Inject

class UploadImageUseCase
@Inject
constructor(
    private val repository: SolveRepository,
    private val imageProcessor: ImageProcessor,
) {
  suspend operator fun invoke(uri: Uri): UploadImageResult {
    return try {
      val imageBytes = imageProcessor.readBytes(uri)
      val imageHash = imageProcessor.computeHash(imageBytes)
      val cached = repository.getCachedSolve(imageHash)

      if (cached != null) return UploadImageResult.CacheHit(cached.id)

      val compressedBytes = imageProcessor.compressForUpload(imageBytes)
      val internalUri = imageProcessor.copyToInternalStorage(compressedBytes)

      repository
          .uploadImage(compressedBytes, "image.jpg")
          .fold(
              onSuccess = { jobId -> UploadImageResult.Uploaded(jobId, internalUri, imageHash) },
              onFailure = { e -> UploadImageResult.Failure(e.message ?: "Upload failed") },
          )
    } catch (e: Exception) {
      UploadImageResult.Failure(e.message ?: "Failed to process image")
    }
  }
}
