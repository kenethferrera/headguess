package com.example.headguess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.headguess.ui.GameViewModel
import com.example.headguess.ui.Role
import com.example.headguess.ui.CountryManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomImpostorQuickPlayScreen(navController: NavHostController, vm: GameViewModel, showPopupOnLoad: Boolean = false) {
    var title by remember { mutableStateOf("") }
    var correctWord by remember { mutableStateOf("") }
    var impostorWord by remember { mutableStateOf("") }
    var selectedPlayerCount by remember { mutableStateOf(3) }
    var selectedImpostorCount by remember { mutableStateOf(1) }
    var showImpostorRole by remember { mutableStateOf(false) }
    var savedTitles by remember { mutableStateOf(emptyList<String>()) }
    var showTitlesDialog by remember { mutableStateOf(false) }
    var editingTitle by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedTitleForGame by remember { mutableStateOf("") }
    
    val playersCount by vm.playersCount
    val gameStarted by vm.gameStarted
    val role by vm.role
    
    // Load saved words and titles
    LaunchedEffect(Unit) {
        // Load saved impostor titles from local storage
        savedTitles = CustomStorage.loadImpostorTitles()
    }
    
    // Show popup on load if requested
    LaunchedEffect(showPopupOnLoad) {
        if (showPopupOnLoad) {
            showTitlesDialog = true
        }
    }
    
    // Start hosting for custom impostor
    LaunchedEffect(Unit) {
        // Reset game state
        vm.gameStarted.value = false
        vm.startHosting(onReady = {}, gameType = "impostor")
        vm.role.value = Role.HOST
    }
    
    // Note: Game navigation is handled by the Play button in the saved titles dialog
    
    // Calculate minimum players based on impostor count: N impostors × 2 + 1
    val minPlayersForImpostors = selectedImpostorCount * 2 + 1
    
    // Auto-adjust player count if it's below minimum
    LaunchedEffect(selectedImpostorCount) {
        if (selectedPlayerCount < minPlayersForImpostors) {
            selectedPlayerCount = minPlayersForImpostors
        }
    }
    
    // Validation: Check if impostor count is valid for player count
    val isValidImpostorCount = selectedImpostorCount <= selectedPlayerCount - 1
    val canStartGame = playersCount >= selectedPlayerCount && isValidImpostorCount && correctWord.isNotEmpty() && impostorWord.isNotEmpty()

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    TextButton(onClick = { 
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
            
            // Word Input Section
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
                                if (showEditDialog && editingTitle.isNotEmpty()) {
                                    // Update existing title - delete old and create new
                                    CustomStorage.deleteImpostorTitle(editingTitle)
                                    CustomStorage.saveImpostorWithTitle(title, listOf(correctWord), listOf(impostorWord))
                                    showEditDialog = false
                                    editingTitle = ""
                                } else {
                                    // Create new title
                                    CustomStorage.saveImpostorWithTitle(title, listOf(correctWord), listOf(impostorWord))
                                }
                                
                                // Reload titles
                                savedTitles = CustomStorage.loadImpostorTitles()
                                
                                title = ""
                                correctWord = ""
                                impostorWord = ""
                            }
                        },
                        enabled = title.isNotEmpty() && correctWord.isNotEmpty() && impostorWord.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (showEditDialog) "Update Words" else "Save Words")
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Game Settings Section
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
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Player Count Selection
                    Text(
                        text = "Number of Players",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Minus button
                        Button(
                            onClick = { 
                                if (selectedPlayerCount > minPlayersForImpostors) {
                                    selectedPlayerCount--
                                }
                            },
                            enabled = selectedPlayerCount > minPlayersForImpostors,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedPlayerCount > minPlayersForImpostors) 
                                    MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "-",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Current count display
                        Text(
                            text = "$selectedPlayerCount",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        // Plus button
                        Button(
                            onClick = { 
                                if (selectedPlayerCount < 20) {
                                    selectedPlayerCount++
                                }
                            },
                            enabled = selectedPlayerCount < 20,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedPlayerCount < 20) 
                                    MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "+",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Impostor Count Selection
                    Text(
                        text = "Number of Impostors",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Minus button
                        Button(
                            onClick = { 
                                if (selectedImpostorCount > 1) {
                                    selectedImpostorCount--
                                }
                            },
                            enabled = selectedImpostorCount > 1,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedImpostorCount > 1) 
                                    MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "-",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Current count display
                        Text(
                            text = "$selectedImpostorCount",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        // Plus button
                        Button(
                            onClick = { 
                                val maxImpostors = minOf(5, selectedPlayerCount - 1)
                                if (selectedImpostorCount < maxImpostors) {
                                    selectedImpostorCount++
                                }
                            },
                            enabled = selectedImpostorCount < minOf(5, selectedPlayerCount - 1),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedImpostorCount < minOf(5, selectedPlayerCount - 1)) 
                                    MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "+",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    // Validation message
                    if (!isValidImpostorCount) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "⚠️ Need at least $minPlayersForImpostors players for $selectedImpostorCount impostor(s) (${selectedImpostorCount} × 2 + 1)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Red
                        )
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Show Impostor Role Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Show impostors their role",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        Switch(
                            checked = showImpostorRole,
                            onCheckedChange = { showImpostorRole = it }
                        )
                    }
                }
            }
            
            
            Spacer(Modifier.height(16.dp))
            
            // Continue Button
            Button(
                onClick = { showTitlesDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
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
                                                editingTitle = titleName
                                                // Load the words for editing
                                                val loadedWords = CustomStorage.loadImpostorByTitle(titleName)
                                                if (loadedWords.guesser.isNotEmpty() && loadedWords.impostor.isNotEmpty()) {
                                                    // Update the input fields with loaded data
                                                    title = editingTitle
                                                    correctWord = loadedWords.guesser.first()
                                                    impostorWord = loadedWords.impostor.first()
                                                }
                                                showTitlesDialog = false
                                                showEditDialog = true
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
                                                // Load words and start game
                                                val loadedWords = CustomStorage.loadImpostorByTitle(titleName)
                                                if (loadedWords.guesser.isNotEmpty() && loadedWords.impostor.isNotEmpty()) {
                                                    // Generate impostor words based on impostor count
                                                    val impostorWordsList = if (selectedImpostorCount > 1) {
                                                        val baseImpostorWords = loadedWords.impostor
                                                        if (baseImpostorWords.isNotEmpty()) {
                                                            (0 until selectedImpostorCount).map { baseImpostorWords[it % baseImpostorWords.size] }
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
                                                    
                                                    // Store the selected title for the game
                                                    selectedTitleForGame = titleName
                                                    // Set country to Custom to use custom impostor words
                                                    CountryManager.setCountry("Custom")
                                                    // Set the category to Custom
                                                    vm.category.value = "Custom"
                                                    // Set the impostor count
                                                    vm.impostorCount.value = selectedImpostorCount
                                                    // Set the selected title
                                                    vm.selectedImpostorTitle.value = titleName
                                                    // Set the show impostor role toggle
                                                    vm.showImpostorRole.value = showImpostorRole
                                                    // Navigate to quick reveal screen with selected player count
                                                    navController.navigate("impostorQuickReveal/${selectedPlayerCount}/Custom")
                                                }
                                                showTitlesDialog = false
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
