package com.oadultradeepfield.starseek.ui.results

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.oadultradeepfield.starseek.domain.model.CelestialObject
import com.oadultradeepfield.starseek.ui.components.LoadingIndicator
import com.oadultradeepfield.starseek.ui.theme.Dimens

@Composable
internal fun ObjectAccordionItem(
    obj: CelestialObject,
    isExpanded: Boolean,
    detailState: ObjectDetailState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val headerPadding =
      if (isExpanded) {
        Modifier.padding(horizontal = Dimens.listItemPadding).padding(top = Dimens.listItemPadding)
      } else {
        Modifier.padding(Dimens.listItemPadding)
      }

  Card(
      modifier =
          modifier
              .fillMaxWidth()
              .padding(vertical = Dimens.spacingXSmall)
              .clickable(onClick = onClick)
              .semantics {
                contentDescription = "${obj.name}, ${obj.type.displayName} in ${obj.constellation}"
              },
  ) {
    Column {
      Box(modifier = Modifier.fillMaxWidth().then(headerPadding)) {
        Text(obj.name, style = MaterialTheme.typography.bodyLarge)

        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            modifier = Modifier.align(Alignment.CenterEnd),
        )
      }

      AnimatedVisibility(
          visible = isExpanded,
          enter = expandVertically(),
          exit = shrinkVertically(),
      ) {
        AccordionDetailContent(detailState)
      }
    }
  }
}

@Composable
private fun AccordionDetailContent(state: ObjectDetailState) {
  when (state) {
    is ObjectDetailState.Loading ->
        LoadingIndicator(
            modifier =
                Modifier.fillMaxWidth()
                    .height(Dimens.bottomSheetMinHeight)
                    .size(Dimens.loadingIndicatorSizeMedium)
        )
    is ObjectDetailState.Loaded -> {
      Column(
          modifier =
              Modifier.fillMaxWidth()
                  .padding(horizontal = Dimens.listItemPadding)
                  .padding(bottom = Dimens.spacingSmall)
      ) {
        Text(
            "${state.detail.type.name.lowercase().replaceFirstChar { it.uppercase() }} in ${state.detail.constellation}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(Dimens.spacingMedium))

        Text("Fun Fact", style = MaterialTheme.typography.titleSmall)

        Spacer(modifier = Modifier.height(Dimens.spacingXSmall))

        Text(state.detail.funFact, style = MaterialTheme.typography.bodySmall)
      }
    }
    is ObjectDetailState.Error ->
        Text(
            text = state.message,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(Dimens.listItemPadding),
        )
    ObjectDetailState.Hidden -> {}
  }
}
