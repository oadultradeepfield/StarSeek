package com.oadultradeepfield.starseek.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.oadultradeepfield.starseek.ui.theme.Dimens
import com.oadultradeepfield.starseek.ui.theme.StarSeekTheme

@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
  Column(
      modifier = modifier.fillMaxSize().padding(Dimens.screenPaddingLarge),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
  ) {
    Text(title, style = MaterialTheme.typography.headlineSmall)

    if (subtitle != null) {
      Spacer(modifier = Modifier.height(Dimens.spacingSmall))
      Text(
          subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    if (action != null) {
      Spacer(modifier = Modifier.height(Dimens.spacingXLarge))
      action()
    }
  }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EmptyStatePreview() {
  StarSeekTheme(dynamicColor = false) {
    EmptyState(
        title = "No items found",
        subtitle = "Try uploading a photo to get started",
    ) {
      Button(onClick = {}) { Text("Get Started") }
    }
  }
}
