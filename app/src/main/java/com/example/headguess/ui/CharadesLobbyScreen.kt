package com.example.headguess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.headguess.ui.GameViewModel
import androidx.activity.compose.BackHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharadesLobbyScreen(
    navController: NavHostController,
    vm: GameViewModel,
    category: String,
    actorCount: Int,
    wordCount: Int
) {
    val playersCount by vm.playersCount
    var selectedActorCount by remember { mutableStateOf(actorCount) }
    var selectedWordCount by remember { mutableStateOf(wordCount) }
    
    LaunchedEffect(Unit) {
        // Ensure ViewModel carries this route's category so host and clients get it
        vm.selectCategory(category)
        vm.startHosting(onReady = {
            vm.publishNSD()
        }, gameType = "charades")
        android.util.Log.d("CharadesLobbyScreen", "Lobby started - playersCount: $playersCount, selectedActorCount: $selectedActorCount")
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Handle system back to stop hosting safely like UI back
        BackHandler {
            vm.quickStopHosting()
            navController.navigateUp()
        }
        TopAppBar(
            title = {},
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFAFAFA)),
            navigationIcon = {
                TextButton(onClick = { 
                    // Safely stop hosting when leaving lobby
                    vm.quickStopHosting()
                    navController.navigateUp() 
                }) {
                    Text("Back")
                }
            }
        )
        
        Spacer(Modifier.height(24.dp))
        
        // Profile section with category
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile image
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("file:///android_asset/Images/${category.uppercase()}.avif")
                        .build(),
                    contentDescription = "Category profile",
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                )
                
                Spacer(Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = "Category: $category",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Host Profile",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        }
        
        // Inline settings steppers
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("How many players will act?", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (selectedActorCount > 1) selectedActorCount--
                        },
                        enabled = selectedActorCount > 1
                    ) { Text("−", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
                    Text(
                        text = selectedActorCount.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    val maxActors = (playersCount).coerceAtLeast(1) // clients count; host is separate
                    IconButton(
                        onClick = {
                            val limit = (playersCount).coerceAtLeast(1)
                            if (selectedActorCount < limit) selectedActorCount++
                        },
                        enabled = selectedActorCount < (playersCount).coerceAtLeast(1)
                    ) { Text("+", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
                }
            }
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("How many words to play?", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (selectedWordCount > 1) selectedWordCount--
                        },
                        enabled = selectedWordCount > 1
                    ) { Text("−", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
                    Text(
                        text = selectedWordCount.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    IconButton(
                        onClick = {
                            if (selectedWordCount < 5) selectedWordCount++
                        },
                        enabled = selectedWordCount < 5
                    ) { Text("+", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
                }
            }
        }
        
        Text(
            text = "Players Joined: $playersCount/20",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        val guessingCount = (playersCount - selectedActorCount).coerceAtLeast(0)
        Text(
            text = "Guessing: $guessingCount",
            style = MaterialTheme.typography.titleMedium,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        Spacer(Modifier.weight(1f))
        
        Button(
            onClick = {
                android.util.Log.d("CharadesLobbyScreen", "Start Game clicked - playersCount: $playersCount, selectedActorCount: $selectedActorCount")
                vm.startCharadesGame(selectedActorCount, selectedWordCount)
                navController.navigate("charadesGame/$category/$selectedActorCount/$selectedWordCount")
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            enabled = playersCount >= selectedActorCount,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (playersCount >= selectedActorCount) {
                    "Start Game"
                } else {
                    "Need ${selectedActorCount - playersCount} more player${if (selectedActorCount - playersCount == 1) "" else "s"}"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
