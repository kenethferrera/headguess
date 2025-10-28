package com.example.headguess.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import com.example.headguess.ui.GameViewModel
import com.example.headguess.data.WordRepository
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomWordsScreen(
    navController: NavHostController, 
    vm: GameViewModel, 
    gameMode: String = "guessword",
    actorCount: Int = 1,
    wordCount: Int = 5
) {
    var input by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var words by remember { mutableStateOf(CustomStorage.loadCategories()) }
    var showLoadDialog by remember { mutableStateOf(false) }
    var showNotification by remember { mutableStateOf(false) }
    var notificationMessage by remember { mutableStateOf("") }
    val canSave = words.isNotEmpty() && title.isNotEmpty()
    val playersCount by vm.playersCount

    Scaffold(topBar = {
        TopAppBar(
            title = { 
                if (gameMode == "yesno") {
                    Text("Custom YES/NO Words")
                } else {
                    Text("Players Joined: $playersCount")
                }
            },
            navigationIcon = {
                TextButton(onClick = { 
                    if (gameMode != "yesno") {
                        vm.stopNSDOnly()
                    }
                    navController.popBackStack() 
                }) {
                    Text("Back")
                }
            }
        )
    }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            // Title input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title for this word list") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            
            // Word input
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Add a word") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row {
                Button(onClick = {
                    val w = input.trim()
                    if (w.isNotEmpty()) {
                        words = words + w
                        input = ""
                    }
                }) { Text("Add") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = {
                        if (CustomStorage.saveCategoriesWithTitle(title, words)) {
                            words = emptyList()
                            title = ""
                        }
                    },
                    enabled = canSave
                ) { Text("Save") }
            }
            Spacer(Modifier.height(12.dp))
            Text("Words (${words.size})")
            LazyColumn(Modifier.height(200.dp)) {
                items(words.size) { index ->
                    val word = words[index]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = word,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = { words = words.filterIndexed { i, _ -> i != index } }
                        ) { Text("Delete") }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            
            // Load button
            OutlinedButton(
                onClick = { showLoadDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Load") }
            
        }
    }
    
    // Load Dialog
    if (showLoadDialog) {
        LoadWordsDialog(
            onDismiss = { showLoadDialog = false },
            gameMode = gameMode,
            actorCount = actorCount,
            wordCount = wordCount,
            playersCount = playersCount,
            onLoadWords = { selectedTitle, loadedWords ->
                title = selectedTitle
                words = loadedWords
                showLoadDialog = false
            },
            onShowNotification = { message ->
                notificationMessage = message
                showNotification = true
                
                // Auto-hide notification after 3 seconds
                CoroutineScope(Dispatchers.Main).launch {
                    delay(3000)
                    showNotification = false
                }
            },
            showNotification = showNotification,
            notificationMessage = notificationMessage,
            onPlayGame = { selectedTitle, loadedWords, timerMinutes ->
                Log.d("CustomWordsScreen", "Play button clicked! Title: $selectedTitle, Words: ${loadedWords.size}, Timer: $timerMinutes, GameMode: $gameMode")
                // Set country to Custom and persist the selected words
                CountryManager.setCountry("Custom")
                CustomStorage.saveCategories(loadedWords)
                // Force WordRepository to reload custom words
                WordRepository().forceReload()
                
                showLoadDialog = false
                
                if (gameMode == "yesno") {
                    // For YES/NO mode, navigate directly to YesNoGameScreen like general mode
                    val wordCount = loadedWords.size.coerceAtMost(10) // Limit to 10 words max
                    val timerMinutesInt = timerMinutes.toIntOrNull() ?: 1
                    navController.navigate("customYesNoGame/Custom/$wordCount/$timerMinutesInt")
                } else if (gameMode == "charades") {
                    // For Charades mode, navigate directly to charades game to start playing
                    vm.selectCategory("Custom")
                    vm.startCharadesGame(actorCount, wordCount, loadedWords)
                    navController.navigate("charadesGame/Custom/$actorCount/$wordCount")
                } else {
                    // For Guess the Word mode, use multiplayer flow
                    vm.selectCategory("Custom")
                    vm.startGameForAll()
                    navController.navigate("countdown")
                }
            }
        )
    }
    
    
    // Automatically start hosting when screen loads (lobby functionality) - only for multiplayer games
    LaunchedEffect(Unit) {
        // Set country to Custom and save words
        CountryManager.setCountry("Custom")
        CustomStorage.saveCategories(words)
        
        // Only start hosting for multiplayer games, not for single-player games (YES/NO)
        if (gameMode != "yesno") {
            val hostingGameType = when (gameMode) {
                "charades" -> "charades"
                else -> "custom"
            }
            vm.startHosting(onReady = { vm.publishNSD() }, gameType = hostingGameType)
        }
        
        // For charades mode, show word selection popup immediately
        if (gameMode == "charades") {
            showLoadDialog = true
        }
    }
}

@Composable
fun LoadWordsDialog(
    onDismiss: () -> Unit,
    onLoadWords: (String, List<String>) -> Unit,
    onPlayGame: (String, List<String>, String) -> Unit,
    gameMode: String = "guessword",
    actorCount: Int = 1,
    wordCount: Int = 5,
    playersCount: Int = 0,
    onShowNotification: (String) -> Unit = {},
    showNotification: Boolean = false,
    notificationMessage: String = ""
) {
    var savedTitles by remember { mutableStateOf(CustomStorage.loadSavedTitles()) }
    var selectedTimer by remember { mutableStateOf(1) }
    
    fun deleteTitle(title: String) {
        CustomStorage.deleteTitle(title)
        savedTitles = CustomStorage.loadSavedTitles()
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp, max = 400.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Load Saved Word Lists",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                if (savedTitles.isEmpty()) {
                    Text("No saved word lists found.")
                } else {
                    LazyColumn(
                        modifier = Modifier.height(if (savedTitles.size > 5) 300.dp else (savedTitles.size * 60).dp)
                    ) {
                        items(savedTitles) { savedTitle ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val wordCount = CustomStorage.loadCategoriesByTitle(savedTitle).size
                                Text(
                                    text = "$savedTitle ($wordCount)",
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        val loadedWords = CustomStorage.loadCategoriesByTitle(savedTitle)
                                        onLoadWords(savedTitle, loadedWords)
                                    }
                                ) { 
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(Modifier.width(4.dp))
                                IconButton(
                                    onClick = { deleteTitle(savedTitle) }
                                ) { 
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                                Spacer(Modifier.width(4.dp))
                                Button(
                                    onClick = {
                                        Log.d("LoadWordsDialog", "Play button clicked for title: $savedTitle")
                                        val loadedWords = CustomStorage.loadCategoriesByTitle(savedTitle)
                                        Log.d("LoadWordsDialog", "Loaded ${loadedWords.size} words: $loadedWords")
                                        
                                        if (gameMode == "charades") {
                                            // Check for insufficient words first
                                            if (loadedWords.size <= actorCount) {
                                                val neededWords = actorCount + 1 - loadedWords.size
                                                val message = "Need $neededWords more word${if (neededWords == 1) "" else "s"} to start"
                                                onShowNotification(message)
                                            } else if (playersCount < actorCount) {
                                                // Show notification for insufficient players
                                                val neededPlayers = actorCount - playersCount
                                                val message = "Need $neededPlayers more player${if (neededPlayers == 1) "" else "s"} to start"
                                                onShowNotification(message)
                                            } else {
                                                onPlayGame(savedTitle, loadedWords, if (gameMode == "charades") "1" else selectedTimer.toString())
                                            }
                                        } else {
                                            onPlayGame(savedTitle, loadedWords, if (gameMode == "charades") "1" else selectedTimer.toString())
                                        }
                                    },
                                    enabled = true,
                                    modifier = Modifier.padding(4.dp)
                                ) { 
                                    Text("Play")
                                }
                            }
                        }
                    }
                }
                
                // Only show timer for non-charades games
                if (gameMode != "charades") {
                    Spacer(Modifier.height(16.dp))
                    Text("Timer (minutes):")
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { selectedTimer = (selectedTimer - 1).coerceAtLeast(1) }
                        ) { Text("-") }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = selectedTimer.toString(),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { selectedTimer = selectedTimer + 1 }
                        ) { Text("+") }
                    }
                }
                
                // Notification inside dialog
                if (showNotification) {
                    Spacer(Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Text(
                            text = notificationMessage,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
        
    }
}
