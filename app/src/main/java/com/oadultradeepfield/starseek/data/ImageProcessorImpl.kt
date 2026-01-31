package com.oadultradeepfield.starseek.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageProcessorImpl
@Inject
constructor(
    @param:ApplicationContext private val context: Context,
    private val httpClient: OkHttpClient,
) {
  suspend fun readBytes(uri: Uri): ByteArray =
      withContext(Dispatchers.IO) {
        val inputStream =
            context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Cannot open image")

        inputStream.use { it.readBytes() }
      }

  fun computeHash(bytes: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(bytes)
    return hashBytes.joinToString("") { "%02x".format(it) }
  }

  suspend fun copyToInternalStorage(bytes: ByteArray): Uri =
      withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, IMAGES_DIR).apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.jpg")

        file.writeBytes(bytes)
        Uri.fromFile(file)
      }

  suspend fun compressForUpload(bytes: ByteArray): ByteArray =
      withContext(Dispatchers.IO) {
        if (bytes.size <= MAX_SIZE_BYTES) return@withContext bytes

        val bitmap =
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: throw IllegalStateException("Cannot decode image")

        var quality = 90
        var compressed: ByteArray

        do {
          val stream = ByteArrayOutputStream()
          bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
          compressed = stream.toByteArray()
          quality -= 10
        } while (compressed.size > MAX_SIZE_BYTES && quality > 10)

        compressed
      }

  suspend fun downloadAndSave(url: String): Uri =
      withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        val response = httpClient.newCall(request).execute()
        val bytes = response.body.bytes()
        copyToInternalStorage(bytes)
      }

  fun deleteImage(uri: Uri) {
    uri.path?.let { File(it).delete() }
  }

  companion object {
    private const val MAX_SIZE_BYTES = 2 * 1024 * 1024
    private const val IMAGES_DIR = "images"
  }
}
