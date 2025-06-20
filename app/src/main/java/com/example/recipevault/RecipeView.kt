package com.example.recipevault

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.example.recipevault.ui.theme.headlineMediumGaramond
import com.example.recipevault.ui.theme.headlineSmallGaramond
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject


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