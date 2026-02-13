package com.oadultradeepfield.starseek.domain.usecase

import com.oadultradeepfield.starseek.domain.model.Solve
import com.oadultradeepfield.starseek.domain.repository.SolveRepository
import javax.inject.Inject

class GetSolveByIdUseCase @Inject constructor(private val repository: SolveRepository) {
  suspend operator fun invoke(id: Long): Solve? = repository.getSolveById(id)
}
