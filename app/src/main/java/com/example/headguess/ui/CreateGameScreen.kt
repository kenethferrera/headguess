package com.example.headguess.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGameScreen(navController: NavHostController, vm: GameViewModel) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(16.dp)
    ) {
        TopAppBar(
            title = {},
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFAFAFA)),
        navigationIcon = { TextButton(onClick = { 
            // Use safer hosting stop when going back
            android.util.Log.d("CreateGameScreen", "User pressed back - stopping hosting safely")
            vm.quickStopHosting()
            navController.navigate("home") 
        }) { Text("Back") } }
        )

        Spacer(Modifier.weight(1f))

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = { navController.navigate("category") }, modifier = Modifier.fillMaxWidth(0.7f)) { Text("Create Game") }
            Spacer(Modifier.height(16.dp))
            Button(onClick = { navController.navigate("join") }, modifier = Modifier.fillMaxWidth(0.7f)) { Text("Join Game") }
        }

        Spacer(Modifier.weight(1f))
    }
}




