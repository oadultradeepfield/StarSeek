package com.oadultradeepfield.starseek.domain.model

import android.net.Uri

sealed class UploadImageResult {
  data class CacheHit(val solveId: Long) : UploadImageResult()

  data class Uploaded(val jobId: String, val imageUri: Uri, val imageHash: String) :
      UploadImageResult()

  data class Failure(val error: String) : UploadImageResult()
}
