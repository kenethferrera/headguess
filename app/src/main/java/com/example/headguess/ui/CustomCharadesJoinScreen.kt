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
import com.example.headguess.network.NetworkDiscovery
import com.example.headguess.network.DiscoveredHost
import com.example.headguess.utils.PermissionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomCharadesJoinScreen(navController: NavHostController, vm: GameViewModel) {
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
                gameType = "charades" // Search for charades hosts
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
            navigationIcon = { TextButton(onClick = { navController.navigateUp() }) { Text("Back") } }
        )

        Spacer(Modifier.height(24.dp))
        
        if (!hasPermission) {
            Text("Nearby devices permission is required to discover hosts.", color = Color.Red)
            Text("Please enable it in app settings.", color = Color.Red)
            Button(onClick = {
                PermissionManager.requestNearbyDevicesPermission(navController.context)
            }, modifier = Modifier.padding(top = 8.dp)) {
                Text("Open Settings")
            }
        } else {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (discoveredHosts.isEmpty()) {
                    Text("Searching for custom charades hosts...")
                    Spacer(Modifier.height(16.dp))
                    CircularProgressIndicator()
                } else {
                    Text("Available Custom Charades Hosts (${discoveredHosts.size})")
                    Spacer(Modifier.height(16.dp))
                    
                    discoveredHosts.forEach { host ->
                        Button(
                            onClick = {
                                vm.joinHost(host.ip) { 
                                    navController.navigate("charadesClientLobby")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) { 
                            Text("${host.serviceName} (${host.ip})") 
                        }
                    }
                }
            }
        }
        
        if (showPermissionDialog && !hasPermission) {
            AlertDialog(
                onDismissRequest = { showPermissionDialog = false },
                title = { Text("Enable Permissions") },
                text = { Text("HeadGuess needs Nearby devices (Android 13+) or Location (Android 5–12) to find hosts on Wi‑Fi/hotspot. You can enable access in App Settings.") },
                confirmButton = {
                    TextButton(onClick = {
                        PermissionManager.openAppSettings(navController.context)
                        showPermissionDialog = false
                    }) { Text("Take me there") }
                },
                dismissButton = {
                    TextButton(onClick = { showPermissionDialog = false }) { Text("Close") }
                }
            )
        }
    }
}
