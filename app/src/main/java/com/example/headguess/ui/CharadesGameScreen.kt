package com.example.headguess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.headguess.ui.GameViewModel
import com.example.headguess.data.WordRepository
import kotlinx.coroutines.delay

@Composable
fun CharadesGameScreen(
    navController: NavHostController,
    vm: GameViewModel,
    category: String,
    actorCount: Int,
    wordCount: Int
) {
    val charadesClientRole by vm.charadesClientRole
    var words by remember { mutableStateOf(emptyList<String>()) }
    val wordRepository = remember { WordRepository() }
    
    val density = LocalDensity.current
    
    // Debug logging
    LaunchedEffect(Unit) {
        android.util.Log.d("CharadesGameScreen", "Screen loaded - Role: $charadesClientRole, Category: $category, WordCount: $wordCount")
    }
    
    // Load words for actors - different logic for host vs client
    LaunchedEffect(charadesClientRole, vm.charadesActorWords.value, vm.role.value, category, wordCount) {
        android.util.Log.d("CharadesGameScreen", "LaunchedEffect triggered - Role: $charadesClientRole, Received words: ${vm.charadesActorWords.value}, GameRole: ${vm.role.value}, Category: $category")
        if (charadesClientRole == "actor") {
            if (vm.role.value == Role.HOST) {
                // Host should use the words they selected directly (for custom charades)
                if (vm.charadesActorWords.value.isNotEmpty()) {
                    words = vm.charadesActorWords.value
                    android.util.Log.d("CharadesGameScreen", "Host using selected words: $words")
                } else {
                    // For general charades, generate words from WordRepository
                    android.util.Log.d("CharadesGameScreen", "Generating words for category: $category")
                    android.util.Log.d("CharadesGameScreen", wordRepository.getDebugInfo())
                    words = (1..wordCount).map { wordRepository.getRandomWord(category) }.filter { it.isNotEmpty() }
                    android.util.Log.d("CharadesGameScreen", "Host generated words from repository: $words")
                    if (words.isEmpty()) {
                        android.util.Log.e("CharadesGameScreen", "No words generated! Category: $category, WordCount: $wordCount")
                    }
                }
            } else {
                // Client should use words received from host
                if (vm.charadesActorWords.value.isNotEmpty()) {
                    words = vm.charadesActorWords.value
                    android.util.Log.d("CharadesGameScreen", "Client using words received from host: $words")
                } else {
                    // For general charades, generate words from WordRepository as fallback
                    android.util.Log.d("CharadesGameScreen", "Client generating fallback words for category: $category")
                    words = (1..wordCount).map { wordRepository.getRandomWord(category) }.filter { it.isNotEmpty() }
                    android.util.Log.d("CharadesGameScreen", "Client generated words from repository (fallback): $words")
                    if (words.isEmpty()) {
                        android.util.Log.e("CharadesGameScreen", "No fallback words generated! Category: $category")
                    }
                }
            }
        } else {
            // For guessers, also generate words so they can peek at them
            if (vm.charadesActorWords.value.isNotEmpty()) {
                words = vm.charadesActorWords.value
                android.util.Log.d("CharadesGameScreen", "Guesser using words from host: $words")
            } else {
                // For general charades, generate words from WordRepository
                android.util.Log.d("CharadesGameScreen", "Guesser generating words for category: $category")
                words = (1..wordCount).map { wordRepository.getRandomWord(category) }.filter { it.isNotEmpty() }
                android.util.Log.d("CharadesGameScreen", "Guesser generated words from repository: $words")
                if (words.isEmpty()) {
                    android.util.Log.e("CharadesGameScreen", "No guesser words generated! Category: $category")
                }
            }
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Main content
        if (charadesClientRole == "actor") {
            CharadesActorScreen(
                words = words,
                onCorrect = { /* could play a sound or haptic */ },
                onSkip = { /* optional feedback */ },
                onDone = { correct ->
                    // Navigate directly to home page
                    navController.navigate("home")
                }
            )
        } else {
            CharadesGuesserScreen(
                words = words,
                onDone = { navController.navigate("home") }
            )
        }
    }
}
