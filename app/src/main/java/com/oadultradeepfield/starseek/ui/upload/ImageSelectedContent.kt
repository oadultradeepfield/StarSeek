package com.oadultradeepfield.starseek.ui.upload

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.oadultradeepfield.starseek.ui.components.ImagePreset
import com.oadultradeepfield.starseek.ui.components.LoadingIndicator
import com.oadultradeepfield.starseek.ui.components.StarSeekAsyncImage
import com.oadultradeepfield.starseek.ui.theme.Dimens
import com.oadultradeepfield.starseek.ui.theme.StarSeekTheme

@Composable
internal fun ImagesSelectedState(
    uris: List<Uri>,
    onUploadClick: () -> Unit,
    onChangeClick: () -> Unit,
) {
  ImageSelectedContent(
      image = {
        Box {
          StarSeekAsyncImage(
              model = uris.first(),
              contentDescription = "Selected image",
              modifier = Modifier.size(Dimens.thumbnailSizeLarge).clip(MaterialTheme.shapes.large),
              preset = ImagePreset.THUMBNAIL_LARGE,
              contentScale = ContentScale.Crop,
          )

          if (uris.size > 1) {
            Box(
                modifier =
                    Modifier.align(Alignment.BottomEnd)
                        .offset(x = 8.dp, y = 8.dp)
                        .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(
                  text = "+${uris.size - 1}",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onPrimary,
              )
            }
          }
        }
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
internal fun MultiImageLoadingState(items: List<ImageProcessingItem>) {
  var currentIndex by rememberSaveable { mutableIntStateOf(0) }
  val safeIndex = currentIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
  if (safeIndex != currentIndex) currentIndex = safeIndex

  val currentItem = items.getOrNull(safeIndex) ?: return

  Column(
      modifier = Modifier.fillMaxSize().padding(Dimens.screenPadding),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
  ) {
    Box(contentAlignment = Alignment.Center) {
      StarSeekAsyncImage(
          model = currentItem.uri,
          contentDescription = "Processing image",
          modifier = Modifier.size(Dimens.thumbnailSizeLarge).clip(MaterialTheme.shapes.large),
          preset = ImagePreset.THUMBNAIL_LARGE,
          contentScale = ContentScale.Crop,
      )

      if (items.size > 1) {
        Text(
            text = "${currentIndex + 1}/${items.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier =
                Modifier.align(Alignment.BottomCenter)
                    .offset(y = (-Dimens.spacingSmall))
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        MaterialTheme.shapes.small,
                    )
                    .padding(horizontal = Dimens.spacingSmall, vertical = Dimens.spacingXSmall),
        )

        if (currentIndex > 0) {
          IconButton(
              onClick = { currentIndex-- },
              modifier = Modifier.align(Alignment.CenterStart).offset(x = (-48).dp),
              colors =
                  IconButtonDefaults.iconButtonColors(
                      containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                  ),
          ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
          }
        }

        if (currentIndex < items.size - 1) {
          IconButton(
              onClick = { currentIndex++ },
              modifier = Modifier.align(Alignment.CenterEnd).offset(x = 48.dp),
              colors =
                  IconButtonDefaults.iconButtonColors(
                      containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                  ),
          ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(Dimens.spacingLarge))
    ImageStatusIndicator(status = currentItem.status)
  }
}

private fun stepToMessage(step: UploadStep): String = when (step) {
  UploadStep.Uploading -> "Uploading..."
  UploadStep.Analyzing -> "Analyzing stars..."
  UploadStep.Saving -> "Saving..."
}

@Composable
private fun ImageStatusIndicator(status: ImageStatus) {
  Row(
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
  ) {
    when (status) {
      is ImageStatus.Pending -> {
        Text(
            "Waiting...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      is ImageStatus.Processing -> {
        LoadingIndicator(Modifier.size(Dimens.loadingIndicatorSizeSmall))
        Spacer(modifier = Modifier.width(Dimens.spacingSmall))
        Text(stepToMessage(status.step), style = MaterialTheme.typography.bodyMedium)
      }
      is ImageStatus.Completed -> {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Completed",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(Dimens.loadingIndicatorSizeSmall),
        )

        Spacer(modifier = Modifier.width(Dimens.spacingSmall))
        Text("Done", style = MaterialTheme.typography.bodyMedium)
      }
      is ImageStatus.Failed -> {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Failed",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(Dimens.loadingIndicatorSizeSmall),
        )

        Spacer(modifier = Modifier.width(Dimens.spacingSmall))

        Text(
            status.error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
      }
    }
  }
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
private fun MultiImageLoadingStatePreview() {
  StarSeekTheme(dynamicColor = false) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Dimens.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
      Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(Dimens.thumbnailSizeLarge)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        Text(
            text = "1/3",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier =
                Modifier.align(Alignment.BottomCenter)
                    .offset(y = (-Dimens.spacingSmall))
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        MaterialTheme.shapes.small,
                    )
                    .padding(horizontal = Dimens.spacingSmall, vertical = Dimens.spacingXSmall),
        )

        IconButton(
            onClick = {},
            modifier = Modifier.align(Alignment.CenterEnd).offset(x = 48.dp),
            colors =
                IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                ),
        ) {
          Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
        }
      }

      Spacer(modifier = Modifier.height(Dimens.spacingLarge))

      Row(
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically,
      ) {
        LoadingIndicator(Modifier.size(Dimens.loadingIndicatorSizeSmall))
        Spacer(modifier = Modifier.width(Dimens.spacingSmall))
        Text("Analyzing stars...", style = MaterialTheme.typography.bodyMedium)
      }
    }
  }
}
