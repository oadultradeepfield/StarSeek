package com.oadultradeepfield.starseek.domain.usecase

import android.net.Uri
import com.oadultradeepfield.starseek.di.BackgroundDispatcher
import com.oadultradeepfield.starseek.domain.model.BatchUploadProgress
import com.oadultradeepfield.starseek.domain.model.ImageUploadState
import com.oadultradeepfield.starseek.domain.model.ImageUploadStatus
import com.oadultradeepfield.starseek.domain.model.PollResult
import com.oadultradeepfield.starseek.domain.model.UploadImageResult
import com.oadultradeepfield.starseek.domain.model.UploadStep
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

class BatchUploadUseCase
@Inject
constructor(
    private val uploadImage: UploadImageUseCase,
    private val pollJobStatus: PollJobStatusUseCase,
    private val saveSolve: SaveSolveUseCase,
    @param:BackgroundDispatcher private val backgroundDispatcher: CoroutineDispatcher,
) {
  operator fun invoke(uris: List<Uri>): Flow<BatchUploadProgress> = channelFlow {
    val statuses =
        uris.associateWith<Uri, ImageUploadStatus> { ImageUploadStatus.Pending }.toMutableMap()
    val mutex = Mutex()

    suspend fun emitProgress() {
      val items = mutex.withLock { statuses.map { (uri, status) -> ImageUploadState(uri, status) } }
      send(BatchUploadProgress(items))
    }

    emitProgress()

    uris
        .map { uri ->
          async(backgroundDispatcher) {
            mutex.withLock { statuses[uri] = ImageUploadStatus.InProgress(UploadStep.Uploading) }
            emitProgress()
            val finalStatus = processUpload(uri, mutex, statuses, ::emitProgress)
            mutex.withLock { statuses[uri] = finalStatus }
            emitProgress()
          }
        }
        .awaitAll()
  }

  private suspend fun processUpload(
      uri: Uri,
      mutex: Mutex,
      statuses: MutableMap<Uri, ImageUploadStatus>,
      emitProgress: suspend () -> Unit,
  ): ImageUploadStatus {
    return when (val uploadResult = uploadImage(uri)) {
      is UploadImageResult.CacheHit -> ImageUploadStatus.Completed(uploadResult.solveId)
      is UploadImageResult.Failure -> ImageUploadStatus.Failed(uploadResult.error)
      is UploadImageResult.Uploaded -> {
        mutex.withLock { statuses[uri] = ImageUploadStatus.InProgress(UploadStep.Analyzing) }
        emitProgress()

        when (val pollResult = pollJobStatus(uploadResult.jobId)) {
          is PollResult.Failure -> ImageUploadStatus.Failed(pollResult.error)
          is PollResult.Success -> {
            mutex.withLock { statuses[uri] = ImageUploadStatus.InProgress(UploadStep.Saving) }
            emitProgress()

            val enrichedSolve =
                pollResult.solve.copy(
                    imageUri = uploadResult.imageUri.toString(),
                    imageHash = uploadResult.imageHash,
                )

            val solveId = saveSolve(enrichedSolve)
            ImageUploadStatus.Completed(solveId)
          }
        }
      }
    }
  }
}
