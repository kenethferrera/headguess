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
import androidx.compose.ui.res.painterResource
import com.example.headguess.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImpostorQuickSetupScreen(navController: NavHostController, vm: GameViewModel) {
    var players by remember { mutableStateOf(3) }
    var category by remember { mutableStateOf(vm.category.value.ifEmpty { "Personality" }) }
    var impostorCount by remember { mutableStateOf(vm.impostorCount.value.coerceIn(1, players)) }
    var showRole by remember { mutableStateOf(vm.showImpostorRole.value) }

    LaunchedEffect(Unit) { vm.selectCategory(category) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(
            title = {},
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFAFAFA)),
            navigationIcon = { IconButton(onClick = { navController.navigateUp() }) { Icon(painterResource(id = R.drawable.ic_back), contentDescription = "Back") } }
        )

        Spacer(Modifier.height(16.dp))

        Text("Quick Play - Impostor", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(16.dp))

        // Players stepper
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Number of Players", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val minPlayers = if (impostorCount >= 2) 5 else 3
                    OutlinedButton(onClick = {
                        val newVal = (players - 1).coerceAtLeast(minPlayers)
                        players = newVal
                    }) { Text("-") }
                    Text(players.toString(), modifier = Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.titleLarge)
                    OutlinedButton(onClick = {
                        players = (players + 1).coerceAtMost(20)
                    }) { Text("+") }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Impostor count and Show Role toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Number of Impostors", style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = {
                            if (impostorCount > 1) {
                                impostorCount -= 1
                                // when dropping below 2, min players becomes 3 (no need to shrink players)
                            }
                        }) { Text("-") }
                        Text(impostorCount.toString(), modifier = Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.titleLarge)
                        OutlinedButton(onClick = {
                            if (impostorCount < 5) {
                                impostorCount += 1
                                // ensure minimum players: if 2+ impostors, min 5
                                val minPlayersNow = if (impostorCount >= 2) 5 else 3
                                if (players < minPlayersNow) players = minPlayersNow
                            }
                        }) { Text("+") }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show impostors their role", style = MaterialTheme.typography.titleMedium)
                    Switch(checked = showRole, onCheckedChange = { showRole = it })
                }
            }
        }

        // Simple category chooser (cycling)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Category: $category", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = {
                    val list = listOf("Personality","Places","Animals","Objects","Food")
                    val idx = list.indexOf(category).coerceAtLeast(0)
                    val next = list[(idx + 1) % list.size]
                    category = next
                    vm.selectCategory(next)
                }) { Text("Change") }
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                // persist to VM so reveal screen uses these settings
                vm.impostorCount.value = impostorCount.coerceIn(1, players)
                vm.showImpostorRole.value = showRole
                navController.navigate("impostorQuickReveal/$players/$category")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = players >= (if (impostorCount >= 2) 5 else 3),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) { Text("Start", fontWeight = FontWeight.Bold) }
    }
}












