package com.digitalchopda.app.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalchopda.app.core.designsystem.BorderStrokeDark
import com.digitalchopda.app.core.designsystem.BorderStrokeLight
import com.digitalchopda.app.core.designsystem.ChopdaCard
import com.digitalchopda.app.core.designsystem.CoralGradientButton
import com.digitalchopda.app.core.designsystem.CoralPrimary
import com.digitalchopda.app.core.designsystem.CoralTangerineGradient
import com.digitalchopda.app.core.designsystem.DangerRed
import com.digitalchopda.app.core.designsystem.DeepObsidianCanvas
import com.digitalchopda.app.core.designsystem.ElectricBrandCyan
import com.digitalchopda.app.core.designsystem.ElevatedSlate
import com.digitalchopda.app.core.designsystem.GridMeshBackground
import com.digitalchopda.app.core.designsystem.MidnightCardSurface
import com.digitalchopda.app.core.designsystem.TextMuted
import com.digitalchopda.app.core.designsystem.TextSubtle
import com.digitalchopda.app.core.designsystem.TextWhite
import com.digitalchopda.app.data.repository.AuthRepository
import kotlinx.coroutines.launch

@Composable
fun LoginRegisterScreen(
    authRepository: AuthRepository,
    onLoginSuccess: () -> Unit
) {
    var isLoginMode by remember { mutableStateOf(true) }
    var mobileNumber by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("admin123") }
    var firmName by remember { mutableStateOf("Sharma Traders") }
    var ownerName by remember { mutableStateOf("Amit Sharma") }
    var rememberMe by remember { mutableStateOf(true) }
    var dpdpConsent by remember { mutableStateOf(true) }
    var passwordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    GridMeshBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Branding Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "DIGITAL ",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = ElectricBrandCyan,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                Text(
                    text = "CHOPDA",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = CoralPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }
            Text(
                text = "Sovereign Khata, Billing & GST Ledger",
                style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Segmented Top Toggle: [LOGIN] | [REGISTER]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MidnightCardSurface)
                    .border(1.dp, BorderStrokeDark, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isLoginMode) CoralTangerineGradient else SolidColor(Color.Transparent))
                        .clickable {
                            isLoginMode = true
                            errorMessage = null
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "LOGIN",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isLoginMode) TextWhite else TextMuted
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!isLoginMode) CoralTangerineGradient else SolidColor(Color.Transparent))
                        .clickable {
                            isLoginMode = false
                            errorMessage = null
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "REGISTER",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (!isLoginMode) TextWhite else TextMuted
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Input Fields Card
            ChopdaCard {
                // If Register: Firm Name & Owner Name
                AnimatedVisibility(visible = !isLoginMode) {
                    Column {
                        Text(
                            text = "Business / Firm Name",
                            style = MaterialTheme.typography.labelMedium.copy(color = TextWhite)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = firmName,
                            onValueChange = { firmName = it },
                            placeholder = { Text("e.g. Sharma Traders", color = TextSubtle) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = ElevatedSlate,
                                unfocusedContainerColor = ElevatedSlate,
                                focusedBorderColor = ElectricBrandCyan,
                                unfocusedBorderColor = BorderStrokeLight,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Owner / Merchant Name",
                            style = MaterialTheme.typography.labelMedium.copy(color = TextWhite)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = ownerName,
                            onValueChange = { ownerName = it },
                            placeholder = { Text("e.g. Amit Sharma", color = TextSubtle) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = ElevatedSlate,
                                unfocusedContainerColor = ElevatedSlate,
                                focusedBorderColor = ElectricBrandCyan,
                                unfocusedBorderColor = BorderStrokeLight,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                // Username or Mobile Number Field
                Text(
                    text = if (isLoginMode) "Username or Mobile Number" else "Mobile Number",
                    style = MaterialTheme.typography.labelMedium.copy(color = TextWhite)
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = mobileNumber,
                    onValueChange = { mobileNumber = it },
                    leadingIcon = {
                        val isDigitsOnly = mobileNumber.all { it.isDigit() } && mobileNumber.isNotEmpty()
                        if (isDigitsOnly) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 12.dp, end = 6.dp)
                            ) {
                                Text("🇮🇳 +91", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(20.dp)
                                        .background(BorderStrokeLight)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "User",
                                tint = ElectricBrandCyan,
                                modifier = Modifier.padding(start = 12.dp, end = 6.dp)
                            )
                        }
                    },
                    placeholder = { Text("admin or 98765 43210", color = TextSubtle) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ElevatedSlate,
                        unfocusedContainerColor = ElevatedSlate,
                        focusedBorderColor = ElectricBrandCyan,
                        unfocusedBorderColor = BorderStrokeLight,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password Field with Eye Visibility Toggle
                Text(
                    text = "Password",
                    style = MaterialTheme.typography.labelMedium.copy(color = TextWhite)
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("Enter your password", color = TextSubtle) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = ElectricBrandCyan
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle password visibility",
                                tint = TextMuted
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ElevatedSlate,
                        unfocusedContainerColor = ElevatedSlate,
                        focusedBorderColor = ElectricBrandCyan,
                        unfocusedBorderColor = BorderStrokeLight,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                // Error Message if any
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(color = DangerRed)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Remember Me & Forgot Password Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = CoralPrimary,
                                uncheckedColor = BorderStrokeLight
                            )
                        )
                        Text(
                            text = "Remember Me",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextWhite)
                        )
                    }

                    if (isLoginMode) {
                        Text(
                            text = "Forgot Password?",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = ElectricBrandCyan,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.clickable {
                                errorMessage = "OTP reset link sent to +91 $mobileNumber"
                            }
                        )
                    }
                }

                // DPDP Consent for Register
                if (!isLoginMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = dpdpConsent,
                            onCheckedChange = { dpdpConsent = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = CoralPrimary,
                                uncheckedColor = BorderStrokeLight
                            )
                        )
                        Text(
                            text = "I agree to Terms & DPDP 2023 Data Privacy Rules",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Full-width Gradient CTA: LOG IN >
                CoralGradientButton(
                    text = if (isLoginMode) "LOG IN  ›" else "CREATE ACCOUNT  ›",
                    isLoading = isLoading,
                    onClick = {
                        if (mobileNumber.isBlank()) {
                            errorMessage = "Please enter username or mobile number"
                            return@CoralGradientButton
                        }
                        if (password.isBlank()) {
                            errorMessage = "Please enter password"
                            return@CoralGradientButton
                        }
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            val result = authRepository.loginWithPassword(mobileNumber, password)
                            isLoading = false
                            if (result.isSuccess) {
                                onLoginSuccess()
                            } else {
                                errorMessage = result.exceptionOrNull()?.message ?: "Login failed"
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Alternate Auth Row
            Text(
                text = "OR CONTINUE WITH",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextSubtle,
                    letterSpacing = 1.2.sp
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Login via OTP
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MidnightCardSurface)
                        .border(1.dp, BorderStrokeLight, RoundedCornerShape(12.dp))
                        .clickable {
                            isLoading = true
                            scope.launch {
                                val res = authRepository.verifyOtp(mobileNumber, "1234")
                                isLoading = false
                                if (res.isSuccess) onLoginSuccess()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📲", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Login via OTP",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = TextWhite,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                // Sign in with Google
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MidnightCardSurface)
                        .border(1.dp, BorderStrokeLight, RoundedCornerShape(12.dp))
                        .clickable {
                            isLoading = true
                            scope.launch {
                                val res = authRepository.loginWithPassword("9876543210", "google_sso")
                                isLoading = false
                                if (res.isSuccess) onLoginSuccess()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🌐", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Google Sign-In",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = TextWhite,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
    }
}
