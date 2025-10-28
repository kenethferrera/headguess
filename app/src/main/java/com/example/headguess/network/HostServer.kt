package com.example.headguess.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.example.headguess.data.WordRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

class HostServer(private val wordRepository: WordRepository, private val gameType: String = "guessword") {
    private var serverSocket: ServerSocket? = null
    private val clients = mutableListOf<Socket>()
    private val mutex = Mutex()
    private var nsdManager: NsdManager? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var category: String = ""
    private var onPlayerCountChangedCb: ((Int) -> Unit)? = null
    private var hostRole: String = "guesser" // Default host role
    private var hostImpostorRole: String = "crewmate"
    private var charadesWords: List<String> = emptyList() // Store actual words for charades
    private var customImpostorWords: List<String> = emptyList() // Store custom impostor words
    private var customCommonWord: String = "" // Store custom common word
    private var showImpostorRole: Boolean = false // Store impostor role visibility setting
    private var onHostWordAssigned: ((String, Boolean, String, String) -> Unit)? = null // Callback for host word assignment (assignedWord, isImpostor, correctWord, impostorWord)
    
    // Word distribution system to ensure different words for each player
    private val usedWords = mutableSetOf<String>()
    private val wordDistributionMutex = Mutex()
    
    companion object {
        const val MAX_CLIENTS = 20 // Maximum number of clients that can join
    }
    
    /**
     * Get a unique word that hasn't been used yet for this game session
     */
    private suspend fun getUniqueWord(): String {
        return wordDistributionMutex.withLock {
            var word: String
            var attempts = 0
            val maxAttempts = 50 // Prevent infinite loop
            
            do {
                word = wordRepository.getRandomWord(category)
                attempts++
            } while (word in usedWords && attempts < maxAttempts)
            
            // If we still have a duplicate after max attempts, clear used words and start fresh
            if (word in usedWords) {
                Log.w("HostServer", "Clearing used words due to word pool exhaustion")
                usedWords.clear()
                word = wordRepository.getRandomWord(category)
            }
            
            usedWords.add(word)
            Log.d("HostServer", "Distributed unique word: $word (used: ${usedWords.size})")
            word
        }
    }

    fun start(selectedCategory: String, onPlayerCountChanged: (Int) -> Unit) {
        category = selectedCategory
        onPlayerCountChangedCb = onPlayerCountChanged
        CoroutineScope(Dispatchers.IO).launch {
            val server = ServerSocket(0) // auto-assign port
            serverSocket = server
            publishService(server.localPort)
            while (!server.isClosed) {
                val socket = server.accept()
                mutex.withLock { 
                    if (clients.size < MAX_CLIENTS) {
                        clients.add(socket)
                        onPlayerCountChanged(clients.size)
                        handleClient(socket)
                    } else {
                        // Reject connection if at maximum capacity
                        Log.w("HostServer", "Maximum client limit reached ($MAX_CLIENTS). Rejecting connection.")
                        runCatching { socket.close() }
                    }
                }
            }
        }
    }

    fun startWithoutNSD(selectedCategory: String, onPlayerCountChanged: (Int) -> Unit, onServerReady: () -> Unit) {
        category = selectedCategory
        onPlayerCountChangedCb = onPlayerCountChanged
        CoroutineScope(Dispatchers.IO).launch {
            val server = ServerSocket(0) // auto-assign port
            serverSocket = server
            Log.d("HostServer", "Server started on port: ${server.localPort}")
            // Notify that server is ready
            onServerReady()
            // Don't publish NSD service yet - wait for category selection
            while (!server.isClosed) {
                val socket = server.accept()
                mutex.withLock { 
                    if (clients.size < MAX_CLIENTS) {
                        clients.add(socket)
                        onPlayerCountChanged(clients.size)
                        handleClient(socket)
                    } else {
                        // Reject connection if at maximum capacity
                        Log.w("HostServer", "Maximum client limit reached ($MAX_CLIENTS). Rejecting connection.")
                        runCatching { socket.close() }
                    }
                }
            }
        }
    }

    private fun handleClient(socket: Socket) {
        CoroutineScope(Dispatchers.IO).launch {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = PrintWriter(socket.getOutputStream(), true)
            // Send category on connect
            writer.println(JSONObject(mapOf("event" to "category", "payload" to category)).toString())
            Log.d("HostServer", "Client connected, total clients: ${clients.size}")
            
            try {
                while (!socket.isClosed) {
                    val line = reader.readLine() ?: break
                    val obj = JSONObject(line)
                    when (obj.optString("event")) {
                        "request_word" -> {
                            val word = getUniqueWord()
                            writer.println(JSONObject(mapOf("event" to "new_word", "payload" to word)).toString())
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("HostServer", "Client connection error: ${e.message}")
            } finally {
                // Always remove client and update count when connection ends
                mutex.withLock { 
                    clients.remove(socket)
                    Log.d("HostServer", "Client disconnected, remaining clients: ${clients.size}")
                    // Notify UI about updated player count when someone leaves
                    onPlayerCountChangedCb?.invoke(clients.size)
                }
                runCatching { socket.close() }
            }
        }
    }

    fun broadcastStart(category: String) {
        this.category = category
        // Reset word distribution for new game
        resetWordDistribution()
        CoroutineScope(Dispatchers.IO).launch {
            // When game starts, unpublish NSD so new clients won't discover host
            unpublishService()
            mutex.withLock {
                clients.forEach { socket ->
                    runCatching {
                        val writer = PrintWriter(socket.getOutputStream(), true)
                        writer.println(JSONObject(mapOf("event" to "category", "payload" to category)).toString())
                        writer.println(JSONObject(mapOf("event" to "game_started", "payload" to "")).toString())
                        Log.d("HostServer", "Broadcasted category: $category and game_started to client")
                        Log.d("HostServer", "Client socket: ${socket.remoteSocketAddress}")
                    }
                }
            }
        }
    }
    
    fun broadcastImpostorStart(category: String, impostorCount: Int) {
        this.category = category
        // Compute roles synchronously so host can read their role immediately
        val snapshot: Pair<List<Socket>, Pair<Int, List<Int>>> = runBlocking {
            mutex.withLock {
                val clientList = clients.toList()
                val totalPlayers = clientList.size
                val totalPlayersWithHost = totalPlayers + 1
                var validImpostorCount = impostorCount
                // Enforce minimum 2 total players for impostor game
                if (totalPlayersWithHost < 2) {
                    validImpostorCount = 0
                }
                // Cap impostors to at most totalPlayersWithHost - 1
                validImpostorCount = validImpostorCount.coerceIn(0, maxOf(0, totalPlayersWithHost - 1))
                val allIndices = (0 until totalPlayersWithHost).toMutableList()
                val impostorIndices = if (validImpostorCount > 0) allIndices.shuffled().take(validImpostorCount) else emptyList()
                hostImpostorRole = if (0 in impostorIndices) "impostor" else "crewmate"
                
                android.util.Log.d("HostServer", "Role assignment: TotalPlayers=$totalPlayersWithHost, ImpostorCount=$impostorCount, ValidImpostorCount=$validImpostorCount, ImpostorIndices=$impostorIndices, HostRole=$hostImpostorRole")
                
                clientList to (validImpostorCount to impostorIndices)
            }
        }

        val clientList = snapshot.first
        val validImpostorCount = snapshot.second.first
        val impostorIndices = snapshot.second.second

        CoroutineScope(Dispatchers.IO).launch {
            unpublishService()
            
            // Generate word pair ONCE for the entire game (not per client!)
            android.util.Log.d("HostServer", "===== GENERATING WORD PAIR FOR GAME - Category: $category =====")
            val (correctWord, impostorWord) = if (category == "Custom" && customImpostorWords.isNotEmpty() && customCommonWord.isNotEmpty()) {
                // For custom games, use provided words
                android.util.Log.d("HostServer", "Using custom words")
                Pair(customCommonWord, customImpostorWords.firstOrNull() ?: "")
            } else {
                // For regular games, generate word pair ONCE
                android.util.Log.d("HostServer", "Calling wordRepository.getImpostorWordPair('$category')")
                val wordPair = wordRepository.getImpostorWordPair(category)
                android.util.Log.d("HostServer", "Received word pair from repository: A='${wordPair.first}', B='${wordPair.second}'")
                // A is always the correct word, B is always the impostor word
                Pair(wordPair.first, wordPair.second)
            }
            
            android.util.Log.d("HostServer", "===== FINAL GAME WORDS: CorrectWord='$correctWord', ImpostorWord='$impostorWord' =====")
            
            clientList.forEachIndexed { index, socket ->
                runCatching {
                    val writer = PrintWriter(socket.getOutputStream(), true)
                    val clientPlayerIndex = index + 1 // host=0
                    val isImpostor = clientPlayerIndex in impostorIndices
                    val role = if (isImpostor) "impostor" else "crewmate"
                    writer.println(JSONObject(mapOf("event" to "category", "payload" to category)).toString())
                    writer.println(JSONObject(mapOf("event" to "impostor_role", "payload" to role)).toString())
                    writer.println(JSONObject(mapOf("event" to "impostor_count", "payload" to validImpostorCount.toString())).toString())
                    writer.println(JSONObject(mapOf("event" to "show_impostor_role", "payload" to showImpostorRole.toString())).toString())
                    
                    // Send the appropriate word based on role
                    val clientWord = if (isImpostor) impostorWord else correctWord
                    writer.println(JSONObject(mapOf("event" to "assigned_word", "payload" to clientWord)).toString())
                    writer.println(JSONObject(mapOf("event" to "correct_word", "payload" to correctWord)).toString())
                    writer.println(JSONObject(mapOf("event" to "impostor_word", "payload" to impostorWord)).toString())
                    android.util.Log.d("HostServer", ">>> Sent to Client #$clientPlayerIndex: assigned='$clientWord', role='$role', correct='$correctWord', impostor='$impostorWord'")
                    
                    // Send game_started event to signal the game has begun
                    writer.println(JSONObject(mapOf("event" to "game_started", "payload" to "")).toString())
                    android.util.Log.d("HostServer", "Sent game_started to client $clientPlayerIndex")
                    
                }
            }
            
            // Assign host's word and role after broadcasting to clients
            assignHostWordAndRole(correctWord, impostorWord, impostorIndices)
        }
    }
    
    private fun assignHostWordAndRole(correctWord: String, impostorWord: String, impostorIndices: List<Int>) {
        // Host is always index 0
        val isHostImpostor = 0 in impostorIndices
        
        android.util.Log.d("HostServer", "Assigning host word and role. IsHostImpostor: $isHostImpostor, CorrectWord: '$correctWord', ImpostorWord: '$impostorWord'")
        
        // Assign the appropriate word based on host's role
        val hostWord = if (isHostImpostor) impostorWord else correctWord
        
        android.util.Log.d("HostServer", "Host assigned word '$hostWord' (role: ${if (isHostImpostor) "impostor" else "crewmate"})")
        
        // Return the host's assigned word, role, and both words for results screen
        onHostWordAssigned?.invoke(hostWord, isHostImpostor, correctWord, impostorWord)
    }
    
    fun broadcastCharadesStart(category: String, actorCount: Int, wordCount: Int) {
        this.category = category

        // Compute roles synchronously so host can read role immediately
        val snapshot: Pair<List<Socket>, Triple<Int, Int, List<Int>>> = runBlocking {
            mutex.withLock {
                val clientList = clients.toList()
                val totalPlayers = clientList.size

                var validActorCount = minOf(actorCount, totalPlayers)
                if (validActorCount != actorCount) {
                    Log.w("HostServer", "Actor count ($actorCount) exceeds available players ($totalPlayers). Using $validActorCount actors.")
                }
                val totalPlayersWithHost = totalPlayers + 1
                if (totalPlayersWithHost >= 2 && validActorCount < 1) {
                    Log.w("HostServer", "Adjusted actor count from $validActorCount to 1 for two-player minimum rule")
                    validActorCount = 1
                }

                val validWordCount = minOf(wordCount, 5)
                if (validWordCount != wordCount) {
                    Log.w("HostServer", "Word count ($wordCount) exceeds maximum (5). Using $validWordCount words.")
                }

                val allPlayerIndices = (0 until totalPlayersWithHost).toMutableList()
                val actorIndices = if (validActorCount > 0) allPlayerIndices.shuffled().take(validActorCount) else emptyList()

                // Set host role now so GameViewModel can read it immediately
                hostRole = if (0 in actorIndices) "actor" else "guesser"
                Log.d("HostServer", "Host role (computed synchronously): $hostRole")

                clientList to Triple(validActorCount, validWordCount, actorIndices)
            }
        }

        val clientList = snapshot.first
        val validActorCount = snapshot.second.first
        val validWordCount = snapshot.second.second
        val actorIndices = snapshot.second.third

        Log.d("HostServer", "Broadcasting start with actors: $actorIndices, validActorCount=$validActorCount, validWordCount=$validWordCount; category=$category")

        CoroutineScope(Dispatchers.IO).launch {
            // When game starts, unpublish NSD so new clients won't discover host
            unpublishService()
            clientList.forEachIndexed { index, socket ->
                runCatching {
                    val writer = PrintWriter(socket.getOutputStream(), true)
                    val clientPlayerIndex = index + 1 // host is index 0
                    val isActor = clientPlayerIndex in actorIndices
                    val role = if (isActor) "actor" else "guesser"

                    writer.println(JSONObject(mapOf("event" to "category", "payload" to category)).toString())
                    writer.println(JSONObject(mapOf("event" to "charades_role", "payload" to role)).toString())
                    writer.println(JSONObject(mapOf("event" to "charades_actor_count", "payload" to validActorCount.toString())).toString())
                    writer.println(JSONObject(mapOf("event" to "charades_word_count", "payload" to validWordCount.toString())).toString())
                    // Send actual words to client
                    writer.println(JSONObject(mapOf("event" to "charades_words", "payload" to charadesWords.joinToString(","))).toString())
                    writer.println(JSONObject(mapOf("event" to "game_started", "payload" to "")).toString())

                    Log.d("HostServer", "Assigned role '$role' to client ${index + 1}/${clientList.size} (player index: $clientPlayerIndex)")
                }
            }
        }
    }

    fun publishNSD() {
        serverSocket?.let { socket ->
            Log.d("HostServer", "Publishing NSD service on port: ${socket.localPort}")
            publishService(socket.localPort)
        } ?: Log.e("HostServer", "Cannot publish NSD - server socket is null")
    }
    
    fun getHostRole(): String {
        return hostRole
    }
    fun getHostImpostorRole(): String {
        return hostImpostorRole
    }
    
    fun setCharadesWords(words: List<String>) {
        charadesWords = words
        Log.d("HostServer", "Set charades words: $words")
    }
    
    fun setCustomImpostorWords(impostorWords: List<String>, commonWord: String) {
        customImpostorWords = impostorWords
        customCommonWord = commonWord
        Log.d("HostServer", "Set custom impostor words: $impostorWords, common: $commonWord")
    }
    
    fun setShowImpostorRole(show: Boolean) {
        showImpostorRole = show
        Log.d("HostServer", "Set show impostor role: $show")
    }
    
    fun setOnHostWordAssigned(callback: (String, Boolean, String, String) -> Unit) {
        onHostWordAssigned = callback
    }
    
    /**
     * Get a unique word for the host (ensures different words for host and clients)
     */
    suspend fun getHostUniqueWord(): String {
        return getUniqueWord()
    }
    
    /**
     * Clean up dead connections and update player count
     */
    fun cleanupDeadConnections() {
        CoroutineScope(Dispatchers.IO).launch {
            mutex.withLock {
                val initialCount = clients.size
                clients.removeAll { socket ->
                    try {
                        socket.isClosed || !socket.isConnected
                    } catch (e: Exception) {
                        true
                    }
                }
                val finalCount = clients.size
                if (initialCount != finalCount) {
                    Log.d("HostServer", "Cleaned up dead connections: $initialCount -> $finalCount")
                    onPlayerCountChangedCb?.invoke(finalCount)
                }
            }
        }
    }
    
    /**
     * Reset the used words list for a new game session
     */
    fun resetWordDistribution() {
        CoroutineScope(Dispatchers.IO).launch {
            wordDistributionMutex.withLock {
                usedWords.clear()
                Log.d("HostServer", "Reset word distribution - cleared used words")
            }
        }
    }

    fun stopNSD() {
        unpublishService()
    }

    fun stop() {
        try {
            unpublishService()
            runCatching { serverSocket?.close() }
            serverSocket = null
            CoroutineScope(Dispatchers.IO).launch {
                mutex.withLock {
                    clients.forEach { socket ->
                        runCatching { 
                            socket.close() 
                        }
                    }
                    clients.clear()
                }
            }
        } catch (e: Exception) {
            Log.e("HostServer", "Error stopping server", e)
        }
    }

    private fun publishService(port: Int) {
        val context = NetworkDiscovery.applicationContext ?: return
        
        // Check if nearby devices permission is granted
        if (!com.example.headguess.utils.PermissionManager.checkNearbyDevicesPermission(context)) {
            Log.w("HostServer", "Nearby devices permission not granted, cannot publish service")
            return
        }
        
        val serviceType = when (gameType) {
            "charades" -> "_charades._tcp."
            "impostor" -> "_impostor._tcp."
            "custom" -> "_custom._tcp."
            else -> "_headguess._tcp."
        }
        Log.d("HostServer", "Publishing service on port: $port (gameType: $gameType, serviceType: $serviceType)")
        nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "HeadGuessHost"
            this.serviceType = serviceType
            setPort(port)
        }
        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
                Log.d("HostServer", "NSD service registered successfully for $gameType ($serviceType)")
            }
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e("HostServer", "NSD service registration failed for $gameType ($serviceType): $errorCode")
            }
            override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                Log.d("HostServer", "NSD service unregistered for $gameType ($serviceType)")
            }
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e("HostServer", "NSD service unregistration failed for $gameType ($serviceType): $errorCode")
            }
        }
        nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    private fun unpublishService() {
        try {
            if (registrationListener != null && nsdManager != null) {
                val serviceType = when (gameType) {
                    "charades" -> "_charades._tcp."
                    "impostor" -> "_impostor._tcp."
                    "custom" -> "_custom._tcp."
                    else -> "_headguess._tcp."
                }
                Log.d("HostServer", "Unpublishing NSD service (gameType: $gameType, serviceType: $serviceType)...")
                nsdManager?.unregisterService(registrationListener!!)
                Log.d("HostServer", "NSD service unpublished successfully for $gameType")
            } else {
                Log.d("HostServer", "No NSD service to unpublish (already unpublished or never published)")
            }
        } catch (e: Exception) {
            Log.e("HostServer", "Error unpublishing NSD service for gameType: $gameType", e)
        } finally {
            registrationListener = null
            nsdManager = null
        }
    }
}




