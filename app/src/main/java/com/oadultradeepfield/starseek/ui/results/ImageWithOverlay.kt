package com.oadultradeepfield.starseek.ui.results

import android.content.res.Configuration
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import coil3.size.Size
import com.oadultradeepfield.starseek.domain.model.CelestialObject
import com.oadultradeepfield.starseek.domain.model.ObjectType
import com.oadultradeepfield.starseek.ui.theme.Dimens
import com.oadultradeepfield.starseek.ui.theme.StarSeekTheme
import kotlin.math.min

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
  val context = LocalContext.current

  Box(modifier = modifier) {
    AsyncImage(
        model = ImageRequest.Builder(context).data(imageUri.toUri()).size(Size.ORIGINAL).build(),
        contentDescription = "Star field image showing ${objects.size} celestial objects",
        modifier =
            Modifier.fillMaxSize().clip(MaterialTheme.shapes.medium).onGloballyPositioned {
              displayedSize = it.size
            },
        contentScale = ContentScale.Fit,
        onState = { state ->
          if (state is AsyncImagePainter.State.Success) {
            intrinsicWidth = state.result.image.width
            intrinsicHeight = state.result.image.height
          }
        },
    )

    if (displayedSize != IntSize.Zero && intrinsicWidth > 0 && intrinsicHeight > 0) {
      val transform =
          remember(displayedSize, intrinsicWidth, intrinsicHeight) {
            computeCoordinateTransform(displayedSize, intrinsicWidth, intrinsicHeight)
          }

      Canvas(modifier = Modifier.fillMaxSize()) {
        objects.forEach { obj ->
          val px = obj.pixelX ?: return@forEach
          val py = obj.pixelY ?: return@forEach
          val isHighlighted = obj.name == highlightedName
          val alpha = if (isHighlighted) glowAlpha else 0.7f
          val radius = if (isHighlighted) 12.dp.toPx() else 8.dp.toPx()
          val transformedX = px * transform.scale + transform.offsetX
          val transformedY = py * transform.scale + transform.offsetY
          drawCircle(
              color = Color.Cyan.copy(alpha = alpha),
              radius = radius,
              center = Offset(transformedX.toFloat(), transformedY.toFloat()),
          )
        }
      }
    }
  }
}

@Composable
internal fun ImageOverlayPreviewContent(
    objects: List<CelestialObject>,
    highlightedName: String?,
    modifier: Modifier = Modifier,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "glow")
  val glowAlpha by
      infiniteTransition.animateFloat(
          initialValue = 0.3f,
          targetValue = 1f,
          animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
          label = "glowAlpha",
      )

  Box(modifier = modifier.clip(MaterialTheme.shapes.medium)) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
    Canvas(modifier = Modifier.fillMaxSize()) {
      objects.forEach { obj ->
        val px = obj.pixelX ?: return@forEach
        val py = obj.pixelY ?: return@forEach
        val isHighlighted = obj.name == highlightedName
        val alpha = if (isHighlighted) glowAlpha else 0.7f
        val radius = if (isHighlighted) 12.dp.toPx() else 8.dp.toPx()
        drawCircle(
            color = Color.Cyan.copy(alpha = alpha),
            radius = radius,
            center = Offset(px.toFloat(), py.toFloat()),
        )
      }
    }
  }
}

private data class CoordinateTransform(val scale: Float, val offsetX: Float, val offsetY: Float)

private fun computeCoordinateTransform(
    displayedSize: IntSize,
    intrinsicWidth: Int,
    intrinsicHeight: Int,
): CoordinateTransform {
  val scaleX = displayedSize.width.toFloat() / intrinsicWidth
  val scaleY = displayedSize.height.toFloat() / intrinsicHeight
  val scale = min(scaleX, scaleY)
  val scaledWidth = intrinsicWidth * scale
  val scaledHeight = intrinsicHeight * scale
  val offsetX = (displayedSize.width - scaledWidth) / 2f
  val offsetY = (displayedSize.height - scaledHeight) / 2f
  return CoordinateTransform(scale, offsetX, offsetY)
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ImageWithOverlayPreview() {
  StarSeekTheme(dynamicColor = false) {
    val objects =
        listOf(
            CelestialObject("Betelgeuse", ObjectType.STAR, "Orion", 100.0, 80.0),
            CelestialObject("Rigel", ObjectType.STAR, "Orion", 200.0, 150.0),
            CelestialObject("Polaris", ObjectType.STAR, "Ursa Minor", 150.0, 50.0),
        )
    ImageOverlayPreviewContent(
        objects = objects,
        highlightedName = "Betelgeuse",
        modifier = Modifier.fillMaxWidth().height(Dimens.imagePreviewHeight),
    )
  }
}
