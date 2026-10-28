package com.digitalchopda.app.presentation.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.digitalchopda.app.DigitalChopdaApplication
import com.digitalchopda.app.core.designsystem.DeepObsidianCanvas
import com.digitalchopda.app.presentation.billing.BillingHubScreen
import com.digitalchopda.app.presentation.dashboard.DashboardScreen
import com.digitalchopda.app.presentation.dashboard.FloatingCapsuleDock
import com.digitalchopda.app.presentation.party.PartyScreen
import com.digitalchopda.app.presentation.products.ProductsScreen
import com.digitalchopda.app.presentation.settings.SettingsScreen

/**
 * 🛸 Main Unified Container with Permanent Bottom Navigation Dock
 *
 * Hosts the 5 core modules:
 * 0 -> Chopda (Operations & Ledger Dashboard)
 * 1 -> Party (Party Master & Khata 360)
 * 2 -> Billing (Billing & Documents Hub: Invoices, Quotations, Sales Returns)
 * 3 -> Product (Catalog & Inventory)
 * 4 -> Settings (Firm Config, GST Tax Hub & Biometrics)
 *
 * Provides fluid horizontal transitions and ensures all screen contents adjust
 * cleanly within the available vertical space above the permanent dock.
 */
@Composable
fun MainContainerScreen(
    app: DigitalChopdaApplication,
    initialTab: Int = 0,
    onNavigateToLeads: () -> Unit,
    onNavigateToPartyDetail: (partyId: Long, initialTab: Int) -> Unit = { _, _ -> },
    onNavigateToCreateInvoice: (mode: String, editId: Long) -> Unit = { _, _ -> },
    onNavigateToDocumentDetail: (docType: String, docId: Long) -> Unit = { _, _ -> },
    onNavigateToPdfViewer: (filePath: String, invoiceId: Long, customerName: String, customerPhone: String, invoiceNumber: String) -> Unit,
    onLogoutSuccess: () -> Unit
) {
    var currentTab by rememberSaveable { mutableIntStateOf(initialTab) }

    // Intercept back presses on secondary tabs to gracefully return to Chopda (Tab 0)
    BackHandler(enabled = currentTab != 0) {
        currentTab = 0
    }
    Scaffold(
        containerColor = DeepObsidianCanvas,
        bottomBar = {
            FloatingCapsuleDock(
                currentTab = currentTab,
                onTabSelect = { tabIndex ->
                    currentTab = tabIndex
                }
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
            color = DeepObsidianCanvas
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    (slideInVertically(
                        animationSpec = spring(
                            dampingRatio = 0.82f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        initialOffsetY = { fullHeight -> (fullHeight * 0.08f).toInt() }
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                    ) + scaleIn(
                        initialScale = 0.95f,
                        animationSpec = spring(
                            dampingRatio = 0.82f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        transformOrigin = TransformOrigin(0.5f, 1.0f)
                    )) togetherWith (
                        fadeOut(
                            animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)
                        ) + scaleOut(
                            targetScale = 0.98f,
                            animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
                            transformOrigin = TransformOrigin(0.5f, 1.0f)
                        )
                    )
                },
                label = "PermanentDockTabTransition"
            ) { tab ->
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DeepObsidianCanvas
                ) {
                    when (tab) {
                        0 -> DashboardScreen(
                            companyRepository = app.companyRepository,
                            billingRepository = app.billingRepository,
                            customerRepository = app.customerRepository,
                            languageManager = app.languageManager,
                            onNavigateToParty = { currentTab = 1 },
                            onNavigateToBilling = { currentTab = 2 },
                            onNavigateToStock = { currentTab = 3 },
                            onNavigateToLeads = onNavigateToLeads,
                            onNavigateToGst = { currentTab = 4 },
                            onNavigateToSettings = { currentTab = 4 },
                            onNavigateToPartyDetail = onNavigateToPartyDetail,
                            onNavigateToDocumentDetail = onNavigateToDocumentDetail,
                            showBottomDock = false
                        )
                        1 -> PartyScreen(
                            customerRepository = app.customerRepository,
                            companyRepository = app.companyRepository,
                            onNavigateBack = { currentTab = 0 },
                            onNavigateToPartyDetail = onNavigateToPartyDetail
                        )
                        2 -> BillingHubScreen(
                            companyRepository = app.companyRepository,
                            billingRepository = app.billingRepository,
                            quotationRepository = app.quotationRepository,
                            creditNoteRepository = app.creditNoteRepository,
                            customerRepository = app.customerRepository,
                            purchaseRepository = app.purchaseRepository,
                            pdfGenerator = app.pdfGenerator,
                            onNavigateToCreateInvoice = { onNavigateToCreateInvoice("invoice", 0L) },
                            onNavigateToCreateQuotation = { onNavigateToCreateInvoice("quotation", 0L) },
                            onNavigateToDocumentDetail = onNavigateToDocumentDetail,
                            onNavigateToPdfViewer = onNavigateToPdfViewer
                        )
                        3 -> ProductsScreen(
                            productRepository = app.productRepository,
                            companyRepository = app.companyRepository,
                            onNavigateBack = { currentTab = 0 }
                        )
                        4 -> SettingsScreen(
                            companyRepository = app.companyRepository,
                            authRepository = app.authRepository,
                            languageManager = app.languageManager,
                            biometricAuthManager = app.biometricAuthManager,
                            onNavigateBack = { currentTab = 0 },
                            onLogoutSuccess = onLogoutSuccess
                        )
                    }
                }
            }
        }
    }
}
