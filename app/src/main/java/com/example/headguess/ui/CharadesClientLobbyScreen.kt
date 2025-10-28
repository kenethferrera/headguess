package com.example.headguess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.headguess.ui.GameViewModel
import androidx.activity.compose.BackHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharadesClientLobbyScreen(
    navController: NavHostController,
    vm: GameViewModel
) {
    val category by vm.category
    val gameStarted by vm.gameStarted
    val charadesClientRole by vm.charadesClientRole
    val charadesActorCount by vm.charadesActorCount
    val charadesWordCount by vm.charadesWordCount
    
    // Safety reset: prevent auto-navigation from stale state when re-entering lobby
    LaunchedEffect(Unit) {
        vm.gameStarted.value = false
        if (vm.charadesClientRole.value.isNotEmpty()) vm.charadesClientRole.value = ""
    }

    // Navigate to game only when we have gameStarted, role, and a non-empty category
    LaunchedEffect(gameStarted, charadesClientRole, category) {
        if (gameStarted && charadesClientRole.isNotEmpty() && category.isNotEmpty()) {
            android.util.Log.d("CharadesClientLobby", "Game started! Role: $charadesClientRole, Category: $category → navigating")
            navController.navigate("charadesGame/$category/$charadesActorCount/$charadesWordCount")
        } else {
            android.util.Log.d("CharadesClientLobby", "Waiting... gameStarted=$gameStarted role='$charadesClientRole' category='$category'")
        }
    }
    
    // Debug: Log current state
    LaunchedEffect(Unit) {
        android.util.Log.d("CharadesClientLobby", "Client lobby started. gameStarted: $gameStarted, category: $category")
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Ensure system back also leaves the client and decrements host count
        BackHandler {
            vm.leaveClient()
            navController.navigateUp()
        }
        TopAppBar(
            title = {},
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFAFAFA)),
            navigationIcon = {
                TextButton(onClick = { 
                    vm.leaveClient()
                    navController.navigateUp() 
                }) {
                    Text("Back")
                }
            }
        )
        
        Spacer(Modifier.height(24.dp))
        
        Text(
            text = "Category: $category",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        CircularProgressIndicator(
            modifier = Modifier.padding(16.dp)
        )
        
        Text(
            text = "Waiting for host to start...",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
    }
}
