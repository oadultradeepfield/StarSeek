package com.oadultradeepfield.starseek.ui.history

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import com.oadultradeepfield.starseek.domain.model.CelestialObject
import com.oadultradeepfield.starseek.domain.model.Solve
import com.oadultradeepfield.starseek.ui.theme.Dimens
import com.oadultradeepfield.starseek.ui.theme.StarSeekTheme
import com.oadultradeepfield.starseek.util.formatRelativeDate

@Composable
internal fun HistoryItem(solve: Solve, onClick: () -> Unit, onDeleteClick: () -> Unit) {
  val summary by
      remember(solve.objects) { derivedStateOf { computeConstellationSummary(solve.objects) } }
  HistoryItemContent(
      summary = summary,
      objectCount = solve.objects.size,
      timestamp = solve.timestamp,
      image = {
        AsyncImage(
            model = solve.imageUri,
            contentDescription = "Thumbnail for $summary",
            modifier = Modifier.size(Dimens.thumbnailSizeSmall).clip(MaterialTheme.shapes.small),
            contentScale = ContentScale.Crop,
        )
      },
      onClick = onClick,
      onDeleteClick = onDeleteClick,
  )
}

@Composable
private fun HistoryItemContent(
    summary: String,
    objectCount: Int,
    timestamp: Long,
    image: @Composable () -> Unit,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
  val itemDescription = "$summary, $objectCount objects, ${formatRelativeDate(timestamp)}"
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
      image()
      Spacer(modifier = Modifier.width(Dimens.cardPadding))
      Column(modifier = Modifier.weight(1f)) {
        Text(summary, style = MaterialTheme.typography.titleMedium)
        Text(
            "$objectCount objects",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            formatRelativeDate(timestamp),
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

internal fun computeConstellationSummary(objects: List<CelestialObject>): String {
  val constellations = objects.map { it.constellation }.distinct()
  return when {
    constellations.size == 1 -> constellations.first()
    constellations.size <= 3 -> constellations.joinToString(", ")
    else -> "${constellations.take(2).joinToString(", ")} +${constellations.size - 2}"
  }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HistoryItemPreview() {
  StarSeekTheme(dynamicColor = false) {
    HistoryItemContent(
        summary = "Orion",
        objectCount = 5,
        timestamp = System.currentTimeMillis(),
        image = {
          Box(
              Modifier.size(Dimens.thumbnailSizeSmall)
                  .clip(MaterialTheme.shapes.small)
                  .background(MaterialTheme.colorScheme.surfaceVariant)
          )
        },
        onClick = {},
        onDeleteClick = {},
    )
  }
}
