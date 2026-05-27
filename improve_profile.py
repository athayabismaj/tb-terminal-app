import os

content = """package com.tbterminal.app.ui.settings

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.tbterminal.app.ui.dashboard.DashboardBrandGreen
import com.tbterminal.app.ui.dashboard.DashboardBrandGreenDark
import com.tbterminal.app.ui.dashboard.DashboardTextPrimary
import com.tbterminal.app.ui.dashboard.DashboardTextSecondary

@Composable
fun SharedProfileScreen(
    userName: String,
    role: String,
    modifier: Modifier = Modifier
) {
    var nameState by remember { mutableStateOf(userName) }
    var usernameState by remember { mutableStateOf(userName.lowercase().replace(" ", "")) }
    var emailState by remember { mutableStateOf(userName.lowercase().replace(" ", "") + "@tbterminal.com") }
    
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(1200) // Simulate network/loading delay
        isLoading = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(bottom = 32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            if (isLoading) {
                ProfileSkeleton()
            } else {
                ProfileContent(
                    nameState = nameState,
                    onNameChange = { nameState = it },
                    usernameState = usernameState,
                    onUsernameChange = { usernameState = it },
                    emailState = emailState,
                    onEmailChange = { emailState = it },
                    role = role
                )
            }
        }
    }
}

@Composable
private fun ProfileContent(
    nameState: String,
    onNameChange: (String) -> Unit,
    usernameState: String,
    onUsernameChange: (String) -> Unit,
    emailState: String,
    onEmailChange: (String) -> Unit,
    role: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Banner and Profile Picture overlapping
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            // Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(DashboardBrandGreen, DashboardBrandGreenDark)
                        )
                    )
            )

            // Profile Picture
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(4.dp) // Border thickness effect
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(DashboardBrandGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = nameState.firstOrNull()?.uppercase() ?: "U",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DashboardBrandGreenDark
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = nameState,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = DashboardTextPrimary
        )
        
        Text(
            text = role.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = DashboardBrandGreenDark,
            modifier = Modifier
                .padding(top = 8.dp)
                .background(DashboardBrandGreen.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Form Fields
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            OutlinedTextField(
                value = nameState,
                onValueChange = onNameChange,
                label = { Text("Nama Lengkap") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DashboardBrandGreen,
                    focusedLabelColor = DashboardBrandGreen
                )
            )
            
            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = usernameState,
                onValueChange = onUsernameChange,
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DashboardBrandGreen,
                    focusedLabelColor = DashboardBrandGreen
                )
            )
            
            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = emailState,
                onValueChange = onEmailChange,
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DashboardBrandGreen,
                    focusedLabelColor = DashboardBrandGreen
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = role.uppercase(),
                onValueChange = {},
                label = { Text("Role") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = Color(0xFFE2E8F0),
                    disabledTextColor = DashboardTextSecondary
                )
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
        
        Button(
            onClick = { /* TODO: Save Profile */ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, bottom = 32.dp)
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DashboardBrandGreenDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Simpan Perubahan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun ProfileSkeleton() {
    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(brush)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(brush)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .width(150.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(brush)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(brush)
        )

        Spacer(modifier = Modifier.height(40.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(brush)
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, bottom = 32.dp)
                .height(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(brush)
        )
    }
}
"""

with open("E:/tbterminalapp/app/src/main/java/com/tbterminal/app/ui/settings/SharedProfileScreen.kt", "w", encoding="utf-8") as f:
    f.write(content)
print("Updated SharedProfileScreen with scroll, skeleton, and modern UI.")
