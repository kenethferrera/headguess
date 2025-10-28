package com.example.headguess.network

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

class ClientConnection {
    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private val TAG = "ClientConnection"

    fun connect(hostIp: String, onEvent: (event: String, payload: String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Connecting to $hostIp:${NetworkDiscovery.port}")
                val s = Socket()
                s.connect(java.net.InetSocketAddress(hostIp, NetworkDiscovery.port), 5000) // 5 second timeout
                socket = s
                writer = PrintWriter(s.getOutputStream(), true)
                val reader = BufferedReader(InputStreamReader(s.getInputStream()))
                
                Log.d(TAG, "Connected successfully")
                
                while (!s.isClosed) {
                    val line = reader.readLine() ?: break
                    try {
                        val obj = JSONObject(line)
                        val event = obj.optString("event")
                        val payload = obj.optString("payload")
                        Log.d(TAG, "Received: $event -> $payload")
                        // Switch to main thread for UI updates
                        CoroutineScope(Dispatchers.Main).launch {
                            onEvent(event, payload)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing JSON: $line", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed", e)
                // Switch to main thread for UI updates
                CoroutineScope(Dispatchers.Main).launch {
                    onEvent("error", "Connection failed: ${e.message}")
                }
            }
        }
    }

    fun requestNewWord() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                writer?.println(JSONObject(mapOf("event" to "request_word", "payload" to "")).toString())
                Log.d(TAG, "Requested new word")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to request new word", e)
            }
        }
    }
    
    fun disconnect() {
        try {
            socket?.close()
            socket = null
            writer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting", e)
        }
    }
}




