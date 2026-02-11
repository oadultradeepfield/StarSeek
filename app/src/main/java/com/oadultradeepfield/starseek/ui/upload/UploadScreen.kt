package com.oadultradeepfield.starseek.ui.upload

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.oadultradeepfield.starseek.ui.components.ErrorState

@Composable
fun UploadScreen(viewModel: UploadViewModel, onNavigateToResults: (List<Long>) -> Unit) {
  val uiState by viewModel.uiState.collectAsState()
  val launcher =
      rememberLauncherForActivityResult(
          contract = ActivityResultContracts.PickMultipleVisualMedia(UploadViewModel.MAX_IMAGES)
      ) { uris ->
        if (uris.isNotEmpty()) viewModel.onImagesSelected(uris)
      }
  val launchPicker = {
    launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
  }
  LaunchedEffect(uiState) {
    if (uiState is UploadUiState.Success) {
      onNavigateToResults((uiState as UploadUiState.Success).solveIds)
      viewModel.reset()
    }
  }
  when (val state = uiState) {
    is UploadUiState.Empty -> WelcomeState(onChoosePhoto = launchPicker)
    is UploadUiState.ImagesSelected ->
        ImagesSelectedState(
            uris = state.uris,
            onUploadClick = { viewModel.onUploadClick() },
            onChangeClick = launchPicker,
        )
    is UploadUiState.Processing -> MultiImageLoadingState(items = state.items)
    is UploadUiState.Success -> {}
    is UploadUiState.Error ->
        ErrorState(
            message = state.message,
            onRetry = state.lastUris?.let { { viewModel.retry() } },
        )
  }
}
