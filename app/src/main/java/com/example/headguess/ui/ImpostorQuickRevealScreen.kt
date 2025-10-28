package com.example.headguess.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.headguess.data.WordRepository
import com.example.headguess.ui.CustomStorage
import kotlinx.coroutines.delay

@Composable
fun ImpostorQuickRevealScreen(navController: NavHostController, vm: GameViewModel, players: Int, category: String, impostorCount: Int = 1) {
    val showImpostorRole = vm.showImpostorRole.value
    val wordRepo = remember { WordRepository() }
    val pair = remember(category) { wordRepo.getImpostorWordPair(category) }
    
    // For custom impostor with multiple impostors, we need to handle multiple impostor words
    val (impostorWords, commonWord) = if (category == "Custom") {
        // Use custom words received from host if available, otherwise load locally
        val receivedImpostorWords = vm.customImpostorWords.value
        val receivedCommonWord = vm.customCommonWord.value
        
        if (receivedImpostorWords.isNotEmpty() && receivedCommonWord.isNotEmpty()) {
            // Use words received from host
            val impostorWordsList = if (impostorCount > 1) {
                // For multiple impostors, repeat the impostor words as needed
                if (receivedImpostorWords.isNotEmpty()) {
                    (0 until impostorCount).map { receivedImpostorWords[it % receivedImpostorWords.size] }
                } else {
                    listOf("Impostor")
                }
            } else {
                listOf(receivedImpostorWords.firstOrNull() ?: "Impostor")
            }
            Pair(impostorWordsList, receivedCommonWord)
        } else {
            // Fallback to local loading
            val selectedTitle = vm.selectedImpostorTitle.value
            val customWords = if (selectedTitle.isNotEmpty()) {
                CustomStorage.loadImpostorByTitle(selectedTitle)
            } else {
                CustomStorage.loadImpostor() // Fallback to default
            }
            val impostorWordsList = if (impostorCount > 1) {
                // For multiple impostors, repeat the impostor words as needed
                val baseImpostorWords = customWords.impostor
                if (baseImpostorWords.isNotEmpty()) {
                    (0 until impostorCount).map { baseImpostorWords[it % baseImpostorWords.size] }
                } else {
                    listOf("Impostor")
                }
            } else {
                listOf(customWords.impostor.firstOrNull() ?: "Impostor")
            }
            val commonWordsList = customWords.guesser.firstOrNull() ?: "Common"
            Pair(impostorWordsList, commonWordsList)
        }
    } else {
        // Regular impostor logic with 2 words
        val impostorGetsA = remember { (0..1).random() == 1 }
        val impostorWord = if (impostorGetsA) pair.first else pair.second
        val commonWord = if (impostorGetsA) pair.second else pair.first
        Pair(listOf(impostorWord), commonWord)
    }
    
    // Randomly select impostor indices
    val impostorIndices = remember(players, impostorCount) { 
        (0 until players).shuffled().take(impostorCount)
    }

    val revealed = remember(players) { mutableStateListOf<Boolean>().apply { repeat(players) { add(false) } } }
    val slideOffsets = remember(players) { mutableStateListOf<Float>().apply { repeat(players) { add(0f) } } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Slide up to reveal your role", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(players) { idx ->
                val slideOffset = slideOffsets.getOrNull(idx) ?: 0f
                
                // Get the word for this player
                val text = if (idx in impostorIndices) {
                    val impostorIndexInList = impostorIndices.indexOf(idx)
                    impostorWords.getOrNull(impostorIndexInList) ?: impostorWords.first()
                } else {
                    commonWord
                }
                val isImpostor = idx in impostorIndices
                val textColor = if (showImpostorRole && isImpostor) Color.Red else Color.Black
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                ) {
                    // Word content (always present)
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = text, 
                            style = MaterialTheme.typography.titleLarge, 
                            fontWeight = FontWeight.Bold, 
                            textAlign = TextAlign.Center,
                            color = textColor
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
                            .pointerInput(idx) {
                                detectDragGestures(
                                    onDragStart = {
                                        // No interaction tracking needed
                                    },
                                    onDragEnd = {
                                        // When drag ends, animate back to cover position
                                        slideOffsets[idx] = 0f
                                    }
                                ) { _, dragAmount ->
                                    // Only allow upward dragging
                                    val newOffset = (slideOffsets[idx] + dragAmount.y).coerceAtLeast(-72f).coerceAtMost(0f)
                                    slideOffsets[idx] = newOffset
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Slide up to reveal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Done button to proceed to home page
        Button(
            onClick = {
                navController.navigate("home")
            },
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
        
        Spacer(Modifier.height(16.dp))
    }
}


