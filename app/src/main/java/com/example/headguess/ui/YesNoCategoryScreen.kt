package com.example.headguess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.example.headguess.R
import coil.compose.AsyncImage
import coil.request.ImageRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YesNoCategoryScreen(navController: NavHostController) {
    val categories = listOf(
        "Personality", "Places", "Food", "Objects",
        "Animals", "TVShows", "Games", "Sports"
    )
    
    var selectedCategory by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(16.dp)
    ) {
        // Minimal header: back only
        TopAppBar(
            title = {},
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFAFAFA)),
            navigationIcon = { IconButton(onClick = { navController.navigateUp() }) { Icon(painterResource(id = R.drawable.ic_back), contentDescription = "Back", modifier = Modifier.size(30.dp)) } }
        )

        val montserrat = FontFamily.SansSerif

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(categories) { category ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clickable { selectedCategory = category },
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (selectedCategory == category) 8.dp else 4.dp
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedCategory == category) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val assetName = when (category) {
                            "TVShows" -> "TV SHOWS.avif"
                            else -> "${category.uppercase()}.avif"
                        }
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data("file:///android_asset/Images/$assetName")
                                .build(),
                            contentDescription = category,
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .aspectRatio(1f)
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        val displayName = when (category) {
                            "TVShows" -> "TV SHOWS"
                            else -> category.uppercase()
                        }
                        val adjustedStyle = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp)
                        Text(
                            text = displayName,
                            style = adjustedStyle,
                            fontWeight = FontWeight.Bold,
                            fontFamily = montserrat,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            color = if (selectedCategory == category) 
                                MaterialTheme.colorScheme.onPrimaryContainer 
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        
        // Continue Button
        Button(
            onClick = {
                if (selectedCategory.isNotEmpty()) {
                    navController.navigate("yesNoSettings/$selectedCategory")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            enabled = selectedCategory.isNotEmpty()
        ) {
            Text("Continue")
        }
    }
}
