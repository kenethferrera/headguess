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
fun CharadesGuesserScreen(
    words: List<String>,
    onDone: () -> Unit
) {
    // Simplified state for single word
    var slideOffset by remember { mutableStateOf(0f) }
    var dragStarted by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Display instruction text with sliding cover instead of actual word
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
        ) {
            // Instruction text content (always present)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Guess the words your teammates act out!", 
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
                            text = "👆 Tap & Drag to Reveal",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
        
        Spacer(Modifier.height(32.dp))
        
        // Done button to proceed to home page
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Done",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}