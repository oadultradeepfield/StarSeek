package com.oadultradeepfield.starseek.image

import coil3.EventListener
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class LoadMetrics(
    val totalLoads: Int = 0,
    val cacheHits: Int = 0,
    val cacheMisses: Int = 0,
    val totalLoadTimeMs: Long = 0,
    val lastLoadTimeMs: Long = 0,
) {
  val averageLoadTimeMs: Long
    get() = if (totalLoads > 0) totalLoadTimeMs / totalLoads else 0

  val cacheHitRate: Float
    get() = if (totalLoads > 0) cacheHits.toFloat() / totalLoads else 0f
}

@Singleton
class ImageLoadingMetrics @Inject constructor() : EventListener() {
  private val _metrics = MutableStateFlow(LoadMetrics())
  val metrics: StateFlow<LoadMetrics> = _metrics.asStateFlow()

  private val requestStartTimes = mutableMapOf<ImageRequest, Long>()

  override fun onStart(request: ImageRequest) {
    requestStartTimes[request] = System.currentTimeMillis()
  }

  override fun onSuccess(request: ImageRequest, result: SuccessResult) {
    val startTime = requestStartTimes.remove(request) ?: return
    val loadTime = System.currentTimeMillis() - startTime

    val isFromCache =
        result.dataSource.name.contains("MEMORY") || result.dataSource.name.contains("DISK")

    _metrics.update { current ->
      current.copy(
          totalLoads = current.totalLoads + 1,
          cacheHits = current.cacheHits + if (isFromCache) 1 else 0,
          cacheMisses = current.cacheMisses + if (isFromCache) 0 else 1,
          totalLoadTimeMs = current.totalLoadTimeMs + loadTime,
          lastLoadTimeMs = loadTime,
      )
    }
  }

  override fun onCancel(request: ImageRequest) {
    requestStartTimes.remove(request)
  }

  override fun onError(request: ImageRequest, result: ErrorResult) {
    requestStartTimes.remove(request)
  }

  fun reset() {
    _metrics.value = LoadMetrics()
    requestStartTimes.clear()
  }
}
