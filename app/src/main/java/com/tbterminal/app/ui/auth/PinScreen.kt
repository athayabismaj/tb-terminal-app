package com.tbterminal.app.ui.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

internal const val MaxPinLength = 6

internal fun canSubmitPin(pin: String, isVerifying: Boolean): Boolean =
    pin.length == MaxPinLength && pin.all(Char::isDigit) && !isVerifying

@Composable
fun PinScreen(
    viewModel: AuthViewModel,
    userName: String,
    onUnlockSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val unlockState by viewModel.unlockState.collectAsState()
    var pin by remember { mutableStateOf("") }
    val isVerifying = unlockState is UnlockState.Loading

    fun submitPin() {
        if (canSubmitPin(pin, isVerifying)) viewModel.unlock(pin)
    }

    fun appendDigit(digit: String) {
        if (isVerifying || pin.length >= MaxPinLength) return
        pin += digit
        if (pin.length == MaxPinLength) viewModel.unlock(pin)
    }

    fun removeDigit() {
        if (!isVerifying && pin.isNotEmpty()) pin = pin.dropLast(1)
    }

    LaunchedEffect(unlockState) {
        when (unlockState) {
            UnlockState.Success -> {
                onUnlockSuccess()
                viewModel.resetUnlockState()
            }
            is UnlockState.Error -> pin = ""
            else -> Unit
        }
    }

    PinContent(
        userName = userName,
        pin = pin,
        unlockState = unlockState,
        onDigitClick = ::appendDigit,
        onBackspaceClick = ::removeDigit,
        onSubmitClick = ::submitPin,
        onBackToLogin = {
            viewModel.resetUnlockState()
            onBackToLogin()
        }
    )
}

@Composable
internal fun PinContent(
    userName: String,
    pin: String,
    unlockState: UnlockState,
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onSubmitClick: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val isVerifying = unlockState is UnlockState.Loading
    val errorMessage = (unlockState as? UnlockState.Error)?.message

    AuthAdaptiveScaffold(
        compactContentAlignment = Alignment.TopStart,
        compactFooter = { AccountSwitchButton(enabled = !isVerifying, onClick = onBackToLogin) }
    ) { compact ->
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(27.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Masukkan PIN",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "6 digit PIN $userName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            PinDots(pinLength = pin.length)

            if (errorMessage != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text(errorMessage, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(Modifier.height(if (compact) 96.dp else 36.dp))

            PinKeypad(
                onDigitClick = onDigitClick,
                onBackspaceClick = onBackspaceClick,
                onSubmitClick = onSubmitClick,
                pin = pin,
                isVerifying = isVerifying
            )

            if (!compact) AccountSwitchButton(enabled = !isVerifying, onClick = onBackToLogin)
        }
    }
}

@Composable
private fun AccountSwitchButton(enabled: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = enabled, modifier = Modifier.height(48.dp)) {
        Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(19.dp))
        Spacer(Modifier.size(8.dp))
        Text("Gunakan akun lain")
    }
}

@Composable
private fun PinDots(pinLength: Int) {
    Row(
        modifier = Modifier.semantics { contentDescription = "$pinLength dari $MaxPinLength digit terisi" },
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(MaxPinLength) { index ->
            val filled = index < pinLength
            val color by animateColorAsState(
                targetValue = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                label = "warna digit PIN"
            )
            val size by animateDpAsState(targetValue = if (filled) 13.dp else 11.dp, label = "ukuran digit PIN")
            Box(modifier = Modifier.size(16.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(size).background(color, CircleShape))
            }
        }
    }
}

@Composable
private fun PinKeypad(
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onSubmitClick: () -> Unit,
    pin: String,
    isVerifying: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9")
        ).forEach { numbers ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                numbers.forEach { number ->
                    PinNumberButton(number, enabled = !isVerifying, modifier = Modifier.weight(1f)) {
                        onDigitClick(number)
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PinActionButton(
                icon = Icons.AutoMirrored.Outlined.Backspace,
                contentDescription = "Hapus digit terakhir",
                enabled = pin.isNotEmpty() && !isVerifying,
                modifier = Modifier.weight(1f),
                onClick = onBackspaceClick
            )
            PinNumberButton("0", enabled = !isVerifying, modifier = Modifier.weight(1f)) { onDigitClick("0") }
            PinActionButton(
                icon = Icons.Outlined.Check,
                contentDescription = "Verifikasi PIN",
                enabled = canSubmitPin(pin, isVerifying),
                primary = true,
                loading = isVerifying,
                modifier = Modifier.weight(1f),
                onClick = onSubmitClick
            )
        }
    }
}

@Composable
private fun PinNumberButton(
    number: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
        tonalElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(number, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PinActionButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    loading: Boolean = false,
    onClick: () -> Unit
) {
    val emphasized = primary && (enabled || loading)
    val containerColor = when {
        emphasized -> MaterialTheme.colorScheme.primary
        primary -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
    }
    val iconColor = when {
        emphasized -> MaterialTheme.colorScheme.onPrimary
        primary -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        contentColor = iconColor,
        border = if (emphasized) null else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
        }
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
            } else {
                Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(25.dp))
            }
        }
    }
}
