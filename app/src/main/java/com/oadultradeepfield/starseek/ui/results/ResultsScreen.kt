package com.oadultradeepfield.starseek.ui.results

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.oadultradeepfield.starseek.domain.model.CelestialObject
import com.oadultradeepfield.starseek.domain.model.Solve
import com.oadultradeepfield.starseek.ui.components.ErrorState
import com.oadultradeepfield.starseek.ui.components.LoadingIndicator
import com.oadultradeepfield.starseek.ui.theme.Dimens

@Composable
fun ResultsScreen(viewModel: ResultsViewModel, solveId: Long) {
  val uiState by viewModel.uiState.collectAsState()
  val objectDetailState by viewModel.objectDetailState.collectAsState()

  LaunchedEffect(solveId) { viewModel.loadFromId(solveId) }

  when (val state = uiState) {
    is ResultsUiState.Loading -> LoadingIndicator(Modifier.fillMaxSize())
    is ResultsUiState.Content ->
        ResultsContent(
            solve = state.solve,
            grouped = state.grouped,
            showAnnotated = state.showAnnotated,
            highlightedName = state.highlightedObjectName,
            onToggle = viewModel::toggleImageView,
            onObjectClick = viewModel::onObjectClick,
        )
    is ResultsUiState.Error -> ErrorState(message = state.message)
  }

  ObjectDetailSheet(state = objectDetailState, onDismiss = viewModel::dismissObjectDetail)
}

@Composable
private fun ResultsContent(
    solve: Solve,
    grouped: GroupedObjects,
    showAnnotated: Boolean,
    highlightedName: String?,
    onToggle: () -> Unit,
    onObjectClick: (String) -> Unit,
) {
  LazyColumn(modifier = Modifier.fillMaxSize().padding(Dimens.screenPadding)) {
    item {
      ImageWithOverlay(
          originalUri = solve.imageUri,
          annotatedUri = solve.annotatedImageUri,
          showAnnotated = showAnnotated,
          objects = solve.objects.filter { it.pixelX != null },
          highlightedName = highlightedName,
          onToggle = onToggle,
          modifier = Modifier.fillMaxWidth().height(Dimens.imagePreviewHeight),
      )
      Spacer(modifier = Modifier.height(Dimens.spacingXLarge))
      Text("${solve.objects.size} objects detected", style = MaterialTheme.typography.headlineSmall)
      Spacer(modifier = Modifier.height(Dimens.spacingLarge))
    }

    grouped.byConstellation.forEach { (constellation, typeMap) ->
      item {
        Text(
            constellation,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(vertical = Dimens.spacingSmall),
        )
      }

      typeMap.forEach { (type, objects) ->
        item {
          Text(
              type.displayName,
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.primary,
              modifier =
                  Modifier.padding(
                      vertical = Dimens.spacingXSmall,
                      horizontal = Dimens.spacingSmall,
                  ),
          )
        }

        items(objects) { obj -> ObjectItem(obj = obj, onClick = { onObjectClick(obj.name) }) }
      }
    }
  }
}

@Composable
private fun ObjectItem(obj: CelestialObject, onClick: () -> Unit) {
  Card(
      modifier =
          Modifier.fillMaxWidth()
              .padding(vertical = Dimens.spacingXSmall, horizontal = Dimens.spacingSmall)
              .clickable(onClick = onClick)
              .semantics {
                contentDescription = "${obj.name}, ${obj.type.displayName} in ${obj.constellation}"
              },
  ) {
    Text(
        obj.name,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(Dimens.listItemPadding),
    )
  }
}
