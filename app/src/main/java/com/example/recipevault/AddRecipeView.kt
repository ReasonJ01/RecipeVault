package com.example.recipevault

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.recipevault.ui.theme.headlineMediumGaramond
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Collections
import javax.inject.Inject
import kotlin.random.Random


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