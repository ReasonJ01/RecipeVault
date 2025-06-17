package com.example.recipevault

import android.media.Image
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.recipevault.ui.theme.RecipeVaultTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RecipeVaultTheme {
                RecipeVaultApp()

            }
        }
    }
}


@Composable
fun ImagePlaceholder(modifier: Modifier = Modifier, text: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .aspectRatio(1f)
            .background(Color.LightGray),
    ) {
        Text(
            text = text,
            color = Color.DarkGray,
            style = MaterialTheme.typography.displayLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun RecipeCard(
    modifier: Modifier,
    title: String,
    description: String,
    image: Image?,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)

    ) {
        if (image == null) {
            ImagePlaceholder(text = title.plus(" (image was null)"))
        } else {
            ImagePlaceholder(text = title)
        }

        Text(
            modifier = Modifier.padding(12.dp),
            text = title,
            style = MaterialTheme.typography.displaySmall
        )
        Text(
            modifier = Modifier.padding(12.dp),
            text = description,
            style = MaterialTheme.typography.bodyMedium
        )
    }

}

@Composable
fun HomeView(modifier: Modifier, navController: NavHostController) {
    val items = List(50) { "Recipe ${it + 1}" }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {
        items(items) { item ->
            RecipeCard(
                modifier = Modifier.fillMaxSize(),
                title = item,
                description = "Lorem Ipsum decourm est lorem ipsum decoum est loreum ipsum decorum est In Compose, use a serializable object or class to define a route. A route describes how to get to a destination, and contains all the information that the destination requires.",
                image = null,
                onClick = { navController.navigate("recipe") }
            )
        }

    }
}

@Composable
fun RecipeView(modifier: Modifier, navController: NavHostController) {
    val title = "Recipe Title"
    val description =
        "Lorem Ipsum decourm est lorem ipsum decoum est loreum ipsum decorum est In Compose, use a serializable object or class to define a route. A route describes how to get to a destination, and contains all the information that the destination requires."


    Column(modifier = modifier) {
        Text(text = "R")
        Button(onClick = { navController.navigateUp() }) { Text(text = "Go Back") }
    }


}

@Composable
fun RecipeVaultApp() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "home",
    ) {
        composable("home") {
            HomeView(
                modifier = Modifier
                    .padding(WindowInsets.safeDrawing.asPaddingValues())
                    .padding(horizontal = 8.dp)
                    .padding(top = 12.dp)
                    .fillMaxSize(),
                navController = navController
            )
        }
        composable("recipe") {
            RecipeView(
                modifier = Modifier
                    .padding(WindowInsets.safeDrawing.asPaddingValues())
                    .padding(horizontal = 8.dp)
                    .padding(top = 12.dp)
                    .fillMaxSize(),
                navController = navController
            )
        }
    }
}



