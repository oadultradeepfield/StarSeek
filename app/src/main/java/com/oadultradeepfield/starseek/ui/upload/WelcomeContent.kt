package com.oadultradeepfield.starseek.ui.upload

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.oadultradeepfield.starseek.R
import com.oadultradeepfield.starseek.ui.theme.Dimens
import com.oadultradeepfield.starseek.ui.theme.StarSeekTheme

@Composable
internal fun WelcomeState(onChoosePhoto: () -> Unit) {
  Column(
      modifier = Modifier.fillMaxSize().padding(Dimens.screenPaddingLarge),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
  ) {
    Icon(
        painter = painterResource(R.drawable.ic_launcher_foreground),
        contentDescription = null,
        modifier = Modifier.size(Dimens.thumbnailSizeMedium),
        tint = MaterialTheme.colorScheme.primary,
    )

    Text("StarSeek", style = MaterialTheme.typography.headlineLarge)

    Spacer(modifier = Modifier.height(Dimens.spacingSmall))

    Text(
        "Unlock the hidden wonders within your night-sky captures.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(Dimens.spacingLarge))

    Button(onClick = onChoosePhoto, modifier = Modifier.fillMaxWidth()) {
      Icon(
          imageVector = Icons.Default.CameraAlt,
          contentDescription = null,
          modifier = Modifier.size(Dimens.spacingLarge),
      )
      Spacer(modifier = Modifier.width(Dimens.spacingMedium))
      Text("Choose Photo", style = MaterialTheme.typography.labelLarge)
    }
  }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WelcomeStatePreview() {
  StarSeekTheme(dynamicColor = false) { WelcomeState(onChoosePhoto = {}) }
}
