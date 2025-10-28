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
fun ImpostorLobbyScreen(
    navController: NavHostController,
    vm: GameViewModel,
    category: String
) {
    val playersCount by vm.playersCount
    
    LaunchedEffect(Unit) {
        // Ensure ViewModel carries this route's category so host and clients get it
        vm.selectCategory(category)
        vm.startHosting(onReady = {
            vm.publishNSD()
        }, gameType = "impostor")
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Handle system back: stop hosting safely
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
        
        Text(
            text = "Players Joined: $playersCount/20",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Game settings
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Game Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(Modifier.height(12.dp))
                
                // Impostor count selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "How many impostors?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { if (vm.impostorCount.value > 1) vm.impostorCount.value -= 1 }) { Text("-") }
                        Text(
                            text = vm.impostorCount.value.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        OutlinedButton(onClick = { vm.impostorCount.value += 1 }) { Text("+") }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Toggle for showing impostor text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Show Impostor Text",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Players can see who the impostor is",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    
                    Switch(
                        checked = vm.showImpostorText.value,
                        onCheckedChange = { vm.showImpostorText.value = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
        
        // Game info
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
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
        
        Spacer(Modifier.weight(1f))
        
        val canStart = playersCount >= 2 // with host makes 3 total
        Button(
            onClick = {
                vm.startImpostorGame()
                navController.navigate("impostorGame/$category")
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            enabled = canStart,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "Start Game",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
