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
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.digitalchopda.app.DigitalChopdaApplication
import com.digitalchopda.app.presentation.auth.BiometricLockScreen
import com.digitalchopda.app.presentation.auth.LoginRegisterScreen
import com.digitalchopda.app.presentation.billing.CreateInvoiceScreen
import com.digitalchopda.app.presentation.billing.DocumentDetailScreen
import com.digitalchopda.app.presentation.dashboard.DashboardScreen
import com.digitalchopda.app.presentation.gst.GstHubScreen
import com.digitalchopda.app.presentation.leads.LeadsScreen
import com.digitalchopda.app.presentation.main.MainContainerScreen
import com.digitalchopda.app.presentation.party.PartyDetailScreen
import com.digitalchopda.app.presentation.pdf.PdfViewerScreen
import com.digitalchopda.app.presentation.products.ProductsScreen
import com.digitalchopda.app.presentation.settings.SettingsScreen
import com.digitalchopda.app.presentation.splash.SplashScreen

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
                companyRepository = app.companyRepository,
                authRepository = app.authRepository,
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

        // Screen 2: Login & Register Screen (Seamless Crossfade Transition from Splash)
        composable(
            route = Screen.LoginRegister.route,
            enterTransition = { fadeIn(animationSpec = tween(400)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
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

        // Screen 3: Operations Dashboard (Permanent Dock Tab 0: Chopda)
        composable(Screen.Dashboard.route) {
            MainContainerScreen(
                app = app,
                initialTab = 0,
                onNavigateToLeads = { navController.navigate(Screen.Leads.route) },
                onNavigateToPartyDetail = { partyId, initialTab ->
                    navController.navigate(Screen.PartyDetail.createRoute(partyId, initialTab))
                },
                onNavigateToCreateInvoice = { mode, editId ->
                    navController.navigate(Screen.CreateInvoice.createRoute(mode, editId))
                },
                onNavigateToDocumentDetail = { docType, docId ->
                    navController.navigate(Screen.DocumentDetail.createRoute(docType, docId))
                },
                onNavigateToPdfViewer = { filePath, invoiceId, customerName, customerPhone, invoiceNumber ->
                    navController.navigate(
                        Screen.PdfViewer.createRoute(
                            filePath = filePath,
                            invoiceId = invoiceId,
                            customerName = customerName,
                            customerPhone = customerPhone,
                            invoiceNumber = invoiceNumber
                        )
                    )
                },
                onLogoutSuccess = {
                    navController.navigate(Screen.LoginRegister.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            )
        }

        // Screen 3.5: Party Master Screen (Permanent Dock Tab 1: Party)
        composable(Screen.Party.route) {
            MainContainerScreen(
                app = app,
                initialTab = 1,
                onNavigateToLeads = { navController.navigate(Screen.Leads.route) },
                onNavigateToPartyDetail = { partyId, initialTab ->
                    navController.navigate(Screen.PartyDetail.createRoute(partyId, initialTab))
                },
                onNavigateToCreateInvoice = { mode, editId ->
                    navController.navigate(Screen.CreateInvoice.createRoute(mode, editId))
                },
                onNavigateToDocumentDetail = { docType, docId ->
                    navController.navigate(Screen.DocumentDetail.createRoute(docType, docId))
                },
                onNavigateToPdfViewer = { filePath, invoiceId, customerName, customerPhone, invoiceNumber ->
                    navController.navigate(
                        Screen.PdfViewer.createRoute(
                            filePath = filePath,
                            invoiceId = invoiceId,
                            customerName = customerName,
                            customerPhone = customerPhone,
                            invoiceNumber = invoiceNumber
                        )
                    )
                },
                onLogoutSuccess = {
                    navController.navigate(Screen.LoginRegister.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            )
        }

        // Screen 4: Billing Hub Screen (Permanent Dock Tab 2: Billing & Documents)
        composable(Screen.Billing.route) {
            MainContainerScreen(
                app = app,
                initialTab = 2,
                onNavigateToLeads = { navController.navigate(Screen.Leads.route) },
                onNavigateToPartyDetail = { partyId, initialTab ->
                    navController.navigate(Screen.PartyDetail.createRoute(partyId, initialTab))
                },
                onNavigateToCreateInvoice = { mode, editId ->
                    navController.navigate(Screen.CreateInvoice.createRoute(mode, editId))
                },
                onNavigateToDocumentDetail = { docType, docId ->
                    navController.navigate(Screen.DocumentDetail.createRoute(docType, docId))
                },
                onNavigateToPdfViewer = { filePath, invoiceId, customerName, customerPhone, invoiceNumber ->
                    navController.navigate(
                        Screen.PdfViewer.createRoute(
                            filePath = filePath,
                            invoiceId = invoiceId,
                            customerName = customerName,
                            customerPhone = customerPhone,
                            invoiceNumber = invoiceNumber
                        )
                    )
                },
                onLogoutSuccess = {
                    navController.navigate(Screen.LoginRegister.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            )
        }

        // Screen 4.2: Create / Edit Invoice or Quotation Full-Screen
        composable(
            route = Screen.CreateInvoice.route,
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType; defaultValue = "invoice" },
                navArgument("editId") { type = NavType.LongType; defaultValue = 0L },
                navArgument("partyId") { type = NavType.LongType; defaultValue = 0L },
                navArgument("partyName") { type = NavType.StringType; defaultValue = "" },
                navArgument("partyPhone") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "invoice"
            val editId = backStackEntry.arguments?.getLong("editId") ?: 0L
            val partyId = backStackEntry.arguments?.getLong("partyId") ?: 0L
            val rawName = backStackEntry.arguments?.getString("partyName") ?: ""
            val partyName = if (rawName.isNotBlank()) java.net.URLDecoder.decode(rawName, "UTF-8") else ""
            val rawPhone = backStackEntry.arguments?.getString("partyPhone") ?: ""
            val partyPhone = if (rawPhone.isNotBlank()) java.net.URLDecoder.decode(rawPhone, "UTF-8") else ""
            CreateInvoiceScreen(
                billingRepository = app.billingRepository,
                companyRepository = app.companyRepository,
                inventoryRepository = app.inventoryRepository,
                customerRepository = app.customerRepository,
                pdfGenerator = app.pdfGenerator,
                printerManager = app.printerManager,
                productRepository = app.productRepository,
                quotationRepository = app.quotationRepository,
                initialMode = mode,
                editId = editId,
                initialPartyId = partyId,
                initialPartyName = partyName,
                initialPartyPhone = partyPhone,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPdfViewer = { filePath, invoiceId, customerName, customerPhone, invoiceNumber, docType, serverOrderId ->
                    navController.navigate(
                        Screen.PdfViewer.createRoute(
                            filePath = filePath,
                            invoiceId = invoiceId,
                            customerName = customerName,
                            customerPhone = customerPhone,
                            invoiceNumber = invoiceNumber,
                            docType = docType,
                            serverOrderId = serverOrderId
                        )
                    )
                }
            )
        }

        // Screen 4.5: Document Detail Screen (Invoices, Quotations, Sales Returns)
        composable(
            route = Screen.DocumentDetail.route,
            arguments = listOf(
                navArgument("docType") { type = NavType.StringType },
                navArgument("docId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val docType = backStackEntry.arguments?.getString("docType") ?: "invoice"
            val docId = backStackEntry.arguments?.getLong("docId") ?: 0L
            DocumentDetailScreen(
                docType = docType,
                docId = docId,
                companyRepository = app.companyRepository,
                billingRepository = app.billingRepository,
                quotationRepository = app.quotationRepository,
                creditNoteRepository = app.creditNoteRepository,
                pdfGenerator = app.pdfGenerator,
                customerRepository = app.customerRepository,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { type, id ->
                    navController.navigate(Screen.CreateInvoice.createRoute(type, id))
                },
                onNavigateToPdfViewer = { filePath, invoiceId, customerName, customerPhone, invoiceNumber, dType, serverOrderId ->
                    navController.navigate(
                        Screen.PdfViewer.createRoute(
                            filePath = filePath,
                            invoiceId = invoiceId,
                            customerName = customerName,
                            customerPhone = customerPhone,
                            invoiceNumber = invoiceNumber,
                            docType = dType,
                            serverOrderId = serverOrderId
                        )
                    )
                }
            )
        }

        // Screen 5: Products Catalog Screen (Permanent Dock Tab 3: Products)
        composable(Screen.Products.route) {
            MainContainerScreen(
                app = app,
                initialTab = 3,
                onNavigateToLeads = { navController.navigate(Screen.Leads.route) },
                onNavigateToPartyDetail = { partyId, initialTab ->
                    navController.navigate(Screen.PartyDetail.createRoute(partyId, initialTab))
                },
                onNavigateToCreateInvoice = { mode, editId ->
                    navController.navigate(Screen.CreateInvoice.createRoute(mode, editId))
                },
                onNavigateToDocumentDetail = { docType, docId ->
                    navController.navigate(Screen.DocumentDetail.createRoute(docType, docId))
                },
                onNavigateToPdfViewer = { filePath, invoiceId, customerName, customerPhone, invoiceNumber ->
                    navController.navigate(
                        Screen.PdfViewer.createRoute(
                            filePath = filePath,
                            invoiceId = invoiceId,
                            customerName = customerName,
                            customerPhone = customerPhone,
                            invoiceNumber = invoiceNumber
                        )
                    )
                },
                onLogoutSuccess = {
                    navController.navigate(Screen.LoginRegister.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            )
        }

        // Screen 6: Leads Manager Screen
        composable(Screen.Leads.route) {
            LeadsScreen(
                leadRepository = app.leadRepository,
                companyRepository = app.companyRepository,
                customerRepository = app.customerRepository,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPartyDetail = { partyId, initialTab ->
                    navController.navigate(Screen.PartyDetail.createRoute(partyId, initialTab))
                }
            )
        }

        // Screen 7: GST & Tax Hub Screen (Redirects to Tab 4: Settings)
        composable(Screen.GstHub.route) {
            MainContainerScreen(
                app = app,
                initialTab = 4,
                onNavigateToLeads = { navController.navigate(Screen.Leads.route) },
                onNavigateToPartyDetail = { partyId, initialTab ->
                    navController.navigate(Screen.PartyDetail.createRoute(partyId, initialTab))
                },
                onNavigateToCreateInvoice = { mode, editId ->
                    navController.navigate(Screen.CreateInvoice.createRoute(mode, editId))
                },
                onNavigateToDocumentDetail = { docType, docId ->
                    navController.navigate(Screen.DocumentDetail.createRoute(docType, docId))
                },
                onNavigateToPdfViewer = { filePath, invoiceId, customerName, customerPhone, invoiceNumber ->
                    navController.navigate(
                        Screen.PdfViewer.createRoute(
                            filePath = filePath,
                            invoiceId = invoiceId,
                            customerName = customerName,
                            customerPhone = customerPhone,
                            invoiceNumber = invoiceNumber
                        )
                    )
                },
                onLogoutSuccess = {
                    navController.navigate(Screen.LoginRegister.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            )
        }

        // Screen 8: Settings & Customization Screen (Permanent Dock Tab 4: Settings)
        composable(Screen.Settings.route) {
            MainContainerScreen(
                app = app,
                initialTab = 4,
                onNavigateToLeads = { navController.navigate(Screen.Leads.route) },
                onNavigateToPartyDetail = { partyId, initialTab ->
                    navController.navigate(Screen.PartyDetail.createRoute(partyId, initialTab))
                },
                onNavigateToCreateInvoice = { mode, editId ->
                    navController.navigate(Screen.CreateInvoice.createRoute(mode, editId))
                },
                onNavigateToDocumentDetail = { docType, docId ->
                    navController.navigate(Screen.DocumentDetail.createRoute(docType, docId))
                },
                onNavigateToPdfViewer = { filePath, invoiceId, customerName, customerPhone, invoiceNumber ->
                    navController.navigate(
                        Screen.PdfViewer.createRoute(
                            filePath = filePath,
                            invoiceId = invoiceId,
                            customerName = customerName,
                            customerPhone = customerPhone,
                            invoiceNumber = invoiceNumber
                        )
                    )
                },
                onLogoutSuccess = {
                    navController.navigate(Screen.LoginRegister.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            )
        }

        // Screen 8.5: Party 360° Unified Hub (Ledger, Invoices, Quotations, Lead Profile)
        composable(
            route = Screen.PartyDetail.route,
            arguments = listOf(
                navArgument("partyId") { type = NavType.LongType },
                navArgument("initialTab") { type = NavType.IntType; defaultValue = 0 }
            )
        ) { backStackEntry ->
            val partyId = backStackEntry.arguments?.getLong("partyId") ?: 0L
            val initialTab = backStackEntry.arguments?.getInt("initialTab") ?: 0
            PartyDetailScreen(
                partyId = partyId,
                initialTab = initialTab,
                companyRepository = app.companyRepository,
                customerRepository = app.customerRepository,
                billingRepository = app.billingRepository,
                quotationRepository = app.quotationRepository,
                purchaseRepository = app.purchaseRepository,
                languageManager = app.languageManager,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCreateInvoice = { customerName, customerPhone, partyId ->
                    navController.navigate(
                        Screen.CreateInvoice.createRoute(
                            mode = "invoice",
                            editId = 0L,
                            partyId = partyId,
                            partyName = customerName,
                            partyPhone = customerPhone
                        )
                    )
                },
                onNavigateToDocumentDetail = { docType, docId ->
                    navController.navigate(Screen.DocumentDetail.createRoute(docType, docId))
                },
                onNavigateToPdfViewer = { filePath, invoiceId, customerName, customerPhone, invoiceNumber ->
                    navController.navigate(
                        Screen.PdfViewer.createRoute(
                            filePath = filePath,
                            invoiceId = invoiceId,
                            customerName = customerName,
                            customerPhone = customerPhone,
                            invoiceNumber = invoiceNumber
                        )
                    )
                }
            )
        }

        // Screen 9: Native In-App PDF Viewer & Dual-Channel Sharing Dock
        composable(
            route = Screen.PdfViewer.route,
            arguments = listOf(
                navArgument("filePath") { type = NavType.StringType },
                navArgument("invoiceId") { type = NavType.LongType; defaultValue = 0L },
                navArgument("customerName") { type = NavType.StringType; defaultValue = "" },
                navArgument("customerPhone") { type = NavType.StringType; defaultValue = "" },
                navArgument("invoiceNumber") { type = NavType.StringType; defaultValue = "" },
                navArgument("docType") { type = NavType.StringType; defaultValue = "invoice" },
                navArgument("serverOrderId") { type = NavType.LongType; defaultValue = 0L }
            )
        ) { backStackEntry ->
            val filePath = backStackEntry.arguments?.getString("filePath").orEmpty()
            val invoiceId = backStackEntry.arguments?.getLong("invoiceId") ?: 0L
            val rawCustomerName = backStackEntry.arguments?.getString("customerName").orEmpty()
            val customerName = try {
                java.net.URLDecoder.decode(rawCustomerName, "UTF-8").replace("+", " ")
            } catch (e: Exception) {
                rawCustomerName.replace("+", " ")
            }
            val customerPhone = backStackEntry.arguments?.getString("customerPhone").orEmpty()
            val invoiceNumber = backStackEntry.arguments?.getString("invoiceNumber").orEmpty()
            val docType = backStackEntry.arguments?.getString("docType") ?: "invoice"
            val serverOrderId = backStackEntry.arguments?.getLong("serverOrderId") ?: 0L
            PdfViewerScreen(
                filePath = filePath,
                invoiceId = invoiceId,
                customerName = customerName,
                customerPhone = customerPhone,
                invoiceNumber = invoiceNumber,
                docType = docType,
                serverOrderId = if (serverOrderId > 0L) serverOrderId else null,
                apiService = app.apiService,
                billingRepository = app.billingRepository,
                quotationRepository = app.quotationRepository,
                fileProviderUriBuilder = { file -> app.pdfGenerator.getFileProviderUri(file) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
