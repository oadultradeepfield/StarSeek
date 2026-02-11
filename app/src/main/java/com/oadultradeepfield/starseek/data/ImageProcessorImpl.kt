package com.oadultradeepfield.starseek.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.oadultradeepfield.starseek.domain.repository.ImageProcessor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageProcessorImpl
@Inject
constructor(@param:ApplicationContext private val context: Context) : ImageProcessor {
  override suspend fun readBytes(uri: Uri): ByteArray =
      withContext(Dispatchers.IO) {
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
      withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, IMAGES_DIR).apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.jpg")

        file.writeBytes(bytes)
        Uri.fromFile(file)
      }

  override suspend fun compressForUpload(bytes: ByteArray): ByteArray =
      withContext(Dispatchers.IO) {
        val bitmap =
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
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

  companion object {
    private const val MAX_SIZE_BYTES = 2 * 1024 * 1024
    private const val IMAGES_DIR = "images"
  }
}
