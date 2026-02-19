package com.oadultradeepfield.starseek.domain.model

sealed class UploadResult {
  data class CacheHit(val solveId: Long) : UploadResult()

  data class Success(val solveId: Long) : UploadResult()

  data class Failure(val error: String) : UploadResult()
}

sealed class UploadProgress {
  data object CheckingCache : UploadProgress()

  data object Uploading : UploadProgress()

  data object Analyzing : UploadProgress()
}
