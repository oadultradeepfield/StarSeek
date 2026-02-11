package com.oadultradeepfield.starseek.ui.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oadultradeepfield.starseek.data.repository.SolveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ResultsViewModel @Inject constructor(private val repository: SolveRepository) : ViewModel() {
  private val _uiState = MutableStateFlow<ResultsUiState>(ResultsUiState.Loading)
  val uiState: StateFlow<ResultsUiState> = _uiState

  private val _objectDetailState = MutableStateFlow<ObjectDetailState>(ObjectDetailState.Hidden)
  val objectDetailState: StateFlow<ObjectDetailState> = _objectDetailState

  fun loadFromId(solveId: Long) {
    viewModelScope.launch {
      val solve = repository.getSolveById(solveId)

      _uiState.update {
        if (solve != null) {
          ResultsUiState.Content(solve, groupObjects(solve))
        } else {
          ResultsUiState.Error("Solve not found")
        }
      }
    }
  }

  fun highlightObject(name: String?) {
    _uiState.update { current ->
      if (current is ResultsUiState.Content) {
        current.copy(highlightedObjectName = name)
      } else current
    }
  }

  fun onObjectClick(objectName: String) {
    highlightObject(objectName)
    _objectDetailState.update { ObjectDetailState.Loading }

    viewModelScope.launch {
      repository
          .getObjectDetail(objectName)
          .fold(
              onSuccess = { detail ->
                _objectDetailState.update { ObjectDetailState.Loaded(detail) }
              },
              onFailure = { e ->
                _objectDetailState.update {
                  ObjectDetailState.Error(e.message ?: "Failed to load details")
                }
              },
          )
    }
  }

  fun dismissObjectDetail() {
    highlightObject(null)
    _objectDetailState.update { ObjectDetailState.Hidden }
  }

  private fun groupObjects(
      solve: com.oadultradeepfield.starseek.domain.model.Solve
  ): GroupedObjects {
    val grouped =
        solve.objects
            .groupBy { it.constellation }
            .mapValues { (_, objects) -> objects.groupBy { it.type } }

    return GroupedObjects(grouped)
  }
}
