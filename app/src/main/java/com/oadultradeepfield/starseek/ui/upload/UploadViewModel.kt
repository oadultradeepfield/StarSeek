package com.oadultradeepfield.starseek.ui.upload

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oadultradeepfield.starseek.data.ImageProcessorImpl
import com.oadultradeepfield.starseek.data.repository.JobStatus
import com.oadultradeepfield.starseek.data.repository.SolveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UploadViewModel
@Inject
constructor(
    private val repository: SolveRepository,
    private val imageProcessor: ImageProcessorImpl,
) : ViewModel() {
  private val _uiState = MutableStateFlow<UploadUiState>(UploadUiState.Empty)
  val uiState: StateFlow<UploadUiState> = _uiState

  private var pollingJob: Job? = null

  fun onImageSelected(uri: Uri) {
    cancelPolling()
    _uiState.update { UploadUiState.ImageSelected(uri) }
  }

  fun onUploadClick() {
    val currentState = _uiState.value
    if (currentState !is UploadUiState.ImageSelected) return
    startUpload(currentState.uri)
  }

  fun retry() {
    val currentState = _uiState.value
    if (currentState !is UploadUiState.Error) return
    val uri = currentState.lastUri ?: return
    _uiState.update { UploadUiState.ImageSelected(uri) }
    startUpload(uri)
  }

  private fun startUpload(uri: Uri) {
    viewModelScope.launch {
      try {
        _uiState.update { UploadUiState.Processing(uri, "Checking cache...") }

        val imageBytes = imageProcessor.readBytes(uri)
        val imageHash = imageProcessor.computeHash(imageBytes)

        val cached = repository.getCachedSolve(imageHash)
        if (cached != null) {
          _uiState.update { UploadUiState.Success(cached) }
          return@launch
        }

        val internalUri = imageProcessor.copyToInternalStorage(imageBytes)
        _uiState.update { UploadUiState.Processing(internalUri, "Uploading...") }

        val compressedBytes = imageProcessor.compressForUpload(imageBytes)
        val result = repository.uploadImage(compressedBytes, "image.jpg")

        result.fold(
            onSuccess = { jobId ->
              _uiState.update { UploadUiState.Processing(internalUri, "Analyzing stars...") }
              pollJobStatus(jobId, internalUri, imageHash)
            },
            onFailure = { e ->
              _uiState.update { UploadUiState.Error(e.message ?: "Upload failed", uri) }
            },
        )
      } catch (e: Exception) {
        _uiState.update { UploadUiState.Error(e.message ?: "Failed to process image", uri) }
      }
    }
  }

  private fun pollJobStatus(jobId: String, imageUri: Uri, imageHash: String) {
    cancelPolling()

    pollingJob =
        viewModelScope.launch {
          while (currentCoroutineContext().isActive) {
            delay(5000)
            currentCoroutineContext().ensureActive()

            val result = repository.getJobStatus(jobId)

            result.fold(
                onSuccess = { status ->
                  when (status) {
                    is JobStatus.Processing -> {}
                    is JobStatus.Success -> {
                      val solve =
                          status.solve.copy(imageUri = imageUri.toString(), imageHash = imageHash)
                      val id = repository.saveSolve(solve)
                      _uiState.update { UploadUiState.Success(solve.copy(id = id)) }
                      return@launch
                    }
                    is JobStatus.Failed -> {
                      _uiState.update { UploadUiState.Error(status.error, imageUri) }
                      return@launch
                    }
                  }
                },
                onFailure = { e ->
                  _uiState.update { UploadUiState.Error(e.message ?: "Polling failed", imageUri) }
                  return@launch
                },
            )
          }
        }
  }

  private fun cancelPolling() {
    pollingJob?.cancel()
    pollingJob = null
  }

  fun reset() {
    cancelPolling()
    _uiState.update { UploadUiState.Empty }
  }
}
