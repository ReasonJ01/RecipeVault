package com.example.recipevault

import android.content.Context
import android.util.Log
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Collections
import javax.inject.Inject
import kotlin.random.Random

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