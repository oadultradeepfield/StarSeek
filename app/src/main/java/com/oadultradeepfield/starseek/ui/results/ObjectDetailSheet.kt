package com.oadultradeepfield.starseek.ui.results

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.oadultradeepfield.starseek.domain.model.ObjectDetail
import com.oadultradeepfield.starseek.ui.components.LoadingIndicator
import com.oadultradeepfield.starseek.ui.theme.Dimens

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
private fun ObjectDetailContent(detail: ObjectDetail) {
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
