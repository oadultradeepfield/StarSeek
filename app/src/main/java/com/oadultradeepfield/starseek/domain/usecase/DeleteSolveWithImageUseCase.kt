package com.oadultradeepfield.starseek.domain.usecase

import androidx.core.net.toUri
import com.oadultradeepfield.starseek.domain.repository.ImageProcessor
import com.oadultradeepfield.starseek.domain.repository.SolveRepository
import javax.inject.Inject

class DeleteSolveWithImageUseCase
@Inject
constructor(
    private val repository: SolveRepository,
    private val imageProcessor: ImageProcessor,
) {
  suspend operator fun invoke(solveId: Long) {
    val solve = repository.getSolveById(solveId) ?: return
    imageProcessor.deleteImage(solve.imageUri.toUri())
    repository.deleteSolve(solveId)
  }
}
