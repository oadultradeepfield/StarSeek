package com.oadultradeepfield.starseek.ui.upload

import android.net.Uri
import com.oadultradeepfield.starseek.domain.model.Solve

sealed class UploadUiState {
    data object Empty : UploadUiState()
    data class ImageSelected(val uri: Uri) : UploadUiState()
    data class Processing(val uri: Uri, val message: String) : UploadUiState()
    data class Success(val solve: Solve) : UploadUiState()
    data class Error(val message: String, val lastUri: Uri? = null) : UploadUiState()
}
