package com.example.headguess.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.headguess.ui.GameViewModel
import com.example.headguess.data.WordRepository
import kotlinx.coroutines.delay

@Composable
fun ImpostorGameScreen(
    navController: NavHostController,
    vm: GameViewModel,
    category: String
) {
    val isImpostor by vm.isImpostor
    val currentWord by vm.currentWord
    val showImpostorRole by vm.showImpostorRole
    var gameStarted by remember { mutableStateOf(false) }
    var timeRemaining by remember { mutableStateOf(300) } // 5 minutes
    var isTimerRunning by remember { mutableStateOf(false) }
    
    // Sliding cover state
    var slideOffset by remember { mutableStateOf(0f) }
    var dragStarted by remember { mutableStateOf(false) }
    
    val wordRepository = remember { WordRepository() }
    
    // Initialize game
    LaunchedEffect(Unit) {
        // Wait for game to start and word to be assigned by host
        // The word and role are already set by GameViewModel when host starts the game
        // or when client receives the assigned word from host
        
        android.util.Log.d("ImpostorGameScreen", "Game screen initialized. Word: '$currentWord', Role: ${if (isImpostor) "impostor" else "crewmate"}")
        
        // Start countdown after a brief delay
        delay(2000)
        gameStarted = true
        isTimerRunning = true
    }
    
    // Timer logic
    LaunchedEffect(isTimerRunning, timeRemaining) {
        if (isTimerRunning && timeRemaining > 0) {
            delay(1000)
            timeRemaining--
        }
        if (timeRemaining == 0) {
            // Time up - navigate to results
            navController.navigate("impostorResult")
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Timer
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Text(
                text = "Time: ${timeRemaining / 60}:${String.format("%02d", timeRemaining % 60)}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
        
        if (!gameStarted) {
            // Pre-game countdown
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎭 IMPOSTOR GAME",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Text(
                        text = "Starting in 3... 2... 1...",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Game content
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Your word:",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Word display with sliding cover
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                    ) {
                        // Word content (always present)
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentWord,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = if (isImpostor && showImpostorRole) Color.Red else MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        // Sliding cover (always present)
                        val animatedOffset by animateFloatAsState(
                            targetValue = slideOffset,
                            animationSpec = tween(300),
                            label = "slide_animation"
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .offset(y = animatedOffset.dp)
                                .background(Color(0xFF2C2C2C))
                                .zIndex(1f)
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = {
                                            dragStarted = false
                                        },
                                        onDragEnd = {
                                            slideOffset = 0f
                                            dragStarted = false
                                        }
                                    ) { _, dragAmount ->
                                        if (!dragStarted && dragAmount.y < -10f) {
                                            dragStarted = true
                                        }
                                        if (dragStarted) {
                                            val newOffset = (slideOffset + dragAmount.y).coerceAtLeast(-120f).coerceAtMost(0f)
                                            slideOffset = newOffset
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Slide up to reveal",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            
            Spacer(Modifier.weight(1f))

            // Done button to exit to results
            Button(
                onClick = {
                    navController.navigate("impostorResult")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Done",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
