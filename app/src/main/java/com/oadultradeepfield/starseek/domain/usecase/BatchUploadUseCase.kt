package com.oadultradeepfield.starseek.domain.usecase

import android.net.Uri
import com.oadultradeepfield.starseek.di.BackgroundDispatcher
import com.oadultradeepfield.starseek.domain.model.BatchUploadProgress
import com.oadultradeepfield.starseek.domain.model.ImageUploadState
import com.oadultradeepfield.starseek.domain.model.ImageUploadStatus
import com.oadultradeepfield.starseek.domain.repository.ImageProcessor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class BatchUploadUseCase
@Inject
constructor(
    private val processSingleUpload: ProcessSingleUploadUseCase,
    private val imageProcessor: ImageProcessor,
    @param:BackgroundDispatcher private val backgroundDispatcher: CoroutineDispatcher,
) {
  operator fun invoke(uris: List<Uri>): Flow<BatchUploadProgress> = channelFlow {
    val statuses =
        ConcurrentHashMap<Uri, ImageUploadStatus>(uris.associateWith { ImageUploadStatus.Pending })

    suspend fun updateAndEmit(uri: Uri, status: ImageUploadStatus) {
      statuses[uri] = status
      send(BatchUploadProgress(statuses.map { (u, s) -> ImageUploadState(u, s) }))
    }

    send(BatchUploadProgress(uris.map { ImageUploadState(it, ImageUploadStatus.Pending) }))

    uris
        .map { uri ->
          async(backgroundDispatcher) {
            processSingleUpload(uri).collect { status -> updateAndEmit(uri, status) }
          }
        }
        .awaitAll()

    imageProcessor.logBenchmarkSummary()
  }
}
