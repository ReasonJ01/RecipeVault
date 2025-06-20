package com.example.recipevault

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject


@Composable
fun RecipeCard(
    modifier: Modifier,
    title: String,
    image: String?,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)

    ) {
        if (image == null) {
            ImagePlaceholder(text = title)
        } else {
            AsyncImage(model = image, contentDescription = null)
        }

        Text(
            modifier = Modifier.padding(12.dp),
            text = title,
            style = MaterialTheme.typography.displaySmall
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeView(
    modifier: Modifier,
    navController: NavHostController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val recipes = viewModel.recipes.collectAsState().value

    val context = LocalContext.current
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var currentApiKey by remember { mutableStateOf(PrefsManager.getApiKey(context)) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet {
                NavigationDrawerItem(
                    label = { Text("Recipes") },
                    selected = true,
                    onClick = {
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Ingredients") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate("ingredients")
                        }
                    }
                )
            }
        },
        drawerState = drawerState
    ) {
        Surface {
            Scaffold(
                floatingActionButton = {
                    FloatingActionButton(onClick = {
                        navController.navigate("addRecipe")
                    }) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Recipe")
                    }
                },
                topBar = {
                    TopAppBar(
                        title = { Text("Recipies") },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    imageVector = Icons.Filled.Menu,
                                    contentDescription = "Menu"
                                )
                            }
                        })
                }


            ) { contentPadding ->
                if (recipes.isEmpty()) {
                    Column(
                        modifier = modifier
                            .padding(contentPadding),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "No recipes found",
                            textAlign = TextAlign.Center,
                        )
                        Button(onClick = { navController.navigate("addRecipe") }) {
                            Text(text = "Add Recipe")
                        }
                    }

                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .padding(contentPadding)
                            .padding(horizontal = 8.dp),
                    ) {
                        items(recipes) { recipe ->
                            RecipeCard(
                                modifier = Modifier.fillMaxSize(),
                                title = recipe.title ?: "No title",
                                image = recipe.imageUrl,
                                onClick = { navController.navigate("recipe/${recipe.recipeId}") }
                            )
                        }

                        item {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }

                        item {
                            Row(
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Button(onClick = { navController.navigate("addRecipe") }) {
                                    Text(text = "Add Recipe")
                                }

                                Button(onClick = { showApiKeyDialog = true }) {
                                    Text("Set API Key")

                                }
                                ApiKeyEntryModal(
                                    showDialog = showApiKeyDialog,
                                    onDismissRequest = { showApiKeyDialog = false },
                                    onApiKeySaved = { savedKey ->
                                        currentApiKey = savedKey // Update the displayed key
                                        // You might want to trigger other actions here, like re-fetching data
                                        showApiKeyDialog = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@HiltViewModel
class HomeViewModel @Inject constructor(
    private val dao: RecipeDao,
) : ViewModel() {
    val recipes = dao.getAllRecipes().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
}



