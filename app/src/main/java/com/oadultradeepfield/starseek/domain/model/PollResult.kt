package com.oadultradeepfield.starseek.domain.model

sealed class PollResult {
  data class Success(val solve: Solve) : PollResult()

  data class Failure(val error: String) : PollResult()
}
