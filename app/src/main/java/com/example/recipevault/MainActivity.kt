package com.example.recipevault

import android.media.Image
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.example.recipevault.ui.theme.RecipeVaultTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            RecipeVaultTheme {
                Scaffold(modifier = Modifier
                    .fillMaxSize()
                    .padding(WindowInsets.safeDrawing.asPaddingValues())) { innerPadding ->
                    HomeView(modifier = Modifier
                        .padding(innerPadding)
                        .padding(16.dp))
                }
            }
        }
    }
}



@Composable
fun ImagePlaceholder(modifier: Modifier = Modifier, text: String){
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .aspectRatio(1f)
            .background(Color.LightGray),
    ){
        Text(text=text, color = Color.DarkGray, style = MaterialTheme.typography.displayLarge, textAlign = TextAlign.Center)
    }
}

@Composable
fun RecipeCard(modifier: Modifier, title: String, desciption: String, image: Image?){
    Card (modifier = modifier){
        if (image == null) {
            ImagePlaceholder(text = title.plus(" (image was null)"))
        } else {
            ImagePlaceholder(text = title)
        }

        Text(modifier = Modifier.padding(12.dp), text=title, style = MaterialTheme.typography.displaySmall)
        Text(modifier = Modifier.padding(12.dp), text=desciption, style = MaterialTheme.typography.bodyMedium)
    }

}

@Composable
fun HomeView( modifier: Modifier){
    val items  = List(50) { "Recipe ${it + 1}" }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = modifier){
        items(items){
            item -> RecipeCard(modifier = Modifier.fillMaxSize(), title =item, desciption = "Lorem Ipsum decourm est lorem ipsum decoum est loreum ipsum decorum est In Compose, use a serializable object or class to define a route. A route describes how to get to a destination, and contains all the information that the destination requires.", image = null)
        }
    }
}