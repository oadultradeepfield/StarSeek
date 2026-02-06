package com.oadultradeepfield.starseek.ui.history

import com.oadultradeepfield.starseek.domain.model.Solve

sealed class HistoryUiState {
  data object Loading : HistoryUiState()

  data object Empty : HistoryUiState()

  data class Content(val solves: List<Solve>) : HistoryUiState()
}
