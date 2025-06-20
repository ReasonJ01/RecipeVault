package com.example.recipevault

import android.content.Context
import android.media.Image
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import coil3.compose.AsyncImage
import com.example.recipevault.ui.theme.RecipeVaultTheme
import com.example.recipevault.ui.theme.headlineMediumGaramond
import com.example.recipevault.ui.theme.headlineSmallGaramond
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.apache.commons.text.similarity.JaroWinklerDistance
import java.util.Collections
import javax.inject.Inject
import kotlin.random.Random


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
fun RecipeCard(
    modifier: Modifier,
    title: String,
    image: Image?,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)

    ) {
        if (image == null) {
            ImagePlaceholder(text = title)
        } else {
            ImagePlaceholder(text = title)
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
                        scope.launch { drawerState.close() }
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
                        modifier = Modifier.padding(contentPadding),
                    ) {
                        items(recipes) { recipe ->
                            RecipeCard(
                                modifier = Modifier.fillMaxSize(),
                                title = recipe.title ?: "No title",
                                image = null,
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


val unitAliases = mapOf(
    "g" to "grams", "gram" to "grams", "grams" to "grams",
    "kg" to "kilograms", "kilogram" to "kilograms",

    "ml" to "millilitres", "millilitre" to "millilitres", "millilitres" to "millilitres",
    "l" to "litres", "litre" to "litres",

    "tsp" to "teaspoons", "teaspoon" to "teaspoons",
    "tbsp" to "tablespoons", "tablespoon" to "tablespoons",

    "oz" to "ounces", "ounce" to "ounces",

    "cup" to "cups", "cups" to "cups",
    "" to ""
)

val singlularUnits = mapOf(
    "grams" to "gram",
    "kilograms" to "kilogram",
    "millilitres" to "millilitre",
    "litres" to "litre",
    "teaspoons" to "teaspoon",
    "tablespoons" to "tablespoon",
    "ounces" to "ounce",
    "cups" to "cup"
)


val shorthandUnits = mapOf(
    "grams" to "g",
    "kilograms" to "kg",
    "millilitres" to "ml",
    "litres" to "l",
    "teaspoons" to " tsp",
    "tablespoons" to " tbsp",
    "ounces" to "oz"
)

sealed class StepSegment
data class TextSegment(val text: String) : StepSegment()
data class IngredientSegment(val name: String, val quantity: String, val unit: String) :
    StepSegment()

fun parseStepString(input: String): List<StepSegment> {
    val result = mutableListOf<StepSegment>()
    var i = 0
    val sb = StringBuilder()

    while (i < input.length) {
        // If we see the start of a new ingredient
        if (input[i] == '@') {
            // If we have a previous ingredient that was not completed,
            // it is malformed and save it as text
            if (sb.isNotEmpty()) {
                result.add(TextSegment(sb.toString()))
                sb.clear()
            }

            // Move past @
            i++
            val nameStart = i
            while (i < input.length && input[i] != '(') i++
            if (i >= input.length) {
                sb.append(input.substring(nameStart))
                break
            }

            val nameEnd = i
            val name = input.substring(nameStart, nameEnd)


            var quantity: String? = null
            var unit: String? = null

            i++
            val paramsStart = i
            while (i < input.length && input[i] != ')') i++
            if (i >= input.length) {
                sb.append(input.substring(nameStart))
                break
            }

            val paramsEnd = i++
            val params = input.substring(paramsStart, paramsEnd).split(",")

            quantity = params.getOrNull(0)?.trim()
            unit = params.getOrNull(1)?.trim()

            if (name.isBlank()) {
                sb.append(input.substring(paramsEnd))
                break
            }

            result.add(IngredientSegment(name, quantity ?: "", unit ?: ""))
        } else {
            sb.append(input[i])
            i++
        }
    }

    if (sb.isNotEmpty()) {
        result.add(TextSegment(sb.toString()))
    }

    return result
}

fun formatIngredient(name: String, quantity: String, rawUnit: String): String {
    val canonical = unitAliases[rawUnit.trim().lowercase()] ?: rawUnit
    val short = shorthandUnits[canonical]

    // Add flour...
    if (canonical.isBlank() && quantity.isBlank()) {
        return name
    }

    // add flour (30g)
    if (short != null) {
        return "$name ($quantity$short)"
    }

    if (quantity.startsWith("1") || quantity.startsWith("0")) {
        val singular = singlularUnits[canonical]

        // add flour (1 smidge/smidges)
        if (singular == null) {
            return "$name ($quantity $canonical)"
        }
        // add flour (1 cup)
        return "$name ($quantity $singular)"
    }

    // add flour (3 cups)
    return "$name ($quantity $canonical)"

}


fun formatStepForDisplay(step: Step): String {
    val parsedStep = parseStepString(step.description ?: "")

    return parsedStep.joinToString("") {
        when (it) {
            is TextSegment -> it.text
            is IngredientSegment -> formatIngredient(it.name.toTitleCase(), it.quantity, it.unit)

        }
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
fun RecipeView(
    modifier: Modifier,
    navController: NavHostController,
    recipeId: Int?,
    viewModel: RecipeViewModel = hiltViewModel()
) {
    val recipe = viewModel.recipe?.collectAsState()?.value
    viewModel.fetchSteps()
    val ingredients = viewModel.stepsWithIngredients.map { it.ingredients }
    val steps = viewModel.stepsWithIngredients
    val flatIngredients = ingredients.flatten()
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        ConfirmDialog(
            title = "Delete Recipe?",
            message = "Are you sure you want to delete this recipe?",
            confirmText = "Delete",

            dismissText = "Cancel",
            onDismiss = { showDialog = false },
            onConfirm = {
                viewModel.deleteRecipe()
                navController.navigateUp()
            }
        )
    }


    Surface {
        if (recipe == null) {
            Text(text = "Loading...")
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            )
            {

                item {
                    Card {
                        ImagePlaceholder(text = recipe.recipe.title ?: "No title")
                    }

                }
                item {
                    Text(
                        text = recipe.recipe.title ?: "No title",
                        style = MaterialTheme.typography.displayMedium
                    )
                }
                item {
                    Text(
                        text = "Ingredients",
                        style = MaterialTheme.typography.headlineMediumGaramond
                    )
                }
                item {
                    FlowRow(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        steps.forEach { step ->
                            val ingredientTags = parseStepString(
                                step.step.description ?: ""
                            ).filterIsInstance<IngredientSegment>()
                            for (tag in ingredientTags) {
                                val url = flatIngredients.find { it.name == tag.name }?.imageUrl
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Card {
                                        if (url != null) {
                                            AsyncImage(
                                                model = url,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .fillMaxWidth(0.3f)
                                            )
                                        } else {
                                            ImagePlaceholder(
                                                text = tag.name.toTitleCase().take(2),
                                                modifier = Modifier
                                                    .fillMaxWidth(0.3f)
                                            )
                                        }
                                    }

                                    Text(text = tag.name.toTitleCase())
                                    Text(text = "${tag.quantity} ${tag.unit}".trim())
                                }

                            }
                        }

                    }
                }
                item {
                    Text(text = "Method", style = MaterialTheme.typography.headlineMediumGaramond)
                }

                itemsIndexed(recipe.steps) { index, step ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Text(
                            modifier = Modifier.padding(8.dp),
                            text = "Step ${index + 1}",
                            style = MaterialTheme.typography.headlineSmallGaramond
                        )
                        Text(text = formatStepForDisplay(step), modifier = Modifier.padding(8.dp))
                    }

                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            onClick = { showDialog = true },
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                        ) {
                            Text(text = "Delete Recipe")
                        }
                        Button(
                            onClick = { navController.navigate("editRecipe/${recipe.recipe.recipeId}") },
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                        ) {
                            Text(text = "Update Recipe")
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun EditRecipeView(
    modifier: Modifier,
    navController: NavHostController,
    viewModel: EditRecipeViewModel = hiltViewModel()
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val steps = viewModel.steps
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            OutlinedTextField(
                isError = !viewModel.titleError.isNullOrEmpty(),
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
        if (steps.isNotEmpty()) {
            itemsIndexed(steps, key = { _, step -> step.stepId }) { index, step ->
                StepElement(
                    onValueChange = { newText -> viewModel.updateStep(index, newText) },
                    swapFunction = { viewModel.swapSteps(index) },
                    onDelete = { viewModel.removeStep(step) },
                    isError = !viewModel.stepErrors[index].isNullOrEmpty(),
                    step = step,
                    index = index,
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth()
                )
            }
        }
        item {
            Box(modifier = Modifier.animateItem()) {
                OutlinedButton(onClick = {
                    viewModel.addNewStep()
                    coroutineScope.launch {
                        listState.animateScrollToItem(viewModel.steps.lastIndex)
                    }
                }) { Text(text = "Add Step") }
            }

        }
        item {
            Button(
                onClick = {
                    viewModel.saveRecipe(
                        context = context,
                        onSuccess = { navController.navigateUp() })
                }, modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 16.dp)
            ) {
                Icon(imageVector = Icons.Filled.Check, contentDescription = "Save")
                Text(text = "Save Recipe")
            }
        }
    }

}

@Composable
fun AddRecipeView(
    modifier: Modifier,
    navController: NavHostController,
    viewModel: AddRecipeViewModel = hiltViewModel()

) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()


    LaunchedEffect(Unit) {
        viewModel.addNewStep()
    }
    val context = LocalContext.current
    val steps = viewModel.steps
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            OutlinedTextField(
                isError = !viewModel.titleError.isNullOrEmpty(),
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
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )

            )
            Text("Method", style = MaterialTheme.typography.headlineMediumGaramond)
        }
        itemsIndexed(steps, key = { _, step -> step.stepId }) { index, step ->
            StepElement(
                onValueChange = { newText -> viewModel.updateStep(index, newText) },
                swapFunction = { viewModel.swapSteps(index) },
                onDelete = { viewModel.removeStep(step) },
                isError = !viewModel.stepErrors[index].isNullOrEmpty(),
                step = step,
                index = index,
                modifier = Modifier
                    .animateItem()
                    .fillMaxWidth()
            )
        }
        item {
            Box(modifier = Modifier.animateItem()) {
                OutlinedButton(onClick = {
                    viewModel.addNewStep()
                    coroutineScope.launch {
                        listState.animateScrollToItem(viewModel.steps.lastIndex)
                    }
                }) { Text(text = "Add Step") }
            }

        }
        item {
            Button(
                onClick = {
                    viewModel.saveRecipe(
                        context = context,
                        onSuccess = { navController.navigateUp() })
                }, modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 16.dp)
            ) {
                Icon(imageVector = Icons.Filled.Check, contentDescription = "Save")
                Text(text = "Save Recipe")
            }
        }
    }

}


@Composable
fun StepElement(
    onValueChange: (String) -> Unit,
    swapFunction: () -> Unit,
    onDelete: () -> Unit,
    step: Step,
    index: Int,
    modifier: Modifier,
    isError: Boolean = false
) {

    Card(modifier = modifier) {
        Column() {
            Text(
                text = "Step ${index + 1}",
                style = MaterialTheme.typography.headlineSmallGaramond,
                modifier = Modifier.padding(8.dp)
            )
            MethodTextField(
                value = step.description ?: "",
                onValueChange = onValueChange,
                isError = isError
            )
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = swapFunction,
                    enabled = index > 0
                ) {

                    Icon(
                        painter = painterResource(R.drawable.baseline_arrow_upward_24),
                        contentDescription = "Move up"
                    )
                }
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
}

fun applySuggestion(input: String, suggestion: String): String {
    val match =
        "@(\\w*(\\(\\))?)".toRegex().findAll(input).lastOrNull()

    val formattedSuggestion = "@".plus(suggestion).plus("()")
    val fullMatch = match?.groupValues?.get(0) ?: ""
    val matchStart = match?.range?.start ?: -1
    val matchEnd = match?.range?.last ?: -1

    if (fullMatch.isEmpty() || matchStart == -1 || matchEnd == -1) {
        return ""
    }

    return input.removeRange(matchStart, matchEnd + 1).plus(formattedSuggestion)


}

fun String.toTitleCase(): String {
    if (isEmpty()) return this
    if (this.contains("_")) {
        return this.replace("_", " ").toTitleCase()
    }
    return this.lowercase().split(" ").joinToString(" ") { it.replaceFirstChar(Char::titlecase) }
}


@Composable
fun MethodTextField(
    value: String,
    onValueChange: (String) -> Unit,
    viewModel: AddRecipeViewModel = hiltViewModel(),
    isError: Boolean = false,

    ) {
    var input by remember { mutableStateOf(TextFieldValue(text = value)) }
    val allSuggestions = viewModel.allIngredients.collectAsState().value

    val suggestions = remember { mutableStateOf<List<String>>(emptyList<String>()) }
    val focusRequester = remember { FocusRequester() }


    LaunchedEffect(input) {
        val levThresh = 2
        val match = "@(\\w*)".toRegex().findAll(input.text).lastOrNull()
        val matchText = match?.groupValues?.get(1) ?: ""
        val matchEnd = match?.range?.last ?: -1
        if (matchText.isNotEmpty() && matchEnd == input.text.lastIndex) {
            val distance = JaroWinklerDistance()
            val ranked = allSuggestions.map {
                it to distance.apply(
                    matchText.lowercase(),
                    it.name?.lowercase() ?: ""
                )
            }.sortedBy { it.second }.take(5).map { it.first.name ?: "" }

            suggestions.value = ranked
        } else suggestions.value = emptyList<String>()


    }

    Column {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(suggestions.value) { suggestion ->
                SuggestionChip(
                    onClick = {
                        val t = applySuggestion(input.text, suggestion)
                        val rangeEnd = (t.length - 1).coerceAtLeast(0)
                        input = input.copy(text = t, selection = TextRange(rangeEnd))
                        focusRequester.requestFocus()
                    },
                    label = { Text(text = suggestion.toTitleCase()) }
                )
            }
        }

        OutlinedTextField(
            value = input,
            isError = isError,
            onValueChange = {
                onValueChange(it.text)
                input = it
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            textStyle = MaterialTheme.typography.bodyLarge,
            placeholder = ({
                Text(
                    text = "Instructions....",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
            )

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


@HiltViewModel
class RecipeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dao: RecipeDao,
    private val stepDao: StepDao
) : ViewModel() {
    val recipeId = savedStateHandle.get<Int>("recipeId")


    val recipe = recipeId?.let {
        dao.getRecipeWithStepsById(it).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )
    }

    private val _stepsWithIngredients = mutableStateOf<List<StepWithIngredients>>(emptyList())
    val stepsWithIngredients: List<StepWithIngredients> get() = _stepsWithIngredients.value

    fun fetchSteps() {
        viewModelScope.launch {
            val steps = recipeId?.let { stepDao.getStepWithIngredientsByRecipeId(it) }
            if (steps != null) {
                _stepsWithIngredients.value = steps.filterNotNull()
            }
        }
    }

    fun deleteRecipe() {
        viewModelScope.launch {
            recipe?.value?.let { dao.delete(it.recipe) }
        }
    }


}

@HiltViewModel
class EditRecipeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recipeDao: RecipeDao,
    private val ingredientDao: IngredientDao,
    private val stepDao: StepDao
) : ViewModel() {
    val recipeId = savedStateHandle.get<Int>("recipeId")

    var recipe by mutableStateOf<Recipe?>(null)
        private set

    var title by mutableStateOf("")
        private set
    var titleError by mutableStateOf<String?>(null)
        private set

    private val _steps = mutableStateListOf<Step>()
    val steps: List<Step> get() = _steps

    private val _stepErrors = mutableStateListOf<String?>()
    val stepErrors: List<String?> get() = _stepErrors


    init {
        Log.d("EditRecipeViewModel", "recipeid $recipeId")

        viewModelScope.launch {
            val loaded = recipeDao.getRecipeWithStepsById(recipeId ?: -1).first()
            if (loaded != null) {
                recipe = loaded.recipe
                title = loaded.recipe.title ?: ""
                _steps.addAll(loaded.steps)
                _stepErrors.addAll(List(loaded.steps.size) { null })
            }
        }
    }


    val allIngredients = ingredientDao.getAllIngredients().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )


    fun saveRecipe(context: Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            var existingRefs =
                stepDao.getAllCrossRefs().map { Pair(it.ingredientId, it.stepId) }.toSet()

            var valid = true
            if (title.isBlank()) {
                titleError = "Title cannot be blank"
                valid = false
            } else {
                titleError = null
            }
            Log.d("EditRecipeViewModel", "title $title")

            _steps.forEachIndexed { index, step ->
                if (step.description.isNullOrEmpty()) {
                    _stepErrors[index] = "Step cannot be blank"
                    valid = false
                } else {
                    _stepErrors[index] = null
                }

            }
            if (_steps.isEmpty()) {
                valid = false
            }

            if (!valid) return@launch



            recipe?.let { recipeDao.updateAll(it.copy(title = title)) }

            stepDao.updateAll(*_steps.toTypedArray())

            _steps.forEachIndexed { index, step ->
                val matches = "@(\\w+)".toRegex().findAll(_steps[index].description ?: "")
                for (match in matches) {
                    val ingredientName = match.groupValues[1]
                    val existingIngredient = ingredientDao.getIngredientByName(ingredientName)
                    val ingredient = existingIngredient ?: Ingredient(
                        ingredientId = Random.nextInt(),
                        name = ingredientName,
                        imageUrl = null
                    )
                    if (existingIngredient == null) {
                        ingredientDao.insertAll(ingredient)
                        val inputData = workDataOf(
                            "ingredientId" to ingredient.ingredientId,
                            "ingredientName" to ingredient.name
                        )

                        val key = PrefsManager.getApiKey(context)
                        if (!key.isNullOrEmpty()) {
                            val workRequest = OneTimeWorkRequestBuilder<IngredientWorker>()
                                .setInputData(inputData)
                                .build()
                            WorkManager.getInstance(context).enqueue(workRequest)
                        }
                    }

                    if (Pair(ingredient.ingredientId, step.stepId) !in existingRefs) {
                        stepDao.insertCrossRef(
                            IngredientStepCrossRef(
                                ingredientId = ingredient.ingredientId,
                                stepId = step.stepId
                            )
                        )
                        existingRefs.plus(Pair(ingredient.ingredientId, step.stepId))
                    }

                }
            }
            onSuccess()
        }

    }

    fun addNewStep() {
        _steps.add(
            Step(
                stepId = Random.nextInt(),
                stepNumber = _steps.size,
                recipeId = recipeId ?: -1,
                description = null
            )
        )
        _stepErrors.add(null)

    }

    fun removeStep(step: Step) {
        val index = _steps.indexOf(step)
        if (index != -1) {
            _steps.removeAt(index)
            _stepErrors.removeAt(index)
        }
    }

    fun updateStep(index: Int, newText: String) {
        _steps[index] = _steps[index].copy(description = newText)
        _stepErrors[index] = null

    }

    fun swapSteps(index: Int) {
        if (index in 1 until _steps.lastIndex + 1)
            Collections.swap(_steps, index, index - 1)
    }

    fun onTitleChange(newTitle: String) {
        title = newTitle
    }
}


@HiltViewModel
class AddRecipeViewModel @Inject constructor(
    private val recipeDao: RecipeDao,
    private val ingredientDao: IngredientDao,
    private val stepDao: StepDao
) : ViewModel() {
    var title by mutableStateOf("")
        private set
    private val _steps = mutableStateListOf<Step>()
    val steps: List<Step> get() = _steps

    var titleError by mutableStateOf<String?>(null)
        private set

    private val _stepErrors = mutableStateListOf<String?>()
    val stepErrors: List<String?> get() = _stepErrors


    val allIngredients = ingredientDao.getAllIngredients().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    private val insertedRefs = mutableSetOf<Pair<Int, Int>>()

    fun saveRecipe(context: Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            var valid = true
            if (title.isBlank()) {
                titleError = "Title cannot be blank"
                valid = false
            } else {
                titleError = null
            }

            _steps.forEachIndexed { index, step ->
                if (step.description.isNullOrBlank()) {
                    _stepErrors[index] = "Step cannot be blank"
                    valid = false
                } else {
                    _stepErrors[index] = null
                }

            }
            if (_steps.isEmpty()) {
                valid = false
            }

            if (!valid) return@launch


            val recipe = Recipe(
                recipeId = Random.nextInt(),
                title = title,
                description = "Test description",
                imageUrl = null
            )
            recipeDao.insertAll(recipe)

            val steps = _steps.map { step ->
                step.copy(recipeId = recipe.recipeId)
            }


            stepDao.insertAll(*steps.toTypedArray())

            steps.forEachIndexed { index, step ->
                val matches = "@(\\w+)".toRegex().findAll(_steps[index].description ?: "")
                for (match in matches) {
                    val ingredientName = match.groupValues[1]
                    val existingIngredient = ingredientDao.getIngredientByName(ingredientName)
                    val ingredient = existingIngredient ?: Ingredient(
                        ingredientId = Random.nextInt(),
                        name = ingredientName,
                        imageUrl = null
                    )
                    if (existingIngredient == null) {
                        ingredientDao.insertAll(ingredient)
                        val inputData = workDataOf(
                            "ingredientId" to ingredient.ingredientId,
                            "ingredientName" to ingredient.name
                        )
                        val workRequest = OneTimeWorkRequestBuilder<IngredientWorker>()
                            .setInputData(inputData)
                            .build()
                        WorkManager.getInstance(context).enqueue(workRequest)


                    }

                    if (Pair(ingredient.ingredientId, step.stepId) !in insertedRefs) {
                        stepDao.insertCrossRef(
                            IngredientStepCrossRef(
                                ingredientId = ingredient.ingredientId,
                                stepId = step.stepId
                            )
                        )
                        insertedRefs.add(Pair(ingredient.ingredientId, step.stepId))
                    }

                }
            }
            onSuccess()
        }

    }

    fun addNewStep() {
        _steps.add(
            Step(
                stepId = Random.nextInt(),
                stepNumber = _steps.size,
                recipeId = -1,
                description = null
            )
        )
        _stepErrors.add(null)

    }

    fun removeStep(step: Step) {
        val index = _steps.indexOf(step)
        if (index != -1) {
            _steps.removeAt(index)
            _stepErrors.removeAt(index)
        }
    }

    fun updateStep(index: Int, newText: String) {
        _steps[index] = _steps[index].copy(description = newText)
        _stepErrors[index] = null

    }

    fun swapSteps(index: Int) {
        if (index in 1 until _steps.lastIndex + 1)
            Collections.swap(_steps, index, index - 1)
    }

    fun onTitleChange(newTitle: String) {
        title = newTitle
    }
}