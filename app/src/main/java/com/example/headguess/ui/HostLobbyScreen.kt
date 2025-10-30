package com.example.headguess.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.compose.ui.res.painterResource
import com.example.headguess.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostLobbyScreen(navController: NavHostController, vm: GameViewModel) {
    var shouldNavigateHome by remember { mutableStateOf(false) }
    
    LaunchedEffect(shouldNavigateHome) {
        if (shouldNavigateHome) {
            navController.navigate("home") {
                popUpTo("home") { inclusive = true }
            }
        }
    }
    
    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        // Handle system back to ensure NSD stops when host leaves lobby
        BackHandler {
            android.util.Log.d("HostLobbyScreen", "System back - stopping hosting safely")
            vm.quickStopHosting()
            navController.navigateUp()
        }
        // Keep NSD active while host stays in lobby; we stop NSD on Back or Start only
        TopAppBar(
            title = {},
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFAFAFA)),
            navigationIcon = { IconButton(onClick = {
                // Use safer hosting stop when leaving lobby
                android.util.Log.d("HostLobbyScreen", "User pressed back - stopping hosting safely")
                vm.quickStopHosting()
                navController.navigateUp()
            }) { Icon(painterResource(id = R.drawable.ic_back), contentDescription = "Back") } }
        )

        // Minimal lobby details only
        Text("Category: ${vm.category.value}")
        Text("Players Joined: ${vm.playersCount.value}/20")

        if (vm.playersCount.value > 0) {
            Button(onClick = {
                vm.startGameForAll()
                navController.navigate("countdown")
            }, modifier = Modifier.padding(top = 16.dp)) {
                Text("Start Game")
            }
        }
    }
}
