package com.example.recipevault

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.recipevault.ui.theme.headlineSmallGaramond
import org.apache.commons.text.similarity.JaroWinklerDistance

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