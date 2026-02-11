package com.oadultradeepfield.starseek.ui.upload

import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import com.oadultradeepfield.starseek.R
import com.oadultradeepfield.starseek.domain.model.Solve
import com.oadultradeepfield.starseek.ui.components.ErrorState
import com.oadultradeepfield.starseek.ui.components.LoadingIndicator
import com.oadultradeepfield.starseek.ui.theme.Dimens
import com.oadultradeepfield.starseek.ui.theme.StarSeekTheme

@Composable
fun UploadScreen(viewModel: UploadViewModel, onNavigateToResults: (Solve) -> Unit) {
  val uiState by viewModel.uiState.collectAsState()

  val launcher =
      rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri?
        ->
        uri?.let { viewModel.onImageSelected(it) }
      }

  LaunchedEffect(uiState) {
    if (uiState is UploadUiState.Success) {
      onNavigateToResults((uiState as UploadUiState.Success).solve)
      viewModel.reset()
    }
  }

  when (val state = uiState) {
    is UploadUiState.Empty -> WelcomeState(onChoosePhoto = { launcher.launch("image/*") })
    is UploadUiState.ImageSelected ->
        ImageSelectedState(
            uri = state.uri,
            onUploadClick = { viewModel.onUploadClick() },
            onChangeClick = { launcher.launch("image/*") },
        )
    is UploadUiState.Processing -> LoadingState(uri = state.uri, message = state.message)
    is UploadUiState.Success -> {}
    is UploadUiState.Error ->
        ErrorState(
            message = state.message,
            onRetry = state.lastUri?.let { { viewModel.retry() } },
        )
  }
}

@Composable
private fun WelcomeState(onChoosePhoto: () -> Unit) {
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

@Composable
internal fun ImageSelectedState(uri: Uri, onUploadClick: () -> Unit, onChangeClick: () -> Unit) {
  ImageSelectedContent(
      image = {
        AsyncImage(
            model = uri,
            contentDescription = "Selected image",
            modifier = Modifier.size(Dimens.thumbnailSizeLarge).clip(MaterialTheme.shapes.large),
            contentScale = ContentScale.Crop,
        )
      },
      onUploadClick = onUploadClick,
      onChangeClick = onChangeClick,
  )
}

@Composable
private fun ImageSelectedContent(
    image: @Composable () -> Unit,
    onUploadClick: () -> Unit,
    onChangeClick: () -> Unit,
) {
  Column(
      modifier = Modifier.fillMaxSize().padding(Dimens.screenPadding),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
  ) {
    image()
    Spacer(modifier = Modifier.height(Dimens.spacingXLarge))

    Button(onClick = onUploadClick, modifier = Modifier.fillMaxWidth()) {
      Icon(
          imageVector = Icons.Default.Search,
          contentDescription = null,
          modifier = Modifier.size(Dimens.spacingLarge),
      )
      Spacer(modifier = Modifier.width(Dimens.spacingMedium))
      Text(text = "Identify Stars", style = MaterialTheme.typography.labelLarge)
    }

    Spacer(modifier = Modifier.height(Dimens.spacingXSmall))

    OutlinedButton(onClick = onChangeClick, modifier = Modifier.fillMaxWidth()) {
      Icon(
          imageVector = Icons.Default.Edit,
          contentDescription = null,
          modifier = Modifier.size(Dimens.spacingLarge),
      )
      Spacer(modifier = Modifier.width(Dimens.spacingMedium))
      Text(text = "Change Photo", style = MaterialTheme.typography.labelLarge)
    }
  }
}

@Composable
internal fun LoadingState(uri: Uri, message: String) {
  LoadingStateContent(
      image = {
        AsyncImage(
            model = uri,
            contentDescription = "Processing image",
            modifier = Modifier.size(Dimens.thumbnailSizeLarge).clip(MaterialTheme.shapes.large),
            contentScale = ContentScale.Crop,
        )
      },
      message = message,
  )
}

@Composable
private fun LoadingStateContent(image: @Composable () -> Unit, message: String) {
  Column(
      modifier = Modifier.fillMaxSize().padding(Dimens.screenPadding),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
  ) {
    image()

    Spacer(modifier = Modifier.height(Dimens.spacingXLarge))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
      LoadingIndicator(Modifier.size(Dimens.loadingIndicatorSizeSmall))
      Spacer(modifier = Modifier.width(Dimens.spacingMedium))
      Text(message, style = MaterialTheme.typography.bodyLarge)
    }
  }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WelcomeStatePreview() {
  StarSeekTheme(dynamicColor = false) { WelcomeState(onChoosePhoto = {}) }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ImageSelectedStatePreview() {
  StarSeekTheme(dynamicColor = false) {
    ImageSelectedContent(
        image = {
          Box(
              Modifier.size(Dimens.thumbnailSizeLarge)
                  .clip(MaterialTheme.shapes.medium)
                  .background(MaterialTheme.colorScheme.surfaceVariant)
          )
        },
        onUploadClick = {},
        onChangeClick = {},
    )
  }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoadingStatePreview() {
  StarSeekTheme(dynamicColor = false) {
    LoadingStateContent(
        image = {
          Box(
              Modifier.size(Dimens.thumbnailSizeLarge)
                  .clip(MaterialTheme.shapes.large)
                  .background(MaterialTheme.colorScheme.surfaceVariant)
          )
        },
        message = "Analyzing star patterns...",
    )
  }
}
