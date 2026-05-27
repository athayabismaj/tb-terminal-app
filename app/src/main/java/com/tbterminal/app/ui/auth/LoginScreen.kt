package com.tbterminal.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LoginBackground = Color(0xFFF8F9FF)
private val LoginOnSurface = Color(0xFF121C2A)
private val LoginPrimary = Color(0xFF006948)
private val LoginOnPrimary = Color.White
private val LoginSurfaceTint = Color(0xFF006C4A)
private val LoginPrimaryFixedDim = Color(0xFF68DBA9)
private val LoginPrimaryFixed = Color(0xFF85F8C4)
private val LoginSecondaryFixed = Color(0xFFFFDDB8)
private val LoginSurfaceContainerHigh = Color(0xFFDEE9FC)
private val LoginSurface = Color(0xFFF8F9FF)
private val LoginOutline = Color(0xFF6D7A72)
private val LoginOutlineVariant = Color(0xFFBCCAC0)
private val LoginSecondaryContainer = Color(0xFFFEA619)
private val LoginOnSecondaryContainer = Color(0xFF684000)
private val LoginError = Color(0xFFB3261E)

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: (role: String, name: String) -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            val state = authState as AuthState.Success
            onLoginSuccess(state.role, state.name)
            viewModel.resetState()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(LoginBackground)
    ) {
        val isWide = maxWidth >= 840.dp

        if (isWide) {
            Row(modifier = Modifier.fillMaxSize()) {
                LoginBrandPanel(
                    modifier = Modifier
                        .weight(5f)
                        .fillMaxHeight()
                )
                LoginFormPanel(
                    username = username,
                    password = password,
                    authState = authState,
                    onUsernameChange = { username = it },
                    onPasswordChange = { password = it },
                    onSubmit = { viewModel.login(username, password) },
                    modifier = Modifier
                        .weight(7f)
                        .fillMaxHeight(),
                    isWide = true
                )
            }
        } else {
            LoginFormPanel(
                username = username,
                password = password,
                authState = authState,
                onUsernameChange = { username = it },
                onPasswordChange = { password = it },
                onSubmit = { viewModel.login(username, password) },
                modifier = Modifier.fillMaxSize(),
                isWide = false
            )
        }
    }
}

@Composable
private fun LoginBrandPanel(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(
            brush = Brush.linearGradient(
                colors = listOf(LoginPrimary, LoginPrimary, LoginSurfaceTint)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 48.dp)
                ) {
                    LoginLogo(
                        containerColor = LoginSurface,
                        iconColor = LoginPrimary,
                        size = 48.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "TB Terminal",
                        color = LoginOnPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "Selamat Datang di TB Terminal",
                    color = LoginOnPrimary,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 56.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "Sistem manajemen penjualan bahan bangunan untuk stok, transaksi, dan laporan operasional harian.",
                    color = LoginPrimaryFixedDim,
                    fontSize = 18.sp,
                    lineHeight = 28.sp
                )
            }

            TodayActivityPanel()
        }
    }
}

@Composable
private fun TodayActivityPanel() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(LoginPrimary.copy(alpha = 0.4f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(24.dp)
    ) {
        Column {
            Text(
                text = "AKTIVITAS HARI INI",
                color = LoginPrimaryFixed,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LoginMetricTile(
                    modifier = Modifier.weight(1f),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = LoginSecondaryFixed,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = "Transaksi"
                ) {
                    Text(
                        text = "1,284",
                        color = LoginOnPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                LoginMetricTile(
                    modifier = Modifier.weight(1f),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = LoginPrimaryFixed,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = "Sistem"
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(LoginPrimaryFixed)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Online",
                            color = LoginOnPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginMetricTile(
    icon: @Composable () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                color = LoginSurfaceContainerHigh,
                fontSize = 12.sp
            )
            content()
        }
    }
}

@Composable
private fun LoginFormPanel(
    username: String,
    password: String,
    authState: AuthState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    isWide: Boolean
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .background(LoginSurface)
            .padding(horizontal = 32.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = if (isWide) Alignment.Start else Alignment.CenterHorizontally
        ) {
            if (!isWide) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 40.dp)
                ) {
                    LoginLogo(
                        containerColor = LoginPrimary,
                        iconColor = LoginOnPrimary,
                        size = 40.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "TB Terminal",
                        color = LoginPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                text = "Masuk ke Akun Anda",
                color = LoginOnSurface,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                textAlign = if (isWide) TextAlign.Start else TextAlign.Center
            )
            Text(
                text = "Masukkan kredensial Anda untuk melanjutkan ke dashboard operasional.",
                color = LoginOutline,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 36.dp),
                textAlign = if (isWide) TextAlign.Start else TextAlign.Center
            )

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = { Text("Username") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = if (passwordVisible) {
                                    "Sembunyikan password"
                                } else {
                                    "Tampilkan password"
                                },
                                tint = LoginPrimary
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) {
                        androidx.compose.ui.text.input.VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(8.dp)
                )

                if (authState is AuthState.Error) {
                    Text(
                        text = authState.message,
                        color = LoginError,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = authState !is AuthState.Loading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LoginSecondaryContainer,
                        contentColor = LoginOnSecondaryContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (authState is AuthState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = LoginOnSecondaryContainer,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Masuk Sistem",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Text(
            text = "TB Terminal System\nv1.0.0",
            color = LoginOutlineVariant,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            textAlign = if (isWide) TextAlign.Start else TextAlign.Center,
            modifier = Modifier
                .align(if (isWide) Alignment.BottomStart else Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        )
    }
}

@Composable
private fun LoginLogo(
    containerColor: Color,
    iconColor: Color,
    size: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Build,
            contentDescription = null,
            tint = iconColor
        )
    }
}
