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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharadesCreateScreen(navController: NavHostController) {
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
            navigationIcon = {
                TextButton(onClick = { navController.navigateUp() }) {
                    Text("Back")
                }
            }
        )
        
        Spacer(Modifier.height(24.dp))
        
        Button(
            onClick = { navController.navigate("charadesCategory") },
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .padding(vertical = 8.dp)
        ) {
            Text("Create Game")
        }
        
        Button(
            onClick = { navController.navigate("charadesJoin") },
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .padding(vertical = 8.dp)
        ) {
            Text("Join Game")
        }
    }
}




