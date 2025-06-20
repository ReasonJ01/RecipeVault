package com.example.recipevault

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.io.FileOutputStream
import kotlin.random.Random

@HiltWorker
class RecipeWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val stepDao: StepDao,
    private val recipeDao: RecipeDao
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        Log.d("RecipeWorker", "Worker started")

        val recipeId = inputData.getInt("ingredientId", -1)

        if (recipeId == -1) {
            Log.d("IngredientWorker", "Invalid input: '$recipeId''")
            return Result.failure()
        }
        val recipe = recipeDao.getRecipeById(recipeId) ?: return Result.failure()
        val title = recipe.title

        val steps = stepDao.getStepWithIngredientsByRecipeId(recipeId)
        val ingredients = steps.flatMap { it.ingredients }
        val ingredientNames = ingredients.map { it.name?.toTitleCase() ?: "" }
        val recipeString =
            "A $title made up of these ingredients:".plus(ingredientNames.joinToString(", "))


        val imageB64 = generateImage(recipeString, applicationContext) ?: return Result.retry()

        val savedPath =
            saveBase64ImageToStorage(applicationContext, imageB64, recipeId.toString())
        Log.d("IngredientWorker", "Image saved to $savedPath")

        // Write to the shared database instance
        recipeDao.updateImageUrl(recipeId, savedPath)
        Log.d("IngredientWorker", "DB updated with image path")

        return Result.success()
    }

    private suspend fun generateImage(description: String, context: Context): String? {
        val key = PrefsManager.getApiKey(context) ?: return null
        Log.d("Worker", "Using key $key")

        val client = RetrofitClient.getClient(key)
        val prompt =
            "An image rendered in a vintage etching or engraving style, featuring fine cross-hatching and a hand-drawn, textured appearance reminiscent of 19th-century botanical or scientific illustrations. The image depicts only the final prepared dish: $description. Do not include any individual ingredients, preparation steps, or alternate forms. The dish should be presented in a single, appropriate serving vessel or on a plate, with no additional objects or garnishes. The composition is centered, in FULL color. The background is plain white. The dish’s colors should be vibrant."
        return try {
            val response = client.generateImage(ImageGenerationRequest(prompt = prompt))
            response.data.firstOrNull()?.b64_json
        } catch (e: Exception) {
            Log.e("Worker", "API error:", e)
            null
        }
    }

    private fun saveBase64ImageToStorage(
        context: Context,
        base64: String,
        filename: String
    ): String {
        val decodedBytes = Base64.decode(base64, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

        val dir = context.getExternalFilesDir("ingredient_images") ?: context.filesDir
        val post = Random.nextInt()
        val file = File(dir, "${filename}_$post.png")

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        return file.absolutePath
    }
}
