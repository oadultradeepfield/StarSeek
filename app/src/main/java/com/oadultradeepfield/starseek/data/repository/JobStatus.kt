package com.oadultradeepfield.starseek.data.repository

import com.oadultradeepfield.starseek.domain.model.Solve

sealed class JobStatus {
  data object Processing : JobStatus()

  data class Success(val solve: Solve) : JobStatus()

  data class Failed(val error: String) : JobStatus()
}
