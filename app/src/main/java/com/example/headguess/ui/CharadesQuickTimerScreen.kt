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
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharadesQuickTimerScreen(navController: NavHostController) {
    var countdown by remember { mutableStateOf(3) }
    var isCountingDown by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (countdown > 0 && isCountingDown) {
            delay(1000)
            countdown--
        }
        if (isCountingDown) {
            // Navigate to game after countdown
            delay(500)
            navController.navigate("charadesGame/Personality/1/3") {
                popUpTo("charadesQuickTimer") { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Get Ready!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(48.dp))

        if (isCountingDown) {
            Text(
                text = countdown.toString(),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = if (countdown <= 1) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
            )
        } else {
            Text(
                text = "GO!",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50)
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Actors: Get ready to perform!\nGuessers: Get ready to guess!",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = Color.Gray
        )

        Spacer(Modifier.height(48.dp))

        OutlinedButton(
            onClick = {
                isCountingDown = false
                navController.navigate("charadesGame/Personality/1/3") {
                    popUpTo("charadesQuickTimer") { inclusive = true }
                }
            },
            enabled = isCountingDown
        ) {
            Text("Skip Countdown")
        }
    }
}


