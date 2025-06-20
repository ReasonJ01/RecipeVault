package com.example.recipevault

import android.content.Context
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import coil3.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientView(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    viewModel: IngredientViewModel = hiltViewModel()
) {
    val ingredients = viewModel.ingredients.collectAsState().value
    val context = LocalContext.current

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()




    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet {
                NavigationDrawerItem(
                    label = { Text("Recipes") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate("home")
                        }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Ingredients") },
                    selected = true,
                    onClick = {
                    }
                )
            }
        },
        drawerState = drawerState
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Ingredients") },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    imageVector = Icons.Filled.Menu,
                                    contentDescription = "Menu"
                                )
                            }
                        })
                }) { contentPadding ->
                LazyColumn(
                    modifier = Modifier.padding(contentPadding),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ingredients, key = { it.ingredientId }) { ingredient ->
                        Log.d("IngredientViewModel", "ingredient: $ingredient")
                        if (ingredient.name == null) return@items

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (ingredient.imageUrl.isNullOrEmpty()) {
                                    ImagePlaceholder(
                                        text = ingredient.name.take(2),
                                        modifier = Modifier
                                            .weight(0.3f)
                                            .aspectRatio(1f)
                                    )
                                } else {
                                    AsyncImage(
                                        model = ingredient.imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier.weight(0.3f)
                                    )

                                }
                                Spacer(modifier = Modifier.weight(0.05f))
                                Text(
                                    text = ingredient.name.toTitleCase(),
                                    modifier = Modifier.weight(0.4f)
                                )
                                Spacer(modifier = Modifier.weight(0.05f))
                                SpinningIconButton(onClick = {
                                    viewModel.regenerateImage(ingredient, context)
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpinningIconButton(onClick: () -> Unit) {
    var spinTrigger by remember { mutableStateOf(false) }
    val rotation = remember { Animatable(0f) }

    IconButton(onClick = {
        spinTrigger = true
        onClick()
    }) {
        LaunchedEffect(spinTrigger) {
            if (spinTrigger) {
                repeat(10) {
                    rotation.snapTo(0f)
                    rotation.animateTo(
                        targetValue = 720f,
                        animationSpec = tween(durationMillis = 1800, easing = EaseOut)
                    )
                }

                spinTrigger = false
            }
        }

        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Spin",
            modifier = Modifier.rotate(rotation.value)
        )
    }
}


@HiltViewModel
class IngredientViewModel @Inject constructor(
    dao: IngredientDao,
) : ViewModel() {
    val ingredients = dao.getAllIngredients().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun regenerateImage(ingredient: Ingredient, context: Context) {
        Log.d("IngredientViewModel", "regenerateImage: $ingredient")
        viewModelScope.launch {
            val key = PrefsManager.getApiKey(context)
            if (key.isNullOrEmpty()) return@launch
            val inputData = workDataOf(
                "ingredientId" to ingredient.ingredientId,
                "ingredientName" to ingredient.name
            )
            val workName = "regenerate_${ingredient.ingredientId}"


            val workRequest = OneTimeWorkRequestBuilder<IngredientWorker>()
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                workName,
                androidx.work.ExistingWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
