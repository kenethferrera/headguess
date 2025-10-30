package com.example.headguess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.compose.ui.res.painterResource
import com.example.headguess.R
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharadesQuickRevealScreen(
    navController: NavHostController,
    vm: GameViewModel,
    players: Int,
    category: String,
    actorCount: Int,
    wordCount: Int
) {
    // Store game settings in ViewModel for later use
    LaunchedEffect(Unit) {
        vm.charadesActorCount.value = actorCount
        vm.charadesWordCount.value = wordCount
    }
    
    var showRole by remember { mutableStateOf(false) }
    var currentPlayer by remember { mutableStateOf(1) }
    var isActor by remember { mutableStateOf(false) }

    // Determine if current player is an actor
    LaunchedEffect(currentPlayer) {
        isActor = currentPlayer <= actorCount
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TopAppBar(
            title = {},
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFAFAFA)),
            navigationIcon = { IconButton(onClick = { navController.navigateUp() }) { Icon(painterResource(id = R.drawable.ic_back), contentDescription = "Back") } }
        )

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Player $currentPlayer of $players",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        if (showRole) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isActor) Color(0xFF4CAF50) else Color(0xFF2196F3)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isActor) "🎭 ACTOR" else "👀 GUESSER",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Text(
                        text = if (isActor) 
                            "You will act out $wordCount words from the $category category"
                        else 
                            "You will guess what the actors are performing",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Tap to reveal your role",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        if (showRole) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (currentPlayer > 1) {
                    OutlinedButton(
                        onClick = {
                            currentPlayer--
                            showRole = false
                        }
                    ) {
                        Text("Previous Player")
                    }
                }
                
                if (currentPlayer < players) {
                    Button(
                        onClick = {
                            currentPlayer++
                            showRole = false
                        }
                    ) {
                        Text("Next Player")
                    }
                } else {
                    Button(
                        onClick = { navController.navigate("charadesQuickTimer") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                    ) {
                        Text("Start Game", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Button(
                onClick = { showRole = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Reveal Role", fontWeight = FontWeight.Bold)
            }
        }
    }
}
