package com.oadultradeepfield.starseek.ui.upload

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oadultradeepfield.starseek.domain.model.JobStatus
import com.oadultradeepfield.starseek.domain.repository.ImageProcessor
import com.oadultradeepfield.starseek.domain.repository.SolveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

@HiltViewModel
class UploadViewModel
@Inject
constructor(
    private val repository: SolveRepository,
    private val imageProcessor: ImageProcessor,
) : ViewModel() {
  private val _uiState = MutableStateFlow<UploadUiState>(UploadUiState.Empty)
  val uiState: StateFlow<UploadUiState> = _uiState

  private val pollingJobs = mutableMapOf<Uri, Job>()
  private val imageStatuses = mutableMapOf<Uri, ImageStatus>()
  private val statusMutex = Mutex()
  private val uploadSemaphore = Semaphore(MAX_CONCURRENT_UPLOADS)

  fun onImagesSelected(uris: List<Uri>) {
    cancelAllPolling()
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
      viewModelScope.launch { uploadSemaphore.withPermit { processImage(uri) } }
    }
  }

  private suspend fun processImage(uri: Uri) {
    try {
      updateImageStatus(uri, ImageStatus.Processing("Checking cache..."))
      val imageBytes = imageProcessor.readBytes(uri)
      val imageHash = imageProcessor.computeHash(imageBytes)
      val cached = repository.getCachedSolve(imageHash)

      if (cached != null) {
        updateImageStatus(uri, ImageStatus.Completed(cached.id))
        checkAllCompleted()
        return
      }

      val internalUri = imageProcessor.copyToInternalStorage(imageBytes)
      updateImageStatus(uri, ImageStatus.Processing("Uploading..."))
      val compressedBytes = imageProcessor.compressForUpload(imageBytes)
      val result = repository.uploadImage(compressedBytes, "image.jpg")

      result.fold(
          onSuccess = { jobId ->
            updateImageStatus(uri, ImageStatus.Processing("Analyzing stars..."))
            pollJobStatus(jobId, uri, internalUri, imageHash)
          },
          onFailure = { e ->
            updateImageStatus(uri, ImageStatus.Failed(e.message ?: "Upload failed"))
            checkAllCompleted()
          },
      )
    } catch (e: Exception) {
      updateImageStatus(uri, ImageStatus.Failed(e.message ?: "Failed to process image"))
      checkAllCompleted()
    }
  }

  private fun pollJobStatus(jobId: String, originalUri: Uri, imageUri: Uri, imageHash: String) {
    pollingJobs[originalUri]?.cancel()
    pollingJobs[originalUri] =
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
                      updateImageStatus(originalUri, ImageStatus.Completed(id))
                      checkAllCompleted()
                      return@launch
                    }
                    is JobStatus.Failed -> {
                      updateImageStatus(originalUri, ImageStatus.Failed(status.error))
                      checkAllCompleted()
                      return@launch
                    }
                  }
                },
                onFailure = { e ->
                  updateImageStatus(originalUri, ImageStatus.Failed(e.message ?: "Polling failed"))
                  checkAllCompleted()
                  return@launch
                },
            )
          }
        }
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

  private fun cancelAllPolling() {
    pollingJobs.values.forEach { it.cancel() }
    pollingJobs.clear()
  }

  fun reset() {
    cancelAllPolling()
    imageStatuses.clear()
    _uiState.update { UploadUiState.Empty }
  }

  companion object {
    const val MAX_IMAGES = 5
    const val MAX_CONCURRENT_UPLOADS = 2
  }
}
