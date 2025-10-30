package com.example.headguess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.headguess.ui.GameViewModel
import com.example.headguess.network.NetworkDiscovery
import com.example.headguess.network.DiscoveredHost
import com.example.headguess.utils.PermissionManager
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomImpostorJoinScreen(navController: NavHostController, vm: GameViewModel) {
    var discoveredHosts by remember { mutableStateOf<List<DiscoveredHost>>(emptyList()) }
    var hasPermission by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var discoveryKey by remember { mutableStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }
    val lastSeenByIp = remember { mutableStateMapOf<String, Long>() }
    val scope = rememberCoroutineScope()
    
    // Removed periodic connection tests; rely solely on NSD events
    
    // Watch for host disconnection and immediately clear hosts + show loading
    LaunchedEffect(vm.hostDisconnected.value) {
        if (vm.hostDisconnected.value) {
            android.util.Log.d("CustomImpostorJoinScreen", "Host disconnected detected - immediately clearing hosts and showing loading")
            
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
        android.util.Log.d("CustomImpostorJoinScreen", "Starting discovery (discoveryKey=$discoveryKey)")
        
        // Clear only; do not globally stop discovery here
        
        // IMMEDIATE: Clear hosts immediately to show loading animation
        discoveredHosts = emptyList()
        
        // SAFETY: Reset any stale client state
        if (vm.role.value == Role.CLIENT) {
            android.util.Log.d("CustomImpostorJoinScreen", "Found existing client state - resetting it")
            vm.leaveClient()
        }
        
        // Check if nearby devices permission is granted
        hasPermission = PermissionManager.isPermissionGranted(navController.context)
        
        if (hasPermission) {
            android.util.Log.d("CustomImpostorJoinScreen", "Starting fresh NSD discovery for impostor + custom (cross)")
            
            NetworkDiscovery.discoverMultipleHosts(
                onHostFound = { ip, serviceName ->
                    android.util.Log.d("CustomImpostorJoinScreen", "Host found: $ip ($serviceName)")
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
                    // Immediate removal to avoid delayed disappearance
                    android.util.Log.d("CustomImpostorJoinScreen", "Host lost via NSD: $ip - removing immediately")
                    discoveredHosts = discoveredHosts.filter { it.ip != ip }
                    lastSeenByIp.remove(ip)
                },
                gameType = "impostor"
            )

            // Cross-discover custom impostor if published that way
            NetworkDiscovery.discoverMultipleHosts(
                onHostFound = { ip, serviceName ->
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
                    // Immediate removal for cross-discovered entries as well
                    discoveredHosts = discoveredHosts.filter { it.ip != ip }
                },
                gameType = "custom"
            )
        } else {
            showPermissionDialog = true
        }
    }
    
    DisposableEffect(Unit) {
        android.util.Log.d("CustomImpostorJoinScreen", "Screen entered")
        onDispose {
            android.util.Log.d("CustomImpostorJoinScreen", "Screen leaving - stopping all discoveries")
            NetworkDiscovery.stopAllDiscoveries()
        }
    }
    // Also handle system back to stop discovery consistently
    BackHandler {
        NetworkDiscovery.stopAllDiscoveries()
        navController.popBackStack()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Join Custom Impostor") },
                navigationIcon = {
                    TextButton(onClick = { 
                        NetworkDiscovery.stopAllDiscoveries()
                        navController.popBackStack() 
                    }) {
                        Text("← Back")
                    }
                }
            )
        }
    ) { padding ->
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
                // Permission required
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🔒",
                            style = MaterialTheme.typography.displayLarge
                        )
                        
                        Spacer(Modifier.height(16.dp))
                        
                        Text(
                            text = "Permission Required",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                        
                        Spacer(Modifier.height(8.dp))
                        
                        Text(
                            text = "This app needs nearby devices permission to discover games on your local network.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(Modifier.height(16.dp))
                        
                        Button(
                            onClick = { 
                                PermissionManager.requestNearbyDevicesPermission(navController.context)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Grant Permission")
                        }
                    }
                }
            } else if (isRefreshing) {
                // Show loading animation when refreshing after host disconnection
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Text(
                        text = "Host disconnected. Searching for new hosts...",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }
            } else if (discoveredHosts.isEmpty()) {
                // Loading state - searching for hosts
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Text(
                        text = "Searching for hosts...",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                // Available hosts
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(discoveredHosts) { host ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Custom Impostor Game",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    Text(
                                        text = "Host: ${host.serviceName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Gray
                                    )
                                }
                                
                                Button(
                                    onClick = { 
                                        try {
                                            // Stop discovery immediately when joining
                                            NetworkDiscovery.stopAllDiscoveries()
                                            
                                            // Ensure any hosting is stopped before joining
                                            if (vm.role.value == Role.HOST) {
                                                android.util.Log.d("CustomImpostorJoinScreen", "Stopping hosting before joining as client")
                                                vm.quickStopHosting()
                                            }
                                            
                                            vm.joinHost(host.ip) {
                                                navController.navigate("impostorClientLobby")
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("CustomImpostorJoinScreen", "Error joining host", e)
                                            // Remove the unreachable host immediately
                                            discoveredHosts = discoveredHosts.filter { it.ip != host.ip }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Join")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
