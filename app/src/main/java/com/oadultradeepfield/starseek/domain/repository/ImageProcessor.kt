package com.oadultradeepfield.starseek.domain.repository

import android.net.Uri

interface ImageProcessor {
  suspend fun readBytes(uri: Uri): ByteArray

  fun computeHash(bytes: ByteArray): String

  suspend fun copyToInternalStorage(bytes: ByteArray): Uri

  suspend fun compressForUpload(bytes: ByteArray): ByteArray

  fun deleteImage(uri: Uri)
}
