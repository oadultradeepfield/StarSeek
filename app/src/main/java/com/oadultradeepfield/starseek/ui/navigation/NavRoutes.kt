package com.oadultradeepfield.starseek.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class NavRoutes {
  @Serializable data object Upload : NavRoutes()

  @Serializable data object History : NavRoutes()

  @Serializable data class Results(val solveId: Long) : NavRoutes()
}
