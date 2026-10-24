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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.digitalchopda.app.core.designsystem.BorderStrokeDark
import com.digitalchopda.app.core.designsystem.BrandPrimaryBlue
import com.digitalchopda.app.core.designsystem.DeepObsidianCanvas
import com.digitalchopda.app.core.designsystem.ElevatedSlate
import com.digitalchopda.app.core.designsystem.EmeraldJama
import com.digitalchopda.app.core.designsystem.MidnightCardSurface
import com.digitalchopda.app.core.designsystem.RubyUdhar
import com.digitalchopda.app.core.designsystem.TextMuted
import com.digitalchopda.app.core.designsystem.TextSubtle
import com.digitalchopda.app.core.designsystem.TextWhite
import com.digitalchopda.app.data.repository.PurchaseRepository
import com.digitalchopda.app.domain.model.PurchaseItem
import com.digitalchopda.app.util.CurrencyFormatter
import kotlinx.coroutines.launch

import com.digitalchopda.app.data.local.entity.CustomerEntity
import com.digitalchopda.app.data.repository.CustomerRepository
import androidx.compose.runtime.collectAsState

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
 * 📥 Record Inward Purchase Dialog (Cargo / Stock Intake)
 * Parity with Web Platform's RecordPurchaseModal.jsx
 */
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

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .imePadding()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, BorderStrokeDark, RoundedCornerShape(20.dp)),
            color = MidnightCardSurface,
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Record Inward Purchase Bill",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = TextWhite
                            )
                        )
                        Text(
                            text = if (supplierId > 0L) "From Supplier: $supplierName" else if (enteredSupplierName.isNotBlank()) "From: $enteredSupplierName" else "Record Stock Intake Bill",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                        )
                    }

                    IconButton(
                        onClick = { if (!isSaving) onDismiss() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Supplier Selector when supplierId is not pre-fixed
                if (supplierId <= 0L) {
                    Text(
                        text = "SUPPLIER / VENDOR NAME",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextSubtle,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = enteredSupplierName,
                            onValueChange = {
                                enteredSupplierName = it
                                showSupplierDropdown = true
                            },
                            placeholder = { Text("Search or type vendor name...", color = TextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandPrimaryBlue,
                                unfocusedBorderColor = BorderStrokeDark,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedContainerColor = DeepObsidianCanvas,
                                unfocusedContainerColor = DeepObsidianCanvas
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        val matchingSuppliers = remember(existingCustomers, enteredSupplierName) {
                            if (enteredSupplierName.isBlank()) emptyList<CustomerEntity>()
                            else existingCustomers.filter { it.name.contains(enteredSupplierName, ignoreCase = true) }.take(5)
                        }

                        androidx.compose.material3.DropdownMenu(
                            expanded = showSupplierDropdown && matchingSuppliers.isNotEmpty(),
                            onDismissRequest = { showSupplierDropdown = false },
                            modifier = Modifier.background(MidnightCardSurface)
                        ) {
                            for (match in matchingSuppliers) {
                                androidx.compose.material3.DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(match.name, color = TextWhite, fontWeight = FontWeight.Bold)
                                            if (match.phone.isNotBlank()) {
                                                Text(match.phone, color = TextMuted, fontSize = 11.sp)
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
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Supplier Bill / Invoice Number
                Text(
                    text = "SUPPLIER INVOICE / BILL NO",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextSubtle,
                        letterSpacing = 0.5.sp
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = supplierInvoiceNo,
                    onValueChange = { supplierInvoiceNo = it },
                    placeholder = { Text("e.g. BILL-9402", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandPrimaryBlue,
                        unfocusedBorderColor = BorderStrokeDark,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedContainerColor = DeepObsidianCanvas,
                        unfocusedContainerColor = DeepObsidianCanvas
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Items Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PURCHASED ITEMS (${items.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextSubtle,
                            letterSpacing = 0.5.sp
                        )
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(BrandPrimaryBlue.copy(alpha = 0.15f))
                            .clickable {
                                items.add(PurchaseDraftItem())
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = BrandPrimaryBlue, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Add Item", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = BrandPrimaryBlue))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                items.forEachIndexed { index, draft ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        color = DeepObsidianCanvas,
                        shape = RoundedCornerShape(12.dp),
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
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = BrandPrimaryBlue)
                                )
                                if (items.size > 1) {
                                    IconButton(
                                        onClick = { items.removeAt(index) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete", tint = RubyUdhar, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = draft.name,
                                onValueChange = {
                                    items[index] = draft.copy(name = it)
                                },
                                placeholder = { Text("Product name (e.g. Copper Wire)", color = TextMuted) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BrandPrimaryBlue,
                                    unfocusedBorderColor = BorderStrokeDark,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedContainerColor = ElevatedSlate,
                                    unfocusedContainerColor = ElevatedSlate
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Quantity
                                OutlinedTextField(
                                    value = draft.qtyText,
                                    onValueChange = {
                                        items[index] = draft.copy(qtyText = it.filter { ch -> ch.isDigit() || ch == '.' })
                                    },
                                    label = { Text("Qty", fontSize = 10.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BrandPrimaryBlue,
                                        unfocusedBorderColor = BorderStrokeDark,
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite,
                                        focusedContainerColor = ElevatedSlate,
                                        unfocusedContainerColor = ElevatedSlate
                                    ),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                // Unit Price
                                OutlinedTextField(
                                    value = draft.priceText,
                                    onValueChange = {
                                        items[index] = draft.copy(priceText = it.filter { ch -> ch.isDigit() || ch == '.' })
                                    },
                                    label = { Text("Rate (₹)", fontSize = 10.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BrandPrimaryBlue,
                                        unfocusedBorderColor = BorderStrokeDark,
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite,
                                        focusedContainerColor = ElevatedSlate,
                                        unfocusedContainerColor = ElevatedSlate
                                    ),
                                    modifier = Modifier.weight(1.2f),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                // GST % Chips (5%, 12%, 18%, 28%)
                                Column(modifier = Modifier.weight(1.3f)) {
                                    Text("GST %", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf(5.0, 12.0, 18.0, 28.0).forEach { rate ->
                                            val isGstSelected = draft.gstRate == rate
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (isGstSelected) BrandPrimaryBlue else ElevatedSlate)
                                                    .clickable { items[index] = draft.copy(gstRate = rate) }
                                                    .padding(horizontal = 6.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = "${rate.toInt()}%",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 10.sp,
                                                        fontWeight = if (isGstSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isGstSelected) TextWhite else TextMuted
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Totals Summary Box
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DeepObsidianCanvas,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStrokeDark)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                            Text(CurrencyFormatter.formatINR(totalSubtotal), style = MaterialTheme.typography.bodySmall.copy(color = TextWhite))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Input Tax (GST)", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted))
                            Text(CurrencyFormatter.formatINR(totalTax), style = MaterialTheme.typography.bodySmall.copy(color = TextWhite))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Grand Total", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextWhite))
                            Text(
                                CurrencyFormatter.formatINR(grandTotal),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = EmeraldJama)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Payment Status & Method Selector
                Text(
                    text = "PAYMENT SETTLEMENT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextSubtle,
                        letterSpacing = 0.5.sp
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
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
                                .background(if (isSelected) (if (status == "paid") EmeraldJama else RubyUdhar).copy(alpha = 0.2f) else ElevatedSlate)
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

                Spacer(modifier = Modifier.height(14.dp))

                // Notes
                Text(
                    text = "REMARKS / NOTES (OPTIONAL)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextSubtle,
                        letterSpacing = 0.5.sp
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    placeholder = { Text("e.g. Received via Truck #GJ-03-AK-2910", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandPrimaryBlue,
                        unfocusedBorderColor = BorderStrokeDark,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedContainerColor = DeepObsidianCanvas,
                        unfocusedContainerColor = DeepObsidianCanvas
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(22.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            val validItems = items.filter { it.name.isNotBlank() && it.unitPrice > 0.0 }
                            if (validItems.isEmpty()) {
                                Toast.makeText(context, "Please enter at least one product with name and price", Toast.LENGTH_SHORT).show()
                                return@Button
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
                                    Toast.makeText(context, "✓ Inward purchase recorded! Stock incremented.", Toast.LENGTH_SHORT).show()
                                    onPurchaseRecorded()
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, "Failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1.4f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldJama)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = TextWhite, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        } else {
                            Text("Save Purchase Bill", fontWeight = FontWeight.Bold, color = TextWhite)
                        }
                    }
                }
            }
        }
    }
}
