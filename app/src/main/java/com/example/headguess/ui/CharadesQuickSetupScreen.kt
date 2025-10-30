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
fun CharadesQuickSetupScreen(navController: NavHostController, vm: GameViewModel) {
    var category by remember { mutableStateOf(vm.category.value.ifEmpty { "Personality" }) }
    var wordCount by remember { mutableStateOf(3) }

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

        Text("Quick Play - Charades", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(32.dp))

        // Category chooser
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
                    val list = listOf("Personality", "Objects", "Animals", "Sports")
                    val idx = list.indexOf(category).coerceAtLeast(0)
                    val next = list[(idx + 1) % list.size]
                    category = next
                    vm.selectCategory(next)
                }) { Text("Change") }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Word count stepper
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
                Text("Words: $wordCount", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { if (wordCount > 1) wordCount -= 1 }) { Text("-") }
                    Text(wordCount.toString(), modifier = Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.titleLarge)
                    OutlinedButton(onClick = { if (wordCount < 10) wordCount += 1 }) { Text("+") }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = { navController.navigate("charadesQuickGame/$category/$wordCount") },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) { Text("Start", fontWeight = FontWeight.Bold) }
    }
}
