package com.oadultradeepfield.starseek.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.oadultradeepfield.starseek.ui.navigation.NavRoutes

enum class BottomNavItem(val route: NavRoutes, val icon: ImageVector, val label: String) {
  Upload(NavRoutes.Upload, Icons.Default.Upload, "Upload"),
  History(NavRoutes.History, Icons.Default.History, "History"),
}

@Composable
fun BottomNavBar(currentRoute: NavRoutes?, onItemClick: (NavRoutes) -> Unit) {
  NavigationBar {
    BottomNavItem.entries.forEach { item ->
      NavigationBarItem(
          selected = currentRoute == item.route,
          onClick = { onItemClick(item.route) },
          icon = { Icon(item.icon, contentDescription = item.label) },
          label = { Text(item.label) },
      )
    }
  }
}
