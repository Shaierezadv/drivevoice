package com.drivevoice.assistant.ui.navigation

sealed class Dest(val route: String) {
    data object Permissions : Dest("permissions")
    data object Driving : Dest("driving")
    data object Settings : Dest("settings")
    data object Help : Dest("help")
}
