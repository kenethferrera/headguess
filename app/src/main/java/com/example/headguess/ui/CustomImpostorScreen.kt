package com.example.headguess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import com.example.headguess.ui.GameViewModel
import com.example.headguess.ui.Role
import com.example.headguess.ui.CustomStorage
import com.example.headguess.ui.CountryManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomImpostorScreen(
    navController: NavHostController,
    vm: GameViewModel,
    playerCount: Int = 3,
    showPopupOnLoad: Boolean = false,
    impostorCount: Int = 1,
    showImpostorRole: Boolean = false
) {
    var title by remember { mutableStateOf("") }
    var correctWord by remember { mutableStateOf("") }
    var impostorWord by remember { mutableStateOf("") }
    var savedTitles by remember { mutableStateOf(emptyList<String>()) }
    var showTitlesDialog by remember { mutableStateOf(false) }
    var showNotification by remember { mutableStateOf(false) }
    var notificationMessage by remember { mutableStateOf("") }
    var editingTitle by remember { mutableStateOf("") }
    
    val playersCount by vm.playersCount
    val gameStarted by vm.gameStarted
    val role by vm.role
    
    // Start hosting for custom impostor
    LaunchedEffect(Unit) {
        // Reset game state when entering the screen
        vm.resetImpostorGameState()
        vm.startHosting(onReady = {
            vm.publishNSD()
        }, gameType = "impostor")
    }
    
    // Navigate to game when started
    LaunchedEffect(gameStarted) {
        if (gameStarted) {
            // Stop NSD service when game starts
            vm.hostServer?.stopNSD()
            navController.navigate("impostorGame/Custom")
        }
    }
    
    // Set role as host
    LaunchedEffect(Unit) {
        vm.role.value = Role.HOST
    }
    
    // Load saved titles and show popup if requested
    LaunchedEffect(Unit) {
        savedTitles = CustomStorage.loadImpostorTitles()
    }
    
    // Cleanup when screen is disposed
    DisposableEffect(Unit) {
        onDispose {
            // Ensure NSD service is stopped when screen is disposed
            vm.hostServer?.stopNSD()
        }
    }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Custom Impostor Lobby") },
                navigationIcon = {
                    TextButton(onClick = { 
                        // Stop NSD service immediately
                        vm.hostServer?.stopNSD()
                        vm.stopHosting()
                        navController.popBackStack() 
                    }) {
                        Text("← Back")
                    }
                }
            ) 
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFAFAFA))
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Lobby Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Players Joined: $playersCount",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Custom Words Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Custom Words",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Title Input
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
            Spacer(Modifier.height(12.dp))

                    // Correct Word Input
            OutlinedTextField(
                        value = correctWord,
                        onValueChange = { correctWord = it },
                        label = { Text("Correct Word (for non-impostors)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
            Spacer(Modifier.height(12.dp))

                    // Impostor Word Input
                    OutlinedTextField(
                        value = impostorWord,
                        onValueChange = { impostorWord = it },
                        label = { Text("Impostor Word") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Save Words Button
            Button(
                onClick = {
                            if (title.isNotEmpty() && correctWord.isNotEmpty() && impostorWord.isNotEmpty()) {
                                if (editingTitle.isNotEmpty()) {
                                    // Update existing title
                                    CustomStorage.deleteImpostorTitle(editingTitle)
                                    CustomStorage.saveImpostorWithTitle(title, listOf(correctWord), listOf(impostorWord))
                                    editingTitle = ""
                                } else {
                                    // Create new title
                                    CustomStorage.saveImpostorWithTitle(title, listOf(correctWord), listOf(impostorWord))
                                }
                                title = ""
                                correctWord = ""
                                impostorWord = ""
                                savedTitles = CustomStorage.loadImpostorTitles()
                            }
                        },
                        enabled = title.isNotEmpty() && correctWord.isNotEmpty() && impostorWord.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (editingTitle.isNotEmpty()) "Update Words" else "Save Words")
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Continue Button
            Button(
                onClick = { 
                    // Show popup with saved titles
                    showTitlesDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Continue",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
    
    // Saved Titles Dialog
    if (showTitlesDialog) {
        AlertDialog(
            onDismissRequest = { showTitlesDialog = false },
            title = { 
                Text(
                    text = "Saved Word Lists",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                    if (savedTitles.isEmpty()) {
                        item {
                            Text(
                                text = "No saved word lists found. Create some words first!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        items(savedTitles) { titleName ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = titleName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Edit button
                                        IconButton(
                                            onClick = {
                                                // Load the words for editing
                                                val loadedWords = CustomStorage.loadImpostorByTitle(titleName)
                                                title = titleName
                                                correctWord = loadedWords.guesser.firstOrNull() ?: ""
                                                impostorWord = loadedWords.impostor.firstOrNull() ?: ""
                                                editingTitle = titleName
                                                showTitlesDialog = false
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        
                                        // Delete button
                                        IconButton(
                                            onClick = {
                                                CustomStorage.deleteImpostorTitle(titleName)
                                                savedTitles = CustomStorage.loadImpostorTitles()
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = Color.Red
                                            )
                                        }
                                        
                                        // Play button
                                        Button(
                                            onClick = {
                                                // Calculate minimum players needed: N impostors × 2 + 1
                                                val minPlayersNeeded = impostorCount * 2 + 1
                                                
                                                if (playersCount + 1 < minPlayersNeeded) {
                                                    // Show notification for insufficient players
                                                    notificationMessage = "Need ${minPlayersNeeded - playersCount - 1} more player(s)."
                                                    showNotification = true
                                                    
                                                    // Hide notification after 3 seconds
                                                    kotlinx.coroutines.GlobalScope.launch {
                                                        kotlinx.coroutines.delay(3000)
                                                        showNotification = false
                                                    }
                                                } else {
                                                    // Load words and start game
                                                    val loadedWords = CustomStorage.loadImpostorByTitle(titleName)
                                                    if (loadedWords.guesser.isNotEmpty() && loadedWords.impostor.isNotEmpty()) {
                                                        // Generate impostor words based on impostor count
                                                        val impostorWordsList = if (impostorCount > 1) {
                                                            val baseImpostorWords = loadedWords.impostor
                                                            if (baseImpostorWords.isNotEmpty()) {
                                                                (0 until impostorCount).map { baseImpostorWords[it % baseImpostorWords.size] }
                                                            } else {
                                                                listOf("Impostor")
                                                            }
                                                        } else {
                                                            listOf(loadedWords.impostor.firstOrNull() ?: "Impostor")
                                                        }
                                                        val commonWord = loadedWords.guesser.firstOrNull() ?: "Common"
                                                        
                                                        // Set custom words in HostServer
                                                        vm.hostServer?.setCustomImpostorWords(impostorWordsList, commonWord)
                                                        // Set show impostor role in HostServer
                                                        vm.hostServer?.setShowImpostorRole(showImpostorRole)
                                                        
                                                        // Set country to Custom to use custom impostor words
                                                        CountryManager.setCountry("Custom")
                                                        // Set the category to Custom
                                                        vm.category.value = "Custom"
                                                        // Set the impostor count
                                                        vm.impostorCount.value = impostorCount
                                                        // Set the selected title
                                                        vm.selectedImpostorTitle.value = titleName
                                                        // Set the show impostor role toggle
                                                        vm.showImpostorRole.value = showImpostorRole
                                                        
                                                        // Start the impostor game - host and clients will get their assigned words
                                                        vm.startImpostorGame()
                                                        
                                                        // The gameStarted flag will trigger navigation in LaunchedEffect
                                                    }
                                                    showTitlesDialog = false
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color.Green
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "Play",
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                    
                    // Notification inside dialog
                    if (showNotification) {
                        Spacer(Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = notificationMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tip: Try decreasing the number of impostors",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    fontStyle = FontStyle.Italic
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showTitlesDialog = false }
                ) {
                    Text("Close")
                }
            }
        )
    }
    
}
