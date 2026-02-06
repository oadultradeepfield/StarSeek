package com.oadultradeepfield.starseek.ui.history

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oadultradeepfield.starseek.data.ImageProcessorImpl
import com.oadultradeepfield.starseek.data.repository.SolveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel
@Inject
constructor(
    private val repository: SolveRepository,
    private val imageProcessor: ImageProcessorImpl,
) : ViewModel() {
  private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
  val uiState: StateFlow<HistoryUiState> = _uiState

  private val _deleteConfirmId = MutableStateFlow<Long?>(null)
  val deleteConfirmId: StateFlow<Long?> = _deleteConfirmId

  init {
    viewModelScope.launch {
      repository.getAllSolves().collect { solves ->
        _uiState.update {
          if (solves.isEmpty()) HistoryUiState.Empty else HistoryUiState.Content(solves)
        }
      }
    }
  }

  fun onDeleteClick(solveId: Long) {
    _deleteConfirmId.update { solveId }
  }

  fun onDeleteConfirm() {
    val id = _deleteConfirmId.value ?: return

    viewModelScope.launch {
      val solve = repository.getSolveById(id)

      solve?.let {
        imageProcessor.deleteImage(it.imageUri.toUri())
        imageProcessor.deleteImage(it.annotatedImageUri.toUri())
        repository.deleteSolve(id)
      }

      _deleteConfirmId.update { null }
    }
  }

  fun onDeleteDismiss() {
    _deleteConfirmId.update { null }
  }
}
