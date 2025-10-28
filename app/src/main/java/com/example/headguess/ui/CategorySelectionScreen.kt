package com.example.headguess.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelectionScreen(navController: NavHostController, vm: GameViewModel) {
    val categories = listOf(
        "Personality", "Places", "Food", "Objects",
        "Animals", "TVShows", "Games", "Sports"
    )
    
    // Don't automatically stop hosting when entering this screen
    // Only stop when explicitly going back
    val ctx = LocalContext.current
    var selectedCategory by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(16.dp)
    ) {
        // Minimal header with back only (match YES/NO)
        TopAppBar(
            title = {},
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFAFAFA)),
        navigationIcon = { TextButton(onClick = { 
            // Use safer hosting stop when going back
            android.util.Log.d("CategorySelectionScreen", "User pressed back - stopping hosting safely")
            vm.quickStopHosting()
            navController.navigate("create") 
        }) { Text("Back") } }
        )

        val gridState = rememberLazyGridState()
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(categories) { cat ->
                val assetName = when (cat) {
                    "TVShows" -> "TV SHOWS.avif"
                    else -> "${cat.uppercase()}.avif"
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedCategory = cat
                        },
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedCategory == cat) MaterialTheme.colorScheme.primaryContainer else Color.White
                    )
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        AsyncImage(
                            model = ImageRequest.Builder(ctx)
                                .data("file:///android_asset/Images/$assetName")
                                .build(),
                            contentDescription = cat,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .aspectRatio(1f)
                        )
                        androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
                        Text(
                            text = cat,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = if (selectedCategory == cat) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Continue Button (matches YES/NO)
        Button(
            onClick = {
                if (selectedCategory.isNotEmpty()) {
                    vm.selectCategory(selectedCategory)
                    if (vm.role.value == Role.HOST && vm.hostServer != null) {
                        // Republish NSD service for new category selection
                        vm.publishNSD()
                        android.util.Log.d("CategorySelection", "NSD service republished for category: $selectedCategory")
                    } else {
                        vm.startHosting(onReady = {
                            vm.publishNSD()
                            android.util.Log.d("CategorySelection", "NSD service published for category: $selectedCategory")
                        })
                    }
                    navController.navigate("lobby")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            enabled = selectedCategory.isNotEmpty()
        ) {
            Text("Continue")
        }
        
        // Optional back button removed to match YES/NO flow; back available in app bar
    }
}




