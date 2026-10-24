package com.digitalchopda.app.presentation.products

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import com.digitalchopda.app.core.designsystem.ChopdaModalDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.digitalchopda.app.core.designsystem.BorderStrokeDark
import com.digitalchopda.app.core.designsystem.BorderStrokeLight
import com.digitalchopda.app.core.designsystem.BrandAccentSoft
import com.digitalchopda.app.core.designsystem.BrandPrimaryBlue
import com.digitalchopda.app.core.designsystem.DangerRed
import com.digitalchopda.app.core.designsystem.DarkNavy
import com.digitalchopda.app.core.designsystem.ElevatedSlate
import com.digitalchopda.app.core.designsystem.EmeraldJama
import com.digitalchopda.app.core.designsystem.MidnightCardSurface
import com.digitalchopda.app.core.designsystem.RubyUdhar
import com.digitalchopda.app.core.designsystem.TextMuted
import com.digitalchopda.app.core.designsystem.TextSubtle
import com.digitalchopda.app.core.designsystem.TextWhite
import com.digitalchopda.app.core.designsystem.bouncyClick
import com.digitalchopda.app.core.localization.LocalAppStrings
import com.digitalchopda.app.domain.model.Product

/**
 * Infers the Indian GST tax slab (0%, 5%, 12%, 18%, 28%) based on the HSN code prefix.
 */
fun inferGstRateFromHsn(hsn: String): Double? {
    val clean = hsn.filter { it.isDigit() }
    if (clean.length < 2) return null
    val p2 = clean.take(2).toIntOrNull() ?: return null
    val p4 = if (clean.length >= 4) clean.take(4).toIntOrNull() else null

    if (p4 != null) {
        when (p4) {
            in 101..1008 -> return 0.0 // Animals, meat, fish, fresh veg/fruits, cereals
            in 4901..4905 -> return 0.0 // Books, newspapers
            in 5001..6310 -> return 5.0 // Silk, wool, cotton, manmade fibers, apparel
            in 901..910 -> return 5.0   // Spices, tea, coffee
            in 1501..1518 -> return 5.0 // Edible oils
            in 1701..1704 -> return 5.0 // Sugar
            in 6401..6406 -> return 5.0 // Footwear (<1000)
            in 3001..3006 -> return 12.0 // Pharmaceuticals
            8517 -> return 18.0         // Mobile phones/telecom
            8471 -> return 18.0         // Computers / IT
            in 8415..8418 -> return 28.0 // ACs, refrigerators
            in 8702..8704 -> return 28.0 // Motor vehicles
            2402 -> return 28.0         // Tobacco
            2523 -> return 28.0         // Cement
        }
    }

    return when (p2) {
        in 1..10 -> 0.0     // Fresh Agri & Dairy
        in 50..63 -> 5.0    // Textiles & Fabrics
        in 64..67 -> 5.0    // Footwear
        in 28..38 -> 18.0   // Chemicals & Cosmetics
        in 39..40 -> 18.0   // Plastics & Rubber
        in 44..49 -> 12.0   // Wood & Paper
        in 68..71 -> 18.0   // Stone, Glass, Jewelry
        in 72..83 -> 18.0   // Metals & Steel
        in 84..85 -> 18.0   // Machinery & Electronics
        in 86..89 -> 18.0   // Transport Equipment
        in 90..96 -> 18.0   // Furniture, Toys, Instruments
        else -> 18.0
    }
}

private fun formatPriceValue(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toLong().toString()
    } else {
        String.format(java.util.Locale.US, "%.2f", value)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditProductDialog(
    companyId: Long,
    product: Product? = null,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    val strings = LocalAppStrings.current
    val isEditing = product != null

    val initialBase = product?.defaultPrice
    val initialGst = product?.gstRate ?: 18.0

    var name by remember { mutableStateOf(product?.name.orEmpty()) }
    var hsnCode by remember { mutableStateOf(product?.hsnCode.orEmpty()) }
    var selectedGstRate by remember { mutableStateOf(initialGst) }
    var selectedUnit by remember { mutableStateOf(product?.unit ?: "PCS") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var autoGstNotice by remember { mutableStateOf<String?>(null) }

    var priceStr by remember {
        mutableStateOf(if (initialBase != null) formatPriceValue(initialBase) else "")
    }
    var finalPriceStr by remember {
        val initialFinal = if (initialBase != null) initialBase * (1.0 + initialGst / 100.0) else null
        mutableStateOf(if (initialFinal != null) formatPriceValue(initialFinal) else "")
    }

    val commonGstRates = listOf(0.0, 5.0, 12.0, 18.0, 28.0)
    val commonUnits = listOf("PCS", "MTR", "KG", "SET", "BOX", "PKT", "BUNDLE")

    ChopdaModalDialog(
        onDismissRequest = onDismiss,
        title = if (isEditing) strings.editProductBtn else strings.addProductBtn,
        subtitle = if (isEditing) "Update product details & tax rate" else "Add item to your business catalog",
        icon = Icons.Default.Inventory2,
        iconTint = BrandPrimaryBlue,
        widthFraction = 1f,
        maxHeightFraction = 0.90f,
        isBottomSheet = true,
        buttons = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkNavy)
                ) {
                    Text("CANCEL", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val trimmedName = name.trim()
                        val priceVal = priceStr.toDoubleOrNull()
                        if (trimmedName.isBlank()) {
                            errorMessage = "Please enter a product name."
                            return@Button
                        }
                        if (priceVal == null || priceVal < 0) {
                            errorMessage = "Please enter a valid price (>= 0)."
                            return@Button
                        }

                        onSave(
                            Product(
                                id = product?.id ?: 0L,
                                companyId = companyId,
                                name = trimmedName,
                                defaultPrice = priceVal,
                                hsnCode = hsnCode.trim().ifBlank { null },
                                gstRate = selectedGstRate,
                                unit = selectedUnit,
                                createdAt = product?.createdAt ?: System.currentTimeMillis()
                            )
                        )
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandPrimaryBlue,
                        contentColor = Color.White
                    )
                ) {
                    Text(if (isEditing) "SAVE" else "ADD PRODUCT", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) {
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(RubyUdhar.copy(alpha = 0.1f))
                            .border(1.dp, RubyUdhar.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = RubyUdhar,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Product Name Field
                Text(
                    text = "${strings.productNameLabel} *",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = DarkNavy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    placeholder = { Text("e.g. Pure Cotton Shahi Saree 6.3m", color = TextSubtle) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandPrimaryBlue,
                        unfocusedBorderColor = BorderStrokeDark,
                        focusedContainerColor = ElevatedSlate,
                        unfocusedContainerColor = ElevatedSlate,
                        focusedTextColor = DarkNavy,
                        unfocusedTextColor = DarkNavy
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // HSN Code Field (with Auto-GST Detection)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HSN Code",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = DarkNavy,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                    if (autoGstNotice != null) {
                        Text(
                            text = autoGstNotice ?: "",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = BrandPrimaryBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = hsnCode,
                    onValueChange = { inputHsn ->
                        hsnCode = inputHsn
                        errorMessage = null
                        val detected = inferGstRateFromHsn(inputHsn)
                        if (detected != null) {
                            selectedGstRate = detected
                            autoGstNotice = "✓ Auto-selected ${detected.toInt()}% GST"
                            val p = priceStr.toDoubleOrNull()
                            if (p != null) {
                                val f = p * (1.0 + detected / 100.0)
                                finalPriceStr = formatPriceValue(f)
                            }
                        } else {
                            autoGstNotice = null
                        }
                    },
                    placeholder = { Text("e.g. 5208 (Textiles) or 8471 (Computers)", color = TextSubtle) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandPrimaryBlue,
                        unfocusedBorderColor = BorderStrokeDark,
                        focusedContainerColor = ElevatedSlate,
                        unfocusedContainerColor = ElevatedSlate,
                        focusedTextColor = DarkNavy,
                        unfocusedTextColor = DarkNavy
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "HSN classifies product for GST. GST rate updates automatically upon entering HSN.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSubtle, fontSize = 11.sp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // GST Rate % Selector
                Text(
                    text = "GST Rate (Tax Slab)",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = DarkNavy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    commonGstRates.forEach { rate ->
                        val isSelected = selectedGstRate == rate
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) BrandPrimaryBlue else ElevatedSlate)
                                .border(
                                    1.dp,
                                    if (isSelected) BrandPrimaryBlue else BorderStrokeDark,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    selectedGstRate = rate
                                    autoGstNotice = null
                                    val p = priceStr.toDoubleOrNull()
                                    if (p != null) {
                                        val f = p * (1.0 + rate / 100.0)
                                        finalPriceStr = formatPriceValue(f)
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${rate.toInt()}%",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = if (isSelected) Color.White else DarkNavy,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Real-time Pricing Row: Base Price & Final Price (with GST)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Base Price Field (Excl. GST)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Price (Base) *",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = DarkNavy,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = priceStr,
                            onValueChange = { input ->
                                priceStr = input
                                errorMessage = null
                                val p = input.toDoubleOrNull()
                                if (p != null) {
                                    val f = p * (1.0 + selectedGstRate / 100.0)
                                    finalPriceStr = formatPriceValue(f)
                                } else if (input.isBlank()) {
                                    finalPriceStr = ""
                                }
                            },
                            placeholder = { Text("e.g. 1000", color = TextSubtle) },
                            prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = DarkNavy) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandPrimaryBlue,
                                unfocusedBorderColor = BorderStrokeDark,
                                focusedContainerColor = ElevatedSlate,
                                unfocusedContainerColor = ElevatedSlate,
                                focusedTextColor = DarkNavy,
                                unfocusedTextColor = DarkNavy
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Final Price Field (with GST)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Final Price (with GST) *",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = DarkNavy,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = finalPriceStr,
                            onValueChange = { input ->
                                finalPriceStr = input
                                errorMessage = null
                                val f = input.toDoubleOrNull()
                                if (f != null) {
                                    // Base price = Final Price / (1 + GST%)
                                    val p = f / (1.0 + selectedGstRate / 100.0)
                                    priceStr = formatPriceValue(p)
                                } else if (input.isBlank()) {
                                    priceStr = ""
                                }
                            },
                            placeholder = { Text("e.g. 1180", color = TextSubtle) },
                            prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = DarkNavy) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandPrimaryBlue,
                                unfocusedBorderColor = BorderStrokeDark,
                                focusedContainerColor = ElevatedSlate,
                                unfocusedContainerColor = ElevatedSlate,
                                focusedTextColor = DarkNavy,
                                unfocusedTextColor = DarkNavy
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Real-time Calculation Breakdown Card
                val liveBase = priceStr.toDoubleOrNull() ?: 0.0
                val liveGstAmount = liveBase * (selectedGstRate / 100.0)
                val liveFinal = liveBase + liveGstAmount

                if (liveBase > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(BrandAccentSoft)
                            .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "REAL-TIME TAX BREAKDOWN",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = BrandPrimaryBlue,
                                        fontSize = 10.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                                Text(
                                    text = "${selectedGstRate.toInt()}% GST",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = BrandPrimaryBlue,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                            HorizontalDivider(color = Color(0xFFDBEAFE), thickness = 1.dp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Base ₹${String.format(java.util.Locale.US, "%,.2f", liveBase)} + Tax ₹${String.format(java.util.Locale.US, "%,.2f", liveGstAmount)}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSubtle,
                                        fontSize = 11.sp
                                    )
                                )
                                Text(
                                    text = "= Final ₹${String.format(java.util.Locale.US, "%,.2f", liveFinal)}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Black,
                                        color = DarkNavy,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Unit of Measurement Selector
                Text(
                    text = strings.unitLabel,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = DarkNavy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    commonUnits.forEach { u ->
                        val isSelected = selectedUnit.equals(u, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) BrandAccentSoft else ElevatedSlate)
                                .border(
                                    1.dp,
                                    if (isSelected) BrandPrimaryBlue else BorderStrokeDark,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedUnit = u }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = u,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isSelected) BrandPrimaryBlue else DarkNavy,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
    }
}
