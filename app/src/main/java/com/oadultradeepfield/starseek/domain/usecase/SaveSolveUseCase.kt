package com.oadultradeepfield.starseek.domain.usecase

import com.oadultradeepfield.starseek.domain.model.Solve
import com.oadultradeepfield.starseek.domain.repository.SolveRepository
import javax.inject.Inject

class SaveSolveUseCase
@Inject
constructor(
    private val repository: SolveRepository,
) {
  suspend operator fun invoke(solve: Solve): Long = repository.saveSolve(solve)
}
