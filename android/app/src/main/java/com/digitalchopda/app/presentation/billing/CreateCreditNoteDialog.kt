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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import com.digitalchopda.app.core.designsystem.BrandPrimaryBlue
import com.digitalchopda.app.core.designsystem.ImperialGoldLight
import com.digitalchopda.app.core.designsystem.getAvatarColors
import com.digitalchopda.app.core.document.DocumentItemParser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MidnightCardSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(RubyUdharBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = RubyUdhar,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "New Sales Return (Credit Note)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                        )
                        Text(
                            text = "Auto-adjusts party khata & generates CN document",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CN Number & Return Reason
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
                        unfocusedTextColor = TextWhite
                    )
                )

                Box(modifier = Modifier.weight(1.3f)) {
                    OutlinedTextField(
                        value = selectedReason,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Reason") },
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
                            unfocusedTextColor = TextWhite
                        )
                    )
                    DropdownMenu(
                        expanded = reasonDropdownExpanded,
                        onDismissRequest = { reasonDropdownExpanded = false }
                    ) {
                        RETURN_REASONS.forEach { reason ->
                            DropdownMenuItem(
                                text = { Text(reason) },
                                onClick = {
                                    selectedReason = reason
                                    reasonDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Party Selector
            Text(
                text = "SELECT PARTY / CUSTOMER *",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextSubtle,
                    letterSpacing = 0.5.sp
                )
            )
            Spacer(modifier = Modifier.height(6.dp))

            var partySearch by remember { mutableStateOf("") }
            var partyDropdownOpen by remember { mutableStateOf(false) }

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedCustomer?.name ?: partySearch,
                    onValueChange = {
                        partySearch = it
                        selectedCustomer = null
                        partyDropdownOpen = true
                    },
                    placeholder = { Text("Search customer by name or phone...", color = TextMuted) },
                    leadingIcon = {
                        if (selectedCustomer != null) {
                            val (cAvatarBg, cAvatarText) = getAvatarColors(selectedCustomer!!.name)
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(cAvatarBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = selectedCustomer!!.name.take(1).uppercase(),
                                    color = cAvatarText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Icon(Icons.Default.Person, contentDescription = null, tint = RubyUdhar)
                        }
                    },
                    trailingIcon = {
                        IconButton(onClick = { partyDropdownOpen = !partyDropdownOpen }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = BrandPrimaryBlue)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RubyUdhar,
                        unfocusedBorderColor = BorderStrokeDark,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                DropdownMenu(
                    expanded = partyDropdownOpen,
                    onDismissRequest = { partyDropdownOpen = false },
                    modifier = Modifier
                        .background(MidnightCardSurface)
                        .border(1.dp, BorderStrokeDark, RoundedCornerShape(10.dp))
                        .fillMaxWidth(0.9f)
                        .heightIn(max = 350.dp)
                ) {
                    val filtered = customers.filter {
                        partySearch.isBlank() ||
                        it.name.contains(partySearch, ignoreCase = true) ||
                        it.phone.contains(partySearch) ||
                        it.gstin.contains(partySearch, ignoreCase = true)
                    }
                    if (filtered.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No matching parties found", fontSize = 12.sp, color = TextMuted) },
                            onClick = {}
                        )
                    } else {
                        for (cust in filtered) {
                            val isSelected = selectedCustomer?.id == cust.id
                            val (cAvatarBg, cAvatarText) = getAvatarColors(cust.name)

                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(cAvatarBg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = cust.name.take(2).uppercase().ifBlank { "P" },
                                                color = cAvatarText,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = cust.name,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                        color = if (isSelected) ImperialGoldLight else TextWhite,
                                                        fontSize = 13.sp
                                                    )
                                                )
                                                if (cust.partyTag.isNotBlank() && cust.partyTag != "Active Party") {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = cust.partyTag,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 9.sp,
                                                            color = TextMuted,
                                                            fontWeight = FontWeight.Medium
                                                        ),
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(ElevatedSlate)
                                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            val line2 = buildString {
                                                if (cust.phone.isNotBlank()) append(cust.phone)
                                                if (cust.gstin.isNotBlank()) {
                                                    if (isNotEmpty()) append(" • ")
                                                    append("GST: ${cust.gstin}")
                                                }
                                            }
                                            if (line2.isNotBlank()) {
                                                Text(
                                                    text = line2,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = TextMuted,
                                                        fontSize = 10.sp
                                                    )
                                                )
                                            }
                                        }

                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = ImperialGoldLight,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedCustomer = cust
                                    partySearch = cust.name
                                    partyDropdownOpen = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

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
                    placeholder = { Text(if (availableInvoices.isNotEmpty()) "Tap to select invoice..." else "No bills found") },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (selectedInvoice != null) {
                                IconButton(onClick = { selectedInvoice = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                                }
                            }
                            IconButton(onClick = { invoiceDropdownOpen = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Open dropdown", tint = TextWhite)
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
                        unfocusedTextColor = TextWhite
                    )
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

            Spacer(modifier = Modifier.height(16.dp))

            // Returned Items List Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RETURNED ITEMS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextSubtle,
                        letterSpacing = 0.5.sp
                    )
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ElevatedSlate)
                        .clickable {
                            returnItems.add(
                                ReturnItemDraft(name = "", quantity = 1, unitPrice = 0.0, gstRatePercent = 5.0)
                            )
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = RubyUdhar, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add Item",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = RubyUdhar
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            returnItems.forEachIndexed { index, item ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = ElevatedSlate,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStrokeDark)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Item #${index + 1}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                            )
                            if (returnItems.size > 1) {
                                IconButton(
                                    onClick = { returnItems.removeAt(index) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RubyUdhar, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        OutlinedTextField(
                            value = item.name,
                            onValueChange = { item.name = it },
                            placeholder = { Text("Product name / description") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RubyUdhar,
                                unfocusedBorderColor = BorderStrokeDark,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = if (item.quantity == 0) "" else item.quantity.toString(),
                                onValueChange = {
                                    item.quantity = it.toIntOrNull() ?: 0
                                },
                                label = { Text("Qty") },
                                modifier = Modifier.weight(0.8f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RubyUdhar,
                                    unfocusedBorderColor = BorderStrokeDark,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                )
                            )

                            OutlinedTextField(
                                value = if (item.unitPrice == 0.0) "" else item.unitPrice.toString(),
                                onValueChange = {
                                    item.unitPrice = it.toDoubleOrNull() ?: 0.0
                                },
                                label = { Text("Rate (₹)") },
                                modifier = Modifier.weight(1.2f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RubyUdhar,
                                    unfocusedBorderColor = BorderStrokeDark,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                )
                            )

                            OutlinedTextField(
                                value = "${item.gstRatePercent.toInt()}%",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("GST") },
                                modifier = Modifier.weight(0.8f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RubyUdhar,
                                    unfocusedBorderColor = BorderStrokeDark,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Item Credit Total: ${CurrencyFormatter.formatINR(item.total)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = RubyUdhar,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Notes / Remarks
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Remarks / Defect Notes") },
                placeholder = { Text("e.g. Returned due to damaged stitching on border") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RubyUdhar,
                    unfocusedBorderColor = BorderStrokeDark,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Financial Summary Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = ElevatedSlate,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderStrokeDark)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Taxable Return Value", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                        Text(CurrencyFormatter.formatINR(totalSubtotal), style = MaterialTheme.typography.bodySmall.copy(color = TextWhite))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Reversed GST Tax", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                        Text(CurrencyFormatter.formatINR(totalTax), style = MaterialTheme.typography.bodySmall.copy(color = TextWhite))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.HorizontalDivider(color = BorderStrokeDark)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOTAL PARTY CREDIT",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = RubyUdhar
                            )
                        )
                        Text(
                            text = CurrencyFormatter.formatINR(grandRefundTotal),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = RubyUdhar
                            )
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(RubyUdharBg)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = RubyUdhar, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = errorMessage!!, color = RubyUdhar, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Button: Issue Credit Note
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSaving) RubyUdhar.copy(alpha = 0.6f) else RubyUdhar)
                    .bouncyClick {
                        if (isSaving) return@bouncyClick
                        if (selectedCustomer == null) {
                            errorMessage = "Please select a party/customer for this credit note"
                            return@bouncyClick
                        }
                        val validItems = returnItems.filter { it.name.isNotBlank() && it.quantity > 0 }
                        if (validItems.isEmpty()) {
                            errorMessage = "Please enter at least one item with name and quantity > 0"
                            return@bouncyClick
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
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Text(
                        text = "Issue Credit Note (${CurrencyFormatter.formatINR(grandRefundTotal)})",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
