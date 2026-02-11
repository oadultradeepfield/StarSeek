package com.oadultradeepfield.starseek.ui.results

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.oadultradeepfield.starseek.domain.model.CelestialObject
import com.oadultradeepfield.starseek.domain.model.ObjectType
import com.oadultradeepfield.starseek.domain.model.Solve
import com.oadultradeepfield.starseek.ui.theme.Dimens
import com.oadultradeepfield.starseek.ui.theme.StarSeekTheme
import kotlinx.coroutines.launch

@Composable
internal fun ObjectList(
    grouped: GroupedObjects,
    objectCount: Int,
    highlightedName: String?,
    objectDetailState: ObjectDetailState,
    onObjectClick: (String) -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
  val coroutineScope = rememberCoroutineScope()
  val horizontalPadding = Modifier.padding(horizontal = Dimens.screenPadding)
  var itemIndex = 0
  val objectIndexMap = mutableMapOf<String, Int>()
  itemIndex++
  grouped.byConstellation.entries.forEachIndexed { constellationIndex, (_, typeMap) ->
    if (constellationIndex > 0) itemIndex++
    itemIndex++
    typeMap.entries.forEachIndexed { typeIndex, (_, objects) ->
      if (typeIndex > 0) itemIndex++
      itemIndex++
      objects.forEach { obj ->
        objectIndexMap[obj.name] = itemIndex
        itemIndex++
      }
    }
  }
  LaunchedEffect(highlightedName) {
    if (highlightedName != null) {
      val index = objectIndexMap[highlightedName]
      if (index != null) {
        coroutineScope.launch { listState.animateScrollToItem(index) }
      }
    }
  }
  LazyColumn(
      modifier = modifier.fillMaxWidth(),
      state = listState,
      contentPadding = PaddingValues(bottom = Dimens.screenPadding),
  ) {
    item {
      Spacer(modifier = Modifier.height(Dimens.spacingXLarge))
      Text(
          "$objectCount object(s) detected",
          style = MaterialTheme.typography.headlineSmall,
          modifier = horizontalPadding,
      )
      Spacer(modifier = Modifier.height(Dimens.spacingLarge))
    }
    grouped.byConstellation.entries.forEachIndexed { index, (constellation, typeMap) ->
      if (index > 0) {
        item { Spacer(modifier = Modifier.height(Dimens.spacingLarge)) }
      }
      item {
        Text(
            constellation,
            style = MaterialTheme.typography.titleLarge,
            modifier = horizontalPadding.padding(vertical = Dimens.spacingSmall),
        )
      }
      typeMap.entries.forEachIndexed { typeIndex, (type, objects) ->
        if (typeIndex > 0) {
          item { Spacer(modifier = Modifier.height(Dimens.spacingMedium)) }
        }
        item {
          Text(
              type.displayName,
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.primary,
              modifier = horizontalPadding.padding(vertical = Dimens.spacingXSmall),
          )
        }
        items(objects, key = { it.name }) { obj ->
          val isExpanded = obj.name == highlightedName
          ObjectAccordionItem(
              obj = obj,
              isExpanded = isExpanded,
              detailState = if (isExpanded) objectDetailState else ObjectDetailState.Hidden,
              onClick = { onObjectClick(obj.name) },
              modifier = horizontalPadding,
          )
        }
      }
    }
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
        objectDetailState = ObjectDetailState.Hidden,
        onObjectClick = {},
        imageSlot = {
          Box(
              Modifier.fillMaxWidth()
                  .height(Dimens.imagePreviewHeight)
                  .background(MaterialTheme.colorScheme.surfaceVariant)
          )
        },
    )
  }
}
