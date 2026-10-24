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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import com.digitalchopda.app.core.designsystem.MidnightCardSurface
import com.digitalchopda.app.core.designsystem.RubyUdhar
import com.digitalchopda.app.core.designsystem.TextMuted
import com.digitalchopda.app.core.designsystem.TextSubtle
import com.digitalchopda.app.core.designsystem.TextWhite
import com.digitalchopda.app.data.repository.CustomerRepository
import com.digitalchopda.app.util.CurrencyFormatter
import kotlinx.coroutines.launch

/**
 * 💸 Record Payment Out Dialog (Paying Supplier / Settling Debt)
 * Parity with Web Platform's RecordPaymentOutModal.jsx
 */
@Composable
fun RecordPaymentOutDialog(
    companyId: Long,
    partyId: Long,
    partyName: String,
    currentBalanceDue: Double,
    customerRepository: CustomerRepository,
    onDismiss: () -> Unit,
    onPaymentRecorded: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var amountText by remember {
        mutableStateOf(if (currentBalanceDue > 0.0) currentBalanceDue.toInt().toString() else "")
    }
    var selectedMode by remember { mutableStateOf("Bank") } // Bank, UPI, Cash, Cheque
    var referenceNo by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val newBalance = (currentBalanceDue - amount).coerceAtLeast(0.0)

    val modes = listOf("Bank", "UPI", "Cash", "Cheque")

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
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
                            text = "Record Payment Out",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = TextWhite
                            )
                        )
                        Text(
                            text = "Paying supplier: $partyName",
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

                Spacer(modifier = Modifier.height(16.dp))

                // Current Debt Tally Banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DeepObsidianCanvas,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStrokeDark)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CURRENT DEBT TO PAY",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp)
                            )
                            Text(
                                text = CurrencyFormatter.formatINR(currentBalanceDue),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = RubyUdhar)
                            )
                        }

                        Text(
                            text = "➔",
                            style = MaterialTheme.typography.titleMedium.copy(color = TextMuted)
                        )

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "REMAINING DEBT",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp)
                            )
                            Text(
                                text = CurrencyFormatter.formatINR(newBalance),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = if (newBalance <= 0.0) com.digitalchopda.app.core.designsystem.EmeraldJama else TextWhite
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Amount Field
                Text(
                    text = "AMOUNT PAID (₹)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextSubtle,
                        letterSpacing = 0.5.sp
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    placeholder = { Text("0.00", color = TextMuted) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = {
                        Text(
                            text = "₹",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BrandPrimaryBlue
                            ),
                            modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                        )
                    },
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

                Spacer(modifier = Modifier.height(14.dp))

                // Payment Mode Selector
                Text(
                    text = "PAYMENT MODE",
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
                    modes.forEach { mode ->
                        val isSelected = selectedMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) BrandPrimaryBlue.copy(alpha = 0.2f) else ElevatedSlate)
                                .border(
                                    1.dp,
                                    if (isSelected) BrandPrimaryBlue else BorderStrokeDark,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedMode = mode }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) BrandPrimaryBlue else TextWhite
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Reference / Cheque No
                Text(
                    text = "REFERENCE / UTR / CHEQUE NO (OPTIONAL)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextSubtle,
                        letterSpacing = 0.5.sp
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = referenceNo,
                    onValueChange = { referenceNo = it },
                    placeholder = { Text("e.g. UTR-9823412 or Cheque 00213", color = TextMuted) },
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
                    placeholder = { Text("e.g. Cleared bill for batch 4", color = TextMuted) },
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
                            if (amount <= 0.0) {
                                Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            scope.launch {
                                isSaving = true
                                val res = customerRepository.recordPaymentOut(
                                    companyId = companyId,
                                    partyId = partyId,
                                    amount = amount,
                                    paymentMode = selectedMode,
                                    referenceNo = referenceNo,
                                    notes = notesText
                                )
                                isSaving = false
                                if (res.isSuccess) {
                                    Toast.makeText(context, "✓ Payment of ${CurrencyFormatter.formatINR(amount)} recorded", Toast.LENGTH_SHORT).show()
                                    onPaymentRecorded()
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
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryBlue)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = TextWhite, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        } else {
                            Text("Record Payment", fontWeight = FontWeight.Bold, color = TextWhite)
                        }
                    }
                }
            }
        }
    }
}
