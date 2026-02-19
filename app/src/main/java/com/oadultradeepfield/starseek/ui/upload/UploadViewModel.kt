package com.oadultradeepfield.starseek.ui.upload

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oadultradeepfield.starseek.domain.model.ImageUploadStatus
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
          batchUpload(uris).collect { progress ->
            val items = progress.items.map { ImageProcessingItem(it.uri, it.status) }
            _uiState.update { UploadUiState.Processing(items) }
          }

          val currentState = _uiState.value
          if (currentState !is UploadUiState.Processing) return@launch

          val successIds =
              currentState.items.mapNotNull { (it.status as? ImageUploadStatus.Completed)?.solveId }

          val firstError =
              currentState.items.firstNotNullOfOrNull {
                (it.status as? ImageUploadStatus.Failed)?.error
              }

          _uiState.update {
            when {
              successIds.isNotEmpty() -> UploadUiState.Success(successIds)
              firstError != null -> UploadUiState.Error(firstError, uris)
              else -> UploadUiState.Error("Unknown error", uris)
            }
          }
        }
  }

  companion object {
    const val MAX_IMAGES = 5
  }
}
