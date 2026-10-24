package com.digitalchopda.app.presentation.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object BiometricLock : Screen("biometric_lock")
    data object LoginRegister : Screen("auth")
    data object Dashboard : Screen("dashboard")
    data object Party : Screen("party")
    data object Billing : Screen("billing")
    data object CreateInvoice : Screen("create_invoice?mode={mode}&editId={editId}&partyId={partyId}&partyName={partyName}&partyPhone={partyPhone}") {
        fun createRoute(
            mode: String = "invoice",
            editId: Long = 0L,
            partyId: Long = 0L,
            partyName: String = "",
            partyPhone: String = ""
        ): String {
            val encName = if (partyName.isNotBlank()) java.net.URLEncoder.encode(partyName, "UTF-8") else ""
            val encPhone = if (partyPhone.isNotBlank()) java.net.URLEncoder.encode(partyPhone, "UTF-8") else ""
            return "create_invoice?mode=$mode&editId=$editId&partyId=$partyId&partyName=$encName&partyPhone=$encPhone"
        }
    }
    data object Products : Screen("products")
    data object Stock : Screen("products") // Aliased to products
    data object Leads : Screen("leads")
    data object GstHub : Screen("gst_hub")
    data object Settings : Screen("settings")
    data object DocumentDetail : Screen("document_detail/{docType}/{docId}") {
        fun createRoute(docType: String, docId: Long): String {
            return "document_detail/$docType/$docId"
        }
    }
    data object PartyDetail : Screen("party_detail/{partyId}?initialTab={initialTab}") {
        fun createRoute(partyId: Long, initialTab: Int = 0): String {
            return "party_detail/$partyId?initialTab=$initialTab"
        }
    }
    data object PdfViewer : Screen("pdf_viewer?filePath={filePath}&invoiceId={invoiceId}&customerName={customerName}&customerPhone={customerPhone}&invoiceNumber={invoiceNumber}&docType={docType}&serverOrderId={serverOrderId}") {
        fun createRoute(
            filePath: String,
            invoiceId: Long = 0L,
            customerName: String = "",
            customerPhone: String = "",
            invoiceNumber: String = "",
            docType: String = "invoice",
            serverOrderId: Long = 0L
        ): String {
            val encodedPath = java.net.URLEncoder.encode(filePath, "UTF-8")
            val encodedName = java.net.URLEncoder.encode(customerName, "UTF-8")
            val encodedPhone = java.net.URLEncoder.encode(customerPhone, "UTF-8")
            val encodedNumber = java.net.URLEncoder.encode(invoiceNumber, "UTF-8")
            val encodedDocType = java.net.URLEncoder.encode(docType, "UTF-8")
            return "pdf_viewer?filePath=$encodedPath&invoiceId=$invoiceId&customerName=$encodedName&customerPhone=$encodedPhone&invoiceNumber=$encodedNumber&docType=$encodedDocType&serverOrderId=$serverOrderId"
        }
    }
}
