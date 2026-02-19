package com.oadultradeepfield.starseek.ui.upload

import android.net.Uri
import com.oadultradeepfield.starseek.domain.model.ImageUploadStatus

data class ImageProcessingItem(val uri: Uri, val status: ImageUploadStatus)

sealed class UploadUiState {
  data object Empty : UploadUiState()

  data class ImagesSelected(val uris: List<Uri>) : UploadUiState()

  data class Processing(val items: List<ImageProcessingItem>) : UploadUiState()

  data class Success(val solveIds: List<Long>) : UploadUiState()

  data class Error(val message: String, val lastUris: List<Uri>? = null) : UploadUiState()
}
