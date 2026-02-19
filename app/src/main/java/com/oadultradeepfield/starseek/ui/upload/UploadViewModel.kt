package com.oadultradeepfield.starseek.ui.upload

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oadultradeepfield.starseek.domain.model.UploadProgress
import com.oadultradeepfield.starseek.domain.model.UploadResult
import com.oadultradeepfield.starseek.domain.usecase.ProcessAndUploadImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject

@HiltViewModel
class UploadViewModel
@Inject
constructor(
    private val processAndUploadImage: ProcessAndUploadImageUseCase,
) : ViewModel() {
  private val _uiState = MutableStateFlow<UploadUiState>(UploadUiState.Empty)

  val uiState: StateFlow<UploadUiState> = _uiState

  private val uploadJobs = mutableMapOf<Uri, Job>()
  private val imageStatuses = mutableMapOf<Uri, ImageStatus>()
  private val statusMutex = Mutex()
  private val uploadSemaphore = Semaphore(MAX_CONCURRENT_UPLOADS)

  fun onImagesSelected(uris: List<Uri>) {
    cancelAllUploads()
    imageStatuses.clear()
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

  private fun startUploads(uris: List<Uri>) {
    imageStatuses.clear()
    uris.forEach { uri -> imageStatuses[uri] = ImageStatus.Pending }

    updateProcessingState()

    uris.forEach { uri ->
      uploadJobs[uri] = viewModelScope.launch { uploadSemaphore.withPermit { processImage(uri) } }
    }
  }

  private suspend fun processImage(uri: Uri) {
    val result =
        processAndUploadImage(uri) { progress ->
          val message =
              when (progress) {
                UploadProgress.CheckingCache -> "Checking cache..."
                UploadProgress.Uploading -> "Uploading..."
                UploadProgress.Analyzing -> "Analyzing stars..."
              }
          viewModelScope.launch { updateImageStatus(uri, ImageStatus.Processing(message)) }
        }

    val status =
        when (result) {
          is UploadResult.CacheHit -> ImageStatus.Completed(result.solveId)
          is UploadResult.Success -> ImageStatus.Completed(result.solveId)
          is UploadResult.Failure -> ImageStatus.Failed(result.error)
        }

    updateImageStatus(uri, status)
    checkAllCompleted()
  }

  private suspend fun updateImageStatus(uri: Uri, status: ImageStatus) {
    statusMutex.withLock {
      imageStatuses[uri] = status
      updateProcessingState()
    }
  }

  private fun updateProcessingState() {
    val items = imageStatuses.map { (uri, status) -> ImageProcessingItem(uri, status) }
    _uiState.update { UploadUiState.Processing(items) }
  }

  private suspend fun checkAllCompleted() {
    statusMutex.withLock {
      val allDone =
          imageStatuses.values.all { it is ImageStatus.Completed || it is ImageStatus.Failed }

      if (!allDone) return

      val successIds =
          imageStatuses.values.filterIsInstance<ImageStatus.Completed>().map { it.solveId }

      val failures = imageStatuses.values.filterIsInstance<ImageStatus.Failed>()

      if (successIds.isNotEmpty()) {
        _uiState.update { UploadUiState.Success(successIds) }
      } else if (failures.isNotEmpty()) {
        _uiState.update { UploadUiState.Error(failures.first().error, imageStatuses.keys.toList()) }
      }
    }
  }

  private fun cancelAllUploads() {
    uploadJobs.values.forEach { it.cancel() }
    uploadJobs.clear()
  }

  fun reset() {
    cancelAllUploads()
    imageStatuses.clear()
    _uiState.update { UploadUiState.Empty }
  }

  companion object {
    const val MAX_IMAGES = 5
    const val MAX_CONCURRENT_UPLOADS = 2
  }
}
