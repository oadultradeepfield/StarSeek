package com.oadultradeepfield.starseek.ui.results

import com.oadultradeepfield.starseek.domain.model.CelestialObject
import com.oadultradeepfield.starseek.domain.model.ObjectDetail
import com.oadultradeepfield.starseek.domain.model.ObjectType
import com.oadultradeepfield.starseek.domain.model.Solve

sealed class ResultsUiState {
  data object Loading : ResultsUiState()

  data class Content(
      val solve: Solve,
      val grouped: GroupedObjects,
      val highlightedObjectName: String? = null,
  ) : ResultsUiState()

  data class Error(val message: String) : ResultsUiState()
}

/** Objects grouped by constellation name, then by object type within each constellation. */
data class GroupedObjects(val byConstellation: Map<String, Map<ObjectType, List<CelestialObject>>>)

sealed class ObjectDetailState {
  data object Hidden : ObjectDetailState()

  data object Loading : ObjectDetailState()

  data class Loaded(val detail: ObjectDetail) : ObjectDetailState()

  data class Error(val message: String) : ObjectDetailState()
}
