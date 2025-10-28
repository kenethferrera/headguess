package com.example.headguess.ui

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.headguess.ui.CustomStorage
import com.example.headguess.ui.GameViewModel
import com.example.headguess.ui.CountryManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomImpostorHostScreen(navController: NavHostController, vm: GameViewModel) {
    var selectedImpostorCount by remember { mutableStateOf(1) }
    var showImpostorRole by remember { mutableStateOf(false) }
    var savedTitles by remember { mutableStateOf(emptyList<String>()) }
    var showTitlesDialog by remember { mutableStateOf(false) }
    
    // Load saved impostor titles
    LaunchedEffect(Unit) {
        // Reset game state when entering the screen
        vm.resetImpostorGameState()
        savedTitles = CustomStorage.loadImpostorTitles()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Host Settings") },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🎭 Custom Impostor Setup",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = "Configure your custom impostor game",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            
            Spacer(Modifier.height(32.dp))
            
            // Impostor Count Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Number of Impostors",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Text(
                        text = "How many impostors will be in the game?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Impostor count with +/- buttons
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
                                if (selectedImpostorCount < 5) {
                                    selectedImpostorCount++
                                }
                            },
                            enabled = selectedImpostorCount < 5,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedImpostorCount < 5) 
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
                    
                    // Show Impostor Role Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Show impostors their role",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Switch(
                            checked = showImpostorRole,
                            onCheckedChange = { showImpostorRole = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(32.dp))
            
            // Continue Button
            Button(
                onClick = { 
                    // Set the impostor count and toggle in GameViewModel
                    vm.impostorCount.value = selectedImpostorCount
                    vm.showImpostorRole.value = showImpostorRole
                    // Navigate to lobby screen with popup
                    navController.navigate("customImpostorWithPopup/3")
                },
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
                                                // TODO: Implement edit functionality
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
                                                // Load words and start game
                                                val loadedWords = CustomStorage.loadImpostorByTitle(titleName)
                                                if (loadedWords.guesser.isNotEmpty() && loadedWords.impostor.isNotEmpty()) {
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
                                                    // Navigate to quick reveal screen with default player count
                                                    navController.navigate("impostorQuickReveal/3/Custom")
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
