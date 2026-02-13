package com.oadultradeepfield.starseek.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.oadultradeepfield.starseek.ui.theme.StarSeekTheme

@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
  Box(modifier = modifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoadingIndicatorPreview() {
  StarSeekTheme(dynamicColor = false) { LoadingIndicator(Modifier.fillMaxSize()) }
}
