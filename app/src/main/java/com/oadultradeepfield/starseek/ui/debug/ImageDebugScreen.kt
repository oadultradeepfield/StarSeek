package com.oadultradeepfield.starseek.ui.debug

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.oadultradeepfield.starseek.ui.components.ImagePreset
import com.oadultradeepfield.starseek.ui.components.StarSeekAsyncImage
import com.oadultradeepfield.starseek.ui.theme.Dimens

@Composable
fun ImageDebugScreen(viewModel: ImageDebugViewModel = hiltViewModel()) {
  val metrics by viewModel.metrics.collectAsState()
  var selectedUri by rememberSaveable { mutableStateOf<Uri?>(null) }
  var enhanceEnabled by rememberSaveable { mutableStateOf(false) }

  val imagePicker =
      rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
        uri?.let { selectedUri = it }
      }

  Column(
      modifier =
          Modifier.fillMaxSize()
              .padding(Dimens.screenPadding)
              .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text("Image Debug", style = MaterialTheme.typography.headlineMedium)

    Spacer(modifier = Modifier.height(Dimens.spacingLarge))

    Button(onClick = { imagePicker.launch("image/*") }) { Text("Select Image") }

    Spacer(modifier = Modifier.height(Dimens.spacingLarge))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Text("Enhancement", style = MaterialTheme.typography.bodyLarge)
      Switch(checked = enhanceEnabled, onCheckedChange = { enhanceEnabled = it })
    }

    Spacer(modifier = Modifier.height(Dimens.spacingLarge))

    selectedUri?.let { uri ->
      Text("Original", style = MaterialTheme.typography.titleMedium)

      Spacer(modifier = Modifier.height(Dimens.spacingSmall))

      StarSeekAsyncImage(
          model = uri,
          contentDescription = "Original image",
          modifier = Modifier.size(200.dp),
          preset = ImagePreset.FULL,
          enhance = false,
      )

      Spacer(modifier = Modifier.height(Dimens.spacingLarge))

      Text(
          if (enhanceEnabled) "Enhanced (Asinh)" else "No Enhancement",
          style = MaterialTheme.typography.titleMedium,
      )

      Spacer(modifier = Modifier.height(Dimens.spacingSmall))

      StarSeekAsyncImage(
          model = uri,
          contentDescription = "Processed image",
          modifier = Modifier.size(200.dp),
          preset = ImagePreset.FULL,
          enhance = enhanceEnabled,
      )
    }

    Spacer(modifier = Modifier.height(Dimens.spacingXLarge))

    Card(modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.padding(Dimens.cardPadding)) {
        Text("Metrics", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(Dimens.spacingSmall))
        MetricRow("Total Loads", metrics.totalLoads.toString())
        MetricRow("Cache Hits", metrics.cacheHits.toString())
        MetricRow("Cache Misses", metrics.cacheMisses.toString())
        MetricRow("Cache Hit Rate", "%.1f%%".format(metrics.cacheHitRate * 100))
        MetricRow("Avg Load Time", "${metrics.averageLoadTimeMs}ms")
        MetricRow("Last Load Time", "${metrics.lastLoadTimeMs}ms")
      }
    }

    Spacer(modifier = Modifier.height(Dimens.spacingLarge))

    Button(onClick = { viewModel.resetMetrics() }) { Text("Reset Metrics") }
  }
}

@Composable
private fun MetricRow(label: String, value: String) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.spacingXSmall),
      horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(label, style = MaterialTheme.typography.bodyMedium)
    Text(value, style = MaterialTheme.typography.bodyMedium)
  }
}
