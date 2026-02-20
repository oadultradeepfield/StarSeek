package com.oadultradeepfield.starseek.ui.results

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImagePainter
import com.oadultradeepfield.starseek.domain.model.CelestialObject
import com.oadultradeepfield.starseek.ui.components.ImagePreset
import com.oadultradeepfield.starseek.ui.components.StarSeekAsyncImage

@Composable
fun ImageWithOverlay(
    imageUri: String,
    objects: List<CelestialObject>,
    highlightedName: String?,
    modifier: Modifier = Modifier,
) {
  var displayedSize by remember { mutableStateOf(IntSize.Zero) }
  var intrinsicWidth by remember { mutableIntStateOf(0) }
  var intrinsicHeight by remember { mutableIntStateOf(0) }
  val infiniteTransition = rememberInfiniteTransition(label = "glow")

  val glowAlpha by
      infiniteTransition.animateFloat(
          initialValue = 0.3f,
          targetValue = 1f,
          animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
          label = "glowAlpha",
      )

  val aspectRatio =
      if (intrinsicWidth > 0 && intrinsicHeight > 0) {
        intrinsicWidth.toFloat() / intrinsicHeight
      } else {
        4f / 3f
      }

  Box(modifier = modifier.aspectRatio(aspectRatio)) {
    StarSeekAsyncImage(
        model = imageUri.toUri(),
        contentDescription = "Star field image showing ${objects.size} celestial objects",
        modifier = Modifier.fillMaxSize().onGloballyPositioned { displayedSize = it.size },
        preset = ImagePreset.FULL,
        contentScale = ContentScale.FillWidth,
        onState = { state ->
          if (state is AsyncImagePainter.State.Success) {
            intrinsicWidth = state.result.image.width
            intrinsicHeight = state.result.image.height
          }
        },
    )

    if (displayedSize != IntSize.Zero && intrinsicWidth > 0 && intrinsicHeight > 0) {
      val transform =
          remember(displayedSize, intrinsicWidth) {
            computeCoordinateTransform(displayedSize, intrinsicWidth)
          }

      Canvas(modifier = Modifier.fillMaxSize().clipToBounds()) {
        objects.forEach { obj ->
          val px = obj.pixelX ?: return@forEach
          val py = obj.pixelY ?: return@forEach

          val isHighlighted = obj.name == highlightedName

          val alpha = if (isHighlighted) glowAlpha else 0.7f
          val radius = if (isHighlighted) 18.dp.toPx() else 14.dp.toPx()

          val center =
              Offset(
                  (px * transform.scale + transform.offsetX).toFloat(),
                  (py * transform.scale + transform.offsetY).toFloat(),
              )

          drawCircle(
              brush =
                  Brush.radialGradient(
                      colors =
                          listOf(
                              Color.Cyan.copy(alpha = alpha),
                              Color.Cyan.copy(alpha = alpha * 0.5f),
                              Color.Transparent,
                          ),
                      center = center,
                      radius = radius,
                  ),
              radius = radius,
              center = center,
          )
        }
      }
    }
  }
}

internal data class CoordinateTransform(val scale: Float, val offsetX: Float, val offsetY: Float)

internal fun computeCoordinateTransform(displayedSize: IntSize, intrinsicWidth: Int) =
    CoordinateTransform(
        scale = displayedSize.width.toFloat() / intrinsicWidth,
        offsetX = 0f,
        offsetY = 0f,
    )
