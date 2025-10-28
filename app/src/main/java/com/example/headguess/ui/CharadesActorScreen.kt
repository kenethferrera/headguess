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
import kotlinx.coroutines.delay

@Composable
fun CharadesActorScreen(
    words: List<String>,
    onCorrect: () -> Unit,
    onSkip: () -> Unit,
    onDone: (List<String>) -> Unit
) {
    // State for multiple words navigation
    var currentWordIndex by remember { mutableStateOf(0) }
    var slideOffset by remember { mutableStateOf(0f) }
    var dragStarted by remember { mutableStateOf(false) }
    
    // Reset slide offset when word changes
    LaunchedEffect(currentWordIndex) {
        slideOffset = 0f
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (words.isNotEmpty()) {
            // Display current word with sliding cover
            val currentWord = words[currentWordIndex]
            val isLastWord = currentWordIndex == words.size - 1
            
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
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Sliding cover (always present)
                val animatedOffset by animateFloatAsState(
                        targetValue = slideOffset,
                        animationSpec = tween(300),
                        label = "slide_animation"
                    )
                    
                    // Cover that slides up to reveal the word
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(y = animatedOffset.dp)
                            .background(Color(0xFF2C2C2C))
                            .zIndex(1f) // Ensure cover is on top
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = {
                                        dragStarted = false
                                    },
                                    onDragEnd = {
                                        // When drag ends, animate back to cover position
                                        slideOffset = 0f
                                        dragStarted = false
                                    }
                                ) { _, dragAmount ->
                                    // Only start dragging after significant upward movement
                                    if (!dragStarted && dragAmount.y < -10f) {
                                        dragStarted = true
                                    }
                                    
                                    if (dragStarted) {
                                        // Only allow upward dragging and prevent excessive sliding
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
            
            // Word counter
            Text(
                text = "Word ${currentWordIndex + 1} of ${words.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(top = 16.dp)
            )
            
            Spacer(Modifier.height(24.dp))
            
            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Previous button (only show if not first word)
                if (currentWordIndex > 0) {
                    OutlinedButton(
                        onClick = {
                            currentWordIndex--
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Previous")
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }
                
                Spacer(Modifier.width(16.dp))
                
                // Next/Done button
                Button(
                    onClick = {
                        if (isLastWord) {
                            onDone(emptyList())
                        } else {
                            currentWordIndex++
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (isLastWord) "Done" else "Next")
                }
            }
        } else {
            // Show message if no words
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No words available",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
            }
        }
    }
}