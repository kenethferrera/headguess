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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImpostorQuickSetupScreen(navController: NavHostController, vm: GameViewModel) {
    var players by remember { mutableStateOf(3) }
    var category by remember { mutableStateOf(vm.category.value.ifEmpty { "Personality" }) }

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
            navigationIcon = { TextButton(onClick = { navController.navigateUp() }) { Text("Back") } }
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
                Text("How many players?", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { if (players > 2) players -= 1 }) { Text("-") }
                    Text(players.toString(), modifier = Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.titleLarge)
                    OutlinedButton(onClick = { if (players < 10) players += 1 }) { Text("+") }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

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
            onClick = { navController.navigate("impostorQuickReveal/$players/$category") },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = players >= 2,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) { Text("Start", fontWeight = FontWeight.Bold) }
    }
}











