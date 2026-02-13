package com.oadultradeepfield.starseek.ui.results

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oadultradeepfield.starseek.domain.model.Solve
import com.oadultradeepfield.starseek.ui.components.ErrorState
import com.oadultradeepfield.starseek.ui.components.LoadingIndicator
import com.oadultradeepfield.starseek.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(viewModel: ResultsViewModel, solveIds: List<Long>, onNavigateBack: () -> Unit) {
  var currentIndex by rememberSaveable { mutableIntStateOf(0) }
  val currentSolveId = solveIds.getOrNull(currentIndex) ?: solveIds.first()
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val objectDetailState by viewModel.objectDetailState.collectAsStateWithLifecycle()
  LaunchedEffect(currentSolveId) { viewModel.loadFromId(currentSolveId) }
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
              imageSlot =
                  if (solveIds.size > 1) {
                    {
                      ImageWithNavigation(
                          solve = state.solve,
                          highlightedName = state.highlightedObjectName,
                          currentIndex = currentIndex,
                          totalCount = solveIds.size,
                          onPrevious = { currentIndex-- },
                          onNext = { currentIndex++ },
                      )
                    }
                  } else null,
          )
      is ResultsUiState.Error -> ErrorState(message = state.message)
    }
  }
}

@Composable
private fun ImageWithNavigation(
    solve: Solve,
    highlightedName: String?,
    currentIndex: Int,
    totalCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
  Box(modifier = Modifier.fillMaxWidth()) {
    ImageWithOverlay(
        imageUri = solve.imageUri,
        objects = solve.objects.filter { it.pixelX != null },
        highlightedName = highlightedName,
        modifier = Modifier.fillMaxWidth(),
    )
    if (currentIndex > 0) {
      IconButton(
          onClick = onPrevious,
          modifier = Modifier.align(Alignment.CenterStart).padding(Dimens.spacingSmall),
          colors =
              IconButtonDefaults.iconButtonColors(
                  containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
              ),
      ) {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous image")
      }
    }
    if (currentIndex < totalCount - 1) {
      IconButton(
          onClick = onNext,
          modifier = Modifier.align(Alignment.CenterEnd).padding(Dimens.spacingSmall),
          colors =
              IconButtonDefaults.iconButtonColors(
                  containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
              ),
      ) {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next image")
      }
    }
    Text(
        text = "${currentIndex + 1}/$totalCount",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = Dimens.spacingSmall),
    )
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
