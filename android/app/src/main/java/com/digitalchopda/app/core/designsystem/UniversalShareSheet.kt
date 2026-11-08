package com.digitalchopda.app.core.designsystem

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalchopda.app.core.document.UniversalShareEngine
import java.io.File

/**
 * 🛸 Enterprise Universal Share Bottom Sheet
 *
 * Provides merchants with a clean 4-tile Bento layout to dispatch documents:
 * 1. 🟢 WhatsApp / WA Business (Direct attachment + Pre-filled greeting)
 * 2. 🌐 System Multi-App Sharesheet (Email, Drive, Telegram, Messages)
 * 3. 📋 Copy Message / UPI Payment Link
 * 4. 🖨️ Thermal POS / A4 Print Slip
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalShareSheet(
    title: String,
    subtitle: String,
    documentFile: File? = null,
    captionMessage: String? = null,
    recipientPhone: String? = null,
    onPrintClick: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MidnightCardSurface,
        scrimColor = Color(0x66000000),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            fontSize = 18.sp
                        )
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSubtle,
                            fontSize = 12.sp
                        )
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(ElevatedSlate)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = BorderStrokeDark, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(18.dp))

            // 4-Tile Bento Grid Dispatch Actions
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Action 1: WhatsApp Priority Dispatch
                ShareOptionTile(
                    icon = Icons.Default.Chat,
                    iconBg = WhatsAppGreen.copy(alpha = 0.15f),
                    iconTint = WhatsAppGreen,
                    title = "Share via WhatsApp",
                    subtitle = if (!recipientPhone.isNullOrBlank()) "Direct to $recipientPhone (PDF + UPI Link)" else "Send to any contact (Regular / Business)",
                    badge = "Instant",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (documentFile != null && documentFile.exists()) {
                            UniversalShareEngine.shareDocument(
                                context = context,
                                file = documentFile,
                                caption = captionMessage,
                                recipientPhone = recipientPhone,
                                preferWhatsApp = true
                            )
                        } else if (!captionMessage.isNullOrBlank()) {
                            UniversalShareEngine.shareTextMessage(
                                context = context,
                                message = captionMessage,
                                recipientPhone = recipientPhone,
                                preferWhatsApp = true
                            )
                        }
                        onDismiss()
                    }
                )

                // Action 2: Android Multi-App Chooser
                ShareOptionTile(
                    icon = Icons.Default.Share,
                    iconBg = BrandPrimaryBlue.copy(alpha = 0.15f),
                    iconTint = BrandPrimaryBlue,
                    title = "Share via Other Apps",
                    subtitle = "Gmail, Telegram, Google Drive, Messages & Nearby Share",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (documentFile != null && documentFile.exists()) {
                            UniversalShareEngine.shareDocument(
                                context = context,
                                file = documentFile,
                                caption = captionMessage,
                                recipientPhone = recipientPhone,
                                preferWhatsApp = false
                            )
                        } else if (!captionMessage.isNullOrBlank()) {
                            UniversalShareEngine.shareTextMessage(
                                context = context,
                                message = captionMessage,
                                recipientPhone = recipientPhone,
                                preferWhatsApp = false
                            )
                        }
                        onDismiss()
                    }
                )

                // Action 3: Copy Text / UPI Link
                if (!captionMessage.isNullOrBlank()) {
                    ShareOptionTile(
                        icon = Icons.Default.ContentCopy,
                        iconBg = ImperialGold.copy(alpha = 0.15f),
                        iconTint = ImperialGold,
                        title = "Copy Message / UPI Link",
                        subtitle = "Copy payment reminder & banking details to clipboard",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            UniversalShareEngine.copyToClipboard(context, title, captionMessage)
                            onDismiss()
                        }
                    )
                }

                // Action 4: Thermal / POS Print Slip
                if (onPrintClick != null) {
                    ShareOptionTile(
                        icon = Icons.Default.Print,
                        iconBg = EmeraldJama.copy(alpha = 0.15f),
                        iconTint = EmeraldJama,
                        title = "Print Invoice / Thermal Slip",
                        subtitle = "Wireless Bluetooth POS (58mm/80mm) or system print spooler",
                        badge = "POS",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPrintClick()
                            onDismiss()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ShareOptionTile(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    badge: String? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, BorderStrokeDark, RoundedCornerShape(14.dp))
            .bouncyClick { onClick() },
        color = ElevatedSlate
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            fontSize = 14.sp
                        )
                    )
                    if (!badge.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(iconTint.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badge,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = iconTint,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextMuted,
                        fontSize = 11.5.sp
                    ),
                    maxLines = 1
                )
            }
        }
    }
}
