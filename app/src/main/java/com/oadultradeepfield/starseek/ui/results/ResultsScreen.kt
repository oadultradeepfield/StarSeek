package com.oadultradeepfield.starseek.ui.results

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.oadultradeepfield.starseek.domain.model.Solve
import com.oadultradeepfield.starseek.ui.components.ErrorState
import com.oadultradeepfield.starseek.ui.components.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(viewModel: ResultsViewModel, solveId: Long, onNavigateBack: () -> Unit) {
  val uiState by viewModel.uiState.collectAsState()
  val objectDetailState by viewModel.objectDetailState.collectAsState()
  LaunchedEffect(solveId) { viewModel.loadFromId(solveId) }
  Column(modifier = Modifier.fillMaxSize()) {
    TopAppBar(
        title = { Text("Results") },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate back")
          }
        },
        windowInsets = WindowInsets(0, 0, 0, 0),
    )
    when (val state = uiState) {
      is ResultsUiState.Loading -> LoadingIndicator(Modifier.fillMaxSize())
      is ResultsUiState.Content ->
          ResultsContent(
              solve = state.solve,
              grouped = state.grouped,
              highlightedName = state.highlightedObjectName,
              objectDetailState = objectDetailState,
              onObjectClick = { name ->
                if (state.highlightedObjectName == name) {
                  viewModel.dismissObjectDetail()
                } else {
                  viewModel.onObjectClick(name)
                }
              },
          )
      is ResultsUiState.Error -> ErrorState(message = state.message)
    }
  }
}

@Composable
internal fun ResultsContent(
    solve: Solve,
    grouped: GroupedObjects,
    highlightedName: String?,
    objectDetailState: ObjectDetailState,
    onObjectClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    imageSlot: (@Composable () -> Unit)? = null,
) {
  val listState = rememberLazyListState()
  Column(modifier = modifier.fillMaxSize()) {
    if (imageSlot != null) {
      imageSlot()
    } else {
      ImageWithOverlay(
          imageUri = solve.imageUri,
          objects = solve.objects.filter { it.pixelX != null },
          highlightedName = highlightedName,
          modifier = Modifier.fillMaxWidth(),
      )
    }
    ObjectList(
        grouped = grouped,
        objectCount = solve.objects.size,
        highlightedName = highlightedName,
        objectDetailState = objectDetailState,
        onObjectClick = onObjectClick,
        listState = listState,
        modifier = Modifier.weight(1f),
    )
  }
}
