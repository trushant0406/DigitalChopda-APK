package com.digitalchopda.app.core.designsystem

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * 🛡️ Non-Dismissible Force Update & Upgrade Safety Dialog
 *
 * Enforces contract safety between Central Database & Android APK.
 * If server min_supported_build > current_build, users cannot proceed without updating.
 */
@Composable
fun ForceUpdateDialog(
    currentVersion: String,
    requiredVersion: String,
    releaseNotes: String? = null,
    updateUrl: String = "https://digitalchopda.com/apk/DigitalChopda-release.apk",
    isMandatory: Boolean = true,
    onDismissSoftUpdate: () -> Unit = {}
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = {
            if (!isMandatory) {
                onDismissSoftUpdate()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isMandatory,
            dismissOnClickOutside = !isMandatory
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(if (isMandatory) Color(0xFFFEE2E2) else Color(0xFFE0E7FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Update Available",
                        tint = if (isMandatory) RubyUdhar else BrandPrimaryBlue,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = if (isMandatory) "Critical Update Required" else "New Version Available",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = DarkNavy
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Version Badge
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(
                        text = "Current: v$currentVersion",
                        textColor = TextSubtle,
                        backgroundColor = ElevatedSlate
                    )
                    Text("➔", color = TextSubtle, fontSize = 12.sp)
                    StatusPill(
                        text = "Latest: v$requiredVersion",
                        textColor = if (isMandatory) RubyUdhar else EmeraldJama,
                        backgroundColor = if (isMandatory) RubyUdhar.copy(alpha = 0.12f) else EmeraldJamaBg
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Body Message
                Text(
                    text = if (isMandatory) {
                        "This version of DigitalChopda is no longer compatible with the central ledger. Please update to continue recording transactions safely without risk of data loss."
                    } else {
                        "A new version of DigitalChopda is available with enhanced performance, fresh features, and ledger sync improvements."
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextMuted,
                        lineHeight = 18.sp
                    ),
                    textAlign = TextAlign.Center
                )

                if (!releaseNotes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElevatedSlate)
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "What's New:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = DarkNavy
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = releaseNotes,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.5.sp,
                                    color = TextMuted,
                                    lineHeight = 16.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Update Button
                Button(
                    onClick = {
                        val fullUrl = if (updateUrl.startsWith("http")) {
                            updateUrl
                        } else {
                            "http://100.99.250.0:3000$updateUrl"
                        }
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl))
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMandatory) RubyUdhar else BrandPrimaryBlue
                    )
                ) {
                    Text(
                        text = if (isMandatory) "UPDATE APP NOW" else "DOWNLOAD UPDATE",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                if (!isMandatory) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onDismissSoftUpdate,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Remind Me Later",
                            style = MaterialTheme.typography.labelMedium.copy(color = TextSubtle)
                        )
                    }
                }
            }
        }
    }
}
