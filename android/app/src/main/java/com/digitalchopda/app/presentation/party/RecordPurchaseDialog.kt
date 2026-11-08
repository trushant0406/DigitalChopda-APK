package com.digitalchopda.app.presentation.party

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.DisposableEffect
import android.view.ViewGroup
import com.digitalchopda.app.core.designsystem.BorderStrokeDark
import com.digitalchopda.app.core.designsystem.BrandPrimaryBlue
import com.digitalchopda.app.core.designsystem.ChopdaCard
import com.digitalchopda.app.core.designsystem.DarkNavy
import com.digitalchopda.app.core.designsystem.DeepObsidianCanvas
import com.digitalchopda.app.core.designsystem.ElevatedSlate
import com.digitalchopda.app.core.designsystem.EmeraldJama
import com.digitalchopda.app.core.designsystem.EmeraldJamaBg
import com.digitalchopda.app.core.designsystem.MidnightCardSurface
import com.digitalchopda.app.core.designsystem.RubyUdhar
import com.digitalchopda.app.core.designsystem.TextMuted
import com.digitalchopda.app.core.designsystem.TextSubtle
import com.digitalchopda.app.core.designsystem.TextWhite
import com.digitalchopda.app.core.designsystem.bouncyClick
import com.digitalchopda.app.core.designsystem.getAvatarColors
import com.digitalchopda.app.data.local.entity.CustomerEntity
import com.digitalchopda.app.data.repository.CustomerRepository
import com.digitalchopda.app.data.repository.PurchaseRepository
import com.digitalchopda.app.domain.model.PurchaseItem
import com.digitalchopda.app.util.CurrencyFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class PurchaseDraftItem(
    var name: String = "",
    var qtyText: String = "1",
    var priceText: String = "",
    var gstRate: Double = 18.0
) {
    val quantity: Double get() = qtyText.toDoubleOrNull() ?: 1.0
    val unitPrice: Double get() = priceText.toDoubleOrNull() ?: 0.0
    val subtotal: Double get() = quantity * unitPrice
    val taxAmount: Double get() = subtotal * (gstRate / 100.0)
    val total: Double get() = subtotal + taxAmount
}

/**
 * 📥 Record Inward Purchase Bill (Vendor Stock Purchase)
 * Full-screen sovereign entry model harmonized with CreateInvoiceScreen architecture.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPurchaseDialog(
    companyId: Long,
    supplierId: Long = 0L,
    supplierName: String = "",
    purchaseRepository: PurchaseRepository,
    customerRepository: CustomerRepository? = null,
    onDismiss: () -> Unit,
    onPurchaseRecorded: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val existingCustomers: List<CustomerEntity> by (customerRepository?.getCustomers(companyId) ?: kotlinx.coroutines.flow.flowOf(emptyList<CustomerEntity>())).collectAsState(initial = emptyList())
    var selectedSupplierId by remember { mutableStateOf(supplierId) }
    var enteredSupplierName by remember { mutableStateOf(supplierName) }
    var enteredSupplierPhone by remember { mutableStateOf("") }
    var showSupplierDropdown by remember { mutableStateOf(false) }
    var supplierSearchQuery by remember { mutableStateOf("") }

    var supplierInvoiceNo by remember { mutableStateOf("") }
    var paymentStatus by remember { mutableStateOf("unpaid") } // "unpaid" (Credit), "paid"
    var paymentMethod by remember { mutableStateOf("cash") } // "cash", "upi", "bank"
    var notesText by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    val items = remember {
        mutableStateListOf(
            PurchaseDraftItem(name = "", qtyText = "1", priceText = "", gstRate = 18.0)
        )
    }

    val totalSubtotal = items.sumOf { it.subtotal }
    val totalTax = items.sumOf { it.taxAmount }
    val grandTotal = totalSubtotal + totalTax

    fun submitPurchase() {
        val validItems = items.filter { it.name.isNotBlank() && it.unitPrice > 0.0 }
        if (validItems.isEmpty()) {
            Toast.makeText(context, "Please enter at least one product with name and price", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            isSaving = true
            val finalSupplierId = if (selectedSupplierId > 0L) {
                selectedSupplierId
            } else {
                val match = existingCustomers.firstOrNull { it.name.equals(enteredSupplierName.trim(), ignoreCase = true) }
                if (match != null) {
                    match.id
                } else {
                    val newParty = CustomerEntity(
                        companyId = companyId,
                        name = enteredSupplierName.trim().ifBlank { "General Vendor" },
                        phone = enteredSupplierPhone.trim(),
                        partyType = "supplier"
                    )
                    val created = customerRepository?.createParty(newParty)?.getOrNull()
                    created ?: 1L
                }
            }

            val domainItems = validItems.map { draft ->
                PurchaseItem(
                    productName = draft.name.trim(),
                    quantity = draft.quantity,
                    unitPrice = draft.unitPrice,
                    gstRate = draft.gstRate
                )
            }
            val res = purchaseRepository.recordPurchase(
                companyId = companyId,
                supplierId = finalSupplierId,
                supplierInvoiceNo = supplierInvoiceNo.trim().ifBlank { "PUR-${System.currentTimeMillis() % 10000}" },
                items = domainItems,
                paymentStatus = paymentStatus,
                paymentMethod = paymentMethod,
                notes = notesText
            )
            isSaving = false
            if (res.isSuccess) {
                Toast.makeText(context, "✓ Inward purchase recorded! Stock added.", Toast.LENGTH_SHORT).show()
                onPurchaseRecorded()
                onDismiss()
            } else {
                Toast.makeText(context, "Failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
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
                                    text = "Record Purchase Bill",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = TextWhite
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = EmeraldJama.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldJama.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = "INWARD STOCK",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldJama
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Auto-increments product inventory & creates supplier payable",
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
                            onClick = { submitPurchase() },
                            enabled = !isSaving
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save",
                                tint = EmeraldJama
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
                            onClick = { submitPurchase() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            enabled = !isSaving,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldJama)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                            } else {
                                Text(
                                    text = "Save Purchase Bill • ${CurrencyFormatter.formatINR(grandTotal)}",
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
                // ─── 1. DOCUMENT IDENTIFIER & BILL META CARD ───
                ChopdaCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (supplierInvoiceNo.isNotBlank()) "BILL: $supplierInvoiceNo" else "NEW INWARD PURCHASE",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = EmeraldJama,
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

                    Text(
                        text = "SUPPLIER INVOICE / BILL NO *",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextSubtle,
                            letterSpacing = 0.5.sp,
                            fontSize = 10.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = supplierInvoiceNo,
                        onValueChange = { supplierInvoiceNo = it },
                        placeholder = { Text("e.g. BILL-9821 / GST-042", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldJama,
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

                // ─── 2. VENDOR / SUPPLIER PARTY CARD ───
                ChopdaCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PURCHASE FROM VENDOR (SUPPLIER)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextSubtle,
                                letterSpacing = 0.5.sp,
                                fontSize = 10.5.sp
                            )
                        )
                        if (supplierId <= 0L) {
                            Text(
                                text = "⚡ Search / New",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = BrandPrimaryBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier.clickable { showSupplierDropdown = true }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val displayName = if (supplierId > 0L) supplierName else enteredSupplierName.ifBlank { "Select Supplier / Vendor..." }
                    val (avatarBg, avatarText) = getAvatarColors(displayName)

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(ElevatedSlate)
                                .border(1.dp, BorderStrokeDark, RoundedCornerShape(10.dp))
                                .clickable(enabled = supplierId <= 0L) { showSupplierDropdown = !showSupplierDropdown }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(avatarBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = displayName.take(2).uppercase().ifBlank { "S" },
                                    color = avatarText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (enteredSupplierName.isNotBlank() || supplierId > 0L) DarkNavy else TextMuted,
                                        fontSize = 14.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val sub = if (enteredSupplierPhone.isNotBlank()) enteredSupplierPhone else if (supplierId > 0L) "Supplier ID: #$supplierId" else "Tap to choose vendor from ledger"
                                Text(
                                    text = sub,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (supplierId <= 0L) {
                                Icon(
                                    imageVector = if (showSupplierDropdown) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Toggle",
                                    tint = BrandPrimaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Dropdown for vendor selection
                        val matchingSuppliers = remember(existingCustomers, supplierSearchQuery) {
                            if (supplierSearchQuery.isBlank()) existingCustomers.take(8)
                            else existingCustomers.filter {
                                it.name.contains(supplierSearchQuery, ignoreCase = true) ||
                                it.phone.contains(supplierSearchQuery) ||
                                it.gstin.contains(supplierSearchQuery, ignoreCase = true)
                            }.take(8)
                        }

                        DropdownMenu(
                            expanded = showSupplierDropdown && supplierId <= 0L,
                            onDismissRequest = { showSupplierDropdown = false },
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .background(MidnightCardSurface)
                                .border(1.dp, BorderStrokeDark, RoundedCornerShape(10.dp))
                        ) {
                            // Search Box inside dropdown
                            Box(modifier = Modifier.padding(8.dp)) {
                                OutlinedTextField(
                                    value = supplierSearchQuery,
                                    onValueChange = { supplierSearchQuery = it },
                                    placeholder = { Text("Search supplier name, phone...", fontSize = 12.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp)) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmeraldJama,
                                        unfocusedBorderColor = BorderStrokeDark,
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite,
                                        focusedContainerColor = ElevatedSlate,
                                        unfocusedContainerColor = ElevatedSlate
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }

                            if (matchingSuppliers.isEmpty() && supplierSearchQuery.isNotBlank()) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "+ Use \"$supplierSearchQuery\" as New Supplier",
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldJama,
                                            fontSize = 12.sp
                                        )
                                    },
                                    onClick = {
                                        enteredSupplierName = supplierSearchQuery.trim()
                                        selectedSupplierId = 0L
                                        showSupplierDropdown = false
                                    }
                                )
                            } else {
                                for (match in matchingSuppliers) {
                                    val (sBg, sText) = getAvatarColors(match.name)
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(sBg),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(match.name.take(1).uppercase(), color = sText, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(match.name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    if (match.phone.isNotBlank()) {
                                                        Text(match.phone, color = TextMuted, fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                        },
                                        onClick = {
                                            enteredSupplierName = match.name
                                            enteredSupplierPhone = match.phone
                                            selectedSupplierId = match.id
                                            showSupplierDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // ─── 3. PURCHASED GOODS / ITEMS CARD ───
                ChopdaCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "PURCHASED GOODS / ITEMS",
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
                                    text = "${items.size} ${if (items.size == 1) "item" else "items"}",
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
                                .background(EmeraldJama.copy(alpha = 0.12f))
                                .border(1.dp, EmeraldJama.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .bouncyClick { items.add(PurchaseDraftItem()) }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = EmeraldJama, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+ Add Item",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldJama,
                                        fontSize = 11.5.sp
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    items.forEachIndexed { index, draft ->
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
                                            color = EmeraldJama
                                        )
                                    )
                                    if (items.size > 1) {
                                        IconButton(
                                            onClick = { items.removeAt(index) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Delete",
                                                tint = RubyUdhar,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = draft.name,
                                    onValueChange = { items[index] = draft.copy(name = it) },
                                    placeholder = { Text("Product name (e.g. Copper Wire)", color = TextMuted) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmeraldJama,
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
                                        value = draft.qtyText,
                                        onValueChange = {
                                            items[index] = draft.copy(qtyText = it.filter { ch -> ch.isDigit() || ch == '.' })
                                        },
                                        label = { Text("Qty", fontSize = 10.sp) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = EmeraldJama,
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
                                        value = draft.priceText,
                                        onValueChange = {
                                            items[index] = draft.copy(priceText = it.filter { ch -> ch.isDigit() || ch == '.' })
                                        },
                                        label = { Text("Rate (₹)", fontSize = 10.sp) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = EmeraldJama,
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
                                                val isGstSelected = draft.gstRate == rate
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(if (isGstSelected) EmeraldJama else MidnightCardSurface)
                                                        .border(1.dp, if (isGstSelected) EmeraldJama else BorderStrokeDark, RoundedCornerShape(6.dp))
                                                        .clickable { items[index] = draft.copy(gstRate = rate) }
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
                                    Text("Line Total (Inc. Tax)", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp))
                                    Text(
                                        text = CurrencyFormatter.formatINR(draft.total),
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = TextWhite)
                                    )
                                }
                            }
                        }
                    }
                }

                // ─── 4. FINANCIAL CALCULATION & TAX CARD ───
                ChopdaCard {
                    Text(
                        text = "PURCHASE FINANCIALS & TAX",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextSubtle,
                            letterSpacing = 0.5.sp,
                            fontSize = 10.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Taxable Subtotal", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                        Text(CurrencyFormatter.formatINR(totalSubtotal), style = MaterialTheme.typography.bodySmall.copy(color = TextWhite, fontWeight = FontWeight.SemiBold))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Input Tax Credit (GST)", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
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
                        Text("Grand Total Payable", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextWhite))
                        Text(
                            text = CurrencyFormatter.formatINR(grandTotal),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = EmeraldJama)
                        )
                    }
                }

                // ─── 5. SETTLEMENT & NOTES CARD ───
                ChopdaCard {
                    Text(
                        text = "PAYMENT SETTLEMENT",
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
                        listOf("unpaid" to "⏳ Credit / Pay Later", "paid" to "✔ Paid Immediately").forEach { (status, label) ->
                            val isSelected = paymentStatus == status
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) (if (status == "paid") EmeraldJama else RubyUdhar).copy(alpha = 0.15f) else ElevatedSlate)
                                    .border(
                                        1.dp,
                                        if (isSelected) (if (status == "paid") EmeraldJama else RubyUdhar) else BorderStrokeDark,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { paymentStatus = status }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) (if (status == "paid") EmeraldJama else RubyUdhar) else TextMuted
                                    )
                                )
                            }
                        }
                    }

                    if (paymentStatus == "paid") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "PAYMENT METHOD",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSubtle, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("cash" to "Cash", "upi" to "UPI", "bank" to "Bank Transfer").forEach { (method, label) ->
                                val isSelected = paymentMethod == method
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) BrandPrimaryBlue.copy(alpha = 0.15f) else ElevatedSlate)
                                        .border(1.dp, if (isSelected) BrandPrimaryBlue else BorderStrokeDark, RoundedCornerShape(8.dp))
                                        .clickable { paymentMethod = method }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) BrandPrimaryBlue else TextMuted
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "REMARKS / NOTES (OPTIONAL)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextSubtle,
                            letterSpacing = 0.5.sp,
                            fontSize = 10.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        placeholder = { Text("e.g. Received via Truck #GJ-03-AK-2910", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldJama,
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

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

