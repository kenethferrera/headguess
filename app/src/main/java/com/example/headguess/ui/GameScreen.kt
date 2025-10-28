package com.example.headguess.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun GameScreen(navController: NavHostController, vm: GameViewModel) {
    val activity = (navController.context as? Activity)
    LaunchedEffect(Unit) {
        // Always request a new word when entering game screen
        vm.requestNewWord()
    }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(24.dp)
    ) {
        Text(vm.currentWord.value.ifEmpty { "Waiting for word..." })
        Button(onClick = { vm.requestNewWord() }, modifier = Modifier.padding(top = 16.dp)) {
            Text("Skip")
        }
        Button(onClick = { 
            if (vm.role.value == Role.HOST) {
                // Host goes directly to home - use safe cleanup
                vm.stopHostingSafely()
                // Restore orientation
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                navController.navigate("home") {
                    popUpTo("home") { inclusive = true }
                    launchSingleTop = true
                }
            } else {
                // Client goes back to home to wait for another host
                vm.stopHosting()
                // Restore orientation
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                navController.navigate("home") {
                    popUpTo("home") { inclusive = true }
                    launchSingleTop = true
                }
            }
        }, modifier = Modifier.padding(top = 16.dp)) {
            Text("Got It!")
        }
    }
}




