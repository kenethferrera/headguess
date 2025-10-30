package com.example.headguess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.headguess.network.NetworkDiscovery
import com.example.headguess.network.DiscoveredHost
import com.example.headguess.ui.GameViewModel
import com.example.headguess.utils.PermissionManager
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Arrangement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import com.example.headguess.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomGuessJoinScreen(navController: NavHostController, vm: GameViewModel) {
    var discoveredHosts by remember { mutableStateOf<List<DiscoveredHost>>(emptyList()) }
    var hasPermission by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var discoveryKey by remember { mutableStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Removed periodic connection test to avoid dropping valid hosts
    
    // Watch for host disconnection and immediately clear hosts + show loading
    LaunchedEffect(vm.hostDisconnected.value) {
        if (vm.hostDisconnected.value) {
            android.util.Log.d("CustomGuessJoinScreen", "Host disconnected detected - immediately clearing hosts and showing loading")
            
            // Immediately clear ALL hosts and show loading
            discoveredHosts = emptyList()
            isRefreshing = true
            
            // No wait - immediate refresh
            kotlinx.coroutines.delay(100)
            
            // Reset the flag and trigger refresh
            vm.hostDisconnected.value = false
            discoveryKey++
            isRefreshing = false
        }
    }
    
    // Re-run discovery whenever discoveryKey changes
    LaunchedEffect(discoveryKey) {
        android.util.Log.d("CustomGuessJoinScreen", "Starting discovery (discoveryKey=$discoveryKey)")
        
        // IMMEDIATE: Clear hosts immediately to show loading animation
        discoveredHosts = emptyList()
        
        // SAFETY: Reset any stale client state
        if (vm.role.value == Role.CLIENT) {
            android.util.Log.d("CustomGuessJoinScreen", "Found existing client state - resetting it")
            vm.leaveClient()
        }
        
        // Check if nearby devices permission is granted
        hasPermission = PermissionManager.isPermissionGranted(navController.context)
        
        if (hasPermission) {
            android.util.Log.d("CustomGuessJoinScreen", "Starting fresh NSD discovery for custom guessword")
            
            NetworkDiscovery.discoverMultipleHosts(
                onHostFound = { ip, serviceName ->
                    android.util.Log.d("CustomGuessJoinScreen", "Host found: $ip ($serviceName)")
                    // Update timestamp for existing host or add new host
                    val existingHost = discoveredHosts.find { it.ip == ip }
                    if (existingHost != null) {
                        // Update timestamp for existing host
                        discoveredHosts = discoveredHosts.map { 
                            if (it.ip == ip) it.copy(timestamp = System.currentTimeMillis()) else it 
                        }
                    } else {
                        // Add new host
                        discoveredHosts = discoveredHosts + DiscoveredHost(ip, serviceName)
                    }
                    // No extra validation on found; rely on NSD and pruning
                },
                onHostLost = { ip ->
                    // Match General behavior: primary game type removes immediately
                    android.util.Log.d("CustomGuessJoinScreen", "Host lost via NSD (custom): $ip - removing immediately")
                    discoveredHosts = discoveredHosts.filter { it.ip != ip }
                },
                gameType = "custom"
            )

            // Also discover general Guess the Word to support cross-connection (host in general, client in custom)
            NetworkDiscovery.discoverMultipleHosts(
                onHostFound = { ip, serviceName ->
                    val existingHost = discoveredHosts.find { it.ip == ip }
                    if (existingHost != null) {
                        discoveredHosts = discoveredHosts.map { if (it.ip == ip) it.copy(timestamp = System.currentTimeMillis()) else it }
                    } else {
                        discoveredHosts = discoveredHosts + DiscoveredHost(ip, serviceName)
                    }
                },
                onHostLost = { ip ->
                    // Cross connection: remove immediately to avoid delay
                    discoveredHosts = discoveredHosts.filter { it.ip != ip }
                },
                gameType = "guessword"
            )
        } else {
            showPermissionDialog = true
        }
    }

    // Removed pruning; rely on NSD onHostLost and debounced removal only
    
    // Stop discovery when leaving this screen (match Guess general)
    DisposableEffect(Unit) {
        android.util.Log.d("CustomGuessJoinScreen", "Screen entered")
        onDispose {
            android.util.Log.d("CustomGuessJoinScreen", "Screen leaving - stopping all discoveries")
            NetworkDiscovery.stopAllDiscoveries()
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Join Custom Game") },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFAFAFA)),
            navigationIcon = {
                IconButton(onClick = { 
                    android.util.Log.d("CustomGuessJoinScreen", "User pressed back - stopping all discoveries")
                    NetworkDiscovery.stopAllDiscoveries()
                    navController.popBackStack() 
                }) {
                    Icon(painterResource(id = R.drawable.ic_back), contentDescription = "Back")
                }
            }
        )
    }) { padding ->
        // Also handle system back to stop discovery consistently
        BackHandler {
            android.util.Log.d("CustomGuessJoinScreen", "System back - stopping all discoveries")
            NetworkDiscovery.stopAllDiscoveries()
            navController.popBackStack()
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFAFAFA))
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
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
                    // No manual Load button; rely on continuous discovery
                    if (discoveredHosts.isNotEmpty()) {
                        Text(
                            text = "Available Custom Game Hosts (${discoveredHosts.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        discoveredHosts.forEach { host ->
                            Button(
                                onClick = {
                                    try {
                                        // Stop discovery immediately when joining (match Guess general)
                                        android.util.Log.d("CustomGuessJoinScreen", "User clicked to join - stopping all discoveries")
                                        NetworkDiscovery.stopAllDiscoveries()
                                        
                                        // Ensure any hosting is stopped before joining
                                        if (vm.role.value == Role.HOST) {
                                            android.util.Log.d("CustomGuessJoinScreen", "Stopping hosting before joining as client")
                                            vm.quickStopHosting()
                                        }
                                        
                                        vm.joinHost(host.ip) {
                                            navController.navigate("lobby")
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("CustomGuessJoinScreen", "Error joining host", e)
                                        // Remove the unreachable host immediately
                                        discoveredHosts = discoveredHosts.filter { it.ip != host.ip }
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
            }
        }
    }
    
    // Permission dialog
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
