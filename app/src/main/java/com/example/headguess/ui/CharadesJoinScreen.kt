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
import com.example.headguess.ui.GameViewModel
import com.example.headguess.network.NetworkDiscovery
import com.example.headguess.network.DiscoveredHost
import com.example.headguess.utils.PermissionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharadesJoinScreen(navController: NavHostController, vm: GameViewModel) {
    var discoveredHosts by remember { mutableStateOf<List<DiscoveredHost>>(emptyList()) }
    var hasPermission by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        // Check if nearby devices permission is granted
        hasPermission = PermissionManager.isPermissionGranted(navController.context)
        
        if (hasPermission) {
            NetworkDiscovery.discoverMultipleHosts(
                onHostFound = { ip, serviceName ->
                    discoveredHosts = discoveredHosts.filter { it.ip != ip } + DiscoveredHost(ip, serviceName)
                },
                onHostLost = { ip ->
                    discoveredHosts = discoveredHosts.filter { it.ip != ip }
                },
                gameType = "charades"
            )
        } else {
            showPermissionDialog = true
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
        TopAppBar(
            title = {},
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFAFAFA)),
            navigationIcon = {
                TextButton(onClick = { navController.navigateUp() }) {
                    Text("Back")
                }
            }
        )
        
        Spacer(Modifier.height(24.dp))
        
        if (discoveredHosts.isEmpty()) {
            Text(
                text = "Searching for hosts...",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            CircularProgressIndicator(
                modifier = Modifier.padding(16.dp)
            )
        } else {
            Text(
                text = "Available Hosts (${discoveredHosts.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            discoveredHosts.forEach { host ->
                Button(
                    onClick = {
                        vm.joinHost(host.ip) {
                            navController.navigate("charadesClientLobby")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "${host.serviceName} (${host.ip})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
    
    // Permission dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Permission Required") },
            text = { Text("This app needs nearby devices permission to discover games on your local network.") },
            confirmButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}
