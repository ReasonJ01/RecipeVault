package com.example.recipevault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.recipevault.ui.theme.RecipeVaultTheme
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RecipeVaultTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RecipeVaultApp()
                }


            }
        }
    }
}


@Composable
fun ImagePlaceholder(modifier: Modifier = Modifier, text: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
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
fun ConfirmDialog(
    title: String = "Confirm",
    message: String,
    confirmText: String = "Yes",
    confirmColor: Color = MaterialTheme.colorScheme.error,
    dismissText: String = "No",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = confirmColor)
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}


@Composable
fun RecipeVaultApp() {
    val navController = rememberNavController()
    val mod = Modifier
        .padding(WindowInsets.safeDrawing.asPaddingValues())
        .padding(horizontal = 8.dp)
        .padding(top = 12.dp)
        .fillMaxSize()
    NavHost(
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
        navController = navController,
        startDestination = "home",
    ) {
        composable("home") {
            HomeView(
                modifier = mod,
                navController = navController
            )
        }
        composable(
            "recipe/{recipeId}",
            arguments = listOf(navArgument("recipeId") { type = NavType.IntType })
        ) { backStackEntry ->
            RecipeView(
                modifier = mod,
                navController = navController,
                recipeId = backStackEntry.arguments?.getInt("recipeId")
            )
        }
        composable("addRecipe") {
            AddRecipeView(modifier = mod, navController = navController)
        }
        composable(
            "editRecipe/{recipeId}", listOf(navArgument("recipeId") { type = NavType.IntType })
        ) { backStackEntry ->
            EditRecipeView(
                modifier = mod,
                navController = navController,
            )
        }
        composable("ingredients") {
            IngredientView(modifier = mod, navController = navController)
        }
    }
}
