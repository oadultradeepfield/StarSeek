package com.oadultradeepfield.starseek.ui.debug

import androidx.lifecycle.ViewModel
import com.oadultradeepfield.starseek.image.ImageLoadingMetrics
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ImageDebugViewModel
@Inject
constructor(
    private val imageLoadingMetrics: ImageLoadingMetrics,
) : ViewModel() {
  val metrics = imageLoadingMetrics.metrics

  fun resetMetrics() {
    imageLoadingMetrics.reset()
  }
}
