package com.oadultradeepfield.starseek.domain.model

import android.net.Uri

sealed class UploadStep {
  data object Uploading : UploadStep()

  data object Analyzing : UploadStep()

  data object Saving : UploadStep()
}

sealed class ImageUploadStatus {
  data object Pending : ImageUploadStatus()

  data class InProgress(val step: UploadStep) : ImageUploadStatus()

  data class Completed(val solveId: Long) : ImageUploadStatus()

  data class Failed(val error: String) : ImageUploadStatus()
}

data class ImageUploadState(val uri: Uri, val status: ImageUploadStatus)

data class BatchUploadProgress(val items: List<ImageUploadState>)
