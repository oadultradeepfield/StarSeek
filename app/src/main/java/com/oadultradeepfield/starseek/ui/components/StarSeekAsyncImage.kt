package com.oadultradeepfield.starseek.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.bitmapConfig
import coil3.size.Size

enum class ImagePreset(val sizeDp: Int?, val bitmapConfig: Bitmap.Config) {
  THUMBNAIL_SMALL(64, Bitmap.Config.RGB_565),
  THUMBNAIL_LARGE(300, Bitmap.Config.RGB_565),
  FULL(null, Bitmap.Config.ARGB_8888),
}

@SuppressLint("LocalContextResourcesRead")
@Composable
fun StarSeekAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    preset: ImagePreset = ImagePreset.FULL,
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center,
    colorFilter: ColorFilter? = null,
    filterQuality: FilterQuality = FilterQuality.Low,
    onState: ((AsyncImagePainter.State) -> Unit)? = null,
) {
  val context = LocalContext.current

  val request =
      ImageRequest.Builder(context)
          .data(model)
          .bitmapConfig(preset.bitmapConfig)
          .apply {
            if (preset.sizeDp != null) {
              val sizePx = (preset.sizeDp * context.resources.displayMetrics.density).toInt()
              size(Size(sizePx, sizePx))
            } else {
              size(Size.ORIGINAL)
            }
          }
          .build()

  AsyncImage(
      model = request,
      contentDescription = contentDescription,
      modifier = modifier,
      contentScale = contentScale,
      alignment = alignment,
      colorFilter = colorFilter,
      filterQuality = filterQuality,
      onState = onState,
  )
}
