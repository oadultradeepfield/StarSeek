package com.oadultradeepfield.starseek.domain.model

import android.net.Uri

sealed class BatchUploadState {
  data class InProgress(val items: List<ImageUploadStatus>) : BatchUploadState()

  data class Completed(val result: BatchUploadResult) : BatchUploadState()
}

sealed class ImageUploadStatus {
  abstract val uri: Uri

  data class Pending(override val uri: Uri) : ImageUploadStatus()

  data class Processing(override val uri: Uri, val progress: UploadProgress) : ImageUploadStatus()

  data class Succeeded(override val uri: Uri, val solveId: Long) : ImageUploadStatus()

  data class Failed(override val uri: Uri, val error: String) : ImageUploadStatus()
}

sealed class BatchUploadResult {
  data class Success(val solveIds: List<Long>) : BatchUploadResult()

  data class AllFailed(val firstError: String) : BatchUploadResult()
}
