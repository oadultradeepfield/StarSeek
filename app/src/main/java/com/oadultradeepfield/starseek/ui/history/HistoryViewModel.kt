package com.oadultradeepfield.starseek.ui.history

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oadultradeepfield.starseek.domain.repository.ImageProcessor
import com.oadultradeepfield.starseek.domain.repository.SolveRepository
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
    private val repository: SolveRepository,
    private val imageProcessor: ImageProcessor,
) : ViewModel() {
  val uiState: StateFlow<HistoryUiState> =
      repository
          .getAllSolves()
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
      val solve = repository.getSolveById(id)

      solve?.let {
        imageProcessor.deleteImage(it.imageUri.toUri())
        repository.deleteSolve(id)
      }

      _deleteConfirmId.update { null }
    }
  }

  fun onDeleteDismiss() {
    _deleteConfirmId.update { null }
  }
}
