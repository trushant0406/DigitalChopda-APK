package com.digitalchopda.app.presentation.billing

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.digitalchopda.app.core.designsystem.BrandPrimaryBlue
import com.digitalchopda.app.core.designsystem.ChopdaCard
import com.digitalchopda.app.core.designsystem.DarkNavy
import com.digitalchopda.app.core.designsystem.DeepObsidianCanvas
import com.digitalchopda.app.core.designsystem.ImperialGoldLight
import com.digitalchopda.app.core.designsystem.getAvatarColors
import com.digitalchopda.app.core.document.DocumentItemParser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.DisposableEffect
import android.view.ViewGroup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalchopda.app.core.designsystem.BorderStrokeDark
import com.digitalchopda.app.core.designsystem.ElevatedSlate
import com.digitalchopda.app.core.designsystem.MidnightCardSurface
import com.digitalchopda.app.core.designsystem.RubyUdhar
import com.digitalchopda.app.core.designsystem.RubyUdharBg
import com.digitalchopda.app.core.designsystem.TextMuted
import com.digitalchopda.app.core.designsystem.TextSubtle
import com.digitalchopda.app.core.designsystem.TextWhite
import com.digitalchopda.app.core.designsystem.bouncyClick
import com.digitalchopda.app.core.document.InvoicePdfGenerator
import com.digitalchopda.app.data.local.entity.CreditNoteEntity
import com.digitalchopda.app.data.local.entity.CustomerEntity
import com.digitalchopda.app.data.local.entity.InvoiceEntity
import com.digitalchopda.app.data.repository.BillingRepository
import com.digitalchopda.app.data.repository.CompanyRepository
import com.digitalchopda.app.data.repository.CreditNoteRepository
import com.digitalchopda.app.data.repository.CustomerRepository
import com.digitalchopda.app.domain.model.Company
import com.digitalchopda.app.domain.model.LineItem
import com.digitalchopda.app.util.CurrencyFormatter
import com.digitalchopda.app.util.formatINR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val RETURN_REASONS = listOf(
    "Sales Return",
    "Damaged / Defective Goods",
    "Incorrect Item Delivered",
    "Post-Sale Discount / Rate Difference",
    "Order Cancellation / Partial Refund",
    "Quality Not Satisfactory",
    "Other"
)

data class ReturnItemDraft(
    var name: String = "",
    var quantity: Int = 1,
    var unitPrice: Double = 0.0,
    var gstRatePercent: Double = 5.0
) {
    val subtotal: Double get() = quantity * unitPrice
    val taxAmount: Double get() = subtotal * (gstRatePercent / 100.0)
    val total: Double get() = subtotal + taxAmount
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCreditNoteDialog(
    companyRepository: CompanyRepository,
    customerRepository: CustomerRepository,
    billingRepository: BillingRepository,
    creditNoteRepository: CreditNoteRepository,
    pdfGenerator: InvoicePdfGenerator,
    initialInvoiceId: Long? = null,
    initialCustomerId: Long? = null,
    onDismiss: () -> Unit,
    onCreditNoteCreated: (CreditNoteEntity, File) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeCompany by remember {
        mutableStateOf(
            Company(
                id = 1L,
                firmName = "Sharma Traders",
                ownerName = "Amit Sharma",
                gstin = "24AAACS1429B1Z8",
                phone = "+91 98765 43210",
                address = "Surat, Gujarat",
                isActive = true
            )
        )
    }

    LaunchedEffect(Unit) {
        activeCompany = companyRepository.getActiveCompany()
    }

    val customers by customerRepository.getCustomersForCompany(activeCompany.id).collectAsState(initial = emptyList())
    val invoices by billingRepository.getInvoicesForCompany(activeCompany.id).collectAsState(initial = emptyList())

    var selectedCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var selectedInvoice by remember { mutableStateOf<InvoiceEntity?>(null) }

    var creditNoteNumber by remember { mutableStateOf("CN-0001") }
    var selectedReason by remember { mutableStateOf(RETURN_REASONS[0]) }
    var reasonDropdownExpanded by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }

    val returnItems = remember {
        mutableStateListOf(
            ReturnItemDraft(name = "", quantity = 1, unitPrice = 0.0, gstRatePercent = 5.0)
        )
    }

    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(activeCompany.id) {
        creditNoteNumber = creditNoteRepository.getNextCreditNoteNumber(activeCompany.id)
    }

    LaunchedEffect(initialCustomerId, customers) {
        if (initialCustomerId != null && selectedCustomer == null) {
            selectedCustomer = customers.firstOrNull { it.id == initialCustomerId }
        }
    }

    fun populateFromInvoice(inv: InvoiceEntity) {
        selectedInvoice = inv
        if (selectedCustomer == null) {
            selectedCustomer = customers.firstOrNull { it.phone == inv.customerPhone }
        }
        val parsed = DocumentItemParser.parseLineItems(
            itemsJson = inv.itemsJson,
            itemsSummary = inv.itemsSummary,
            defaultSubtotal = inv.subtotal
        )
        returnItems.clear()
        if (parsed.isNotEmpty()) {
            parsed.forEach { item ->
                returnItems.add(
                    ReturnItemDraft(
                        name = item.title,
                        quantity = item.quantity.toInt().coerceAtLeast(1),
                        unitPrice = item.unitPrice,
                        gstRatePercent = item.gstRatePercent
                    )
                )
            }
        } else {
            returnItems.add(
                ReturnItemDraft(
                    name = inv.itemsSummary.split(",").firstOrNull()?.trim() ?: "Returned Item",
                    quantity = 1,
                    unitPrice = inv.subtotal,
                    gstRatePercent = 5.0
                )
            )
        }

        if (inv.serverOrderId != null && creditNoteRepository.apiService != null) {
            scope.launch {
                try {
                    val res = creditNoteRepository.apiService.getReturnableOrderItems(inv.serverOrderId)
                    if (res.isSuccessful && res.body() != null) {
                        val cloudItems = res.body()!!.items
                        if (cloudItems.isNotEmpty()) {
                            returnItems.clear()
                            cloudItems.forEach { cItem ->
                                returnItems.add(
                                    ReturnItemDraft(
                                        name = cItem.productName,
                                        quantity = cItem.quantity.toInt().coerceAtLeast(1),
                                        unitPrice = cItem.unitPrice,
                                        gstRatePercent = cItem.gstRate ?: 5.0
                                    )
                                )
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    LaunchedEffect(initialInvoiceId, invoices) {
        if (initialInvoiceId != null && selectedInvoice == null) {
            val inv = invoices.firstOrNull { it.id == initialInvoiceId }
            if (inv != null) {
                populateFromInvoice(inv)
            }
        }
    }

    val totalSubtotal = returnItems.sumOf { it.subtotal }
    val totalTax = returnItems.sumOf { it.taxAmount }
    val grandRefundTotal = totalSubtotal + totalTax

    fun submitCreditNote() {
        if (isSaving) return
        if (selectedCustomer == null) {
            errorMessage = "Please select a party/customer for this credit note"
            return
        }
        val validItems = returnItems.filter { it.name.isNotBlank() && it.quantity > 0 }
        if (validItems.isEmpty()) {
            errorMessage = "Please enter at least one item with name and quantity > 0"
            return
        }

        isSaving = true
        errorMessage = null

        scope.launch {
            try {
                val cust = selectedCustomer!!
                val summary = validItems.joinToString(", ") { "${it.name} (x${it.quantity})" }
                val lineItems = validItems.map {
                    LineItem(
                        title = it.name,
                        quantity = it.quantity,
                        unitPrice = it.unitPrice,
                        gstRatePercent = it.gstRatePercent
                    )
                }
                val itemsJson = DocumentItemParser.toJson(lineItems)
                val entity = CreditNoteEntity(
                    companyId = activeCompany.id,
                    creditNoteNumber = creditNoteNumber.trim().ifBlank { "CN-0001" },
                    customerName = cust.name,
                    customerPhone = cust.phone,
                    orderId = selectedInvoice?.id,
                    originalInvoiceNumber = selectedInvoice?.invoiceNumber ?: "",
                    reason = selectedReason,
                    itemsSummary = summary,
                    itemsJson = itemsJson,
                    subtotal = totalSubtotal,
                    taxAmount = totalTax,
                    totalAmount = grandRefundTotal,
                    notes = notes.trim(),
                    status = "ACTIVE"
                )

                val newId = creditNoteRepository.createCreditNote(entity)
                val savedEntity = entity.copy(id = newId)

                val pdfFile = withContext(Dispatchers.IO) {
                    pdfGenerator.generateCreditNotePdf(activeCompany, savedEntity, lineItems)
                }

                isSaving = false
                Toast.makeText(context, "Credit Note ${savedEntity.creditNoteNumber} Issued ✓", Toast.LENGTH_SHORT).show()
                onCreditNoteCreated(savedEntity, pdfFile)
            } catch (e: Exception) {
                isSaving = false
                errorMessage = e.localizedMessage ?: "Failed to issue credit note"
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        val dialogView = LocalView.current
        DisposableEffect(dialogView) {
            val window = (dialogView.parent as? DialogWindowProvider)?.window
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            onDispose {}
        }

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            containerColor = DeepObsidianCanvas,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "New Sales Return",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = TextWhite
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = RubyUdharBg,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, RubyUdhar.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = "CREDIT NOTE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RubyUdhar
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Reverses GST liability & adjusts customer khata ledger",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { if (!isSaving) onDismiss() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextWhite
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { submitCreditNote() },
                            enabled = !isSaving
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save",
                                tint = RubyUdhar
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightCardSurface)
                )
                HorizontalDivider(color = BorderStrokeDark, thickness = 1.dp)
            },
            bottomBar = {
                // Fixed Bottom Action Dock
                Surface(
                    color = MidnightCardSurface,
                    shadowElevation = 12.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStrokeDark)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .padding(bottom = 20.dp)
                    ) {
                        Button(
                            onClick = { submitCreditNote() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            enabled = !isSaving,
                            colors = ButtonDefaults.buttonColors(containerColor = RubyUdhar)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                            } else {
                                Text(
                                    text = "Issue Credit Note • ${CurrencyFormatter.formatINR(grandRefundTotal)}",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ─── 1. DOCUMENT IDENTIFIER & META CARD ───
                ChopdaCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = creditNoteNumber.ifBlank { "CN-0001" },
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = RubyUdhar,
                                fontSize = 14.5.sp
                            )
                        )

                        val dateStr = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(Date())
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = TextMuted,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = BorderStrokeDark, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = creditNoteNumber,
                            onValueChange = { creditNoteNumber = it },
                            label = { Text("CN Number") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RubyUdhar,
                                unfocusedBorderColor = BorderStrokeDark,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedContainerColor = ElevatedSlate,
                                unfocusedContainerColor = ElevatedSlate
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Box(modifier = Modifier.weight(1.3f)) {
                            OutlinedTextField(
                                value = selectedReason,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Return Reason") },
                                trailingIcon = {
                                    IconButton(onClick = { reasonDropdownExpanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextMuted)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { reasonDropdownExpanded = true },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RubyUdhar,
                                    unfocusedBorderColor = BorderStrokeDark,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedContainerColor = ElevatedSlate,
                                    unfocusedContainerColor = ElevatedSlate
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                            DropdownMenu(
                                expanded = reasonDropdownExpanded,
                                onDismissRequest = { reasonDropdownExpanded = false },
                                modifier = Modifier.background(MidnightCardSurface)
                            ) {
                                RETURN_REASONS.forEach { reason ->
                                    DropdownMenuItem(
                                        text = { Text(reason, color = TextWhite) },
                                        onClick = {
                                            selectedReason = reason
                                            reasonDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Optional Original Invoice Linker
                    var invoiceDropdownOpen by remember { mutableStateOf(false) }
                    val availableInvoices = remember(selectedCustomer, invoices) {
                        if (selectedCustomer != null) {
                            invoices.filter { it.customerPhone == selectedCustomer?.phone }
                        } else {
                            invoices
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedInvoice?.invoiceNumber ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Original Invoice Reference (Optional)") },
                            placeholder = { Text(if (availableInvoices.isNotEmpty()) "Tap to link original bill..." else "No bills found") },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (selectedInvoice != null) {
                                        IconButton(onClick = { selectedInvoice = null }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                                        }
                                    }
                                    IconButton(onClick = { invoiceDropdownOpen = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Open dropdown", tint = TextMuted)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { invoiceDropdownOpen = true },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RubyUdhar,
                                unfocusedBorderColor = BorderStrokeDark,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedContainerColor = ElevatedSlate,
                                unfocusedContainerColor = ElevatedSlate
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        DropdownMenu(
                            expanded = invoiceDropdownOpen && availableInvoices.isNotEmpty(),
                            onDismissRequest = { invoiceDropdownOpen = false },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .background(MidnightCardSurface)
                        ) {
                            availableInvoices.forEach { inv ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = "${inv.invoiceNumber} • ${inv.customerName}",
                                                fontWeight = FontWeight.Bold,
                                                color = TextWhite,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = "Total: ₹${inv.grandTotal} • ${inv.itemsSummary}",
                                                color = TextSubtle,
                                                fontSize = 11.sp,
                                                maxLines = 1
                                            )
                                        }
                                    },
                                    onClick = {
                                        populateFromInvoice(inv)
                                        invoiceDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }
                }

                // ─── 2. CUSTOMER PARTY SELECTOR CARD ───
                ChopdaCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RETURN FROM PARTY (CUSTOMER)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextSubtle,
                                letterSpacing = 0.5.sp,
                                fontSize = 10.5.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val custDisplayName = selectedCustomer?.name ?: "Choose Customer / Party..."
                    val (cAvatarBg, cAvatarText) = getAvatarColors(custDisplayName)
                    var partyDropdownOpen by remember { mutableStateOf(false) }
                    var partySearch by remember { mutableStateOf("") }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(ElevatedSlate)
                                .border(1.dp, BorderStrokeDark, RoundedCornerShape(10.dp))
                                .clickable { partyDropdownOpen = !partyDropdownOpen }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(cAvatarBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = custDisplayName.take(2).uppercase().ifBlank { "C" },
                                    color = cAvatarText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = custDisplayName,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedCustomer != null) DarkNavy else TextMuted,
                                        fontSize = 14.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val custSubtitle = buildString {
                                    if (selectedCustomer?.phone?.isNotBlank() == true) append(selectedCustomer!!.phone)
                                    if (selectedCustomer?.gstin?.isNotBlank() == true) {
                                        if (isNotEmpty()) append(" • ")
                                        append("GST: ${selectedCustomer!!.gstin}")
                                    }
                                    if (isEmpty()) append("Tap to choose customer from khata")
                                }
                                Text(
                                    text = custSubtitle,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Icon(
                                imageVector = if (partyDropdownOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle",
                                tint = RubyUdhar,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = partyDropdownOpen,
                            onDismissRequest = { partyDropdownOpen = false },
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .background(MidnightCardSurface)
                                .border(1.dp, BorderStrokeDark, RoundedCornerShape(10.dp))
                        ) {
                            Box(modifier = Modifier.padding(8.dp)) {
                                OutlinedTextField(
                                    value = partySearch,
                                    onValueChange = { partySearch = it },
                                    placeholder = { Text("Search customer name, phone...", fontSize = 12.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp)) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = RubyUdhar,
                                        unfocusedBorderColor = BorderStrokeDark,
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite,
                                        focusedContainerColor = ElevatedSlate,
                                        unfocusedContainerColor = ElevatedSlate
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }

                            val filteredParties = customers.filter {
                                partySearch.isBlank() ||
                                it.name.contains(partySearch, ignoreCase = true) ||
                                it.phone.contains(partySearch) ||
                                it.gstin.contains(partySearch, ignoreCase = true)
                            }.take(8)

                            for (cust in filteredParties) {
                                val (pBg, pText) = getAvatarColors(cust.name)
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(pBg),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(cust.name.take(1).uppercase(), color = pText, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(cust.name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                if (cust.phone.isNotBlank()) {
                                                    Text(cust.phone, color = TextMuted, fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedCustomer = cust
                                        partyDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }
                }

                // ─── 3. RETURNED ITEMS CARD ───
                ChopdaCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "RETURNED ITEMS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextSubtle,
                                    letterSpacing = 0.5.sp,
                                    fontSize = 10.5.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ElevatedSlate
                            ) {
                                Text(
                                    text = "${returnItems.size} ${if (returnItems.size == 1) "item" else "items"}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        color = TextMuted,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(RubyUdhar.copy(alpha = 0.12f))
                                .border(1.dp, RubyUdhar.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .bouncyClick {
                                    returnItems.add(ReturnItemDraft(name = "", quantity = 1, unitPrice = 0.0, gstRatePercent = 5.0))
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = RubyUdhar, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+ Add Item",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = RubyUdhar,
                                        fontSize = 11.5.sp
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    returnItems.forEachIndexed { index, item ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = ElevatedSlate,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderStrokeDark)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Item #${index + 1}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = RubyUdhar
                                        )
                                    )
                                    if (returnItems.size > 1) {
                                        IconButton(
                                            onClick = { returnItems.removeAt(index) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = RubyUdhar,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = item.name,
                                    onValueChange = { item.name = it },
                                    placeholder = { Text("Product name / return description", color = TextMuted) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = RubyUdhar,
                                        unfocusedBorderColor = BorderStrokeDark,
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite,
                                        focusedContainerColor = MidnightCardSurface,
                                        unfocusedContainerColor = MidnightCardSurface
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = if (item.quantity == 0) "" else item.quantity.toString(),
                                        onValueChange = {
                                            item.quantity = it.toIntOrNull() ?: 0
                                        },
                                        label = { Text("Qty", fontSize = 10.sp) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = RubyUdhar,
                                            unfocusedBorderColor = BorderStrokeDark,
                                            focusedTextColor = TextWhite,
                                            unfocusedTextColor = TextWhite,
                                            focusedContainerColor = MidnightCardSurface,
                                            unfocusedContainerColor = MidnightCardSurface
                                        ),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    OutlinedTextField(
                                        value = if (item.unitPrice == 0.0) "" else item.unitPrice.toString(),
                                        onValueChange = {
                                            item.unitPrice = it.toDoubleOrNull() ?: 0.0
                                        },
                                        label = { Text("Rate (₹)", fontSize = 10.sp) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = RubyUdhar,
                                            unfocusedBorderColor = BorderStrokeDark,
                                            focusedTextColor = TextWhite,
                                            unfocusedTextColor = TextWhite,
                                            focusedContainerColor = MidnightCardSurface,
                                            unfocusedContainerColor = MidnightCardSurface
                                        ),
                                        modifier = Modifier.weight(1.2f),
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    // GST % chips (0%, 5%, 12%, 18%, 28%)
                                    Column(modifier = Modifier.weight(1.4f)) {
                                        Text("GST %", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                            listOf(0.0, 5.0, 12.0, 18.0, 28.0).forEach { rate ->
                                                val isGstSelected = item.gstRatePercent == rate
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(if (isGstSelected) RubyUdhar else MidnightCardSurface)
                                                        .border(1.dp, if (isGstSelected) RubyUdhar else BorderStrokeDark, RoundedCornerShape(6.dp))
                                                        .clickable { item.gstRatePercent = rate }
                                                        .padding(horizontal = 4.dp, vertical = 5.dp)
                                                ) {
                                                    Text(
                                                        text = "${rate.toInt()}%",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 9.sp,
                                                            fontWeight = if (isGstSelected) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (isGstSelected) Color.White else TextMuted
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Line Refund Total", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp))
                                    Text(
                                        text = CurrencyFormatter.formatINR(item.total),
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = RubyUdhar)
                                    )
                                }
                            }
                        }
                    }
                }

                // ─── 4. FINANCIAL SUMMARY CARD ───
                ChopdaCard {
                    Text(
                        text = "RETURN FINANCIALS & GST REVERSAL",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextSubtle,
                            letterSpacing = 0.5.sp,
                            fontSize = 10.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Taxable Return Value", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                        Text(CurrencyFormatter.formatINR(totalSubtotal), style = MaterialTheme.typography.bodySmall.copy(color = TextWhite, fontWeight = FontWeight.SemiBold))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Reversed GST Tax", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                        Text(CurrencyFormatter.formatINR(totalTax), style = MaterialTheme.typography.bodySmall.copy(color = TextWhite, fontWeight = FontWeight.SemiBold))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = BorderStrokeDark, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Customer Credit", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextWhite))
                        Text(
                            text = CurrencyFormatter.formatINR(grandRefundTotal),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = RubyUdhar)
                        )
                    }
                }

                // ─── 5. SETTLEMENT & NOTES CARD ───
                ChopdaCard {
                    Text(
                        text = "SETTLEMENT & REMARKS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextSubtle,
                            letterSpacing = 0.5.sp,
                            fontSize = 10.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("ledger" to "📓 Adjust in Party Khata", "cash" to "💵 Cash / UPI Refund").forEach { (mode, label) ->
                            val isSelected = (mode == "ledger") // Default to ledger adjustment
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) RubyUdhar.copy(alpha = 0.15f) else ElevatedSlate)
                                    .border(
                                        1.dp,
                                        if (isSelected) RubyUdhar else BorderStrokeDark,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) RubyUdhar else TextMuted
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "REMARKS / DEFECT NOTES",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextSubtle,
                            letterSpacing = 0.5.sp,
                            fontSize = 10.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = { Text("e.g. Returned due to damaged packaging or rate difference", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RubyUdhar,
                            unfocusedBorderColor = BorderStrokeDark,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedContainerColor = ElevatedSlate,
                            unfocusedContainerColor = ElevatedSlate
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                if (errorMessage != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(RubyUdhar.copy(alpha = 0.12f))
                            .border(1.dp, RubyUdhar.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = RubyUdhar, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = errorMessage!!, color = RubyUdhar, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
