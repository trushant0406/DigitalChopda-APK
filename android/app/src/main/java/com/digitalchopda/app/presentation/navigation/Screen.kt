package com.digitalchopda.app.presentation.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object BiometricLock : Screen("biometric_lock")
    data object LoginRegister : Screen("auth")
    data object Dashboard : Screen("dashboard")
    data object CreateInvoice : Screen("create_invoice")
    data object Stock : Screen("stock")
    data object Leads : Screen("leads")
    data object GstHub : Screen("gst_hub")
    data object Settings : Screen("settings")
}
