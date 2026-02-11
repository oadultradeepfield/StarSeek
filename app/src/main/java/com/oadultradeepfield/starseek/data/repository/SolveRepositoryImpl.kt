package com.oadultradeepfield.starseek.data.repository

import com.oadultradeepfield.starseek.data.local.ObjectDetailDao
import com.oadultradeepfield.starseek.data.local.ObjectDetailEntity
import com.oadultradeepfield.starseek.data.local.SolveDao
import com.oadultradeepfield.starseek.data.mapper.SolveMapper
import com.oadultradeepfield.starseek.data.remote.StarSeekApi
import com.oadultradeepfield.starseek.data.remote.dto.JobStatusType
import com.oadultradeepfield.starseek.domain.model.ObjectDetail
import com.oadultradeepfield.starseek.domain.model.Solve
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class SolveRepositoryImpl
@Inject
constructor(
    private val api: StarSeekApi,
    private val dao: SolveDao,
    private val objectDetailDao: ObjectDetailDao,
    private val mapper: SolveMapper,
) : SolveRepository {
  override suspend fun getCachedSolve(imageHash: String): Solve? =
      dao.getSolveByHash(imageHash)?.let { mapper.mapToDomain(it) }

  override suspend fun uploadImage(imageBytes: ByteArray, fileName: String): Result<String> =
      runCatching {
        val requestBody = imageBytes.toRequestBody("image/jpeg".toMediaType())
        val part = MultipartBody.Part.createFormData("image", fileName, requestBody)
        api.uploadImage(part).jobId
      }

  override suspend fun getJobStatus(jobId: String): Result<JobStatus> = runCatching {
    val response = api.getJobStatus(jobId)

    when (response.status) {
      JobStatusType.PROCESSING -> JobStatus.Processing
      JobStatusType.SUCCESS -> JobStatus.Success(mapper.mapToDomain(response.result!!))
      JobStatusType.FAILED -> JobStatus.Failed(response.error ?: "Unknown error")
    }
  }

  override suspend fun saveSolve(solve: Solve): Long {
    val entity = mapper.mapToEntity(solve)
    return dao.insert(entity)
  }

  override fun getAllSolves(): Flow<List<Solve>> =
      dao.getAllSolves().map { entities -> entities.map { mapper.mapToDomain(it) } }

  override suspend fun getSolveById(id: Long): Solve? =
      dao.getSolveById(id)?.let { mapper.mapToDomain(it) }

  override suspend fun deleteSolve(id: Long) = dao.deleteById(id)

  override suspend fun getObjectDetail(objectName: String): Result<ObjectDetail> = runCatching {
    val cached = objectDetailDao.getByName(objectName)
    if (cached != null) {
      return@runCatching ObjectDetail(
          name = cached.name,
          type = mapper.mapToObjectType(cached.type),
          constellation = cached.constellation,
          funFact = cached.funFact,
      )
    }
    val response = api.getObjectDetail(objectName)
    objectDetailDao.insert(
        ObjectDetailEntity(
            name = response.name,
            type = response.type,
            constellation = response.constellation,
            funFact = response.funFact,
        )
    )
    ObjectDetail(
        name = response.name,
        type = mapper.mapToObjectType(response.type),
        constellation = response.constellation,
        funFact = response.funFact,
    )
  }
}
