package com.digitalchopda.app.presentation.auth

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalchopda.app.core.designsystem.BorderStrokeDark
import com.digitalchopda.app.core.designsystem.BorderStrokeLight
import com.digitalchopda.app.core.designsystem.BrandPrimaryBlue
import com.digitalchopda.app.core.designsystem.ChopdaCard
import com.digitalchopda.app.core.designsystem.ChopdaClothGradient
import com.digitalchopda.app.core.designsystem.CoralGradientButton
import com.digitalchopda.app.core.designsystem.CoralPrimary
import com.digitalchopda.app.core.designsystem.CoralTangerineGradient
import com.digitalchopda.app.core.designsystem.DangerRed
import com.digitalchopda.app.core.designsystem.DangerRedBg
import com.digitalchopda.app.core.designsystem.ElevatedSlate
import com.digitalchopda.app.core.designsystem.GridMeshBackground
import com.digitalchopda.app.core.designsystem.ImperialGold
import com.digitalchopda.app.core.designsystem.MidnightCardSurface
import com.digitalchopda.app.core.designsystem.TextMuted
import com.digitalchopda.app.core.designsystem.TextSubtle
import com.digitalchopda.app.core.designsystem.TextWhite
import com.digitalchopda.app.core.localization.AppLanguage
import com.digitalchopda.app.core.localization.LanguageManager
import com.digitalchopda.app.core.localization.LocalAppStrings
import com.digitalchopda.app.data.repository.AuthRepository
import kotlinx.coroutines.launch

@Composable
fun LoginRegisterScreen(
    authRepository: AuthRepository,
    languageManager: LanguageManager? = null,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val currentLang by (languageManager?.currentLanguage?.collectAsState() ?: remember { mutableStateOf(AppLanguage.GUJARATI) })

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

    fun submitAuth() {
        if (mobileNumber.isBlank()) {
            errorMessage = strings.enterUsernameOrMobileError
            return
        }
        if (password.isBlank()) {
            errorMessage = strings.enterPasswordError
            return
        }
        isLoading = true
        errorMessage = null
        scope.launch {
            val result = authRepository.loginWithPassword(mobileNumber.trim(), password)
            isLoading = false
            if (result.isSuccess) {
                onLoginSuccess()
            } else {
                errorMessage = result.exceptionOrNull()?.message ?: "Authentication failed"
            }
        }
    }

    GridMeshBackground {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            val minContentHeight = maxHeight

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = minContentHeight)
                        .widthIn(max = 440.dp)
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Top Utility Bar: Sacred Pill & Language Switcher
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Sacred Invocation Badge
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MidnightCardSurface)
                                .border(1.dp, BorderStrokeDark, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "卐 શુભ લાભ 卐",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = BrandPrimaryBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        // Language Switcher Pill
                        if (languageManager != null) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MidnightCardSurface)
                                    .border(1.dp, BorderStrokeDark, RoundedCornerShape(12.dp))
                                    .clickable {
                                        val next = languageManager.cycleNextLanguage()
                                        Toast.makeText(context, "${next.displayName} (${next.nativeName})", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🌐", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = currentLang.nativeName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Sacred Ledger Crest Emblem
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(ChopdaClothGradient)
                            .border(1.5.dp, BrandPrimaryBlue.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📖", fontSize = 26.sp)
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 2.dp, y = 2.dp)
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(ImperialGold),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("卐", fontSize = 9.sp, color = Color(0xFF1E1005), fontWeight = FontWeight.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Branding Header
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "DIGITAL ",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color = TextWhite,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = "CHOPDA",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color = BrandPrimaryBlue,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                    Text(
                        text = strings.appSubtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Segmented Top Toggle: [LOGIN] | [REGISTER]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MidnightCardSurface)
                            .border(1.dp, BorderStrokeDark, RoundedCornerShape(12.dp))
                            .padding(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (isLoginMode) CoralTangerineGradient else SolidColor(Color.Transparent))
                                .clickable {
                                    isLoginMode = true
                                    errorMessage = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.loginTab,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLoginMode) Color.White else TextMuted
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (!isLoginMode) CoralTangerineGradient else SolidColor(Color.Transparent))
                                .clickable {
                                    isLoginMode = false
                                    errorMessage = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.registerTab,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isLoginMode) Color.White else TextMuted
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Main Input Fields Card
                    ChopdaCard {
                        // Register Mode Fields (Firm Name & Owner Name)
                        AnimatedVisibility(visible = !isLoginMode) {
                            Column {
                                Text(
                                    text = strings.businessFirmNameLabel,
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
                                        focusedBorderColor = BrandPrimaryBlue,
                                        unfocusedBorderColor = BorderStrokeLight,
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = strings.ownerMerchantNameLabel,
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
                                        focusedBorderColor = BrandPrimaryBlue,
                                        unfocusedBorderColor = BorderStrokeLight,
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }

                        // Username or Mobile Number Field
                        Text(
                            text = if (isLoginMode) strings.usernameOrMobileLabel else strings.mobileNumberLabel,
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
                                        Text("🇮🇳 +91", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .height(18.dp)
                                                .background(BorderStrokeLight)
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "User",
                                        tint = BrandPrimaryBlue,
                                        modifier = Modifier.padding(start = 12.dp, end = 6.dp)
                                    )
                                }
                            },
                            placeholder = { Text("admin or 98765 43210", color = TextSubtle) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = ElevatedSlate,
                                unfocusedContainerColor = ElevatedSlate,
                                focusedBorderColor = BrandPrimaryBlue,
                                unfocusedBorderColor = BorderStrokeLight,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Password Field with Eye Visibility Toggle
                        Text(
                            text = strings.passwordLabel,
                            style = MaterialTheme.typography.labelMedium.copy(color = TextWhite)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = { Text("••••••••", color = TextSubtle) },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = BrandPrimaryBlue
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
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { submitAuth() }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = ElevatedSlate,
                                unfocusedContainerColor = ElevatedSlate,
                                focusedBorderColor = BrandPrimaryBlue,
                                unfocusedBorderColor = BorderStrokeLight,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        // Quick Demo Fill Chips (in Login Mode)
                        if (isLoginMode) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${strings.quickFillLabel}:",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextSubtle, fontSize = 11.sp)
                                )
                                // Admin chip
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ElevatedSlate)
                                        .border(1.dp, BorderStrokeLight, RoundedCornerShape(8.dp))
                                        .clickable {
                                            mobileNumber = "admin"
                                            password = "admin123"
                                            errorMessage = null
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "👑 ${strings.adminDemoUser}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = BrandPrimaryBlue,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                                // Merchant chip
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ElevatedSlate)
                                        .border(1.dp, BorderStrokeLight, RoundedCornerShape(8.dp))
                                        .clickable {
                                            mobileNumber = "9876543210"
                                            password = "admin123"
                                            errorMessage = null
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "🏪 ${strings.merchantDemoUser}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }

                        // Error Banner if any
                        AnimatedVisibility(
                            visible = errorMessage != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DangerRedBg)
                                        .border(1.dp, DangerRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("⚠️", fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = errorMessage ?: "",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = DangerRed,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

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
                                        checkedColor = BrandPrimaryBlue,
                                        uncheckedColor = BorderStrokeLight
                                    )
                                )
                                Text(
                                    text = strings.rememberMeLabel,
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextWhite)
                                )
                            }

                            if (isLoginMode) {
                                Text(
                                    text = strings.forgotPasswordLabel,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = BrandPrimaryBlue,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    modifier = Modifier.clickable {
                                        errorMessage = "OTP reset link sent to $mobileNumber"
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
                                        checkedColor = BrandPrimaryBlue,
                                        uncheckedColor = BorderStrokeLight
                                    )
                                )
                                Text(
                                    text = strings.dpdpConsentLabel,
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Full-width Gradient CTA
                        CoralGradientButton(
                            text = if (isLoginMode) strings.loginButtonText else strings.registerButtonText,
                            isLoading = isLoading,
                            onClick = { submitAuth() }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Alternate Auth Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = 1.dp,
                            color = BorderStrokeDark
                        )
                        Text(
                            text = "  ${strings.orContinueWithLabel}  ",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSubtle,
                                letterSpacing = 1.sp,
                                fontSize = 10.sp
                            )
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = 1.dp,
                            color = BorderStrokeDark
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Login via OTP
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MidnightCardSurface)
                                .border(1.dp, BorderStrokeDark, RoundedCornerShape(12.dp))
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
                                Text("📲", fontSize = 15.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.loginViaOtpLabel,
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
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MidnightCardSurface)
                                .border(1.dp, BorderStrokeDark, RoundedCornerShape(12.dp))
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
                                Text("🌐", fontSize = 15.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.googleSignInLabel,
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
    }
}
