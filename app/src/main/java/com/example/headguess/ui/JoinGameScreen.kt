package com.example.headguess.ui

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavHostController
import com.example.headguess.network.NetworkDiscovery
import com.example.headguess.network.DiscoveredHost
import com.example.headguess.utils.PermissionManager
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import com.example.headguess.R

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun JoinGameScreen(navController: NavHostController, vm: GameViewModel) {
    var discoveredHosts by remember { mutableStateOf<List<DiscoveredHost>>(emptyList()) }
    var connectionError by remember { mutableStateOf("") }
    var hasPermission by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var discoveryKey by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    // Refresh discovery when host disconnects
    LaunchedEffect(vm.hostDisconnected.value) {
        if (vm.hostDisconnected.value) {
            discoveredHosts = emptyList()
            vm.hostDisconnected.value = false
            discoveryKey++
        }
    }

    LaunchedEffect(discoveryKey) {
        // Check if nearby devices permission is granted
        hasPermission = PermissionManager.isPermissionGranted(navController.context)
        
        if (hasPermission) {
            // Search for General Guess the Word hosts
            NetworkDiscovery.discoverMultipleHosts(
                onHostFound = { ip, serviceName ->
                    discoveredHosts = discoveredHosts.filter { it.ip != ip } + DiscoveredHost(ip, serviceName)
                },
                onHostLost = { ip ->
                    discoveredHosts = discoveredHosts.filter { it.ip != ip }
                },
                gameType = "guessword"
            )

            // Also search for Custom Guess the Word hosts
            NetworkDiscovery.discoverMultipleHosts(
                onHostFound = { ip, serviceName ->
                    discoveredHosts = discoveredHosts.filter { it.ip != ip } + DiscoveredHost(ip, serviceName)
                },
                onHostLost = { ip ->
                    // Cross connection: remove immediately to avoid delay
                    discoveredHosts = discoveredHosts.filter { it.ip != ip }
                },
                gameType = "custom"
            )
        } else {
            showPermissionDialog = true
        }
    }

    // Removed pruning to avoid dropping valid hosts; rely on NSD onHostLost only
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        TopAppBar(
            title = {},
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFAFAFA)),
            navigationIcon = { IconButton(onClick = { 
                NetworkDiscovery.stopAllDiscoveries()
                navController.navigateUp() 
            }) { Icon(painterResource(id = R.drawable.ic_back), contentDescription = "Back") } }
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
                if (discoveredHosts.isNotEmpty()) {
                    Text("Available Hosts (${discoveredHosts.size})")
                    Spacer(Modifier.height(16.dp))
                    
                    discoveredHosts.forEach { host ->
                        Button(
                            onClick = {
                                connectionError = ""
                                try {
                                    NetworkDiscovery.stopAllDiscoveries()
                                    vm.joinHost(host.ip) { navController.navigate("lobby") }
                                } catch (e: Exception) {
                                    connectionError = "Failed to connect: ${e.message}"
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
        
        if (connectionError.isNotEmpty()) {
            Text("Error: $connectionError", color = Color.Red)
        }
        
        // Removed manual IP and generic Join button; join by tapping available IP

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
    // System back should also stop discovery
    BackHandler {
        NetworkDiscovery.stopAllDiscoveries()
        navController.navigateUp()
    }
}




