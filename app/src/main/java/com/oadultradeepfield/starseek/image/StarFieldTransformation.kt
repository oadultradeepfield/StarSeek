package com.oadultradeepfield.starseek.image

import android.graphics.Bitmap
import androidx.core.graphics.get
import coil3.size.Size
import coil3.transform.Transformation
import kotlin.math.asinh

class StarFieldTransformation(
    private val beta: Float = 0.1f,
    private val darkThreshold: Int = 50,
) : Transformation() {
  override val cacheKey: String = "star_field_asinh_$beta"

  override suspend fun transform(input: Bitmap, size: Size): Bitmap {
    if (!isDarkImage(input)) return input
    return applyAsinhStretch(input)
  }

  private fun isDarkImage(bitmap: Bitmap): Boolean {
    val sampleSize = minOf(bitmap.width, bitmap.height, 100)

    val stepX = bitmap.width / sampleSize
    val stepY = bitmap.height / sampleSize

    var totalLuminance = 0L
    var sampleCount = 0

    for (y in 0 until bitmap.height step stepY) {
      for (x in 0 until bitmap.width step stepX) {
        val pixel = bitmap[x, y]
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF

        totalLuminance += (0.299 * r + 0.587 * g + 0.114 * b).toLong()
        sampleCount++
      }
    }

    return (totalLuminance / sampleCount) < darkThreshold
  }

  private fun applyAsinhStretch(input: Bitmap): Bitmap {
    val width = input.width
    val height = input.height
    val output = input.copy(Bitmap.Config.ARGB_8888, true)
    val pixels = IntArray(width * height)

    output.getPixels(pixels, 0, width, 0, 0, width, height)

    val asinhInvBeta = asinh(1.0 / beta).toFloat()

    for (i in pixels.indices) {
      val pixel = pixels[i]
      val a = (pixel shr 24) and 0xFF
      val r = stretchChannel((pixel shr 16) and 0xFF, asinhInvBeta)
      val g = stretchChannel((pixel shr 8) and 0xFF, asinhInvBeta)
      val b = stretchChannel(pixel and 0xFF, asinhInvBeta)
      pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    output.setPixels(pixels, 0, width, 0, 0, width, height)
    return output
  }

  private fun stretchChannel(value: Int, asinhInvBeta: Float): Int {
    val normalized = value / 255f
    val stretched = (asinh(normalized / beta) / asinhInvBeta)

    return (stretched.coerceIn(0f, 1f) * 255).toInt()
  }
}
