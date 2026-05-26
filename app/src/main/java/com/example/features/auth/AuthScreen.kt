package com.example.features.auth

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.components.BrandButton
import com.example.components.BrandTextField
import com.example.core.di.ServiceLocator
import com.example.ui.theme.BrandBackground
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandTextMuted
import com.example.ui.theme.BrandTextPrimary
import androidx.compose.ui.res.stringResource
import com.example.R
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val authRepo = ServiceLocator.authRepository

    var isLoginTab by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    Scaffold(
        containerColor = BrandBackground,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Editorial Top Branding
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "WasetPlus",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandPrimary,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isLoginTab) androidx.compose.ui.res.stringResource(R.string.auth_welcome_back) else androidx.compose.ui.res.stringResource(R.string.auth_join_network),
                    fontSize = 15.sp,
                    color = BrandTextMuted,
                    textAlign = TextAlign.Center
                )
            }

            // Centralized Content Card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(24.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Segmented Sign-In / Sign-Up tab selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(BrandBackground, RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(
                                    if (isLoginTab) Color.White else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .align(Alignment.CenterVertically)
                                .padding(vertical = 4.dp)
                                .BoxClickable { isLoginTab = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = androidx.compose.ui.res.stringResource(R.string.login),
                                fontSize = 14.sp,
                                fontWeight = if (isLoginTab) FontWeight.Bold else FontWeight.Medium,
                                color = if (isLoginTab) BrandTextPrimary else BrandTextMuted
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(
                                    if (!isLoginTab) Color.White else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .align(Alignment.CenterVertically)
                                .padding(vertical = 4.dp)
                                .BoxClickable { isLoginTab = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = androidx.compose.ui.res.stringResource(R.string.register),
                                fontSize = 14.sp,
                                fontWeight = if (!isLoginTab) FontWeight.Bold else FontWeight.Medium,
                                color = if (!isLoginTab) BrandTextPrimary else BrandTextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Crossfade(targetState = isLoginTab, label = "form_animation") { isLogin ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (!isLogin) {
                                BrandTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    placeholder = androidx.compose.ui.res.stringResource(R.string.full_name_label)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            BrandTextField(
                                value = email,
                                onValueChange = { email = it },
                                placeholder = androidx.compose.ui.res.stringResource(R.string.email_address_label),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            BrandTextField(
                                value = password,
                                onValueChange = { password = it },
                                placeholder = androidx.compose.ui.res.stringResource(R.string.password_label),
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    if (isLoading) {
                        CircularProgressIndicator(color = BrandPrimary)
                    } else {
                        BrandButton(
                            text = if (isLoginTab) androidx.compose.ui.res.stringResource(R.string.sign_in_button) else androidx.compose.ui.res.stringResource(R.string.create_account_button),
                            onClick = {
                                if (email.isBlank() || password.isBlank() || (!isLoginTab && name.isBlank())) {
                                    Toast.makeText(context, context.getString(R.string.fields_required_error), Toast.LENGTH_SHORT).show()
                                    return@BrandButton
                                }
                                isLoading = true
                                coroutineScope.launch {
                                    val result = if (isLoginTab) {
                                        authRepo.signIn(email, password)
                                    } else {
                                        authRepo.signUp(email, password, name)
                                    }
                                    isLoading = false
                                    result.onSuccess {
                                        Toast.makeText(context, context.getString(R.string.welcome_user_toast, it.name), Toast.LENGTH_SHORT).show()
                                        onAuthSuccess()
                                    }.onFailure {
                                        Toast.makeText(context, context.getString(R.string.auth_failed_error, it.message ?: ""), Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // Footer / Bottom back navigate option
            TextButton(
                onClick = onBack,
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.go_back_button),
                    fontSize = 14.sp,
                    color = BrandTextMuted,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// Inline custom clickable representation to avoid unrequested visual tabs or complexity
@Composable
private fun Modifier.BoxClickable(onClick: () -> Unit): Modifier {
    return this.background(Color.Transparent)
        .padding(0.dp)
        .wrapContentSize()
        .then(
            Modifier.background(Color.Transparent)
        )
        .clickable(
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
}
