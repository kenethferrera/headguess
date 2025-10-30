package com.example.headguess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.compose.ui.res.painterResource
import com.example.headguess.R
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.example.headguess.ui.GameViewModel
import kotlinx.coroutines.delay
import androidx.activity.compose.BackHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImpostorClientLobbyScreen(navController: NavHostController, vm: GameViewModel) {
    val gameStarted by vm.gameStarted
    val category by vm.category
    val currentWord by vm.currentWord
    val isImpostor by vm.isImpostor
    val hostDisconnected by vm.hostDisconnected
    
    LaunchedEffect(Unit) {
        // Reset any stale state
        vm.gameStarted.value = false
    }
    
    // Handle host disconnection - immediately navigate back
    LaunchedEffect(hostDisconnected) {
        if (hostDisconnected) {
            android.util.Log.d("ImpostorClientLobbyScreen", "Host disconnected - immediately navigating back to join screen")
            // Pop back to join screen (either "impostorJoin" or "customImpostorJoin")
            navController.popBackStack()
        }
    }
    
    // Navigate to game when host starts AND we have received our assigned word
    LaunchedEffect(gameStarted, category, currentWord, isImpostor) {
        if (gameStarted && category.isNotEmpty() && currentWord.isNotEmpty()) {
            android.util.Log.d("ImpostorClientLobby", "Game started! Category: $category, Word: $currentWord, Role: ${if (isImpostor) "impostor" else "crewmate"} → navigating")
            navController.navigate("impostorGame/$category")
        } else {
            android.util.Log.d("ImpostorClientLobby", "Waiting... gameStarted=$gameStarted category='$category' word='$currentWord'")
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Ensure system back leaves client and decrements host count
        BackHandler {
            vm.leaveClient()
            navController.navigateUp()
        }
        TopAppBar(
            title = {},
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFAFAFA)),
            navigationIcon = { IconButton(onClick = { 
                vm.leaveClient()
                navController.navigateUp() 
            }) { Icon(painterResource(id = R.drawable.ic_back), contentDescription = "Back") } }
        )
        
        Spacer(Modifier.height(24.dp))
        
        if (category.isNotEmpty()) {
            Text(
                text = "Category: $category",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        Text(
            text = "Waiting for host to start the game...",
            style = MaterialTheme.typography.titleMedium,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        CircularProgressIndicator(
            modifier = Modifier.padding(16.dp)
        )
        
        Spacer(Modifier.height(32.dp))
        
        // Game info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Game Rules",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(Modifier.height(8.dp))
                
                Text(
                    text = "• One random player becomes the impostor\n" +
                            "• Everyone else gets the same word\n" +
                            "• Impostor gets a different word from the same category\n" +
                            "• Ask questions to find the impostor!\n" +
                            "• Vote to eliminate suspected impostor",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4
                )
            }
        }
    }
}
