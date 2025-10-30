package com.example.headguess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.compose.ui.res.painterResource
import com.example.headguess.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(navController: NavHostController, vm: GameViewModel) {
    val hostDisconnected by vm.hostDisconnected
    
    LaunchedEffect(vm.gameStarted.value) {
        // When gameStarted is true, navigate to countdown
        if (vm.gameStarted.value) {
            navController.navigate("countdown")
        }
    }
    
    // Handle host disconnection - immediately navigate back
    LaunchedEffect(hostDisconnected) {
        if (hostDisconnected) {
            android.util.Log.d("LobbyScreen", "Host disconnected - immediately navigating back to join screen")
            // Pop back to join screen (either "join" or "customGuessJoin")
            navController.popBackStack()
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
        // Ensure system back also leaves the client and decrements host count
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
        
        Text(
            text = "Category: ${vm.category.value}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        CircularProgressIndicator(
            modifier = Modifier.padding(16.dp)
        )
        
        Text(
            text = "Waiting for host to start...",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
    }
}
