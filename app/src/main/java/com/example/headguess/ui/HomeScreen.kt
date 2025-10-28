package com.example.headguess.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.core.view.WindowInsetsCompat
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import kotlinx.coroutines.launch

// Data class for game information
data class Game(
    val title: String,
    val description: String,
    val iconResId: Int,
    val route: String
)

// Data class for navigation menu items
data class NavigationItem(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

// Data class for navigation sections
data class NavigationSection(
    val title: String,
    val items: List<NavigationItem>
)

// Sample game list
val gameList = listOf(
    Game(
        title = "YES or NO Word Guess",
        description = "Ask yes or no questions to guess the word!",
        iconResId = android.R.drawable.ic_menu_edit, // Using system icon as placeholder
        route = "yesNoCategory" // Route to YES or NO game
    ),
    Game(
        title = "Guess the Word",
        description = "Hold your phone on your head and guess the word from your friends' clues.",
        iconResId = android.R.drawable.ic_menu_edit, // Using system icon as placeholder
        route = "create" // Navigate to create/join page first - THE ACTUAL GAME
    ),
    Game(
        title = "Charades Classic",
        description = "Act it out! Guess without saying the word.",
        iconResId = android.R.drawable.ic_menu_myplaces, // Using system icon as placeholder
        route = "charadesCreate"
    ),
    Game(
        title = "Impostor",
        description = "Find the impostor among your friends!",
        iconResId = android.R.drawable.ic_menu_help, // Using system icon as placeholder
        route = "impostorHome"
    )
)

// Country list
val countryList = listOf(
    "General",
    "Custom",
    "Algeria", "Argentina", "Bangladesh", "Brazil", "Chile", "Colombia", 
    "Egypt", "Ethiopia", "Ghana", "India", "Indonesia", "Iraq", "Japan", 
    "Kenya", "Malaysia", "Mexico", "Morocco", "Myanmar", "Nepal", "Nigeria", 
    "Pakistan", "Peru", "Philippines", "Russia", "Saudi Arabia", 
    "South Africa", "South Korea", "Sri Lanka", "Sudan", "Tanzania", 
    "Thailand", "Turkey", "UAE", "Ukraine", "United States", "Venezuela", "Vietnam"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    var isVisible by remember { mutableStateOf(false) }
    var selectedCountry by remember { mutableStateOf(CountryManager.selectedCountry) }
    var expanded by remember { mutableStateOf(false) }
    
    // Username state
    val context = LocalContext.current
    var currentUsername by remember { mutableStateOf(UsernameManager.getUsername(context)) }
    var showUsernameDialog by remember { mutableStateOf(false) }
    var drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // Trigger animation on composition
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    // Navigation sections
    val navigationSections = listOf(
        NavigationSection(
            title = "GAMES",
            items = listOf(
                NavigationItem(
                    title = "Home",
                    icon = Icons.Default.Home,
                    onClick = { /* Already on home */ }
                )
            )
        ),
        NavigationSection(
            title = "SETTINGS",
            items = listOf(
                NavigationItem(
                    title = "Username",
                    icon = Icons.Default.Person,
                    onClick = { 
                        showUsernameDialog = true
                        scope.launch { drawerState.close() }
                    }
                ),
                NavigationItem(
                    title = "Settings",
                    icon = Icons.Default.Settings,
                    onClick = { /* TODO: Navigate to settings */ }
                )
            )
        ),
        NavigationSection(
            title = "HELP",
            items = listOf(
                NavigationItem(
                    title = "About",
                    icon = Icons.Default.Info,
                    onClick = { /* TODO: Show about dialog */ }
                ),
                NavigationItem(
                    title = "Help",
                    icon = Icons.Default.Info,
                    onClick = { /* TODO: Show help */ }
                )
            )
        )
    )
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NavigationDrawerContent(
                username = currentUsername,
                navigationSections = navigationSections,
                onDismiss = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFAFAFA))
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Custom Header with Hamburger Menu and Country Selection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hamburger Menu Button
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Country Selection Dropdown
                Box {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCountry,
                            onValueChange = { },
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .width(140.dp),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            countryList.forEach { country ->
                                DropdownMenuItem(
                                    text = { Text(country) },
                                    onClick = {
                                        selectedCountry = country
                                        CountryManager.setCountry(country)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        
        // Game Cards Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(gameList) { game ->
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(600)) + 
                           slideInVertically(
                               animationSpec = tween(600),
                               initialOffsetY = { it / 3 }
                           )
                ) {
                    GameCard(
                        game = game,
                        onClick = {
                            // If Custom country is selected, route to custom input first
                            if (CountryManager.selectedCountry == "Custom") {
                                when {
                                    game.route.startsWith("impostor") -> navController.navigate("customImpostorCreateJoin")
                                    game.route == "create" -> navController.navigate("customGuessCreate") // Guess the Word
                                    game.route == "yesNoCategory" -> navController.navigate("customWords/yesno") // YES or NO Word Guess
                                    else -> navController.navigate("customCharadesCreateJoin") // Charades
                                }
                            } else {
                                navController.navigate(game.route)
                            }
                        }
                    )
                }
            }
        }
        
        // Username Edit Dialog
        if (showUsernameDialog) {
            UsernameEditDialog(
                currentUsername = currentUsername,
                onUsernameChanged = { newUsername ->
                    currentUsername = newUsername
                    UsernameManager.setUsername(context, newUsername)
                },
                onDismiss = { showUsernameDialog = false }
            )
        }
        }
    }
}

@Composable
fun NavigationDrawerContent(
    username: String,
    navigationSections: List<NavigationSection>,
    onDismiss: () -> Unit
) {
    var soundEnabled by remember { mutableStateOf(true) }
    
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .background(Color(0xFF2C2C2C))
            .padding(16.dp)
            .width(200.dp)
    ) {
        // Username section with edit button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🧍", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Username: $username",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick = { /* TODO: Edit username */ },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("✎", color = Color.White)
            }
        }
        
        // Sound toggle section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🔊", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Sound:",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = soundEnabled,
                onCheckedChange = { soundEnabled = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF4CAF50),
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color(0xFF666666)
                )
            )
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Sign in with Google button
        Button(
            onClick = { /* TODO: Google sign in */ },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4285F4)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Sign in with Google",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Divider line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFF666666))
        )
        
        Spacer(Modifier.height(16.dp))
        
        // Privacy Policy section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📜", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Privacy Policy",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier.clickable { /* TODO: Open privacy policy */ }
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "(App version)",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
        
        Spacer(Modifier.weight(1f))
    }
}

@Composable
fun GameCard(
    game: Game,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Game Icon
            Icon(
                painter = painterResource(id = game.iconResId),
                contentDescription = game.title,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Game Title
            Text(
                text = game.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Game Description
            Text(
                text = game.description,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Play Button
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Play",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun UsernameEditDialog(
    currentUsername: String,
    onUsernameChanged: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var username by remember { mutableStateOf(currentUsername) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Username",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter your username:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (username.isNotBlank()) {
                        onUsernameChanged(username.trim())
                        onDismiss()
                    }
                },
                enabled = username.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

