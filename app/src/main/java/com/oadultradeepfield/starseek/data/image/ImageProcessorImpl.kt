package com.oadultradeepfield.starseek.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.oadultradeepfield.starseek.BuildConfig
import com.oadultradeepfield.starseek.di.BackgroundDispatcher
import com.oadultradeepfield.starseek.domain.repository.ImageProcessor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.measureNanoTime

@Singleton
class ImageProcessorImpl
@Inject
constructor(
    @param:ApplicationContext private val context: Context,
    @param:BackgroundDispatcher private val backgroundDispatcher: CoroutineDispatcher,
) : ImageProcessor {
  private data class BenchmarkResult(val memKb: Int, val payloadKb: Int, val timeMs: Double)

  private val benchmarkResults = ConcurrentHashMap<String, MutableList<BenchmarkResult>>()

  override suspend fun readBytes(uri: Uri): ByteArray =
      withContext(backgroundDispatcher) {
        val inputStream =
            context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Cannot open image")

        inputStream.use { it.readBytes() }
      }

  override fun computeHash(bytes: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(bytes)
    return hashBytes.joinToString("") { "%02x".format(it) }
  }

  override suspend fun copyToInternalStorage(bytes: ByteArray): Uri =
      withContext(backgroundDispatcher) {
        val dir = File(context.filesDir, IMAGES_DIR).apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.jpg")

        file.writeBytes(bytes)
        Uri.fromFile(file)
      }

  override suspend fun compressForUpload(bytes: ByteArray): ByteArray =
      withContext(backgroundDispatcher) {
        if (BuildConfig.DEBUG) collectBenchmark(bytes)

        val opts =
            BitmapFactory.Options().apply {
              inPreferredConfig = Bitmap.Config.RGB_565
              inSampleSize = 2
            }

        val bitmap =
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                ?: throw IllegalStateException("Cannot decode image")

        var quality = if (bytes.size <= MAX_SIZE_BYTES) 95 else 90
        var compressed: ByteArray

        do {
          val stream = ByteArrayOutputStream()
          bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
          compressed = stream.toByteArray()
          quality -= 10
        } while (compressed.size > MAX_SIZE_BYTES && quality > 10)

        compressed
      }

  override fun deleteImage(uri: Uri) {
    uri.path?.let { File(it).delete() }
  }

  private fun collectBenchmark(bytes: ByteArray) {
    data class Config(val name: String, val config: Bitmap.Config, val sampleSize: Int)

    val configs =
        listOf(
            Config("ARGB_8888", Bitmap.Config.ARGB_8888, 1),
            Config("RGB_565", Bitmap.Config.RGB_565, 1),
            Config("RGB_565 2x", Bitmap.Config.RGB_565, 2),
            Config("RGB_565 4x", Bitmap.Config.RGB_565, 4),
        )

    val results =
        configs.map { cfg ->
          var bitmap: Bitmap? = null
          val timeNs = measureNanoTime {
            val opts =
                BitmapFactory.Options().apply {
                  inPreferredConfig = cfg.config
                  inSampleSize = cfg.sampleSize
                }
            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
          }

          val memKb = (bitmap?.byteCount ?: 0) / 1024
          val stream = ByteArrayOutputStream()

          bitmap?.compress(Bitmap.CompressFormat.JPEG, 95, stream)

          val payloadKb = stream.size() / 1024
          bitmap?.recycle()
          cfg.name to BenchmarkResult(memKb, payloadKb, timeNs / 1_000_000.0)
        }

    results.forEach { (name, result) ->
      benchmarkResults.compute(name) { _, list -> (list ?: mutableListOf()).apply { add(result) } }
    }
  }

  override fun logBenchmarkSummary() {
    if (!BuildConfig.DEBUG) return

    if (benchmarkResults.isEmpty()) return
    val count = benchmarkResults.values.firstOrNull()?.size ?: return

    Log.d(TAG, "=== Image Decode Benchmark (avg of $count images) ===")

    val averaged =
        CONFIG_ORDER.mapNotNull { name ->
          benchmarkResults.remove(name)?.let { list ->
            val avgMem = list.map { it.memKb }.average().toInt()
            val avgPayload = list.map { it.payloadKb }.average().toInt()
            val avgTime = list.map { it.timeMs }.average()
            name to BenchmarkResult(avgMem, avgPayload, avgTime)
          }
        }

    if (averaged.isEmpty()) return

    val baseline = averaged.first().second
    Log.d(
        TAG,
        "| Config | Memory (KB) | Payload (KB) | Time (ms) | Memory Δ | Payload Δ | Time Δ |",
    )

    Log.d(
        TAG,
        "|--------|-------------|--------------|-----------|----------|-----------|--------|",
    )

    averaged.forEach { (name, r) ->
      val memDelta = formatDelta(r.memKb, baseline.memKb, name == averaged.first().first)

      val payloadDelta =
          formatDelta(r.payloadKb, baseline.payloadKb, name == averaged.first().first)

      val timeDelta = formatDelta(r.timeMs, baseline.timeMs, name == averaged.first().first)
      val timeStr = String.format(java.util.Locale.US, "%.1f", r.timeMs)

      Log.d(
          TAG,
          "| $name | ${r.memKb} | ${r.payloadKb} | $timeStr | $memDelta | $payloadDelta | $timeDelta |",
      )
    }
  }

  private fun formatDelta(value: Number, baseline: Number, isBaseline: Boolean): String {
    if (isBaseline) return "baseline"
    val pct = (value.toDouble() - baseline.toDouble()) / baseline.toDouble() * 100
    return String.format(java.util.Locale.US, "%+.0f", pct) + "%"
  }

  companion object {
    private const val TAG = "ImageProcessor"
    private const val MAX_SIZE_BYTES = 2 * 1024 * 1024
    private const val IMAGES_DIR = "images"
    private val CONFIG_ORDER = listOf("ARGB_8888", "RGB_565", "RGB_565 2x", "RGB_565 4x")
  }
}
