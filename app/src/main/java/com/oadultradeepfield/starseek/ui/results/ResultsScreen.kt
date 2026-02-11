package com.oadultradeepfield.starseek.ui.results

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.oadultradeepfield.starseek.domain.model.CelestialObject
import com.oadultradeepfield.starseek.domain.model.ObjectType
import com.oadultradeepfield.starseek.domain.model.Solve
import com.oadultradeepfield.starseek.ui.components.ErrorState
import com.oadultradeepfield.starseek.ui.components.LoadingIndicator
import com.oadultradeepfield.starseek.ui.theme.Dimens
import com.oadultradeepfield.starseek.ui.theme.StarSeekTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(viewModel: ResultsViewModel, solveId: Long, onNavigateBack: () -> Unit) {
  val uiState by viewModel.uiState.collectAsState()
  val objectDetailState by viewModel.objectDetailState.collectAsState()

  LaunchedEffect(solveId) { viewModel.loadFromId(solveId) }

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text("Results") },
            navigationIcon = {
              IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate back")
              }
            },
        )
      },
  ) { innerPadding ->
    when (val state = uiState) {
      is ResultsUiState.Loading -> LoadingIndicator(Modifier.fillMaxSize().padding(innerPadding))
      is ResultsUiState.Content ->
          ResultsContent(
              solve = state.solve,
              grouped = state.grouped,
              highlightedName = state.highlightedObjectName,
              onObjectClick = viewModel::onObjectClick,
              modifier = Modifier.padding(innerPadding),
          )
      is ResultsUiState.Error -> ErrorState(message = state.message)
    }
  }

  ObjectDetailSheet(state = objectDetailState, onDismiss = viewModel::dismissObjectDetail)
}

@Composable
internal fun ResultsContent(
    solve: Solve,
    grouped: GroupedObjects,
    highlightedName: String?,
    onObjectClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    imageSlot: (@Composable () -> Unit)? = null,
) {
  LazyColumn(modifier = modifier.fillMaxSize().padding(Dimens.screenPadding)) {
    item {
      if (imageSlot != null) {
        imageSlot()
      } else {
        ImageWithOverlay(
            imageUri = solve.imageUri,
            objects = solve.objects.filter { it.pixelX != null },
            highlightedName = highlightedName,
            modifier = Modifier.fillMaxWidth().height(Dimens.imagePreviewHeight),
        )
      }
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
internal fun ObjectItem(obj: CelestialObject, onClick: () -> Unit) {
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

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ResultsContentPreview() {
  StarSeekTheme(dynamicColor = false) {
    val objects =
        listOf(
            CelestialObject("Betelgeuse", ObjectType.STAR, "Orion"),
            CelestialObject("Rigel", ObjectType.STAR, "Orion"),
            CelestialObject("Orion Nebula", ObjectType.NEBULA, "Orion"),
            CelestialObject("Polaris", ObjectType.STAR, "Ursa Minor"),
        )
    val grouped =
        GroupedObjects(
            mapOf(
                "Orion" to
                    mapOf(
                        ObjectType.STAR to
                            objects.filter {
                              it.constellation == "Orion" && it.type == ObjectType.STAR
                            },
                        ObjectType.NEBULA to objects.filter { it.type == ObjectType.NEBULA },
                    ),
                "Ursa Minor" to
                    mapOf(
                        ObjectType.STAR to objects.filter { it.constellation == "Ursa Minor" },
                    ),
            )
        )
    ResultsContent(
        solve =
            Solve(
                id = 1,
                imageUri = "",
                imageHash = "",
                objects = objects,
                timestamp = System.currentTimeMillis(),
            ),
        grouped = grouped,
        highlightedName = null,
        onObjectClick = {},
        imageSlot = {
          Box(
              Modifier.fillMaxWidth()
                  .height(Dimens.imagePreviewHeight)
                  .clip(MaterialTheme.shapes.medium)
                  .background(MaterialTheme.colorScheme.surfaceVariant)
          )
        },
    )
  }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ObjectItemPreview() {
  StarSeekTheme(dynamicColor = false) {
    ObjectItem(
        obj = CelestialObject("Betelgeuse", ObjectType.STAR, "Orion"),
        onClick = {},
    )
  }
}
