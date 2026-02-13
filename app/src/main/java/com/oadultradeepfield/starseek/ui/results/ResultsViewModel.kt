package com.oadultradeepfield.starseek.ui.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oadultradeepfield.starseek.domain.model.groupByConstellation
import com.oadultradeepfield.starseek.domain.usecase.GetObjectDetailUseCase
import com.oadultradeepfield.starseek.domain.usecase.GetSolveByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResultsViewModel
@Inject
constructor(
    private val getSolveById: GetSolveByIdUseCase,
    private val getObjectDetail: GetObjectDetailUseCase,
) : ViewModel() {
  private val _uiState = MutableStateFlow<ResultsUiState>(ResultsUiState.Loading)
  val uiState: StateFlow<ResultsUiState> = _uiState

  private val _objectDetailState = MutableStateFlow<ObjectDetailState>(ObjectDetailState.Hidden)
  val objectDetailState: StateFlow<ObjectDetailState> = _objectDetailState

  fun loadFromId(solveId: Long) {
    viewModelScope.launch {
      val solve = getSolveById(solveId)

      _uiState.update {
        if (solve != null) {
          ResultsUiState.Content(solve, GroupedObjects(solve.objects.groupByConstellation()))
        } else {
          ResultsUiState.Error("Solve not found")
        }
      }
    }
  }

  fun highlightObject(name: String?) {
    _uiState.update { current ->
      if (current is ResultsUiState.Content) current.copy(highlightedObjectName = name) else current
    }
  }

  fun onObjectClick(objectName: String) {
    highlightObject(objectName)
    _objectDetailState.update { ObjectDetailState.Loading }

    viewModelScope.launch {
      getObjectDetail(objectName)
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
}
