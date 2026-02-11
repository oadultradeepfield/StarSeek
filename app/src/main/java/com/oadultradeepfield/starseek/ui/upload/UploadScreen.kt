package com.oadultradeepfield.starseek.ui.upload

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.oadultradeepfield.starseek.domain.model.Solve
import com.oadultradeepfield.starseek.ui.components.ErrorState

@Composable
fun UploadScreen(viewModel: UploadViewModel, onNavigateToResults: (Solve) -> Unit) {
  val uiState by viewModel.uiState.collectAsState()
  val launcher =
      rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri?
        ->
        uri?.let { viewModel.onImageSelected(it) }
      }
  LaunchedEffect(uiState) {
    if (uiState is UploadUiState.Success) {
      onNavigateToResults((uiState as UploadUiState.Success).solve)
      viewModel.reset()
    }
  }
  when (val state = uiState) {
    is UploadUiState.Empty -> WelcomeState(onChoosePhoto = { launcher.launch("image/*") })
    is UploadUiState.ImageSelected ->
        ImageSelectedState(
            uri = state.uri,
            onUploadClick = { viewModel.onUploadClick() },
            onChangeClick = { launcher.launch("image/*") },
        )
    is UploadUiState.Processing -> LoadingState(uri = state.uri, message = state.message)
    is UploadUiState.Success -> {}
    is UploadUiState.Error ->
        ErrorState(
            message = state.message,
            onRetry = state.lastUri?.let { { viewModel.retry() } },
        )
  }
}
