package com.kuliahku.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import com.kuliahku.R
import com.kuliahku.ui.components.AnimatedEntry
import com.kuliahku.ui.components.KuliahkuLogo
import com.kuliahku.ui.theme.cardShadow

@Composable
fun WelcomeScreen(
    onStart: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = maxOf(0.dp, padding.calculateTopPadding()),
                    bottom = maxOf(0.dp, padding.calculateBottomPadding())
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            AnimatedEntry(delay = 100) {
                KuliahkuLogo(modifier = Modifier.height(32.dp))
            }
            
            Spacer(modifier = Modifier.weight(0.5f))
            
            // Ilustrasi Utama dibuat Full Width (mentok kanan kiri)
            AnimatedEntry(delay = 200) {
                Image(
                    painter = painterResource(id = R.drawable.img_welcome), // Menggunakan ilustrasi asli yang telah direstorasi
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(
                                constraints.copy(
                                    maxWidth = constraints.maxWidth + 48.dp.roundToPx()
                                )
                            )
                            layout(placeable.width, placeable.height) {
                                placeable.place(-24.dp.roundToPx(), 0)
                            }
                        }
                        .height(320.dp)
                )
            }
            
            Spacer(modifier = Modifier.weight(0.5f))
            
            AnimatedEntry(delay = 300) {
                Text(
                    text = "Siap Untuk Memulai Hari\nHari Mu?",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            AnimatedEntry(delay = 400) {
                Text(
                    text = "Setiap langkah kecil hari ini, adalah bagian dari masa depan yang sedang kamu bangun.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = 24.sp
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Custom Swipe/Pill Button
            AnimatedEntry(delay = 500) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .cardShadow(shapeRadius = 28.dp, blurRadius = 25.dp, offsetY = 8.dp)
                        .clickable { onStart() },
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Ayo Kita Mulai",
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 24.dp),
                            fontWeight = FontWeight.Medium
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
