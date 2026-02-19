package com.oadultradeepfield.starseek.ui.upload

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oadultradeepfield.starseek.domain.model.BatchUploadResult
import com.oadultradeepfield.starseek.domain.model.BatchUploadState
import com.oadultradeepfield.starseek.domain.model.ImageUploadStatus
import com.oadultradeepfield.starseek.domain.model.UploadProgress
import com.oadultradeepfield.starseek.domain.usecase.BatchUploadUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UploadViewModel
@Inject
constructor(
    private val batchUpload: BatchUploadUseCase,
) : ViewModel() {
  private val _uiState = MutableStateFlow<UploadUiState>(UploadUiState.Empty)
  val uiState: StateFlow<UploadUiState> = _uiState

  private var uploadJob: Job? = null

  fun onImagesSelected(uris: List<Uri>) {
    uploadJob?.cancel()
    _uiState.update { UploadUiState.ImagesSelected(uris) }
  }

  fun onUploadClick() {
    val currentState = _uiState.value
    if (currentState !is UploadUiState.ImagesSelected) return
    startUploads(currentState.uris)
  }

  fun retry() {
    val currentState = _uiState.value
    if (currentState !is UploadUiState.Error) return
    val uris = currentState.lastUris ?: return
    _uiState.update { UploadUiState.ImagesSelected(uris) }
    startUploads(uris)
  }

  fun reset() {
    uploadJob?.cancel()
    _uiState.update { UploadUiState.Empty }
  }

  private fun startUploads(uris: List<Uri>) {
    uploadJob =
        viewModelScope.launch {
          batchUpload(uris).collect { state -> _uiState.update { mapToUiState(state, uris) } }
        }
  }

  private fun mapToUiState(state: BatchUploadState, uris: List<Uri>): UploadUiState =
      when (state) {
        is BatchUploadState.InProgress ->
            UploadUiState.Processing(
                state.items.map { status ->
                  ImageProcessingItem(status.uri, mapStatusToImageStatus(status))
                }
            )
        is BatchUploadState.Completed ->
            when (val result = state.result) {
              is BatchUploadResult.Success -> UploadUiState.Success(result.solveIds)
              is BatchUploadResult.AllFailed -> UploadUiState.Error(result.firstError, uris)
            }
      }

  private fun mapStatusToImageStatus(status: ImageUploadStatus): ImageStatus =
      when (status) {
        is ImageUploadStatus.Pending -> ImageStatus.Pending
        is ImageUploadStatus.Processing ->
            ImageStatus.Processing(progressToMessage(status.progress))
        is ImageUploadStatus.Succeeded -> ImageStatus.Completed(status.solveId)
        is ImageUploadStatus.Failed -> ImageStatus.Failed(status.error)
      }

  private fun progressToMessage(progress: UploadProgress): String =
      when (progress) {
        UploadProgress.CheckingCache -> "Checking cache..."
        UploadProgress.Uploading -> "Uploading..."
        UploadProgress.Analyzing -> "Analyzing stars..."
      }

  companion object {
    const val MAX_IMAGES = 5
  }
}
