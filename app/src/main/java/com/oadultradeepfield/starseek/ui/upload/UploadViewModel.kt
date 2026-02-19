package com.oadultradeepfield.starseek.ui.upload

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oadultradeepfield.starseek.di.BackgroundDispatcher
import com.oadultradeepfield.starseek.domain.model.PollResult
import com.oadultradeepfield.starseek.domain.model.UploadImageResult
import com.oadultradeepfield.starseek.domain.usecase.PollJobStatusUseCase
import com.oadultradeepfield.starseek.domain.usecase.SaveSolveUseCase
import com.oadultradeepfield.starseek.domain.usecase.UploadImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@HiltViewModel
class UploadViewModel
@Inject
constructor(
    private val uploadImage: UploadImageUseCase,
    private val pollJobStatus: PollJobStatusUseCase,
    private val saveSolve: SaveSolveUseCase,
    @BackgroundDispatcher private val backgroundDispatcher: CoroutineDispatcher,
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
          val statuses = uris.associateWith<Uri, ImageStatus> { ImageStatus.Pending }.toMutableMap()
          val mutex = Mutex()

          fun emitProgress() {
            _uiState.update {
              UploadUiState.Processing(
                  statuses.map { (uri, status) -> ImageProcessingItem(uri, status) }
              )
            }
          }

          emitProgress()

          val jobs =
              uris.map { uri ->
                launch(backgroundDispatcher) {
                  mutex.withLock {
                    statuses[uri] = ImageStatus.Processing(UploadStep.Uploading)
                    emitProgress()
                  }
                  val uploadResult = uploadImage(uri)
                  val finalStatus =
                      processUploadResult(uploadResult, uri, mutex, statuses, ::emitProgress)
                  mutex.withLock {
                    statuses[uri] = finalStatus
                    emitProgress()
                  }
                }
              }

          jobs.joinAll()

          val successIds =
              statuses.values.filterIsInstance<ImageStatus.Completed>().map { it.solveId }
          val failures = statuses.values.filterIsInstance<ImageStatus.Failed>()
          _uiState.update {
            when {
              successIds.isNotEmpty() -> UploadUiState.Success(successIds)
              failures.isNotEmpty() -> UploadUiState.Error(failures.first().error, uris)
              else -> UploadUiState.Error("Unknown error", uris)
            }
          }
        }
  }

  private suspend fun processUploadResult(
      uploadResult: UploadImageResult,
      uri: Uri,
      mutex: Mutex,
      statuses: MutableMap<Uri, ImageStatus>,
      emitProgress: () -> Unit,
  ): ImageStatus {
    return when (uploadResult) {
      is UploadImageResult.CacheHit -> ImageStatus.Completed(uploadResult.solveId)
      is UploadImageResult.Failure -> ImageStatus.Failed(uploadResult.error)
      is UploadImageResult.Uploaded -> {
        mutex.withLock {
          statuses[uri] = ImageStatus.Processing(UploadStep.Analyzing)
          emitProgress()
        }
        when (val pollResult = pollJobStatus(uploadResult.jobId)) {
          is PollResult.Failure -> ImageStatus.Failed(pollResult.error)
          is PollResult.Success -> {
            mutex.withLock {
              statuses[uri] = ImageStatus.Processing(UploadStep.Saving)
              emitProgress()
            }
            val enrichedSolve =
                pollResult.solve.copy(
                    imageUri = uploadResult.imageUri.toString(),
                    imageHash = uploadResult.imageHash,
                )
            val solveId = saveSolve(enrichedSolve)
            ImageStatus.Completed(solveId)
          }
        }
      }
    }
  }

  companion object {
    const val MAX_IMAGES = 5
  }
}
