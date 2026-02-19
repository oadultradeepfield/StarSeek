package com.oadultradeepfield.starseek.ui.upload

import android.net.Uri

sealed class UploadStep {
  data object Uploading : UploadStep()

  data object Analyzing : UploadStep()

  data object Saving : UploadStep()
}

sealed class ImageStatus {
  data object Pending : ImageStatus()

  data class Processing(val step: UploadStep) : ImageStatus()

  data class Completed(val solveId: Long) : ImageStatus()

  data class Failed(val error: String) : ImageStatus()
}

data class ImageProcessingItem(val uri: Uri, val status: ImageStatus)

sealed class UploadUiState {
  data object Empty : UploadUiState()

  data class ImagesSelected(val uris: List<Uri>) : UploadUiState()

  data class Processing(val items: List<ImageProcessingItem>) : UploadUiState()

  data class Success(val solveIds: List<Long>) : UploadUiState()

  data class Error(val message: String, val lastUris: List<Uri>? = null) : UploadUiState()
}
