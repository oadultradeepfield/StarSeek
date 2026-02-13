package com.oadultradeepfield.starseek.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oadultradeepfield.starseek.domain.usecase.DeleteSolveWithImageUseCase
import com.oadultradeepfield.starseek.domain.usecase.ObserveSolvesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel
@Inject
constructor(
    observeSolves: ObserveSolvesUseCase,
    private val deleteSolveWithImage: DeleteSolveWithImageUseCase,
) : ViewModel() {
  val uiState: StateFlow<HistoryUiState> =
      observeSolves()
          .map { solves ->
            if (solves.isEmpty()) HistoryUiState.Empty else HistoryUiState.Content(solves)
          }
          .stateIn(
              scope = viewModelScope,
              started = SharingStarted.WhileSubscribed(5_000),
              initialValue = HistoryUiState.Loading,
          )

  private val _deleteConfirmId = MutableStateFlow<Long?>(null)
  val deleteConfirmId: StateFlow<Long?> = _deleteConfirmId

  fun onDeleteClick(solveId: Long) {
    _deleteConfirmId.update { solveId }
  }

  fun onDeleteConfirm() {
    val id = _deleteConfirmId.value ?: return

    viewModelScope.launch {
      deleteSolveWithImage(id)
      _deleteConfirmId.update { null }
    }
  }

  fun onDeleteDismiss() {
    _deleteConfirmId.update { null }
  }
}
