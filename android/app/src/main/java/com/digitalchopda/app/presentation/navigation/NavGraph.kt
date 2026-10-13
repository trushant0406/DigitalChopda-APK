package com.digitalchopda.app.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.digitalchopda.app.DigitalChopdaApplication
import com.digitalchopda.app.presentation.auth.BiometricLockScreen
import com.digitalchopda.app.presentation.auth.LoginRegisterScreen
import com.digitalchopda.app.presentation.billing.CreateInvoiceScreen
import com.digitalchopda.app.presentation.dashboard.DashboardScreen
import com.digitalchopda.app.presentation.gst.GstHubScreen
import com.digitalchopda.app.presentation.leads.LeadsScreen
import com.digitalchopda.app.presentation.settings.SettingsScreen
import com.digitalchopda.app.presentation.splash.SplashScreen
import com.digitalchopda.app.presentation.stock.StockScreen

@Composable
fun DigitalChopdaNavGraph(
    navController: NavHostController,
    app: DigitalChopdaApplication
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        // Screen 1: Splash Screen
        composable(Screen.Splash.route) {
            val isLoggedIn = app.authRepository.isUserLoggedIn()
            val isBiometricEnabled by app.biometricAuthManager.isBiometricEnabled.collectAsState()
            SplashScreen(
                isLoggedIn = isLoggedIn,
                languageManager = app.languageManager,
                onNavigateNext = { loggedIn ->
                    val destination = if (isBiometricEnabled && !app.biometricAuthManager.isSessionAuthenticated) {
                        Screen.BiometricLock.route
                    } else if (loggedIn) {
                        Screen.Dashboard.route
                    } else {
                        Screen.LoginRegister.route
                    }
                    navController.navigate(destination) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // Screen 1.5: Hardware Biometric Lock Screen
        composable(Screen.BiometricLock.route) {
            BiometricLockScreen(
                biometricAuthManager = app.biometricAuthManager,
                languageManager = app.languageManager,
                onAuthSuccess = {
                    val destination = if (app.authRepository.isUserLoggedIn()) {
                        Screen.Dashboard.route
                    } else {
                        Screen.LoginRegister.route
                    }
                    navController.navigate(destination) {
                        popUpTo(Screen.BiometricLock.route) { inclusive = true }
                    }
                }
            )
        }

        // Screen 2: Login & Register Screen
        composable(Screen.LoginRegister.route) {
            LoginRegisterScreen(
                authRepository = app.authRepository,
                languageManager = app.languageManager,
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.LoginRegister.route) { inclusive = true }
                    }
                }
            )
        }

        // Screen 3: Operations Dashboard
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                companyRepository = app.companyRepository,
                billingRepository = app.billingRepository,
                customerRepository = app.customerRepository,
                languageManager = app.languageManager,
                onNavigateToBilling = { navController.navigate(Screen.CreateInvoice.route) },
                onNavigateToStock = { navController.navigate(Screen.Stock.route) },
                onNavigateToLeads = { navController.navigate(Screen.Leads.route) },
                onNavigateToGst = { navController.navigate(Screen.GstHub.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        // Screen 4: Create Invoice Screen
        composable(Screen.CreateInvoice.route) {
            CreateInvoiceScreen(
                billingRepository = app.billingRepository,
                companyRepository = app.companyRepository,
                inventoryRepository = app.inventoryRepository,
                customerRepository = app.customerRepository,
                pdfGenerator = app.pdfGenerator,
                printerManager = app.printerManager,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Screen 5: Manage Stock Screen
        composable(Screen.Stock.route) {
            StockScreen(
                inventoryRepository = app.inventoryRepository,
                companyRepository = app.companyRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Screen 6: Leads Manager Screen
        composable(Screen.Leads.route) {
            LeadsScreen(
                leadRepository = app.leadRepository,
                companyRepository = app.companyRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Screen 7: GST & Tax Hub Screen
        composable(Screen.GstHub.route) {
            GstHubScreen(
                mockApi = app.mockApi,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Screen 8: Settings & Customization Screen
        composable(Screen.Settings.route) {
            SettingsScreen(
                companyRepository = app.companyRepository,
                authRepository = app.authRepository,
                mockApi = app.mockApi,
                languageManager = app.languageManager,
                biometricAuthManager = app.biometricAuthManager,
                onNavigateBack = { navController.popBackStack() },
                onLogoutSuccess = {
                    navController.navigate(Screen.LoginRegister.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
