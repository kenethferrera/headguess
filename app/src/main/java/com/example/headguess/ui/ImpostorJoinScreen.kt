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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.example.headguess.ui.GameViewModel
import com.example.headguess.network.NetworkDiscovery
import com.example.headguess.network.DiscoveredHost
import com.example.headguess.utils.PermissionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImpostorJoinScreen(navController: NavHostController, vm: GameViewModel) {
    var discoveredHosts by remember { mutableStateOf<List<DiscoveredHost>>(emptyList()) }
    var hasPermission by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var discoveryKey by remember { mutableStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }
    val lastSeenByIp = remember { mutableStateMapOf<String, Long>() }
    
    // No periodic socket tests; rely solely on NSD events (matches Charades/Guess)
    
    // Watch for host disconnection and immediately clear hosts + show loading
    LaunchedEffect(vm.hostDisconnected.value) {
        if (vm.hostDisconnected.value) {
            android.util.Log.d("ImpostorJoinScreen", "Host disconnected detected - immediately clearing hosts and showing loading")
            
            // Immediately clear ALL hosts and show loading
            discoveredHosts = emptyList()
            isRefreshing = true
            
            // Stop all discoveries immediately (removed)
            
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
        android.util.Log.d("ImpostorJoinScreen", "Starting discovery (discoveryKey=$discoveryKey)")
        
        // Clear only; do not globally stop discovery here
        
        // IMMEDIATE: Clear hosts immediately to show loading animation
        discoveredHosts = emptyList()
        
        // SAFETY: Reset any stale client state
        if (vm.role.value == Role.CLIENT) {
            android.util.Log.d("ImpostorJoinScreen", "Found existing client state - resetting it")
            vm.leaveClient()
        }
        
        // Check if nearby devices permission is granted
        hasPermission = PermissionManager.isPermissionGranted(navController.context)
        
        if (hasPermission) {
            android.util.Log.d("ImpostorJoinScreen", "Starting fresh NSD discovery for impostor")
            
            NetworkDiscovery.discoverMultipleHosts(
                onHostFound = { ip, serviceName ->
                    android.util.Log.d("ImpostorJoinScreen", "Host found: $ip ($serviceName)")
                    // Update timestamp for existing host or add new host
                    lastSeenByIp[ip] = System.currentTimeMillis()
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
                },
                onHostLost = { ip ->
                    android.util.Log.d("ImpostorJoinScreen", "Host lost via NSD: $ip - removing immediately")
                    discoveredHosts = discoveredHosts.filter { it.ip != ip }
                    lastSeenByIp.remove(ip)
                },
                gameType = "impostor"
            )

            // Also discover custom to enable cross-connection if host publishes as custom
            NetworkDiscovery.discoverMultipleHosts(
                onHostFound = { ip, serviceName ->
                    android.util.Log.d("ImpostorJoinScreen", "Custom host found: $ip ($serviceName)")
                    val existingHost = discoveredHosts.find { it.ip == ip }
                    if (existingHost != null) {
                        discoveredHosts = discoveredHosts.map { 
                            if (it.ip == ip) it.copy(timestamp = System.currentTimeMillis()) else it 
                        }
                    } else {
                        discoveredHosts = discoveredHosts + DiscoveredHost(ip, serviceName)
                    }
                },
                onHostLost = { ip ->
                    android.util.Log.d("ImpostorJoinScreen", "Custom host lost via NSD: $ip - removing immediately")
                    discoveredHosts = discoveredHosts.filter { it.ip != ip }
                },
                gameType = "custom"
            )
        } else {
            showPermissionDialog = true
        }
    }
    
    // Stop discovery when leaving this screen (match Guess the Word behavior)
    DisposableEffect(Unit) {
        android.util.Log.d("ImpostorJoinScreen", "Screen entered")
        onDispose {
            android.util.Log.d("ImpostorJoinScreen", "Screen leaving - stopping all discoveries")
            NetworkDiscovery.stopAllDiscoveries()
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
            navigationIcon = { IconButton(onClick = {
                // Stop NSD discovery before leaving (consistent with Guess the Word)
                NetworkDiscovery.stopAllDiscoveries()
                navController.navigateUp()
            }) { Icon(painterResource(id = R.drawable.ic_back), contentDescription = "Back") } }
        )
        
        Spacer(Modifier.height(24.dp))
        
        if (isRefreshing) {
            // Show loading animation when refreshing after host disconnection
            CircularProgressIndicator(
                modifier = Modifier.padding(16.dp)
            )
            Text("Host disconnected. Searching for new hosts...", color = Color.Gray)
        } else if (discoveredHosts.isEmpty()) {
            // Show loading animation when no hosts are available
            CircularProgressIndicator(
                modifier = Modifier.padding(16.dp)
            )
            Text("Searching for hosts...", color = Color.Gray)
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
                        try {
                            // Stop discovery immediately when joining (consistent with Guess the Word)
                            android.util.Log.d("ImpostorJoinScreen", "User clicked to join - stopping all discoveries")
                            NetworkDiscovery.stopAllDiscoveries()
                            
                            // Ensure any hosting is stopped before joining
                            if (vm.role.value == Role.HOST) {
                                android.util.Log.d("ImpostorJoinScreen", "Stopping hosting before joining as client")
                                vm.quickStopHosting()
                            }
                            
                            vm.joinHost(host.ip) {
                                navController.navigate("impostorClientLobby")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("ImpostorJoinScreen", "Error joining host", e)
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








