package com.tbterminal.app.ui.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val MaxPinLength = 6

private val PinBackground = Color(0xFFF4FAFD)
private val PinPrimary = Color(0xFF00694C)
private val PinPrimaryContainer = Color(0xFF008560)
private val PinOnSurface = Color(0xFF161D1F)
private val PinOutline = Color(0xFF6D7A73)
private val PinSurfaceContainerHighest = Color(0xFFDDE4E6)
private val PinError = Color(0xFFBA1A1A)
private val PinErrorContainer = Color(0xFFFFDAD6)
private val PinActionSurface = Color(0xFFEEF5F7)

@Composable
fun PinScreen(
    viewModel: AuthViewModel,
    userName: String,
    onUnlockSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val unlockState by viewModel.unlockState.collectAsState()
    val errorMessage = (unlockState as? UnlockState.Error)?.message
    var pin by remember { mutableStateOf("") }
    val isVerifying = unlockState is UnlockState.Loading

    fun submitPin() {
        if (!isVerifying && pin.length == MaxPinLength) {
            viewModel.unlock(pin)
        }
    }

    fun appendDigit(digit: String) {
        if (isVerifying || pin.length >= MaxPinLength) {
            return
        }

        pin += digit
        if (pin.length == MaxPinLength) {
            viewModel.unlock(pin)
        }
    }

    fun removeDigit() {
        if (!isVerifying && pin.isNotEmpty()) {
            pin = pin.dropLast(1)
        }
    }

    LaunchedEffect(unlockState) {
        when (unlockState) {
            UnlockState.Success -> {
                onUnlockSuccess()
                viewModel.resetUnlockState()
            }

            is UnlockState.Error -> {
                pin = ""
            }

            else -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PinBackground)
            .padding(horizontal = 32.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(PinPrimary, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Verifikasi Keamanan",
                color = PinOnSurface,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Masukkan 6 digit PIN untuk $userName",
                color = PinOutline,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            PinDots(pinLength = pin.length)

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PinErrorContainer
                ) {
                    Text(
                        text = errorMessage,
                        color = PinError,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            PinKeypad(
                onDigitClick = ::appendDigit,
                onBackspaceClick = ::removeDigit,
                onSubmitClick = ::submitPin,
                isVerifying = isVerifying
            )

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable(enabled = !isVerifying) {
                        viewModel.resetUnlockState()
                        onBackToLogin()
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = PinOutline,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "GANTI AKUN",
                    color = PinOutline,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "TB Terminal System  |  v1.0.0",
                color = PinOutline.copy(alpha = 0.72f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PinDots(
    pinLength: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(MaxPinLength) { index ->
            val filled = index < pinLength
            val dotColor by animateColorAsState(
                targetValue = if (filled) PinPrimary else PinSurfaceContainerHighest,
                animationSpec = tween(durationMillis = 220),
                label = "pinDotColor"
            )
            val dotSize by animateDpAsState(
                targetValue = if (filled) 14.dp else 12.dp,
                animationSpec = tween(durationMillis = 220),
                label = "pinDotSize"
            )

            Box(
                modifier = Modifier.size(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .background(dotColor, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun PinKeypad(
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onSubmitClick: () -> Unit,
    isVerifying: Boolean
) {
    Column(
        modifier = Modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PinNumberRow(numbers = listOf("1", "2", "3"), onDigitClick = onDigitClick)
        PinNumberRow(numbers = listOf("4", "5", "6"), onDigitClick = onDigitClick)
        PinNumberRow(numbers = listOf("7", "8", "9"), onDigitClick = onDigitClick)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PinActionButton(
                icon = Icons.Default.Clear,
                backgroundColor = PinActionSurface,
                iconColor = PinOutline,
                contentDescription = "Hapus digit",
                modifier = Modifier.weight(1f),
                onClick = onBackspaceClick
            )
            PinNumberButton(
                number = "0",
                modifier = Modifier.weight(1f),
                onClick = { onDigitClick("0") }
            )
            PinActionButton(
                icon = Icons.Default.Check,
                backgroundColor = PinPrimaryContainer,
                iconColor = Color.White,
                contentDescription = "Verifikasi PIN",
                modifier = Modifier.weight(1f),
                isPrimary = true,
                isLoading = isVerifying,
                onClick = onSubmitClick
            )
        }
    }
}

@Composable
private fun PinNumberRow(
    numbers: List<String>,
    onDigitClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        numbers.forEach { number ->
            PinNumberButton(
                number = number,
                modifier = Modifier.weight(1f),
                onClick = { onDigitClick(number) }
            )
        }
    }
}

@Composable
private fun PinNumberButton(
    number: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = number,
                color = PinOnSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PinActionButton(
    icon: ImageVector,
    backgroundColor: Color,
    iconColor: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        shadowElevation = if (isPrimary) 8.dp else 0.dp,
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = iconColor,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
