package com.example.headguess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.headguess.data.WordRepository
import com.example.headguess.ui.CustomStorage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ImpostorQuickRevealScreen(navController: NavHostController, vm: GameViewModel, players: Int, category: String, impostorCount: Int = 1) {
    val showRole = vm.showImpostorRole.value
    val impostorCountState = vm.impostorCount.value.coerceIn(1, players)
    val wordRepo = remember { WordRepository() }
    val pair = remember(category) { wordRepo.getImpostorWordPair(category) }
    
    // For custom impostor with multiple impostors, we need to handle multiple impostor words
    val (impostorWords, commonWord) = if (category == "Custom") {
        // Use custom words received from host if available, otherwise load locally
        val receivedImpostorWords = vm.customImpostorWords.value
        val receivedCommonWord = vm.customCommonWord.value
        
        if (receivedImpostorWords.isNotEmpty() && receivedCommonWord.isNotEmpty()) {
            // Use words received from host
            val impostorWordsList = if (impostorCountState > 1) {
                // For multiple impostors, repeat the impostor words as needed
                if (receivedImpostorWords.isNotEmpty()) {
                    (0 until impostorCountState).map { receivedImpostorWords[it % receivedImpostorWords.size] }
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
            val impostorWordsList = if (impostorCountState > 1) {
                // For multiple impostors, repeat the impostor words as needed
                val baseImpostorWords = customWords.impostor
                if (baseImpostorWords.isNotEmpty()) {
                    (0 until impostorCountState).map { baseImpostorWords[it % baseImpostorWords.size] }
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
    val impostorIndices = remember(players, impostorCountState) { 
        (0 until players).shuffled().take(impostorCountState)
    }

    val revealed = remember(players) { mutableStateListOf<Boolean>().apply { repeat(players) { add(false) } } }
    val locked = remember(players) { mutableStateListOf<Boolean>().apply { repeat(players) { add(false) } } }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Tap to reveal your role", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
        Spacer(Modifier.height(12.dp))
        // Player cards list (scrollable)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed((0 until players).toList()) { _, idx ->
                // Get the word for this player
                val text = if (idx in impostorIndices) {
                    val impostorIndexInList = impostorIndices.indexOf(idx)
                    impostorWords.getOrNull(impostorIndexInList) ?: impostorWords.first()
                } else {
                    commonWord
                }
                val isImpostor = idx in impostorIndices
                val textColor = if (showRole && isImpostor) Color.Red else Color.Black
                
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
                    // Tap cover that disappears on first tap, then locks after 2s
                    if (!revealed[idx]) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF2C2C2C))
                                .clickable(enabled = !locked[idx]) {
                                    if (!locked[idx]) {
                                        revealed[idx] = true
                                        // auto-lock after 2 seconds
                                        scope.launch {
                                            delay(2000)
                                            locked[idx] = true
                                            // hide the word again once locked
                                            revealed[idx] = false
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (locked[idx]) "Locked" else "Tap to Reveal",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    // When all cards are locked, show a 5-minute timer dialog with Home action
    val allLocked by remember(locked) { derivedStateOf { locked.all { it } && locked.isNotEmpty() } }
    var showTimer by remember { mutableStateOf(false) }
    var remaining by remember(showTimer) { mutableStateOf(300) }

    LaunchedEffect(allLocked) {
        if (allLocked) {
            showTimer = true
            remaining = 300
        }
    }

    LaunchedEffect(showTimer, remaining) {
        if (showTimer && remaining > 0) {
            delay(1000)
            remaining--
        }
    }

    if (showTimer) {
        AlertDialog(
            onDismissRequest = { /* block dismiss */ },
            confirmButton = {
                TextButton(onClick = { navController.navigate("home") }) {
                    Text("Home")
                }
            },
            // Remove title and explanatory text, show only large timer
            title = {},
            text = {
                val minutes = remaining / 60
                val seconds = remaining % 60
                Text(
                    text = String.format("%d:%02d", minutes, seconds),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    textAlign = TextAlign.Center
                )
            }
        )
    }
}


