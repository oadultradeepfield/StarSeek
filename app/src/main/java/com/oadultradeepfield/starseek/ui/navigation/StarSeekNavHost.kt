package com.oadultradeepfield.starseek.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.oadultradeepfield.starseek.ui.history.HistoryScreen
import com.oadultradeepfield.starseek.ui.results.ResultsScreen
import com.oadultradeepfield.starseek.ui.upload.UploadScreen

private const val TRANSITION_DURATION = 300

@Composable
fun StarSeekNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
  NavHost(
      navController = navController,
      startDestination = NavRoutes.Upload,
      modifier = modifier,
      enterTransition = {
        fadeIn(animationSpec = tween(TRANSITION_DURATION)) +
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                tween(TRANSITION_DURATION),
            )
      },
      exitTransition = {
        fadeOut(animationSpec = tween(TRANSITION_DURATION)) +
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                tween(TRANSITION_DURATION),
            )
      },
      popEnterTransition = {
        fadeIn(animationSpec = tween(TRANSITION_DURATION)) +
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                tween(TRANSITION_DURATION),
            )
      },
      popExitTransition = {
        fadeOut(animationSpec = tween(TRANSITION_DURATION)) +
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                tween(TRANSITION_DURATION),
            )
      },
  ) {
    composable<NavRoutes.Upload> {
      UploadScreen(
          viewModel = hiltViewModel(),
          onNavigateToResults = { solve -> navController.navigate(NavRoutes.Results(solve.id)) },
      )
    }

    composable<NavRoutes.History> {
      HistoryScreen(
          viewModel = hiltViewModel(),
          onSolveClick = { solveId -> navController.navigate(NavRoutes.Results(solveId)) },
      )
    }

    composable<NavRoutes.Results> { backStackEntry ->
      val route = backStackEntry.toRoute<NavRoutes.Results>()
      ResultsScreen(viewModel = hiltViewModel(), solveId = route.solveId)
    }
  }
}
