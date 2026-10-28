package com.digitalchopda.app.presentation.party

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Receipt
import com.digitalchopda.app.core.designsystem.ChopdaModalDialog
import com.digitalchopda.app.core.sound.SoundEffectsManager
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.digitalchopda.app.core.network.ApiPaymentAllocationItemDto
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalchopda.app.core.designsystem.BorderStrokeDark
import com.digitalchopda.app.core.designsystem.BrandPrimaryBlue
import com.digitalchopda.app.core.designsystem.DarkNavy
import com.digitalchopda.app.core.designsystem.ElevatedSlate
import com.digitalchopda.app.core.designsystem.EmeraldJama
import com.digitalchopda.app.core.designsystem.MidnightCardSurface
import com.digitalchopda.app.core.designsystem.RubyUdhar
import com.digitalchopda.app.core.designsystem.TextMuted
import com.digitalchopda.app.core.designsystem.TextSubtle
import com.digitalchopda.app.data.local.entity.InvoiceEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentDialog(
    isJama: Boolean,
    partyName: String,
    unpaidInvoices: List<InvoiceEntity> = emptyList(),
    initialInvoiceId: Long? = null,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, paymentMode: String, notes: String, selectedInvoice: InvoiceEntity?) -> Unit,
    onConfirmWithAllocations: ((amount: Double, paymentMode: String, notes: String, selectedInvoice: InvoiceEntity?, allocations: List<ApiPaymentAllocationItemDto>?) -> Unit)? = null
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var selectedInvoice by remember {
        mutableStateOf<InvoiceEntity?>(
            unpaidInvoices.firstOrNull { it.id == initialInvoiceId }
        )
    }
    var isMultiBillMode by remember { mutableStateOf(false) }
    val allocatedMap = remember { mutableStateMapOf<Long, Double>() }
    val selectedInvoiceIds = remember { mutableStateMapOf<Long, Boolean>() }

    var amountText by remember {
        mutableStateOf(
            if (selectedInvoice != null) {
                val due = selectedInvoice!!.balanceDue
                if (due % 1.0 == 0.0) due.toInt().toString() else String.format(java.util.Locale.US, "%.2f", due)
            } else ""
        )
    }
    var selectedMode by remember { mutableStateOf("Cash") }
    var notesText by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf<String?>(null) }
    var invoiceDropdownExpanded by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val primaryColor = if (isJama) EmeraldJama else RubyUdhar
    val titleText = if (isJama) "Payment In (Receive Payment)" else "Payment Out (Give Credit)"
    val subText = if (isJama) "You Received Money from $partyName" else "You Gave Money / Credit to $partyName"

    val modes = listOf("Cash", "UPI", "Cheque", "Bank")

    ChopdaModalDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = titleText,
        subtitle = subText,
        icon = if (isJama) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
        iconTint = primaryColor,
        widthFraction = 1f,
        maxHeightFraction = 0.90f,
        scrollable = true,
        isBottomSheet = true,
        buttons = {
            Button(
                onClick = {
                    if (isSaving) return@Button
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0.0) {
                        amountError = "Please enter a valid amount greater than 0"
                        return@Button
                    }
                    isSaving = true
                    try {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    } catch (_: Exception) {}
                    SoundEffectsManager.announcePaymentReceived(
                        amount = amount,
                        customerName = partyName,
                        isHindi = false
                    )
                    if (isJama && isMultiBillMode && unpaidInvoices.size > 1) {
                        val allocationsList = unpaidInvoices.mapNotNull { inv ->
                            if (selectedInvoiceIds[inv.id] == true) {
                                val amt = allocatedMap[inv.id] ?: 0.0
                                if (amt > 0.0) {
                                    ApiPaymentAllocationItemDto(
                                        orderId = inv.serverOrderId ?: inv.id,
                                        amount = amt
                                    )
                                } else null
                            } else null
                        }
                        if (onConfirmWithAllocations != null) {
                            onConfirmWithAllocations(amount, selectedMode, notesText.trim(), null, allocationsList)
                        } else {
                            onConfirm(amount, selectedMode, notesText.trim(), null)
                        }
                    } else {
                        if (onConfirmWithAllocations != null) {
                            onConfirmWithAllocations(amount, selectedMode, notesText.trim(), selectedInvoice, null)
                        } else {
                            onConfirm(amount, selectedMode, notesText.trim(), selectedInvoice)
                        }
                    }
                },
                enabled = !isSaving,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = if (isJama) {
                            if (isMultiBillMode && unpaidInvoices.size > 1) {
                                val count = selectedInvoiceIds.count { it.value }
                                if (count > 0) "Confirm + Receive (Split $count Bills)" else "Confirm + Receive"
                            } else if (selectedInvoice != null) {
                                "Confirm + Receive for ${selectedInvoice!!.invoiceNumber}"
                            } else {
                                "Confirm + Receive"
                            }
                        } else "Confirm - Give",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Settle Against Bill Selection (for Payment In)
            if (isJama && unpaidInvoices.isNotEmpty()) {
                if (unpaidInvoices.size > 1) {
                    // Mode Selector: Single Bill vs Multi-Bill Split
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElevatedSlate)
                            .border(1.dp, BorderStrokeDark, RoundedCornerShape(10.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!isMultiBillMode) BrandPrimaryBlue else Color.Transparent)
                                .clickable { isMultiBillMode = false }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Single Bill / Advance",
                                fontWeight = if (!isMultiBillMode) FontWeight.Bold else FontWeight.Medium,
                                color = if (!isMultiBillMode) Color.White else DarkNavy,
                                fontSize = 12.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isMultiBillMode) BrandPrimaryBlue else Color.Transparent)
                                .clickable {
                                    isMultiBillMode = true
                                    val amt = amountText.toDoubleOrNull() ?: 0.0
                                    if (amt > 0.0 && allocatedMap.isEmpty()) {
                                        var remaining = amt
                                        for (inv in unpaidInvoices) {
                                            if (remaining <= 0.0) break
                                            val take = Math.min(remaining, inv.balanceDue)
                                            if (take > 0.0) {
                                                allocatedMap[inv.id] = take
                                                selectedInvoiceIds[inv.id] = true
                                                remaining -= take
                                            }
                                        }
                                    }
                                }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Split Multi-Bills (${unpaidInvoices.size})",
                                fontWeight = if (isMultiBillMode) FontWeight.Bold else FontWeight.Medium,
                                color = if (isMultiBillMode) Color.White else DarkNavy,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                if (isMultiBillMode && unpaidInvoices.size > 1) {
                    // Multi-Bill Itemized Allocation
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ALLOCATE AGAINST BILLS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextSubtle,
                                    letterSpacing = 0.8.sp,
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = "⚡ Auto-Distribute Total",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BrandPrimaryBlue,
                                    fontSize = 11.5.sp
                                ),
                                modifier = Modifier
                                    .clickable {
                                        val amt = amountText.toDoubleOrNull() ?: 0.0
                                        var remaining = amt
                                        allocatedMap.clear()
                                        selectedInvoiceIds.clear()
                                        for (inv in unpaidInvoices) {
                                            if (remaining <= 0.0) break
                                            val take = Math.min(remaining, inv.balanceDue)
                                            if (take > 0.0) {
                                                allocatedMap[inv.id] = take
                                                selectedInvoiceIds[inv.id] = true
                                                remaining -= take
                                            }
                                        }
                                    }
                                    .padding(vertical = 2.dp)
                            )
                        }

                        unpaidInvoices.forEach { inv ->
                            val isChecked = selectedInvoiceIds[inv.id] == true
                            val allocVal = allocatedMap[inv.id] ?: 0.0

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ElevatedSlate)
                                    .border(
                                        1.dp,
                                        if (isChecked) BrandPrimaryBlue else BorderStrokeDark,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { checked ->
                                                selectedInvoiceIds[inv.id] = checked
                                                if (checked && (allocatedMap[inv.id] ?: 0.0) <= 0.0) {
                                                    val currentSum = allocatedMap.entries
                                                        .filter { selectedInvoiceIds[it.key] == true && it.key != inv.id }
                                                        .sumOf { it.value }
                                                    val total = amountText.toDoubleOrNull() ?: 0.0
                                                    val avail = Math.max(0.0, total - currentSum)
                                                    allocatedMap[inv.id] = if (avail > 0.0) Math.min(avail, inv.balanceDue) else inv.balanceDue
                                                }
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = BrandPrimaryBlue,
                                                checkmarkColor = Color.White
                                            )
                                        )
                                        Column {
                                            Text(
                                                text = inv.invoiceNumber,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = DarkNavy,
                                                    fontSize = 13.sp
                                                )
                                            )
                                            Text(
                                                text = "Due ₹ ${String.format(java.util.Locale.US, "%.2f", inv.balanceDue)} (Total ₹ ${inv.grandTotal.toInt()})",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = RubyUdhar,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Full Due",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = BrandPrimaryBlue,
                                            fontSize = 11.5.sp
                                        ),
                                        modifier = Modifier
                                            .clickable {
                                                selectedInvoiceIds[inv.id] = true
                                                allocatedMap[inv.id] = inv.balanceDue
                                                val newTotal = allocatedMap.entries
                                                    .filter { selectedInvoiceIds[it.key] == true }
                                                    .sumOf { it.value }
                                                if (newTotal > (amountText.toDoubleOrNull() ?: 0.0)) {
                                                    amountText = if (newTotal % 1.0 == 0.0) newTotal.toInt().toString() else String.format(java.util.Locale.US, "%.2f", newTotal)
                                                }
                                            }
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    )
                                }

                                if (isChecked) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = if (allocVal > 0.0) {
                                            if (allocVal % 1.0 == 0.0) allocVal.toInt().toString() else String.format(java.util.Locale.US, "%.2f", allocVal)
                                        } else "",
                                        onValueChange = { input ->
                                            val filtered = input.filter { char -> char.isDigit() || char == '.' }
                                            val parsed = filtered.toDoubleOrNull() ?: 0.0
                                            allocatedMap[inv.id] = parsed
                                        },
                                        label = { Text("Settlement for ${inv.invoiceNumber}", fontSize = 11.sp) },
                                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = BrandPrimaryBlue, fontSize = 12.sp) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = BrandPrimaryBlue,
                                            unfocusedBorderColor = BorderStrokeDark
                                        )
                                    )
                                }
                            }
                        }

                        // Summary Chip for Multi-Bill Allocation
                        val totalAllocated = allocatedMap.entries
                            .filter { selectedInvoiceIds[it.key] == true }
                            .sumOf { it.value }
                        val currentEnteredAmount = amountText.toDoubleOrNull() ?: 0.0
                        val unallocated = Math.max(0.0, currentEnteredAmount - totalAllocated)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MidnightCardSurface)
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Allocated: ₹ ${String.format(java.util.Locale.US, "%.2f", totalAllocated)}",
                                fontWeight = FontWeight.Bold,
                                color = BrandPrimaryBlue,
                                fontSize = 12.sp
                            )
                            if (unallocated > 0.01) {
                                Text(
                                    text = "Advance: ₹ ${String.format(java.util.Locale.US, "%.2f", unallocated)}",
                                    fontWeight = FontWeight.SemiBold,
                                    color = EmeraldJama,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                } else {
                    // Single Bill / Advance Dropdown
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SETTLE AGAINST BILL (OPTIONAL)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextSubtle,
                                    letterSpacing = 0.8.sp,
                                    fontSize = 11.sp
                                )
                            )
                            if (selectedInvoice != null) {
                                Text(
                                    text = "Pay Full Due (₹${selectedInvoice!!.balanceDue.toInt()})",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = BrandPrimaryBlue,
                                        fontSize = 11.5.sp
                                    ),
                                    modifier = Modifier
                                        .clickable {
                                            val due = selectedInvoice!!.balanceDue
                                            amountText = if (due % 1.0 == 0.0) due.toInt().toString() else String.format(java.util.Locale.US, "%.2f", due)
                                            amountError = null
                                        }
                                        .padding(vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ElevatedSlate)
                                    .border(
                                        1.dp,
                                        if (selectedInvoice != null) BrandPrimaryBlue else BorderStrokeDark,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { invoiceDropdownExpanded = true }
                                    .padding(horizontal = 14.dp, vertical = 11.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Receipt,
                                        contentDescription = null,
                                        tint = if (selectedInvoice != null) BrandPrimaryBlue else TextSubtle,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        if (selectedInvoice != null) {
                                            Text(
                                                text = "${selectedInvoice!!.invoiceNumber}  •  Due: ₹ ${String.format(java.util.Locale.US, "%.2f", selectedInvoice!!.balanceDue)}",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = DarkNavy,
                                                    fontSize = 13.5.sp
                                                )
                                            )
                                            Text(
                                                text = "Total: ₹ ${selectedInvoice!!.grandTotal.toInt()} (${selectedInvoice!!.itemsSummary.ifBlank { "Invoice" }})",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = TextSubtle,
                                                    fontSize = 11.5.sp
                                                ),
                                                maxLines = 1
                                            )
                                        } else {
                                            Text(
                                                text = "General Advance / On-Account",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = DarkNavy,
                                                    fontSize = 13.5.sp
                                                )
                                            )
                                            Text(
                                                text = "General credit to party ledger without targeting a specific bill",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = TextSubtle,
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select invoice",
                                    tint = DarkNavy
                                )
                            }

                            DropdownMenu(
                                expanded = invoiceDropdownExpanded,
                                onDismissRequest = { invoiceDropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.88f)
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = "General Advance / On-Account",
                                                fontWeight = if (selectedInvoice == null) FontWeight.Bold else FontWeight.Medium,
                                                color = DarkNavy
                                            )
                                            Text(
                                                text = "General credit without targeting a specific bill",
                                                fontSize = 11.sp,
                                                color = TextSubtle
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedInvoice = null
                                        invoiceDropdownExpanded = false
                                    }
                                )

                                HorizontalDivider(color = BorderStrokeDark.copy(alpha = 0.5f))

                                unpaidInvoices.forEach { inv ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = inv.invoiceNumber,
                                                        fontWeight = if (selectedInvoice?.id == inv.id) FontWeight.Bold else FontWeight.SemiBold,
                                                        color = DarkNavy
                                                    )
                                                    Text(
                                                        text = "Total ₹ ${inv.grandTotal.toInt()} • ${inv.itemsSummary.take(24)}",
                                                        fontSize = 11.sp,
                                                        color = TextSubtle,
                                                        maxLines = 1
                                                    )
                                                }
                                                Text(
                                                    text = "Due ₹ ${inv.balanceDue.toInt()}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.5.sp,
                                                    color = RubyUdhar
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedInvoice = inv
                                            val due = inv.balanceDue
                                            amountText = if (due % 1.0 == 0.0) due.toInt().toString() else String.format(java.util.Locale.US, "%.2f", due)
                                            amountError = null
                                            invoiceDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Large Amount Input
            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it.filter { char -> char.isDigit() || char == '.' }
                    amountError = null
                },
                label = { Text("Amount (₹) *") },
                leadingIcon = {
                    Text(
                        text = "₹",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        ),
                        modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                    )
                },
                placeholder = { Text("0.00") },
                isError = amountError != null,
                supportingText = {
                    if (amountError != null) {
                        Text(amountError!!, color = RubyUdhar)
                    } else if (selectedInvoice != null) {
                        Text("Invoice Due: ₹${String.format(java.util.Locale.US, "%.2f", selectedInvoice!!.balanceDue)}", color = TextSubtle)
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = BorderStrokeDark,
                    focusedTextColor = DarkNavy,
                    unfocusedTextColor = DarkNavy
                )
            )

            // Payment Mode Segmented Selector
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "PAYMENT MODE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextSubtle,
                        letterSpacing = 0.8.sp,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ElevatedSlate)
                        .border(1.dp, BorderStrokeDark, RoundedCornerShape(10.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    modes.forEach { mode ->
                        val isSelected = selectedMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) primaryColor else Color.Transparent)
                                .clickable { selectedMode = mode }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else DarkNavy,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }

            // Reference / Notes Field
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                label = { Text("Reference / Notes (Details / Cheque No)") },
                placeholder = { Text("e.g. Paid via GPay / Cheque #123456") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandPrimaryBlue,
                    unfocusedBorderColor = BorderStrokeDark,
                    focusedTextColor = DarkNavy,
                    unfocusedTextColor = DarkNavy
                )
            )
        }
    }
}
