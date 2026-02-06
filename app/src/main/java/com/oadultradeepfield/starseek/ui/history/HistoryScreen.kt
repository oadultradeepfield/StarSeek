package com.oadultradeepfield.starseek.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import coil3.compose.AsyncImage
import com.oadultradeepfield.starseek.domain.model.CelestialObject
import com.oadultradeepfield.starseek.domain.model.Solve
import com.oadultradeepfield.starseek.ui.components.EmptyState
import com.oadultradeepfield.starseek.ui.components.LoadingIndicator
import com.oadultradeepfield.starseek.ui.theme.Dimens
import com.oadultradeepfield.starseek.util.formatRelativeDate

@Composable
fun HistoryScreen(viewModel: HistoryViewModel, onSolveClick: (Long) -> Unit) {
  val uiState by viewModel.uiState.collectAsState()
  val deleteConfirmId by viewModel.deleteConfirmId.collectAsState()

  when (val state = uiState) {
    is HistoryUiState.Loading -> LoadingIndicator(Modifier.fillMaxSize())
    is HistoryUiState.Empty ->
        EmptyState(
            title = "No solved images yet",
            subtitle = "Upload a night sky photo to get started",
        )
    is HistoryUiState.Content ->
        HistoryList(
            solves = state.solves,
            onSolveClick = onSolveClick,
            onDeleteClick = viewModel::onDeleteClick,
        )
  }

  if (deleteConfirmId != null) {
    DeleteConfirmDialog(
        onConfirm = viewModel::onDeleteConfirm,
        onDismiss = viewModel::onDeleteDismiss,
    )
  }
}

@Composable
private fun HistoryList(
    solves: List<Solve>,
    onSolveClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
) {
  LazyColumn(modifier = Modifier.fillMaxSize().padding(Dimens.screenPadding)) {
    items(solves, key = { it.id }) { solve ->
      HistoryItem(
          solve = solve,
          onClick = { onSolveClick(solve.id) },
          onDeleteClick = { onDeleteClick(solve.id) },
      )
    }
  }
}

@Composable
private fun HistoryItem(solve: Solve, onClick: () -> Unit, onDeleteClick: () -> Unit) {
  val summary by
      remember(solve.objects) { derivedStateOf { computeConstellationSummary(solve.objects) } }
  val itemDescription =
      "$summary, ${solve.objects.size} objects, ${formatRelativeDate(solve.timestamp)}"

  Card(
      modifier =
          Modifier.fillMaxWidth()
              .padding(vertical = Dimens.spacingXSmall)
              .clickable(onClick = onClick)
              .semantics { contentDescription = itemDescription }
  ) {
    Row(
        modifier = Modifier.padding(Dimens.cardPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      AsyncImage(
          model = solve.imageUri,
          contentDescription = "Thumbnail for $summary",
          modifier = Modifier.size(Dimens.thumbnailSizeSmall).clip(MaterialTheme.shapes.small),
          contentScale = ContentScale.Crop,
      )

      Spacer(modifier = Modifier.width(Dimens.cardPadding))

      Column(modifier = Modifier.weight(1f)) {
        Text(summary, style = MaterialTheme.typography.titleMedium)
        Text(
            "${solve.objects.size} objects",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            formatRelativeDate(solve.timestamp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      IconButton(onClick = onDeleteClick) {
        Icon(Icons.Default.Delete, contentDescription = "Delete $summary")
      }
    }
  }
}

private fun computeConstellationSummary(objects: List<CelestialObject>): String {
  val constellations = objects.map { it.constellation }.distinct()
  return when {
    constellations.size == 1 -> constellations.first()
    constellations.size <= 3 -> constellations.joinToString(", ")
    else -> "${constellations.take(2).joinToString(", ")} +${constellations.size - 2}"
  }
}

@Composable
private fun DeleteConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text("Delete this result?") },
      text = { Text("This action cannot be undone.") },
      confirmButton = { TextButton(onClick = onConfirm) { Text("Delete") } },
      dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
