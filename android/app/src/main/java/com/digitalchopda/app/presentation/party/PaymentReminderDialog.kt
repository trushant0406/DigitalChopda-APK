package com.digitalchopda.app.presentation.party

import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalchopda.app.core.designsystem.BorderStrokeDark
import com.digitalchopda.app.core.designsystem.BrandPrimaryBlue
import com.digitalchopda.app.core.designsystem.ChopdaModalDialog
import com.digitalchopda.app.core.designsystem.DarkNavy
import com.digitalchopda.app.core.designsystem.ElevatedSlate
import com.digitalchopda.app.core.designsystem.EmeraldJama
import com.digitalchopda.app.core.designsystem.EmeraldJamaBg
import com.digitalchopda.app.core.designsystem.MidnightCardSurface
import com.digitalchopda.app.core.designsystem.RubyUdhar
import com.digitalchopda.app.core.designsystem.TextMuted
import com.digitalchopda.app.core.designsystem.TextSubtle
import com.digitalchopda.app.core.designsystem.TextWhite
import com.digitalchopda.app.core.localization.AppLanguage
import com.digitalchopda.app.core.localization.LanguageManager
import com.digitalchopda.app.core.payment.UpiPaymentUtils
import com.digitalchopda.app.domain.model.Company
import java.util.Locale

@Composable
fun PaymentReminderDialog(
    partyName: String,
    partyPhone: String,
    outstandingAmount: Double,
    activeCompany: Company,
    invoiceNumber: String? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedLanguage by remember { mutableStateOf<AppLanguage>(AppLanguage.GUJARATI) }
    var amountInput by remember {
        mutableStateOf(if (outstandingAmount > 0) String.format(Locale.US, "%.0f", outstandingAmount) else "500")
    }

    val currentAmount = amountInput.toDoubleOrNull() ?: outstandingAmount.coerceAtLeast(0.0)

    val message = remember(partyName, activeCompany, currentAmount, selectedLanguage, invoiceNumber) {
        UpiPaymentUtils.buildWhatsAppReminderMessage(
            customerName = partyName,
            firmName = activeCompany.firmName,
            amount = currentAmount,
            upiVpa = activeCompany.upiVpa,
            invoiceNumber = invoiceNumber,
            language = selectedLanguage
        )
    }

    ChopdaModalDialog(
        onDismissRequest = onDismiss,
        title = "WhatsApp Payment Reminder",
        subtitle = "$partyName (${partyPhone.ifBlank { "No phone" }})",
        icon = Icons.Default.Chat,
        iconTint = EmeraldJama,
        widthFraction = 0.94f,
        maxHeightFraction = 0.90f,
        scrollable = true,
        buttons = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        UpiPaymentUtils.launchWhatsApp(
                            context = context,
                            phoneNumber = partyPhone,
                            message = message
                        )
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldJama,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SEND ON WHATSAPP",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStrokeDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text(
                        text = "CANCEL",
                        color = TextMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Language Selection Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Language:",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                )
                listOf(
                    AppLanguage.GUJARATI to "ગુજરાતી",
                    AppLanguage.HINDI to "हिन्दी",
                    AppLanguage.ENGLISH to "English"
                ).forEach { (lang, label) ->
                    val isSelected = selectedLanguage == lang
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) BrandPrimaryBlue else ElevatedSlate)
                            .clickable { selectedLanguage = lang }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isSelected) Color.White else TextMuted,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                }
            }

            // Amount Input Field
            OutlinedTextField(
                value = amountInput,
                onValueChange = { input ->
                    if (input.all { it.isDigit() || it == '.' }) {
                        amountInput = input
                    }
                },
                label = { Text("Reminder Amount (₹)") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.CurrencyRupee,
                        contentDescription = null,
                        tint = RubyUdhar,
                        modifier = Modifier.size(18.dp)
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandPrimaryBlue,
                    unfocusedBorderColor = BorderStrokeDark,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedLabelColor = BrandPrimaryBlue,
                    unfocusedLabelColor = TextMuted,
                    cursorColor = BrandPrimaryBlue
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Direct UPI Deep Link Info Badge
            Surface(
                color = DarkNavy,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderStrokeDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(EmeraldJamaBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = EmeraldJama,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Direct NPCI Tap-to-Pay Link",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = TextWhite,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "VPA: ${activeCompany.upiVpa.ifBlank { "merchant@upi" }}",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // WhatsApp Live Preview Box
            Text(
                text = "MESSAGE PREVIEW",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextSubtle,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            )

            Surface(
                color = Color(0xFF0D141C),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2D3D)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextWhite,
                            lineHeight = 18.sp
                        )
                    )
                }
            }
        }
    }
}
