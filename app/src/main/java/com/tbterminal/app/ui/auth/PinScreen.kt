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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.ui.theme.TbGreen
import com.tbterminal.app.ui.theme.TbGreenDark
import com.tbterminal.app.ui.theme.TbGreenLight
import com.tbterminal.app.ui.theme.TbText
import com.tbterminal.app.ui.theme.TbTextMuted

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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            
            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.size(56.dp).padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                color = TbGreenLight.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, Color(0xFFC2D9CC))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = TbGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            val initials = userName.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
            Surface(
                modifier = Modifier.padding(bottom = 16.dp),
                shape = RoundedCornerShape(999.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(24.dp).background(TbGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(initials, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(userName, color = TbText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    
                    Box(modifier = Modifier.padding(horizontal = 8.dp).width(1.dp).height(12.dp).background(Color(0xFFE2E8F0)))
                    
                    Surface(
                        color = TbGreenLight.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text("Pemilik Toko", color = TbGreen, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Medium)
                    }
                }
            }

            Text(
                text = "Masukkan PIN",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Masukkan 6 digit PIN untuk otentikasi kasir",
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            PinDots(pinLength = pin.length)

            if (errorMessage != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
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

            Spacer(Modifier.height(if (compact) 48.dp else 36.dp))

            PinKeypad(
                onDigitClick = onDigitClick,
                onBackspaceClick = onBackspaceClick,
                onSubmitClick = onSubmitClick,
                pin = pin,
                isVerifying = isVerifying
            )

            if (!compact) {
                Spacer(Modifier.height(24.dp))
                AccountSwitchButton(enabled = !isVerifying, onClick = onBackToLogin)
            }
        }
    }
}

@Composable
private fun AccountSwitchButton(enabled: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = enabled, modifier = Modifier.height(48.dp)) {
        Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(18.dp), tint = TbGreenDark)
        Spacer(Modifier.size(6.dp))
        Text("Gunakan akun lain", color = TbGreenDark, fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
            val dotColor by animateColorAsState(targetValue = if (filled) TbGreen else Color.White, label = "warna digit PIN")
            val borderColor by animateColorAsState(targetValue = if (filled) TbGreenLight else Color(0xFFCBD5E1), label = "warna border PIN")
            val borderWidth by animateDpAsState(targetValue = if (filled) 4.dp else 2.dp, label = "lebar border PIN")
            
            Surface(
                modifier = Modifier.size(14.dp),
                shape = CircleShape,
                color = dotColor,
                border = BorderStroke(borderWidth, borderColor)
            ) {}
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
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
                isBackspace = true,
                modifier = Modifier.weight(1f),
                onClick = onBackspaceClick
            )
            PinNumberButton("0", enabled = !isVerifying, modifier = Modifier.weight(1f)) { onDigitClick("0") }
            PinActionButton(
                icon = Icons.Outlined.Check,
                contentDescription = "Verifikasi PIN",
                enabled = canSubmitPin(pin, isVerifying),
                isBackspace = false,
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
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        contentColor = Color(0xFF1E293B),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        shadowElevation = 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(number, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PinActionButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    isBackspace: Boolean,
    loading: Boolean = false,
    onClick: () -> Unit
) {
    val containerColor = if (isBackspace) Color(0xFFF1F5F9) else TbGreen
    val iconColor = if (isBackspace) Color(0xFF475569) else Color.White
    
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        contentColor = iconColor,
        border = if (isBackspace) BorderStroke(1.dp, Color(0xFFE2E8F0)) else null,
        shadowElevation = if (isBackspace) 0.dp else 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(28.dp))
            }
        }
    }
}
