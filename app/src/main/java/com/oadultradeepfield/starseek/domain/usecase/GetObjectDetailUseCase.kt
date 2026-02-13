package com.oadultradeepfield.starseek.domain.usecase

import com.oadultradeepfield.starseek.domain.model.ObjectDetail
import com.oadultradeepfield.starseek.domain.repository.SolveRepository
import javax.inject.Inject

class GetObjectDetailUseCase @Inject constructor(private val repository: SolveRepository) {
  suspend operator fun invoke(objectName: String): Result<ObjectDetail> =
      repository.getObjectDetail(objectName)
}
