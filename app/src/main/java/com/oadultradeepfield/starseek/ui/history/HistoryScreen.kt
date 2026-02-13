package com.oadultradeepfield.starseek.ui.history

import android.content.res.Configuration
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oadultradeepfield.starseek.domain.model.CelestialObject
import com.oadultradeepfield.starseek.domain.model.ObjectType
import com.oadultradeepfield.starseek.domain.model.Solve
import com.oadultradeepfield.starseek.ui.components.EmptyState
import com.oadultradeepfield.starseek.ui.components.LoadingIndicator
import com.oadultradeepfield.starseek.ui.theme.Dimens
import com.oadultradeepfield.starseek.ui.theme.StarSeekTheme

@Composable
fun HistoryScreen(viewModel: HistoryViewModel, onSolveClick: (Long) -> Unit) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val deleteConfirmId by viewModel.deleteConfirmId.collectAsStateWithLifecycle()

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
            count = state.solves.size,
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
internal fun HistoryList(
    solves: List<Solve>,
    count: Int,
    onSolveClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
) {
  LazyColumn(modifier = Modifier.fillMaxSize().padding(Dimens.screenPadding)) {
    item {
      Text(
          "$count solved image${if (count == 1) "" else "s"}",
          style = MaterialTheme.typography.headlineSmall,
      )

      Spacer(modifier = Modifier.height(Dimens.spacingLarge))
    }

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
internal fun DeleteConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text("Delete this result?") },
      text = { Text("This action cannot be undone.") },
      confirmButton = { TextButton(onClick = onConfirm) { Text("Delete") } },
      dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HistoryListPreview() {
  StarSeekTheme(dynamicColor = false) {
    val solves =
        listOf(
            Solve(
                id = 1,
                imageUri = "",
                imageHash = "",
                objects =
                    listOf(
                        CelestialObject("Betelgeuse", ObjectType.STAR, "Orion"),
                        CelestialObject("Rigel", ObjectType.STAR, "Orion"),
                    ),
                timestamp = System.currentTimeMillis(),
            ),
            Solve(
                id = 2,
                imageUri = "",
                imageHash = "",
                objects =
                    listOf(
                        CelestialObject("Polaris", ObjectType.STAR, "Ursa Minor"),
                    ),
                timestamp = System.currentTimeMillis() - 86400000,
            ),
        )

    HistoryList(solves = solves, count = solves.size, onSolveClick = {}, onDeleteClick = {})
  }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DeleteConfirmDialogPreview() {
  StarSeekTheme(dynamicColor = false) { DeleteConfirmDialog(onConfirm = {}, onDismiss = {}) }
}
