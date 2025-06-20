package com.example.recipevault


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