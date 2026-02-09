package com.oadultradeepfield.starseek

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.oadultradeepfield.starseek.ui.components.BottomNavBar
import com.oadultradeepfield.starseek.ui.navigation.NavRoutes
import com.oadultradeepfield.starseek.ui.navigation.StarSeekNavHost
import com.oadultradeepfield.starseek.ui.theme.StarSeekTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      StarSeekTheme {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        val showBottomBar =
            currentDestination?.hierarchy?.none { it.hasRoute<NavRoutes.Results>() } != false

        val currentRoute =
            when {
              currentDestination?.hierarchy?.any { it.hasRoute<NavRoutes.Upload>() } == true ->
                  NavRoutes.Upload
              currentDestination?.hierarchy?.any { it.hasRoute<NavRoutes.History>() } == true ->
                  NavRoutes.History
              else -> NavRoutes.Upload
            }

        Scaffold(
            bottomBar = {
              if (showBottomBar) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onItemClick = { route ->
                      navController.navigate(route) {
                        popUpTo(NavRoutes.Upload) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                      }
                    },
                )
              }
            }
        ) { innerPadding ->
          StarSeekNavHost(navController = navController, modifier = Modifier.padding(innerPadding))
        }
      }
    }
  }
}
