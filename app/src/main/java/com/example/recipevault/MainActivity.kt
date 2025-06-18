package com.example.recipevault

import android.media.Image
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.recipevault.ui.theme.RecipeVaultTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.util.Collections
import java.util.UUID
import javax.inject.Inject


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
fun HomeView(
    modifier: Modifier,
    navController: NavHostController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val recipes = viewModel.recipes.collectAsState().value

    if (recipes.isEmpty()) {
        Column(
            modifier = modifier
                .padding(16.dp),
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
            modifier = modifier
        ) {
            items(recipes) { recipe ->
                RecipeCard(
                    modifier = Modifier.fillMaxSize(),
                    title = recipe.title ?: "No title",
                    description = recipe.description ?: "No description",
                    image = null,
                    onClick = { navController.navigate("recipe") }
                )
            }
        }
    }


}


@Composable
fun RecipeView(
    modifier: Modifier,
    navController: NavHostController,
) {
    val title = "Recipe Title"
    val description =
        "Lorem Ipsum decourm est lorem ipsum decoum est loreum ipsum decorum est In Compose, use a serializable object or class to define a route. A route describes how to get to a destination, and contains all the information that the destination requires."


    Column(modifier = modifier) {
        Card() { ImagePlaceholder(text = title) }
        Text(text = "Recipe Title")
        Button(onClick = { navController.navigateUp() }) { Text(text = "Go Back") }
    }


}


@Composable
fun AddRecipeView(
    modifier: Modifier,
    navController: NavHostController,
    viewModel: AddRecipeViewModel = hiltViewModel()

) {
    val steps = viewModel.steps
    LazyColumn(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            OutlinedTextField(
                value = viewModel.title,
                onValueChange = viewModel::onTitleChange,
                textStyle = MaterialTheme.typography.displayMedium,
                placeholder = ({
                    Text(
                        text = "Recipe Title",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.background,
                    unfocusedBorderColor = MaterialTheme.colorScheme.background,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )

            )
            Text("Method", style = MaterialTheme.typography.displaySmall)
        }
        itemsIndexed(steps, key = { _, step -> step.id }) { index, step ->
            StepElement(
                onValueChange = { newText -> viewModel.updateStep(index, newText) },
                swapFunction = { viewModel.swapSteps(index) },
                onDelete = { viewModel.removeStep(step) },
                step = step,
                index = index,
                modifier = Modifier
                    .animateItem()
                    .fillMaxWidth()
            )
        }
        item {
            Box(modifier = Modifier.animateItem()) {
                Button(onClick = { viewModel.addNewStep() }) { Text(text = "Add Step") }
            }

        }
    }

}


@Composable
fun StepElement(
    onValueChange: (String) -> Unit,
    swapFunction: () -> Unit,
    onDelete: () -> Unit,
    step: StepEl,
    index: Int,
    modifier: Modifier
) {
    Column(modifier = modifier.padding(0.dp)) {
        Text(
            text = "Step ${index + 1}",
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.padding(8.dp)
        )
        MethodTextField(
            value = step.text,
            onValueChange = onValueChange,
            parser = { AnnotatedString(it) })
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = swapFunction,
            ) { Icon(imageVector = Icons.Filled.KeyboardArrowUp, contentDescription = "Move up") }
            IconButton(
                onClick = onDelete,
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

fun suggestionComplete(input: String, suggestion: String): String {
    val match =
        "@(\\w*(\\(\\))?)".toRegex().findAll(input).lastOrNull()?.groupValues

    val formattedSuggestion = "@".plus(suggestion).plus("()")
    val fullMatch = match?.get(0) ?: ""


    Log.d("Suggestion", "Match: $fullMatch Formatted suggestion $formattedSuggestion")

    if (fullMatch.isEmpty()) {
        return ""
    }

    var maxMatchIndex = 0
    for (i in fullMatch.indices) {
        if (fullMatch[i].lowercase() == formattedSuggestion[i].lowercase()) {
            maxMatchIndex++
        }
    }



    return formattedSuggestion.substring(maxMatchIndex)


}

@Composable
fun MethodTextField(
    value: String,
    onValueChange: (String) -> Unit,
    parser: (String) -> AnnotatedString
) {
    var input by remember { mutableStateOf(TextFieldValue("")) }
    val allSuggestions = listOf("Carrot", "Potato", "Onion", "Garlic", "Ginger")
    var suggestions = remember { mutableStateOf<List<String>>(emptyList<String>()) }
    val focusRequester = remember { FocusRequester() }


    LaunchedEffect(input) {
        val match = "@(\\w*)".toRegex().findAll(input.text).lastOrNull()
        val matchText = match?.groupValues?.get(1) ?: ""
        val matchEnd = match?.range?.last ?: -1
        if (matchText.isNotEmpty() && matchEnd == input.text.lastIndex) {
            suggestions.value =
                allSuggestions.filter { it.startsWith(matchText, ignoreCase = true) }
        } else suggestions.value = emptyList<String>()


    }

    Column {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(suggestions.value) { suggestion ->
                SuggestionChip(
                    onClick = {
                        val t = input.text + suggestionComplete(input.text, suggestion)
                        val rangeEnd = (t.length - 1).coerceAtLeast(0)
                        input = input.copy(text = t, selection = TextRange(rangeEnd))
                        focusRequester.requestFocus()
                    },
                    label = { Text(text = suggestion) }
                )
            }
        }

        BasicTextField(
            value = input,
            onValueChange = {
                onValueChange(it.text)
                input = it
            },

            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            textStyle = MaterialTheme.typography.bodyLarge

        )

    }
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
        navController = navController,
        startDestination = "home",
    ) {
        composable("home") {
            HomeView(
                modifier = mod,
                navController = navController
            )
        }
        composable("recipe") {
            RecipeView(
                modifier = mod,
                navController = navController
            )
        }
        composable("addRecipe") {
            AddRecipeView(modifier = mod, navController = navController)
        }
    }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val dao: RecipeDao
) : ViewModel() {
    val recipes = dao.getAllRecipes().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
}

data class StepEl(
    val id: String = UUID.randomUUID().toString(),
    val text: String = ""
)


@HiltViewModel
class AddRecipeViewModel @Inject constructor() : ViewModel() {
    var title by mutableStateOf("")
        private set
    private val _steps = mutableStateListOf<StepEl>()
    val steps: List<StepEl> get() = _steps


    fun addNewStep() {
        _steps.add(StepEl())

    }

    fun removeStep(step: StepEl) {
        _steps.remove(step)
    }

    fun updateStep(index: Int, newText: String) {
        _steps[index] = _steps[index].copy(text = newText)
    }

    fun swapSteps(index: Int) {
        if (index in 1 until _steps.lastIndex + 1)
            Collections.swap(_steps, index, index - 1)
    }

    fun onTitleChange(newTitle: String) {
        title = newTitle
    }
}