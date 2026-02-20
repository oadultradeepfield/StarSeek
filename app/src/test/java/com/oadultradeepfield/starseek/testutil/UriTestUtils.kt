package com.oadultradeepfield.starseek.testutil

import android.net.Uri
import io.mockk.every
import io.mockk.mockk

fun mockUri(path: String = "file:///test/image.jpg"): Uri {
  val uri = mockk<Uri>()
  every { uri.toString() } returns path
  return uri
}
