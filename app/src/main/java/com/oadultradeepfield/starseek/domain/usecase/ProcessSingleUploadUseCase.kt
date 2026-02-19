package com.oadultradeepfield.starseek.domain.usecase

import android.net.Uri
import com.oadultradeepfield.starseek.domain.model.ImageUploadStatus
import com.oadultradeepfield.starseek.domain.model.PollResult
import com.oadultradeepfield.starseek.domain.model.UploadImageResult
import com.oadultradeepfield.starseek.domain.model.UploadStep
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ProcessSingleUploadUseCase
@Inject
constructor(
    private val uploadImage: UploadImageUseCase,
    private val pollJobStatus: PollJobStatusUseCase,
    private val saveSolve: SaveSolveUseCase,
) {
  operator fun invoke(uri: Uri): Flow<ImageUploadStatus> = flow {
    emit(ImageUploadStatus.InProgress(UploadStep.Uploading))

    when (val uploadResult = uploadImage(uri)) {
      is UploadImageResult.CacheHit -> emit(ImageUploadStatus.Completed(uploadResult.solveId))
      is UploadImageResult.Failure -> emit(ImageUploadStatus.Failed(uploadResult.error))

      is UploadImageResult.Uploaded -> {
        emit(ImageUploadStatus.InProgress(UploadStep.Analyzing))

        when (val pollResult = pollJobStatus(uploadResult.jobId)) {
          is PollResult.Failure -> emit(ImageUploadStatus.Failed(pollResult.error))
          is PollResult.Success -> {
            emit(ImageUploadStatus.InProgress(UploadStep.Saving))

            val enrichedSolve =
                pollResult.solve.copy(
                    imageUri = uploadResult.imageUri.toString(),
                    imageHash = uploadResult.imageHash,
                )

            emit(ImageUploadStatus.Completed(saveSolve(enrichedSolve)))
          }
        }
      }
    }
  }
}
