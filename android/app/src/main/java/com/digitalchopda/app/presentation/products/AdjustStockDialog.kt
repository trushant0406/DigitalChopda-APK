package com.digitalchopda.app.presentation.products

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.digitalchopda.app.core.designsystem.BorderStrokeDark
import com.digitalchopda.app.core.designsystem.BrandPrimaryBlue
import com.digitalchopda.app.core.designsystem.DangerRed
import com.digitalchopda.app.core.designsystem.DeepObsidianCanvas
import com.digitalchopda.app.core.designsystem.ElevatedSlate
import com.digitalchopda.app.core.designsystem.EmeraldJama
import com.digitalchopda.app.core.designsystem.MidnightCardSurface
import com.digitalchopda.app.core.designsystem.RubyUdhar
import com.digitalchopda.app.core.designsystem.TextMuted
import com.digitalchopda.app.core.designsystem.TextSubtle
import com.digitalchopda.app.core.designsystem.TextWhite
import com.digitalchopda.app.data.repository.ProductRepository
import com.digitalchopda.app.domain.model.Product
import kotlinx.coroutines.launch

/**
 * Quick Stock Adjustment Dialog (Inventory Reconciliation)
 * Parity with Web Platform's AdjustStockModal.jsx
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdjustStockDialog(
    product: Product,
    productRepository: ProductRepository,
    onDismiss: () -> Unit,
    onAdjusted: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var adjustmentType by remember { mutableStateOf("add") }
    var quantityText by remember { mutableStateOf("") }
    var selectedReason by remember { mutableStateOf("Supplier Delivery") }
    var customNotes by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    val qty = quantityText.toDoubleOrNull() ?: 0.0

    val calculatedNewStock = when (adjustmentType) {
        "add" -> product.stockQuantity + qty
        "reduce" -> (product.stockQuantity - qty).coerceAtLeast(0.0)
        "set" -> qty
        else -> product.stockQuantity
    }

    val reasonPresets = when (adjustmentType) {
        "add" -> listOf("Supplier Delivery", "Customer Return", "Inventory Found", "Other Intake")
        "reduce" -> listOf("Damaged Goods", "Physical Theft / Loss", "Scrap / Expired", "Sample", "Internal Use")
        else -> listOf("Physical Shelf Audit", "Annual Stock Count", "Correction of Error")
    }

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
                            text = "Adjust Stock Quantity",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = TextWhite
                            )
                        )
                        Text(
                            text = "${product.name} • Current: ${product.stockQuantity.toInt()} ${product.unit}",
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

                Spacer(modifier = Modifier.height(18.dp))

                // Adjustment Type Selector Tabs (Add / Reduce / Set)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple("add", "+ Add Stock", EmeraldJama),
                        Triple("reduce", "- Reduce", RubyUdhar),
                        Triple("set", "= Set Exact", BrandPrimaryBlue)
                    ).forEach { (type, label, color) ->
                        val isSelected = adjustmentType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) color.copy(alpha = 0.2f) else ElevatedSlate)
                                .border(
                                    1.dp,
                                    if (isSelected) color else BorderStrokeDark,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    adjustmentType = type
                                    selectedReason = when (type) {
                                        "add" -> "Supplier Delivery"
                                        "reduce" -> "Damaged Goods"
                                        else -> "Physical Shelf Audit"
                                    }
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) color else TextMuted
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quantity Input Field
                Text(
                    text = when (adjustmentType) {
                        "add" -> "UNITS TO ADD (+)"
                        "reduce" -> "UNITS TO REDUCE (-)"
                        else -> "NEW EXACT STOCK COUNT (=)"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextSubtle,
                        letterSpacing = 0.5.sp
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    placeholder = { Text("e.g. 10", color = TextMuted) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    trailingIcon = {
                        Text(
                            text = product.unit,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            ),
                            modifier = Modifier.padding(end = 12.dp)
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

                // Live Stock Tally Card
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
                                text = "CURRENT STOCK",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp)
                            )
                            Text(
                                text = "${product.stockQuantity.toInt()} ${product.unit}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextWhite)
                            )
                        }

                        Text(
                            text = "➔",
                            style = MaterialTheme.typography.titleMedium.copy(color = TextMuted)
                        )

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "NEW PROJECTED STOCK",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp)
                            )
                            Text(
                                text = "${calculatedNewStock.toInt()} ${product.unit}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = if (calculatedNewStock <= 0.0) DangerRed else EmeraldJama
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Reason Presets
                Text(
                    text = "REASON FOR ADJUSTMENT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextSubtle,
                        letterSpacing = 0.5.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    reasonPresets.forEach { preset ->
                        val isSelected = selectedReason == preset
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) BrandPrimaryBlue.copy(alpha = 0.2f) else ElevatedSlate)
                                .border(
                                    1.dp,
                                    if (isSelected) BrandPrimaryBlue else BorderStrokeDark,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedReason = preset }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = preset,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) BrandPrimaryBlue else TextWhite
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Optional Notes
                Text(
                    text = "REMARKS / AUDIT NOTE (OPTIONAL)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextSubtle,
                        letterSpacing = 0.5.sp
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = customNotes,
                    onValueChange = { customNotes = it },
                    placeholder = { Text("e.g. Shelf count verified", color = TextMuted) },
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
                            if (qty <= 0.0 && adjustmentType != "set") {
                                Toast.makeText(context, "Please enter a valid quantity", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            scope.launch {
                                isSaving = true
                                val res = productRepository.adjustStock(
                                    companyId = product.companyId,
                                    productId = product.id,
                                    adjustmentType = adjustmentType,
                                    quantity = qty,
                                    reason = selectedReason,
                                    notes = customNotes.ifBlank { null }
                                )
                                isSaving = false
                                if (res.isSuccess) {
                                    Toast.makeText(context, "✓ Stock updated to ${calculatedNewStock.toInt()} ${product.unit}", Toast.LENGTH_SHORT).show()
                                    onAdjusted()
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (adjustmentType) {
                                "add" -> EmeraldJama
                                "reduce" -> RubyUdhar
                                else -> BrandPrimaryBlue
                            }
                        )
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = TextWhite, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        } else {
                            Text("Confirm Adjustment", fontWeight = FontWeight.Bold, color = TextWhite)
                        }
                    }
                }
            }
        }
    }
}
