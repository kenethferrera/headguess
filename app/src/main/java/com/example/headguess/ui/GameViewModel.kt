package com.example.headguess.ui

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.headguess.data.WordRepository
import com.example.headguess.network.ClientConnection
import com.example.headguess.network.HostServer
import com.example.headguess.ui.CountryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class Role { HOST, CLIENT, NONE }

class GameViewModel : ViewModel() {
    val role = mutableStateOf(Role.NONE)
    val category = mutableStateOf("")
    val playersCount = mutableStateOf(0)
    val currentWord = mutableStateOf("")
    val hostStatus = mutableStateOf("Idle")
    val gameStarted = mutableStateOf(false)
    val correctWords = mutableStateOf(listOf<String>())
    val hostDisconnected = mutableStateOf(false)
    
    // Charades game specific states
    val charadesClientRole = mutableStateOf("") // "actor" or "guesser"
    val charadesActorWords = mutableStateOf(emptyList<String>())
    val charadesActorCount = mutableStateOf(1)
    val charadesWordCount = mutableStateOf(1)
    
    // Impostor game specific states
    val isImpostor = mutableStateOf(false)
    val showImpostorText = mutableStateOf(false)
    val impostorCount = mutableStateOf(1)
    val selectedImpostorTitle = mutableStateOf("")
    val showImpostorRole = mutableStateOf(false)
    val customImpostorWords = mutableStateOf(emptyList<String>())
    val customCommonWord = mutableStateOf("")
    val correctWord = mutableStateOf("") // The common word that crewmates get
    val impostorWord = mutableStateOf("") // The impostor word that impostors get

    var hostServer: HostServer? = null
        private set
    private var client: ClientConnection? = null
    private val wordRepo = WordRepository()

    fun selectCategory(cat: String) {
        category.value = cat
        // Reset game state when selecting new category
        gameStarted.value = false
        currentWord.value = ""
        playersCount.value = 0
        correctWords.value = emptyList()
    }

    fun startHosting(onReady: () -> Unit, gameType: String = "guessword") {
        android.util.Log.d("GameViewModel", "Starting new hosting session for gameType: $gameType")
        
        // Only stop existing NSD service if we're switching game types
        if (hostServer != null) {
            android.util.Log.d("GameViewModel", "Stopping existing NSD service before starting new one")
            try {
                hostServer?.stopNSD()
                // Small delay to ensure service is unpublished
                Thread.sleep(200)
            } catch (e: Exception) {
                android.util.Log.e("GameViewModel", "Error stopping existing NSD service", e)
            }
        }
        
        role.value = Role.HOST
        // Reset player count for new lobby
        playersCount.value = 0
        hostStatus.value = "Waiting for players..."
        viewModelScope.launch(Dispatchers.IO) {
            val server = HostServer(wordRepo, gameType)
            hostServer = server
            // Start server but don't publish NSD yet
            server.startWithoutNSD(category.value, { count ->
                // Ensure state updates happen on the main thread so UI reflects disconnects
                viewModelScope.launch(Dispatchers.Main) {
                    playersCount.value = count
                }
            }) {
                // Server is ready, call onReady callback
                onReady()
            }
            // Start periodic cleanup of dead connections
            startConnectionCleanup()
        }
    }

    fun publishNSD() {
        hostServer?.publishNSD()
    }
    
    /**
     * Periodically cleanup dead connections
     */
    fun startConnectionCleanup() {
        viewModelScope.launch {
            while (true) {
                delay(5000) // Check every 5 seconds
                hostServer?.cleanupDeadConnections()
            }
        }
    }

    fun startGameForAll() {
        // Reset current word for host
        currentWord.value = ""
        gameStarted.value = true
        hostServer?.broadcastStart(category.value)
        // Stop NSD service so host IP becomes unavailable
        android.util.Log.d("GameViewModel", "Stopping NSD service for game start...")
        hostServer?.stopNSD()
        android.util.Log.d("GameViewModel", "NSD service stopped for game start")
        // Host gets a new word immediately
        requestNewWord()
    }
    
    fun startCharadesGame(actorCount: Int, wordCount: Int, words: List<String> = emptyList()) {
        // Reset current word for host
        currentWord.value = ""
        gameStarted.value = true
        // Reset player count for new game
        playersCount.value = 0
        
        // Set the charades words in the host server and for the host to use
        if (words.isNotEmpty()) {
            hostServer?.setCharadesWords(words)
            charadesActorWords.value = words // Set words for host to use
            android.util.Log.d("GameViewModel", "Set charades words in host server and for host: $words")
        }
        
        hostServer?.broadcastCharadesStart(category.value, actorCount, wordCount)
        // Stop NSD service so host IP becomes unavailable
        hostServer?.stopNSD()
        
        // Get host role from server
        val hostRole = hostServer?.getHostRole() ?: "guesser"
        charadesClientRole.value = hostRole
        android.util.Log.d("GameViewModel", "Host role assigned: $hostRole")
    }

    fun stopHosting() {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                android.util.Log.d("GameViewModel", "Starting stopHosting cleanup")
                
                // Stop NSD service first
                hostServer?.stopNSD()
                
                // Stop server on IO thread
                viewModelScope.launch(Dispatchers.IO) {
                    hostServer?.stop()
                }
                hostServer = null
                
                // Disconnect client
                client?.disconnect()
                client = null
                
                // Reset all state on main thread
                role.value = Role.NONE
                gameStarted.value = false
                currentWord.value = ""
                category.value = ""
                playersCount.value = 0
                hostStatus.value = "Idle"
                
                android.util.Log.d("GameViewModel", "Hosting stopped successfully")
            } catch (e: Exception) {
                android.util.Log.e("GameViewModel", "Error stopping hosting", e)
                // Reset state even if there's an error
                role.value = Role.NONE
                gameStarted.value = false
                currentWord.value = ""
                category.value = ""
                playersCount.value = 0
                hostStatus.value = "Idle"
            }
        }
    }

    fun stopHostingSafely() {
        try {
            android.util.Log.d("GameViewModel", "Starting safe host cleanup")
            
            // Only stop NSD service, don't stop the server
            android.util.Log.d("GameViewModel", "Stopping NSD service...")
            hostServer?.stopNSD()
            android.util.Log.d("GameViewModel", "NSD service stopped")
            
            // Reset state without stopping server
            role.value = Role.NONE
            gameStarted.value = false
            currentWord.value = ""
            category.value = ""
            playersCount.value = 0
            hostStatus.value = "Idle"
            
            android.util.Log.d("GameViewModel", "Safe host cleanup completed")
        } catch (e: Exception) {
            android.util.Log.e("GameViewModel", "Error in safe host cleanup", e)
            // Reset state even if there's an error
            role.value = Role.NONE
            gameStarted.value = false
            currentWord.value = ""
            category.value = ""
            playersCount.value = 0
            hostStatus.value = "Idle"
        }
    }
    
    /**
     * Stop NSD service only (for navigation back) - doesn't reset state or stop server
     */
    fun stopNSDOnly() {
        try {
            android.util.Log.d("GameViewModel", "Stopping NSD service only...")
            hostServer?.stopNSD()
            android.util.Log.d("GameViewModel", "NSD service stopped")
        } catch (e: Exception) {
            android.util.Log.e("GameViewModel", "Error stopping NSD service", e)
        }
    }
    
    fun quickStopHosting() {
        try {
            android.util.Log.d("GameViewModel", "Quick stopping hosting...")
            hostServer?.stopNSD()
            hostServer = null
            role.value = Role.NONE
            android.util.Log.d("GameViewModel", "Quick hosting stop completed")
        } catch (e: Exception) {
            android.util.Log.e("GameViewModel", "Error in quick hosting stop", e)
            role.value = Role.NONE
        }
    }

    fun leaveClient() {
        try {
            client?.disconnect()
        } catch (_: Exception) {}
        client = null
        role.value = Role.NONE
        gameStarted.value = false
        currentWord.value = ""
        hostDisconnected.value = false
    }

    fun joinHost(hostIp: String, onConnected: () -> Unit) {
        role.value = Role.CLIENT
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val c = ClientConnection()
                client = c
                var connectionEstablished = false
                
                // Add timeout for connection
                viewModelScope.launch(Dispatchers.IO) {
                    kotlinx.coroutines.delay(10000) // 10 second timeout
                    if (!connectionEstablished) {
                        android.util.Log.e("GameViewModel", "Connection timeout")
                    }
                }
                
                c.connect(hostIp) { event, payload ->
                    when (event) {
                        "host_disconnected" -> {
                            android.util.Log.w("GameViewModel", "Host disconnected")
                            hostDisconnected.value = true
                            leaveClient()
                        }
                        "new_word" -> currentWord.value = payload
                        "category" -> {
                            category.value = payload
                            // Don't change client's mode - let client maintain its own General/Custom mode
                            // The client should only receive the category data, not change its mode
                            // Reset current word when category changes
                            currentWord.value = ""
                            if (!connectionEstablished) {
                                connectionEstablished = true
                                // Call onConnected - now on main thread from ClientConnection
                                onConnected()
                            }
                        }
                        "game_started" -> {
                            // Game started, set flag for LobbyScreen to handle
                            android.util.Log.d("GameViewModel", "Received game_started event from host")
                            gameStarted.value = true
                        }
                        "charades_role" -> {
                            charadesClientRole.value = payload
                            android.util.Log.d("GameViewModel", "Assigned Charades role: $payload")
                        }
                        "charades_actor_count" -> {
                            charadesActorCount.value = payload.toIntOrNull() ?: 1
                            android.util.Log.d("GameViewModel", "Charades actor count: $payload")
                        }
                        "charades_word_count" -> {
                            charadesWordCount.value = payload.toIntOrNull() ?: 1
                            android.util.Log.d("GameViewModel", "Charades word count: $payload")
                        }
                        "charades_words" -> {
                            charadesActorWords.value = payload.split(",").filter { it.isNotEmpty() }
                            android.util.Log.d("GameViewModel", "Charades words received: ${charadesActorWords.value}")
                        }
                        "impostor_role" -> {
                            android.util.Log.d("GameViewModel", "Received impostor_role event: $payload, current role: ${role.value}")
                            // Only set role for clients, not for host
                            if (role.value != Role.HOST) {
                                isImpostor.value = payload == "impostor"
                                android.util.Log.d("GameViewModel", "Set client impostor role: ${isImpostor.value}")
                            } else {
                                android.util.Log.d("GameViewModel", "Ignoring impostor_role event for host")
                            }
                        }
                        "impostor_count" -> {
                            impostorCount.value = payload.toIntOrNull() ?: 1
                            android.util.Log.d("GameViewModel", "Impostor count: $payload")
                        }
                        "show_impostor_role" -> {
                            showImpostorRole.value = payload.toBoolean()
                            android.util.Log.d("GameViewModel", "Show impostor role: $payload")
                        }
                        "custom_impostor_words" -> {
                            customImpostorWords.value = payload.split(",").filter { it.isNotEmpty() }
                            android.util.Log.d("GameViewModel", "Custom impostor words received: ${customImpostorWords.value}")
                        }
                        "custom_common_word" -> {
                            customCommonWord.value = payload
                            android.util.Log.d("GameViewModel", "Custom common word received: $payload")
                        }
                        "assigned_word" -> {
                            currentWord.value = payload
                            android.util.Log.d("GameViewModel", "===== CLIENT RECEIVED assigned_word: '$payload' (role: ${role.value}, isImpostor: ${isImpostor.value}) =====")
                        }
                        "correct_word" -> {
                            android.util.Log.d("GameViewModel", "Received correct_word event: $payload, current role: ${role.value}")
                            // Only set correct word for clients, not for host
                            if (role.value != Role.HOST) {
                                correctWord.value = payload
                                android.util.Log.d("GameViewModel", "Set client correct word: $payload")
                            } else {
                                android.util.Log.d("GameViewModel", "Ignoring correct_word event for host")
                            }
                        }
                        "impostor_word" -> {
                            android.util.Log.d("GameViewModel", "Received impostor_word event: $payload, current role: ${role.value}")
                            // Only set impostor word for clients, not for host
                            if (role.value != Role.HOST) {
                                impostorWord.value = payload
                                android.util.Log.d("GameViewModel", "Set client impostor word: $payload")
                            } else {
                                android.util.Log.d("GameViewModel", "Ignoring impostor_word event for host")
                            }
                        }
                        "error" -> {
                            // Handle connection error
                            android.util.Log.e("GameViewModel", "Connection error: $payload")
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("GameViewModel", "Failed to join host", e)
            }
        }
    }

    fun requestNewWord() {
        if (role.value == Role.HOST) {
            // Use HostServer's unique word distribution to ensure different words for host and clients
            CoroutineScope(Dispatchers.IO).launch {
                val uniqueWord = hostServer?.getHostUniqueWord() ?: wordRepo.getRandomWord(category.value)
                currentWord.value = uniqueWord
            }
        } else {
            client?.requestNewWord()
        }
    }
    
    fun resetImpostorGameState() {
        gameStarted.value = false
        currentWord.value = ""
        correctWord.value = ""
        impostorWord.value = ""
        isImpostor.value = false
        customImpostorWords.value = emptyList()
        customCommonWord.value = ""
        selectedImpostorTitle.value = ""
        // Don't reset showImpostorRole - it's a host setting that should persist
        impostorCount.value = 1
        playersCount.value = 0
        android.util.Log.d("GameViewModel", "Impostor game state reset")
    }
    
    fun startImpostorGame() {
        if (role.value == Role.HOST) {
            hostServer?.setOnHostWordAssigned { word, isImpostorRole, correct, impostor ->
                currentWord.value = word
                isImpostor.value = isImpostorRole
                
                // Set correct word and impostor word for results screen directly from callback
                correctWord.value = correct
                impostorWord.value = impostor
                
                android.util.Log.d("GameViewModel", "Host received assigned word: $word (role: ${if (isImpostorRole) "impostor" else "crewmate"}), correctWord: $correct, impostorWord: $impostor")
            }
            
            android.util.Log.d("GameViewModel", "Host starting impostor game - HostServer will assign role and word")
            
            // Notify all clients with impostor roles so they can leave the lobby
            hostServer?.broadcastImpostorStart(category.value, impostorCount.value)
            // Stop NSD to hide the lobby once the game begins
            hostServer?.stopNSD()

            gameStarted.value = true
        }
    }
}
