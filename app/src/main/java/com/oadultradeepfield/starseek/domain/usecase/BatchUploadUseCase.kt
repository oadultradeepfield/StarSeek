package com.oadultradeepfield.starseek.domain.usecase

import android.net.Uri
import com.oadultradeepfield.starseek.di.BackgroundDispatcher
import com.oadultradeepfield.starseek.domain.model.BatchUploadResult
import com.oadultradeepfield.starseek.domain.model.BatchUploadState
import com.oadultradeepfield.starseek.domain.model.ImageUploadStatus
import com.oadultradeepfield.starseek.domain.model.UploadResult
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BatchUploadUseCase
@Inject
constructor(
    private val processAndUploadImage: ProcessAndUploadImageUseCase,
    @param:BackgroundDispatcher private val backgroundDispatcher: CoroutineDispatcher,
) {
  operator fun invoke(uris: List<Uri>): Flow<BatchUploadState> = channelFlow {
    val statuses =
        uris.associateWith<Uri, ImageUploadStatus> { ImageUploadStatus.Pending(it) }.toMutableMap()

    val mutex = Mutex()

    send(BatchUploadState.InProgress(statuses.values.toList()))

    val jobs =
        uris.map { uri ->
          launch(backgroundDispatcher) {
            val result =
                processAndUploadImage(uri) { progress ->
                  launch {
                    mutex.withLock {
                      statuses[uri] = ImageUploadStatus.Processing(uri, progress)
                      send(BatchUploadState.InProgress(statuses.values.toList()))
                    }
                  }
                }

            mutex.withLock {
              statuses[uri] =
                  when (result) {
                    is UploadResult.CacheHit -> ImageUploadStatus.Succeeded(uri, result.solveId)
                    is UploadResult.Success -> ImageUploadStatus.Succeeded(uri, result.solveId)
                    is UploadResult.Failure -> ImageUploadStatus.Failed(uri, result.error)
                  }

              send(BatchUploadState.InProgress(statuses.values.toList()))
            }
          }
        }

    jobs.joinAll()

    val successIds =
        statuses.values.filterIsInstance<ImageUploadStatus.Succeeded>().map { it.solveId }

    val failures = statuses.values.filterIsInstance<ImageUploadStatus.Failed>()
    val finalResult =
        when {
          successIds.isNotEmpty() -> BatchUploadResult.Success(successIds)
          failures.isNotEmpty() -> BatchUploadResult.AllFailed(failures.first().error)
          else -> BatchUploadResult.AllFailed("Unknown error")
        }

    send(BatchUploadState.Completed(finalResult))
  }
}
