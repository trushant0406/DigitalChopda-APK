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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import com.digitalchopda.app.core.designsystem.ChopdaModalDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.digitalchopda.app.core.designsystem.BorderStrokeDark
import com.digitalchopda.app.core.designsystem.BorderStrokeLight
import com.digitalchopda.app.core.designsystem.BrandAccentSoft
import com.digitalchopda.app.core.designsystem.BrandPrimaryBlue
import com.digitalchopda.app.core.designsystem.DarkNavy
import com.digitalchopda.app.core.designsystem.ElevatedSlate
import com.digitalchopda.app.core.designsystem.EmeraldJama
import com.digitalchopda.app.core.designsystem.MidnightCardSurface
import com.digitalchopda.app.core.designsystem.RubyUdhar
import com.digitalchopda.app.core.designsystem.TextMuted
import com.digitalchopda.app.core.designsystem.TextSubtle
import com.digitalchopda.app.core.designsystem.TextWhite
import com.digitalchopda.app.data.local.entity.CustomerEntity

val PARTY_TAG_OPTIONS = listOf("Active Party", "Wholesale", "Retail", "Distributor", "VIP", "Supplier", "Corporate")

@Composable
fun AddEditPartyDialog(
    companyId: Long,
    party: CustomerEntity? = null,
    onDismiss: () -> Unit,
    onSave: (CustomerEntity) -> Unit
) {
    val isEdit = party != null

    var name by remember { mutableStateOf(party?.name ?: "") }
    var phone by remember { mutableStateOf(party?.phone ?: "") }
    var partyType by remember { mutableStateOf(party?.partyType ?: "customer") } // customer / supplier
    var partyTag by remember { mutableStateOf(party?.partyTag ?: "Active Party") }
    var openingBalance by remember { mutableStateOf(if ((party?.openingBalance ?: 0.0) != 0.0) party!!.openingBalance.toString() else "0") }
    var openingBalanceType by remember { mutableStateOf(party?.openingBalanceType ?: "to_receive") } // to_receive / to_pay
    var creditLimit by remember { mutableStateOf(if ((party?.creditLimit ?: 0.0) != 0.0) party!!.creditLimit.toString() else "0") }
    var gstin by remember { mutableStateOf(party?.gstin ?: "") }
    var pan by remember { mutableStateOf(party?.pan ?: "") }
    var address by remember { mutableStateOf(party?.address ?: "") }
    var city by remember { mutableStateOf(party?.city ?: "") }
    var state by remember { mutableStateOf(party?.state ?: "Gujarat") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    ChopdaModalDialog(
        onDismissRequest = onDismiss,
        title = if (isEdit) "Edit Party" else "+ Add New Party",
        subtitle = "Khata Master • Sovereign Ledger",
        icon = Icons.Default.Person,
        widthFraction = 1f,
        maxHeightFraction = 0.92f,
        isBottomSheet = true,
        buttons = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = ElevatedSlate, contentColor = DarkNavy),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("Cancel", color = DarkNavy, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        if (name.isBlank()) {
                            errorMessage = "Party name is required"
                            return@Button
                        }
                        if (phone.isBlank()) {
                            errorMessage = "Phone number is required"
                            return@Button
                        }

                        val openBalNum = openingBalance.toDoubleOrNull() ?: 0.0
                        val credLimitNum = creditLimit.toDoubleOrNull() ?: 0.0

                        val initialOpeningSigned = if (party != null) {
                            if (party.openingBalanceType == "to_receive") party.openingBalance else -party.openingBalance
                        } else 0.0
                        val newOpeningSigned = if (openingBalanceType == "to_receive") openBalNum else -openBalNum
                        val openingDelta = newOpeningSigned - initialOpeningSigned

                        val finalBalance = if (party != null) {
                            party.balance + openingDelta
                        } else {
                            newOpeningSigned
                        }

                        val newParty = party?.copy(
                            companyId = companyId,
                            name = name.trim(),
                            phone = phone.trim(),
                            partyType = partyType,
                            partyTag = partyTag,
                            openingBalance = openBalNum,
                            openingBalanceType = openingBalanceType,
                            creditLimit = credLimitNum,
                            gstin = gstin.trim(),
                            pan = pan.trim(),
                            address = address.trim(),
                            city = city.trim(),
                            state = state.trim(),
                            balance = finalBalance
                        ) ?: CustomerEntity(
                            id = 0L,
                            companyId = companyId,
                            name = name.trim(),
                            phone = phone.trim(),
                            partyType = partyType,
                            partyTag = partyTag,
                            openingBalance = openBalNum,
                            openingBalanceType = openingBalanceType,
                            creditLimit = credLimitNum,
                            gstin = gstin.trim(),
                            pan = pan.trim(),
                            address = address.trim(),
                            city = city.trim(),
                            state = state.trim(),
                            balance = finalBalance
                        )
                        onSave(newParty)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryBlue),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSaving,
                    modifier = Modifier.weight(1.5f).height(48.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.height(16.dp).width(16.dp))
                    } else {
                        Text(if (isEdit) "UPDATE PARTY" else "SAVE PARTY", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) {
        // Error message
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = RubyUdhar,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Party Name Field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMessage = null },
                    label = { Text("Party Name *") },
                    placeholder = { Text("e.g. Ramesh Bhai / Sharma Textiles") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = BrandPrimaryBlue,
                        unfocusedBorderColor = BorderStrokeDark,
                        focusedLabelColor = BrandPrimaryBlue,
                        unfocusedLabelColor = TextSubtle
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Phone Field
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; errorMessage = null },
                    label = { Text("Mobile Phone *") },
                    placeholder = { Text("e.g. 9876543210") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = BrandPrimaryBlue,
                        unfocusedBorderColor = BorderStrokeDark,
                        focusedLabelColor = BrandPrimaryBlue,
                        unfocusedLabelColor = TextSubtle
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Party Type Selector (Customer / Supplier)
                Text(
                    text = "PARTY TYPE",
                    color = TextSubtle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("customer" to "Customer", "supplier" to "Supplier").forEach { (type, label) ->
                        val isSelected = partyType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) BrandPrimaryBlue else ElevatedSlate)
                                .border(1.dp, if (isSelected) BrandPrimaryBlue else BorderStrokeDark, RoundedCornerShape(10.dp))
                                .clickable { partyType = type }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else DarkNavy,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tag Chips
                Text(
                    text = "CATEGORY / TAG",
                    color = TextSubtle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(PARTY_TAG_OPTIONS) { tag ->
                        val isSelected = partyTag == tag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) BrandAccentSoft else ElevatedSlate)
                                .border(1.dp, if (isSelected) BrandPrimaryBlue else BorderStrokeDark, RoundedCornerShape(8.dp))
                                .clickable { partyTag = tag }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = tag,
                                color = if (isSelected) BrandPrimaryBlue else DarkNavy,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Opening Balance & Type
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = openingBalance,
                        onValueChange = { openingBalance = it },
                        label = { Text("Opening Bal (₹)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = BrandPrimaryBlue,
                            unfocusedBorderColor = BorderStrokeDark,
                            focusedLabelColor = BrandPrimaryBlue,
                            unfocusedLabelColor = TextSubtle
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "BAL TYPE",
                            color = TextSubtle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (openingBalanceType == "to_receive") EmeraldJama.copy(alpha = 0.15f) else ElevatedSlate)
                                    .border(1.dp, if (openingBalanceType == "to_receive") EmeraldJama else BorderStrokeDark, RoundedCornerShape(8.dp))
                                    .clickable { openingBalanceType = "to_receive" }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "To Receive\n(Debit)",
                                    color = if (openingBalanceType == "to_receive") EmeraldJama else DarkNavy,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 12.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (openingBalanceType == "to_pay") RubyUdhar.copy(alpha = 0.15f) else ElevatedSlate)
                                    .border(1.dp, if (openingBalanceType == "to_pay") RubyUdhar else BorderStrokeDark, RoundedCornerShape(8.dp))
                                    .clickable { openingBalanceType = "to_pay" }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "To Pay\n(Credit)",
                                    color = if (openingBalanceType == "to_pay") RubyUdhar else DarkNavy,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Credit Limit
                OutlinedTextField(
                    value = creditLimit,
                    onValueChange = { creditLimit = it },
                    label = { Text("Credit Limit (₹)") },
                    placeholder = { Text("0.00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = BrandPrimaryBlue,
                        unfocusedBorderColor = BorderStrokeDark,
                        focusedLabelColor = BrandPrimaryBlue,
                        unfocusedLabelColor = TextSubtle
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // GSTIN & PAN
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = gstin,
                        onValueChange = { gstin = it.uppercase() },
                        label = { Text("GSTIN") },
                        placeholder = { Text("24XXXXX...") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = BrandPrimaryBlue,
                            unfocusedBorderColor = BorderStrokeDark,
                            focusedLabelColor = BrandPrimaryBlue,
                            unfocusedLabelColor = TextSubtle
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = pan,
                        onValueChange = { pan = it.uppercase() },
                        label = { Text("PAN Number") },
                        placeholder = { Text("ABCDE1234F") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = BrandPrimaryBlue,
                            unfocusedBorderColor = BorderStrokeDark,
                            focusedLabelColor = BrandPrimaryBlue,
                            unfocusedLabelColor = TextSubtle
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Address & City
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City") },
                        placeholder = { Text("Surat") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = BrandPrimaryBlue,
                            unfocusedBorderColor = BorderStrokeDark,
                            focusedLabelColor = BrandPrimaryBlue,
                            unfocusedLabelColor = TextSubtle
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = state,
                        onValueChange = { state = it },
                        label = { Text("State") },
                        placeholder = { Text("Gujarat") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = BrandPrimaryBlue,
                            unfocusedBorderColor = BorderStrokeDark,
                            focusedLabelColor = BrandPrimaryBlue,
                            unfocusedLabelColor = TextSubtle
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Billing Address") },
                    placeholder = { Text("Shop / Office address...") },
                    maxLines = 2,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = BrandPrimaryBlue,
                        unfocusedBorderColor = BorderStrokeDark,
                        focusedLabelColor = BrandPrimaryBlue,
                        unfocusedLabelColor = TextSubtle
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
    }
}
