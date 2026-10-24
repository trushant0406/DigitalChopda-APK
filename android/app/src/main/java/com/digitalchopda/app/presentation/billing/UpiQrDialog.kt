package com.digitalchopda.app.presentation.billing

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalchopda.app.core.designsystem.BrandPrimaryBlue
import com.digitalchopda.app.core.designsystem.SuccessGreen
import com.digitalchopda.app.core.designsystem.TextMuted
import com.digitalchopda.app.core.designsystem.TextWhite
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.util.Locale

import com.digitalchopda.app.core.designsystem.BorderStrokeDark
import com.digitalchopda.app.core.designsystem.ChopdaModalDialog

@Composable
fun UpiQrDialog(
    upiVpa: String,
    firmName: String,
    amount: Double,
    invoiceNumber: String,
    onDismiss: () -> Unit,
    onPaymentConfirmed: () -> Unit
) {
    val upiUri = remember(upiVpa, firmName, amount, invoiceNumber) {
        val cleanVpa = upiVpa.ifBlank { "merchant@upi" }
        val encodedFirm = Uri.encode(firmName)
        val formattedAmount = String.format(Locale.US, "%.2f", amount)
        val note = Uri.encode("Bill $invoiceNumber")
        "upi://pay?pa=$cleanVpa&pn=$encodedFirm&am=$formattedAmount&cu=INR&tn=$note"
    }

    val qrBitmap = remember(upiUri) {
        generateQrBitmap(upiUri, size = 512)
    }

    androidx.compose.runtime.DisposableEffect(qrBitmap) {
        onDispose {
            try {
                if (qrBitmap != null && !qrBitmap.isRecycled) {
                    qrBitmap.recycle()
                }
            } catch (_: Exception) {}
        }
    }

    ChopdaModalDialog(
        onDismissRequest = onDismiss,
        title = "Instant Counter UPI",
        subtitle = "Invoice $invoiceNumber",
        icon = Icons.Default.QrCode2,
        iconTint = BrandPrimaryBlue,
        widthFraction = 0.94f,
        maxHeightFraction = 0.88f,
        scrollable = true,
        buttons = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPaymentConfirmed,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuccessGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PAYMENT RECEIVED",
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
                    Text(text = "CANCEL", color = TextMuted, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = firmName,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            )
            Text(
                text = "VPA: ${upiVpa.ifBlank { "merchant@upi" }}",
                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // White paper container for high optical contrast scanner reliability
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFDDE1E6), RoundedCornerShape(16.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "UPI Payment QR",
                        modifier = Modifier.size(196.dp)
                    )
                } else {
                    Text("QR Generation Error", color = Color.Red, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "₹ %.2f".format(amount),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = BrandPrimaryBlue
                )
            )
            Text(
                text = "Scan with any UPI app (GPay, PhonePe, Paytm)",
                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
            )
        }
    }
}

private fun generateQrBitmap(content: String, size: Int): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (_: Exception) {
        null
    }
}
