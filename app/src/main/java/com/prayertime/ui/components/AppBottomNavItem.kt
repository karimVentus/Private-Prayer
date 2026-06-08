package com.prayertime.ui.components

import androidx.compose.ui.graphics.vector.ImageVector

data class AppBottomNavItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)
