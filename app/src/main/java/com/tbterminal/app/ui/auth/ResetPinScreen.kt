package com.tbterminal.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.ui.theme.Brand
import com.tbterminal.app.ui.theme.FieldBorder
import com.tbterminal.app.ui.theme.FieldErrorColor
import com.tbterminal.app.ui.theme.Ink
import com.tbterminal.app.ui.theme.KawanSans
import com.tbterminal.app.ui.theme.OnBrand
import com.tbterminal.app.ui.theme.RadiusControl
import com.tbterminal.app.ui.theme.SurfaceWhite

@Composable
fun ResetPinScreen(
    viewModel: AuthViewModel,
    onResetSuccess: () -> Unit,
    onCancelToLogin: () -> Unit
) {
    val resetPinState by viewModel.resetPinState.collectAsState()
    val requiresPasswordPrompt = !viewModel.hasVerifiedPasswordForPinReset

    var currentPassword by rememberSaveable { mutableStateOf("") }
    var newPin by rememberSaveable { mutableStateOf("") }
    var confirmPin by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(resetPinState) {
        if (resetPinState is ResetPinState.Success) {
            onResetSuccess()
            viewModel.resetResetPinState()
        }
    }

    val isLoading = resetPinState is ResetPinState.Loading
    val errorMessage = (resetPinState as? ResetPinState.Error)?.message

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brand),
        contentAlignment = Alignment.TopCenter
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .widthIn(max = 390.dp)
                .fillMaxSize()
        ) {
            val sheetMinHeight = (maxHeight - 232.dp).coerceAtLeast(0.dp)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(232.dp)
                        .clipToBounds()
                ) {
                    RingsPattern(
                        radii = listOf(50.dp, 95.dp, 140.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .wrapContentSize(align = Alignment.TopEnd, unbounded = true)
                            .offset(x = 80.dp, y = (-80).dp)
                            .size(300.dp)
                    )
                    DotsPattern(
                        dotRadius = 1.5.dp,
                        step = 20.dp,
                        maskAtBottomRight = true,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(width = 170.dp, height = 90.dp)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 28.dp, top = 64.dp, end = 28.dp)
                    ) {
                        HeroTitle(
                            text = "Atur PIN Baru",
                            fontSize = 32.sp,
                            lineBoxHeight = (32f * 1.1f).dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        HeroSubtitle(
                            text = "Buat 6 digit PIN baru untuk otentikasi terminal kasir",
                            fontSize = 15.sp,
                            lineBoxHeight = (15f * 1.21f).dp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = sheetMinHeight)
                        .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .background(SurfaceWhite)
                        .navigationBarsPadding()
                        .padding(28.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (requiresPasswordPrompt) {
                            OutlinedTextField(
                                value = currentPassword,
                                onValueChange = {
                                    currentPassword = it
                                    if (resetPinState is ResetPinState.Error) {
                                        viewModel.resetResetPinState()
                                    }
                                },
                                label = { Text("Kata sandi saat ini") },
                                singleLine = true,
                                enabled = !isLoading,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Next
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Brand,
                                    unfocusedBorderColor = FieldBorder,
                                    focusedLabelColor = Brand
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reset-pin-password")
                            )
                        }

                        OutlinedTextField(
                            value = newPin,
                            onValueChange = { raw ->
                                newPin = raw.filter(Char::isDigit).take(MaxPinLength)
                                if (resetPinState is ResetPinState.Error) {
                                    viewModel.resetResetPinState()
                                }
                            },
                            label = { Text("PIN baru (4-6 digit)") },
                            singleLine = true,
                            enabled = !isLoading,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Next
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Brand,
                                unfocusedBorderColor = FieldBorder,
                                focusedLabelColor = Brand
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reset-pin-new")
                        )

                        OutlinedTextField(
                            value = confirmPin,
                            onValueChange = { raw ->
                                confirmPin = raw.filter(Char::isDigit).take(MaxPinLength)
                                if (resetPinState is ResetPinState.Error) {
                                    viewModel.resetResetPinState()
                                }
                            },
                            label = { Text("Konfirmasi PIN baru") },
                            singleLine = true,
                            enabled = !isLoading,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Brand,
                                unfocusedBorderColor = FieldBorder,
                                focusedLabelColor = Brand
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reset-pin-confirm")
                        )

                        if (errorMessage != null) {
                            Text(
                                text = errorMessage,
                                color = FieldErrorColor,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.testTag("reset-pin-error")
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.resetPinAfterLogin(
                                    newPin = newPin,
                                    confirmPin = confirmPin,
                                    currentPasswordInput = if (requiresPasswordPrompt) currentPassword else null
                                )
                            },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(RadiusControl),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Brand,
                                contentColor = OnBrand
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("reset-pin-submit")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = OnBrand,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Simpan PIN Baru",
                                    fontFamily = KawanSans,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clickable(
                                    enabled = !isLoading,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        viewModel.cancelForgotPinFlow()
                                        onCancelToLogin()
                                    }
                                )
                                .testTag("reset-pin-cancel"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Kembali ke Login",
                                fontFamily = KawanSans,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Ink
                            )
                        }
                    }
                }
            }
        }
    }
}
