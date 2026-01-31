package com.oadultradeepfield.starseek.data.repository

import com.oadultradeepfield.starseek.domain.model.ObjectDetail
import com.oadultradeepfield.starseek.domain.model.Solve
import kotlinx.coroutines.flow.Flow

interface SolveRepository {
  suspend fun getCachedSolve(imageHash: String): Solve?

  suspend fun uploadImage(imageBytes: ByteArray, fileName: String): Result<String>

  suspend fun getJobStatus(jobId: String): Result<JobStatus>

  suspend fun saveSolve(solve: Solve): Long

  fun getAllSolves(): Flow<List<Solve>>

  suspend fun getSolveById(id: Long): Solve?

  suspend fun deleteSolve(id: Long)

  suspend fun getObjectDetail(objectName: String): Result<ObjectDetail>
}
