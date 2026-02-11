package com.oadultradeepfield.starseek.ui.results

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.oadultradeepfield.starseek.domain.model.ObjectDetail
import com.oadultradeepfield.starseek.domain.model.ObjectType
import com.oadultradeepfield.starseek.ui.components.LoadingIndicator
import com.oadultradeepfield.starseek.ui.theme.Dimens
import com.oadultradeepfield.starseek.ui.theme.StarSeekTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObjectDetailSheet(state: ObjectDetailState, onDismiss: () -> Unit) {
  if (state == ObjectDetailState.Hidden) return

  val sheetState = rememberModalBottomSheetState()

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    when (state) {
      is ObjectDetailState.Loading ->
          LoadingIndicator(
              modifier = Modifier.height(Dimens.bottomSheetMinHeight),
          )
      is ObjectDetailState.Loaded -> ObjectDetailContent(state.detail)
      is ObjectDetailState.Error ->
          Text(
              text = state.message,
              color = MaterialTheme.colorScheme.error,
              modifier = Modifier.padding(Dimens.spacingXLarge),
          )
      ObjectDetailState.Hidden -> {}
    }
  }
}

@Composable
internal fun ObjectDetailContent(detail: ObjectDetail) {
  Column(modifier = Modifier.fillMaxWidth().padding(Dimens.spacingXLarge)) {
    Text(detail.name, style = MaterialTheme.typography.headlineMedium)
    Spacer(modifier = Modifier.height(Dimens.spacingSmall))
    Text(
        "${detail.type.name.lowercase().replaceFirstChar { it.uppercase() }} in ${detail.constellation}",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(Dimens.spacingLarge))
    Text("Fun Fact", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(Dimens.spacingXSmall))
    Text(detail.funFact, style = MaterialTheme.typography.bodyMedium)
    Spacer(modifier = Modifier.height(Dimens.spacingXLarge))
  }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ObjectDetailContentPreview() {
  StarSeekTheme(dynamicColor = false) {
    Surface {
      ObjectDetailContent(
          detail =
              ObjectDetail(
                  name = "Betelgeuse",
                  type = ObjectType.STAR,
                  constellation = "Orion",
                  funFact =
                      "Betelgeuse is a red supergiant star that is one of the largest visible to the naked eye. It's so large that if placed at the center of our solar system, its surface would extend past the orbit of Mars.",
              )
      )
    }
  }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ObjectDetailLoadingPreview() {
  StarSeekTheme(dynamicColor = false) {
    Surface {
      LoadingIndicator(modifier = Modifier.fillMaxWidth().height(Dimens.bottomSheetMinHeight))
    }
  }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ObjectDetailErrorPreview() {
  StarSeekTheme(dynamicColor = false) {
    Surface {
      Text(
          text = "Failed to load object details",
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.padding(Dimens.spacingXLarge),
      )
    }
  }
}
