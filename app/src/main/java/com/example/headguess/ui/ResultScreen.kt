package com.example.headguess.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun ResultScreen(navController: NavHostController, vm: GameViewModel) {
    var shouldNavigateHome by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        android.util.Log.d("ResultScreen", "ResultScreen reached - Role: ${vm.role.value}")
        android.util.Log.d("ResultScreen", "Correct words to display: ${vm.correctWords.value}")
    }
    
    LaunchedEffect(shouldNavigateHome) {
        if (shouldNavigateHome) {
            navController.navigate("home") {
                popUpTo("home") { inclusive = true }
            }
        }
    }
    
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(24.dp)
    ) {
        val words = vm.correctWords.value
        if (words.isNotEmpty()) {
            LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                items(words) { w ->
                    Text(w, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (vm.role.value == Role.HOST) {
            Button(onClick = { navController.navigate("category") }, modifier = Modifier.padding(top = 16.dp)) {
                Text("Play Again")
            }
            Button(onClick = { 
                vm.stopHosting()
                shouldNavigateHome = true
            }, modifier = Modifier.padding(top = 8.dp)) {
                Text("Done")
            }
            Button(onClick = { 
                vm.stopHosting()
                shouldNavigateHome = true
            }, modifier = Modifier.padding(top = 8.dp)) {
                Text("Home")
            }
        } else {
            Button(onClick = { 
                vm.stopHosting()
                shouldNavigateHome = true
            }, modifier = Modifier.padding(top = 16.dp)) {
                Text("Back to Home")
            }
            Button(onClick = { 
                shouldNavigateHome = true
            }, modifier = Modifier.padding(top = 8.dp)) {
                Text("Home")
            }
        }
    }
}




