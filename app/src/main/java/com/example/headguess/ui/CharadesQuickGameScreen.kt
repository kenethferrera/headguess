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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import androidx.compose.ui.res.painterResource
import com.example.headguess.data.WordRepository
import kotlinx.coroutines.launch
import com.example.headguess.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharadesQuickGameScreen(
    navController: NavHostController,
    category: String,
    wordCount: Int
) {
    var words by remember { mutableStateOf(emptyList<String>()) }
    var currentWordIndex by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    
    // Sliding cover state
    var slideOffset by remember { mutableStateOf(0f) }
    var dragStarted by remember { mutableStateOf(false) }

    // Load words from the selected category
    LaunchedEffect(category, wordCount) {
        scope.launch {
            isLoading = true
            val wordRepo = WordRepository()
            words = (1..wordCount).map { wordRepo.getRandomWord(category) }.filter { it.isNotEmpty() }
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(
            title = {},
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFAFAFA)),
            navigationIcon = { IconButton(onClick = { navController.navigateUp() }) { Icon(painterResource(id = R.drawable.ic_back), contentDescription = "Back") } }
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Charades - $category",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Word ${currentWordIndex + 1} of $wordCount",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (words.isNotEmpty()) {
            // Word display with sliding cover
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                // Word card background
                Card(
                    modifier = Modifier.fillMaxSize(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = words[currentWordIndex],
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                // Sliding cover
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
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Done button (always visible)
                OutlinedButton(
                    onClick = { navController.navigate("home") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Done", fontWeight = FontWeight.Medium)
                }

                // Next button (only visible if there are more words)
                if (currentWordIndex < words.size - 1) {
                    Button(
                        onClick = {
                            currentWordIndex++
                            slideOffset = 0f // Reset cover when moving to next word
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Next", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // No words available
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No words available",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
