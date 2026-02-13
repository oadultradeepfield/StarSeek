package com.oadultradeepfield.starseek.domain.usecase

import com.oadultradeepfield.starseek.domain.model.Solve
import com.oadultradeepfield.starseek.domain.repository.SolveRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSolvesUseCase @Inject constructor(private val repository: SolveRepository) {
  operator fun invoke(): Flow<List<Solve>> = repository.getAllSolves()
}
