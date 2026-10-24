package com.digitalchopda.app.presentation.gst

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalchopda.app.core.designsystem.BorderStrokeLight
import com.digitalchopda.app.core.designsystem.BrandPrimaryBlue
import com.digitalchopda.app.core.designsystem.ChopdaCard
import com.digitalchopda.app.core.designsystem.CoralGradientButton
import com.digitalchopda.app.core.designsystem.DarkNavy
import com.digitalchopda.app.core.designsystem.ElevatedSlate
import com.digitalchopda.app.core.designsystem.EmeraldJama
import com.digitalchopda.app.core.designsystem.RubyUdhar
import com.digitalchopda.app.core.designsystem.TextMuted
import com.digitalchopda.app.core.designsystem.TextSubtle
import com.digitalchopda.app.core.designsystem.WarningAmber
import com.digitalchopda.app.core.network.ApiPreFilingAnomalyDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreFilingAnomalySheet(
    anomalies: List<ApiPreFilingAnomalyDto>,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (anomalies.isEmpty()) EmeraldJama.copy(alpha = 0.12f)
                                else RubyUdhar.copy(alpha = 0.12f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (anomalies.isEmpty()) Icons.Default.CheckCircle else Icons.Default.Security,
                            contentDescription = null,
                            tint = if (anomalies.isEmpty()) EmeraldJama else RubyUdhar,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "GST Pre-Filing Health Check",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = DarkNavy
                            )
                        )
                        Text(
                            text = if (anomalies.isEmpty()) "All compliance checks passed"
                            else "${anomalies.size} compliance anomal${if (anomalies.size == 1) "y" else "ies"} detected",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (anomalies.isEmpty()) EmeraldJama else TextSubtle
                            )
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSubtle
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = BorderStrokeLight)
            Spacer(modifier = Modifier.height(16.dp))

            if (anomalies.isEmpty()) {
                // All Clear State
                ChopdaCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = EmeraldJama.copy(alpha = 0.06f),
                    borderColor = EmeraldJama.copy(alpha = 0.25f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldJama,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "Ready For Error-Free Filing",
                            fontWeight = FontWeight.Bold,
                            color = DarkNavy,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "No missing HSN codes, duplicate invoice numbers, or GSTIN checksum mismatches found. Your GSTR-1 & GSTR-3B liability schedules match 100%.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    items(anomalies, key = { it.id }) { anomaly ->
                        AnomalyCardItem(anomaly = anomaly)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            CoralGradientButton(
                text = if (anomalies.isEmpty()) "Close Health Check" else "Acknowledge & Continue",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AnomalyCardItem(anomaly: ApiPreFilingAnomalyDto) {
    val isCritical = anomaly.severity.equals("CRITICAL", ignoreCase = true)
    val isWarning = anomaly.severity.equals("WARNING", ignoreCase = true)

    val severityColor = when {
        isCritical -> RubyUdhar
        isWarning -> WarningAmber
        else -> BrandPrimaryBlue
    }

    val severityBg = severityColor.copy(alpha = 0.08f)
    val severityBorder = severityColor.copy(alpha = 0.25f)

    ChopdaCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color.White,
        borderColor = severityBorder
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Badges row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(severityBg)
                            .border(1.dp, severityBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = anomaly.severity.uppercase(),
                            color = severityColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ElevatedSlate)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = anomaly.category,
                            color = DarkNavy,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (!anomaly.refNumber.isNullOrBlank()) {
                    Text(
                        text = "Ref: ${anomaly.refNumber}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSubtle,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = anomaly.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = DarkNavy
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Description
            Text(
                text = anomaly.description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextMuted,
                    lineHeight = 18.sp
                )
            )

            // Suggestion box
            if (!anomaly.suggestion.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(BrandPrimaryBlue.copy(alpha = 0.05f))
                        .border(1.dp, BrandPrimaryBlue.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = BrandPrimaryBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = anomaly.suggestion,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = DarkNavy,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
    }
}
