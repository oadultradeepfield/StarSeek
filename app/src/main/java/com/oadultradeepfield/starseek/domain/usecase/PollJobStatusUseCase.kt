package com.oadultradeepfield.starseek.domain.usecase

import com.oadultradeepfield.starseek.domain.model.JobStatus
import com.oadultradeepfield.starseek.domain.model.PollResult
import com.oadultradeepfield.starseek.domain.repository.SolveRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import javax.inject.Inject

class PollJobStatusUseCase
@Inject
constructor(
    private val repository: SolveRepository,
) {
  suspend operator fun invoke(jobId: String): PollResult {
    while (currentCoroutineContext().isActive) {
      delay(POLL_INTERVAL_MS)
      currentCoroutineContext().ensureActive()
      val result = repository.getJobStatus(jobId)
      result.fold(
          onSuccess = { status ->
            when (status) {
              is JobStatus.Processing -> {}
              is JobStatus.Success -> return PollResult.Success(status.solve)
              is JobStatus.Failed -> return PollResult.Failure(status.error)
            }
          },
          onFailure = { e ->
            return PollResult.Failure(e.message ?: "Polling failed")
          },
      )
    }
    return PollResult.Failure("Cancelled")
  }

  companion object {
    private const val POLL_INTERVAL_MS = 5000L
  }
}
