package com.example.headguess

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.headguess.ui.*
import com.example.headguess.ui.Role
import com.example.headguess.network.NetworkDiscovery
import com.example.headguess.utils.PermissionManager
import com.example.headguess.utils.NotificationHelper

class MainActivity : ComponentActivity() {
    companion object {
        private const val REQ_PERMISSIONS = 100
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide action bar explicitly
        actionBar?.hide()
        
        // Basic window configuration - theme handles status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Request runtime permissions with system dialog (Android 6+)
        requestRuntimePermissionsIfNeeded()
        
        setContent {
            NetworkDiscovery.init(applicationContext)
            App()
        }
    }

    private fun requestRuntimePermissionsIfNeeded() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions += Manifest.permission.NEARBY_WIFI_DEVICES
            // Optional but recommended to show our notification fallback if denied
            permissions += Manifest.permission.POST_NOTIFICATIONS
        } else {
            // Older Android required location for NSD discovery
            permissions += Manifest.permission.ACCESS_FINE_LOCATION
        }

        val need = permissions.any {
            ContextCompat.checkSelfPermission(this, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (need) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), REQ_PERMISSIONS)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_PERMISSIONS) {
            val allGranted = grantResults.isNotEmpty() && grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }
            if (!allGranted) {
                // Show a notification with guidance and action to open settings
                NotificationHelper.showNearbyPermissionNotification(this)
            }
        }
    }
}

@Composable
fun App() {
    val navController = rememberNavController()
    val gameViewModel: GameViewModel = viewModel()

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AppNavHost(navController, gameViewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost(navController: NavHostController, vm: GameViewModel) {
    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController) }
        // Custom input routes
        composable("customWords") { CustomWordsScreen(navController, vm) }
        composable("customWords/{gameMode}") { backStackEntry ->
            val gameMode = backStackEntry.arguments?.getString("gameMode") ?: "guessword"
            CustomWordsScreen(navController, vm, gameMode)
        }
        composable("customImpostorCreateJoin") { CustomImpostorCreateJoinScreen(navController) }
        composable("customImpostorQuickPlay") { CustomImpostorQuickPlayScreen(navController, vm) }
        composable("customImpostorQuickPlayWithPopup") { CustomImpostorQuickPlayScreen(navController, vm, showPopupOnLoad = true) }
        composable("customImpostorHost") { CustomImpostorHostScreen(navController, vm) }
        composable("customImpostorJoin") { CustomImpostorJoinScreen(navController, vm) }
        composable("customImpostor/{playerCount}") { backStackEntry ->
            val playerCount = backStackEntry.arguments?.getString("playerCount")?.toIntOrNull() ?: 3
            CustomImpostorScreen(navController, vm, playerCount)
        }
        composable("customImpostorWithPopup/{playerCount}") { backStackEntry ->
            val playerCount = backStackEntry.arguments?.getString("playerCount")?.toIntOrNull() ?: 3
            CustomImpostorScreen(navController, vm, playerCount, showPopupOnLoad = true, impostorCount = vm.impostorCount.value, showImpostorRole = vm.showImpostorRole.value)
        }
        composable("customGuessCreate") { CustomGuessCreateScreen(navController) }
        composable("customGuessJoin") { CustomGuessJoinScreen(navController, vm) }
        composable("customCharadesJoin") { CustomCharadesJoinScreen(navController, vm) }
        composable("customCharadesCreateJoin") { CustomCharadesCreateJoinScreen(navController) }
        composable("customCharadesHost") { CustomCharadesHostScreen(navController) }
        composable("customWords/{gameMode}/{actorCount}/{wordCount}") { backStackEntry ->
            val gameMode = backStackEntry.arguments?.getString("gameMode") ?: "guessword"
            val actorCount = backStackEntry.arguments?.getString("actorCount")?.toIntOrNull() ?: 1
            val wordCount = backStackEntry.arguments?.getString("wordCount")?.toIntOrNull() ?: 5
            CustomWordsScreen(navController, vm, gameMode, actorCount, wordCount)
        }
        composable("category") { CategorySelectionScreen(navController, vm) }
        composable("create") { CreateGameScreen(navController, vm) }
        composable("join") { JoinGameScreen(navController, vm) }
        composable("lobby") { 
            if (vm.role.value == Role.HOST) {
                HostLobbyScreen(navController, vm)
            } else {
                LobbyScreen(navController, vm)
            }
        }
        composable("countdown") { CountdownScreen(navController, vm) }
        composable("game") { GameScreen(navController, vm) }
        composable("result") { ResultScreen(navController, vm) }
        
        // YES or NO Word Guess game routes
        composable("yesNoCategory") { YesNoCategoryScreen(navController) }
        composable("yesNoSettings/{category}") { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: ""
            YesNoSettingsScreen(navController, category)
        }
        composable("yesNoGame/{category}/{wordCount}/{timerMinutes}") { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: ""
            val wordCount = backStackEntry.arguments?.getString("wordCount")?.toIntOrNull() ?: 1
            val timerMinutes = backStackEntry.arguments?.getString("timerMinutes")?.toIntOrNull() ?: 1
            YesNoGameScreen(navController, category, wordCount, timerMinutes, vm)
        }
        composable("customYesNoGame/{category}/{wordCount}/{timerMinutes}") { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: ""
            val wordCount = backStackEntry.arguments?.getString("wordCount")?.toIntOrNull() ?: 1
            val timerMinutes = backStackEntry.arguments?.getString("timerMinutes")?.toIntOrNull() ?: 1
            YesNoGameScreen(navController, category, wordCount, timerMinutes, vm)
        }
        
        // Charades game routes
        composable("charadesCreate") { CharadesCreateScreen(navController) }
        composable("charadesQuickSetup") { CharadesQuickSetupScreen(navController, vm) }
        composable("charadesQuickGame/{category}/{wordCount}") { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: "Personality"
            val wordCount = backStackEntry.arguments?.getString("wordCount")?.toIntOrNull() ?: 3
            CharadesQuickGameScreen(navController, category, wordCount)
        }
        composable("charadesCategory") { CharadesCategoryScreen(navController) }
        composable("charadesJoin") { CharadesJoinScreen(navController, vm) }
        composable("charadesSettings/{category}") { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: ""
            CharadesSettingsScreen(navController, category)
        }
        composable("charadesLobby/{category}/{actorCount}/{wordCount}") { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: ""
            val actorCount = backStackEntry.arguments?.getString("actorCount")?.toIntOrNull() ?: 1
            val wordCount = backStackEntry.arguments?.getString("wordCount")?.toIntOrNull() ?: 1
            CharadesLobbyScreen(navController, vm, category, actorCount, wordCount)
        }
        composable("charadesClientLobby") { 
            if (vm.role.value == Role.HOST) {
                // This shouldn't happen, but fallback to host lobby
                CharadesLobbyScreen(navController, vm, "", 1, 1)
            } else {
                CharadesClientLobbyScreen(navController, vm)
            }
        }
        composable("charadesGame/{category}/{actorCount}/{wordCount}") { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: ""
            val actorCount = backStackEntry.arguments?.getString("actorCount")?.toIntOrNull() ?: 1
            val wordCount = backStackEntry.arguments?.getString("wordCount")?.toIntOrNull() ?: 1
            CharadesGameScreen(navController, vm, category, actorCount, wordCount)
        }
        
        // Impostor game routes
        composable("impostorHome") { ImpostorHomeScreen(navController) }
        composable("impostorQuickSetup") { ImpostorQuickSetupScreen(navController, vm) }
        composable("impostorQuickReveal/{players}/{category}") { backStackEntry ->
            val players = backStackEntry.arguments?.getString("players")?.toIntOrNull() ?: 3
            val category = backStackEntry.arguments?.getString("category") ?: "Personality"
            ImpostorQuickRevealScreen(navController, vm, players, category, vm.impostorCount.value)
        }
        composable("impostorQuickTimer") { ImpostorQuickTimerScreen(navController) }
        composable("impostorCreate") { ImpostorCreateScreen(navController) }
        composable("impostorCategory") { ImpostorCategoryScreen(navController) }
        composable("impostorJoin") { ImpostorJoinScreen(navController, vm) }
        composable("impostorLobby/{category}") { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: ""
            ImpostorLobbyScreen(navController, vm, category)
        }
        composable("impostorClientLobby") { 
            if (vm.role.value == Role.HOST) {
                // This shouldn't happen, but fallback to host lobby
                ImpostorLobbyScreen(navController, vm, "")
            } else {
                ImpostorClientLobbyScreen(navController, vm)
            }
        }
        composable("impostorGame/{category}") { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: ""
            ImpostorGameScreen(navController, vm, category)
        }
        composable("impostorResult") { ImpostorResultScreen(navController, vm) }
    }
}


