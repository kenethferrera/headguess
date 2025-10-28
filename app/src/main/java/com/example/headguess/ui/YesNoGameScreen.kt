package com.example.headguess.ui

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.activity.ComponentActivity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.headguess.data.WordRepository
import kotlinx.coroutines.delay
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import android.view.OrientationEventListener
import android.view.KeyEvent
import android.view.View
import android.media.AudioManager
import android.database.ContentObserver
import android.provider.Settings
import android.os.Handler
import android.os.Looper
import org.json.JSONObject

// Helper function to load custom words outside of composable
private fun loadCustomWords(context: android.content.Context): List<String> {
    return try {
        val f = java.io.File(context.filesDir, "custom_categories.json")
        println("Custom file exists: ${f.exists()}") // Debug log
        if (f.exists()) {
            val content = f.readText()
            println("Custom file content: $content") // Debug log
            val json = JSONObject(content)
            val customArray = json.getJSONArray("Custom")
            val words = (0 until customArray.length()).map { customArray.getString(it) }
            println("Parsed custom words: $words") // Debug log
            words
        } else {
            println("Custom file does not exist") // Debug log
            emptyList()
        }
    } catch (e: Exception) {
        android.util.Log.e("YesNoGameScreen", "Failed to load custom words", e)
        println("Exception loading custom words: ${e.message}") // Debug log
        emptyList()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YesNoGameScreen(
    navController: NavHostController,
    category: String,
    wordCount: Int,
    timerMinutes: Int,
    vm: GameViewModel? = null
) {
    val context = LocalContext.current as ComponentActivity
    val activity = context as android.app.Activity
    val wordRepository = remember { WordRepository() }
    val coroutineScope = rememberCoroutineScope()
    
    // Game state
    var currentWordIndex by remember { mutableStateOf(0) }
    var currentWord by remember { mutableStateOf("") }
    var timeRemaining by remember { mutableStateOf(timerMinutes * 60) } // in seconds
    var isTimerRunning by remember { mutableStateOf(false) }
    var isGameStarted by remember { mutableStateOf(false) }
    var isPreStart by remember { mutableStateOf(true) }
    var preStartCountdown by remember { mutableStateOf(3) }
    var isShowingWord by remember { mutableStateOf(false) }
    var isRotating by remember { mutableStateOf(false) }
    var rotationCountdown by remember { mutableStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    val correctWords = remember { mutableStateListOf<String>() }
    var lastOrientation by remember { mutableStateOf(Configuration.ORIENTATION_LANDSCAPE) }
    var rotationTrigger by remember { mutableStateOf(0) }
    var pendingRotate by remember { mutableStateOf(false) }
    var lastScreenWidth by remember { mutableStateOf(0) }
    var lastScreenHeight by remember { mutableStateOf(0) }
    var lastRollDeg by remember { mutableStateOf<Float?>(null) }
    var lastZSign by remember { mutableStateOf<Int?>(null) }
    var lastFlipAtMs by remember { mutableStateOf(0L) }
    var lastWordChangeTime by remember { mutableStateOf(0L) }
    
    // Helper function to prevent duplicate word changes
    fun canChangeWord(): Boolean {
        val currentTime = SystemClock.elapsedRealtime()
        return currentTime - lastWordChangeTime > 500 // 500ms debounce
    }
    
    // Context is already available from line 66
    
    // State for custom words
    var customWords by remember { mutableStateOf<List<String>>(emptyList()) }
    var customWordsLoaded by remember { mutableStateOf(false) }
    
    // Load custom words in LaunchedEffect
    LaunchedEffect(category) {
        if (category == "Custom" && !customWordsLoaded) {
            println("Loading custom words for category: $category") // Debug log
            customWords = loadCustomWords(context)
            println("Loaded custom words: $customWords") // Debug log
            println("Custom words count: ${customWords.size}") // Debug log
            customWordsLoaded = true
        }
    }
    
    val words = remember(customWords, category, wordCount) {
        if (category == "Custom") {
            if (customWords.isNotEmpty()) {
                // Use the exact custom words, limited to wordCount
                customWords.take(wordCount).also { 
                    println("Using custom words: $it") // Debug log
                }
            } else {
                // Fallback to random words if custom words not loaded yet
                (1..wordCount).map { wordRepository.getRandomWord(category) }.also { 
                    println("Generated words (fallback): $it") // Debug log
                }
            }
        } else {
            // For regular categories, generate random words as before
            (1..wordCount).map { wordRepository.getRandomWord(category) }.also { 
                println("Generated words: $it") // Debug log
            }
        }
    }
    
    // Force landscape orientation and allow rotation detection
    LaunchedEffect(Unit) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }
    
    // Timer logic
    LaunchedEffect(isTimerRunning, timeRemaining) {
        if (isTimerRunning && timeRemaining > 0 && !isPaused) {
            delay(1000)
            timeRemaining--
        }
        if (timeRemaining == 0 && isGameStarted) {
            // Time up → exit game and show summary
            runCatching { activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
            // Persist exactly the selected number of words (even if not all were played)
            vm?.correctWords?.value = words.take(wordCount)
            // Navigate back using provided navController to a simple results screen
            navController.navigate("result") {
                popUpTo("home") { inclusive = false }
            }
        }
    }
    
    // Pre-start 3s countdown, then show first word and start main timer
    LaunchedEffect(Unit) {
        if (!isGameStarted) {
            // Initialize screen dimensions to prevent false rotation detection
            lastScreenWidth = context.resources.displayMetrics.widthPixels
            lastScreenHeight = context.resources.displayMetrics.heightPixels

            isPreStart = true
            preStartCountdown = 3
            while (preStartCountdown > 0) {
                delay(1000)
                preStartCountdown--
            }
            isPreStart = false

            isGameStarted = true
            isShowingWord = true
            currentWord = words[currentWordIndex]
            isTimerRunning = true

            println("Game started with word: $currentWord") // Debug log
            println("Initial screen: ${lastScreenWidth}x${lastScreenHeight}") // Debug log
        }
    }
    
    // Rotation detection and word changing (orientation + sensor-based flip)
    LaunchedEffect(Unit) {
        delay(1200) // Let pre-start settle
        while (true) {
            val currentOrientation = context.resources.configuration.orientation
            val screenWidth = context.resources.displayMetrics.widthPixels
            val screenHeight = context.resources.displayMetrics.heightPixels

            val screenSizeChanged = (screenWidth != lastScreenWidth || screenHeight != lastScreenHeight)
            if (screenSizeChanged) {
                lastScreenWidth = screenWidth
                lastScreenHeight = screenHeight
                pendingRotate = true
            }

            if (pendingRotate && isGameStarted && currentWordIndex < words.size - 1 && !isRotating) {
                println("Rotation detected (pendingRotate=true)")
                rotationTrigger++
                pendingRotate = false
            }

            delay(120)
        }
    }

    // OrientationEventListener fallback — works on many devices for flip within landscape
    DisposableEffect(Unit) {
        val listener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (!isGameStarted || isRotating || isPreStart || currentWordIndex >= words.size - 1) return
                // orientation is 0..359 degrees relative to natural orientation
                // We consider a flip within landscape when orientation crosses near 0/180 boundaries
                val prev = lastRollDeg ?: orientation.toFloat()
                val cur = orientation.toFloat()
                lastRollDeg = cur
                val delta = Math.abs(((cur - prev + 540f) % 360f) - 180f)
                if (delta > 120f) {
                    pendingRotate = true
                }
            }
        }
        runCatching { listener.enable() }
        onDispose { runCatching { listener.disable() } }
    }

    // Accelerometer fallback: detect quick flip by Z-axis sign change with threshold and debounce
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager
        val accel: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val accelListener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
                if (!isGameStarted || isRotating || isPreStart || currentWordIndex >= words.size - 1) return
                val z = event.values[2] // toward outside of screen
                val sign = if (z >= 0f) 1 else -1
                val now = SystemClock.elapsedRealtime()
                val prevSign = lastZSign
                lastZSign = sign
                if (prevSign != null && prevSign != sign && kotlin.math.abs(z) > 3.5f) {
                    // Debounce: don't trigger more than once per 1.2s
                    if (now - lastFlipAtMs > 1200) {
                        lastFlipAtMs = now
                        pendingRotate = true
                    }
                }
            }
        }
        if (accel != null) {
            sensorManager.registerListener(accelListener, accel, SensorManager.SENSOR_DELAY_GAME)
        }
        onDispose { runCatching { sensorManager.unregisterListener(accelListener) } }
    }
    
    // Volume button fallback: trigger 3s countdown on volume up/down press
    val composeView = LocalView.current
    DisposableEffect(composeView) {
        val activity = context
        val view = composeView
        // Ensure both Compose root and window decor can receive key events
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        view.post { view.requestFocus() }
        val windowView = activity.window?.decorView
        windowView?.isFocusable = true
        windowView?.isFocusableInTouchMode = true
        windowView?.post { windowView.requestFocus() }

        val keyListener = View.OnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN &&
                (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)) {
                if (!isRotating && isGameStarted && !isPreStart && currentWordIndex < words.size - 1 && canChangeWord()) {
                    // Start 2s countdown immediately on either volume key
                    lastWordChangeTime = SystemClock.elapsedRealtime()
                    isShowingWord = false
                    isRotating = true
                    rotationCountdown = 2
                    true // consume to avoid system volume change
                } else true
            } else false
        }
        view.setOnKeyListener(keyListener)
        windowView?.setOnKeyListener(keyListener)
        onDispose {
            view.setOnKeyListener(null)
            windowView?.setOnKeyListener(null)
        }
    }

    // System volume change observer fallback — triggers when volume buttons adjust volume
    DisposableEffect(Unit) {
        val audio = context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager
        var lastVol = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                val current = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (current != lastVol) {
                    lastVol = current
                    if (!isRotating && isGameStarted && !isPreStart && currentWordIndex < words.size - 1 && canChangeWord()) {
                        // Start countdown immediately even if OEM swallows key event (single press)
                        lastWordChangeTime = SystemClock.elapsedRealtime()
                        isShowingWord = false
                        isRotating = true
                        rotationCountdown = 2
                    }
                }
            }
        }
        context.contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, observer)
        onDispose { runCatching { context.contentResolver.unregisterContentObserver(observer) } }
    }

    // React to rotation trigger
    LaunchedEffect(rotationTrigger) {
        if (rotationTrigger > 0 && isGameStarted && currentWordIndex < words.size - 1) {
            println("Rotation trigger activated: $rotationTrigger") // Debug log
            // Start a 3-second inter-word countdown for rotation WITHOUT resetting the round timer
            // Timer keeps running while the 3-second countdown is displayed
            isShowingWord = false
            isRotating = true
            rotationCountdown = 3
        }
    }
    
    // Word rotation logic
    LaunchedEffect(isRotating, rotationCountdown) {
        if (isRotating && rotationCountdown > 0) {
            delay(1000)
            rotationCountdown--
        } else if (isRotating && rotationCountdown == 0) {
            // Show next word
            currentWordIndex++
            println("Changing to word $currentWordIndex: ${words.getOrNull(currentWordIndex)}") // Debug log
            if (currentWordIndex < words.size) {
                // Record the previous word as correctly delivered
                if (currentWord.isNotEmpty()) correctWords.add(currentWord)
                currentWord = words[currentWordIndex]
                isShowingWord = true
                isRotating = false
                // Update timestamp when word actually changes
                lastWordChangeTime = SystemClock.elapsedRealtime()
                // Do NOT reset round timer; keep counting down across words
            }
        }
    }
    
    // Format time display
    val minutes = timeRemaining / 60
    val seconds = timeRemaining % 60
    val timeDisplay = String.format("%02d:%02d", minutes, seconds)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Timer / Pre-start display
            if (isPreStart) {
                Text(
                    text = "Starting in $preStartCountdown",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
            } else {
                Text(
                    text = timeDisplay,
                    style = MaterialTheme.typography.headlineLarge,
                    color = if (timeRemaining <= 30) Color.Red else Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
            }
            
            // Inter-word countdown (2s) visual
            if (isRotating) {
                Text(
                    text = rotationCountdown.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Word display (revised for better fit)
            if (isShowingWord && !isRotating) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .padding(horizontal = 16.dp)
                        .clickable { 
                            if (wordCount > 1 && currentWordIndex < wordCount - 1 && canChangeWord()) {
                                println("Word tapped for next word") // Debug log
                                // Do not reset the round timer; just enter inter-word countdown
                                lastWordChangeTime = SystemClock.elapsedRealtime()
                                isShowingWord = false
                                isRotating = true
                                rotationCountdown = 2
                            }
                        },
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Text(
                        text = currentWord,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = Color.Black,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp, horizontal = 16.dp),
                        softWrap = true,
                        maxLines = 3
                    )
                }
            }
            
            // Hide helper texts and counters per request
        }
        
        // Stop/Play and Done controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = {
                    isPaused = !isPaused
                    isTimerRunning = !isPaused
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPaused) Color(0xFF2E7D32) else Color.Red
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) { Text(if (isPaused) "PLAY" else "STOP", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White) }

            Spacer(Modifier.width(16.dp))

            Button(
                onClick = {
                    // Done early → exit and show words provided so far plus current
                    runCatching { activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
                    // Persist exactly the selected number of words (even if not all were played)
                    vm?.correctWords?.value = words.take(wordCount)
                    navController.navigate("result") {
                        popUpTo("home") { inclusive = false }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) { Text("DONE", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White) }
        }
        
        // Removed debug/next/test buttons as requested
        
    }
    
    // Cleanup on exit
    DisposableEffect(Unit) {
        onDispose {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}
