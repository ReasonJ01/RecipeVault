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
class IngredientWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val ingredientDao: IngredientDao
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        Log.d("IngredientWorker", "Worker started")

        val ingredientId = inputData.getInt("ingredientId", -1)
        val ingredientName = inputData.getString("ingredientName")

        if (ingredientId == -1 || ingredientName.isNullOrEmpty()) {
            Log.d("IngredientWorker", "Invalid input: '$ingredientId' or '$ingredientName'")
            return Result.failure()
        }

        val imageB64 = generateImage(ingredientName, applicationContext) ?: return Result.retry()

        val savedPath =
            saveBase64ImageToStorage(applicationContext, imageB64, ingredientId.toString())
        Log.d("IngredientWorker", "Image saved to $savedPath")

        // Write to the shared database instance
        ingredientDao.updateImageUrl(ingredientId, savedPath)
        Log.d("IngredientWorker", "DB updated with image path")

        return Result.success()
    }

    private suspend fun generateImage(ingredientName: String, context: Context): String? {
        val key = PrefsManager.getApiKey(context) ?: return null
        Log.d("Worker", "Using key $key")

        val client = RetrofitClient.getClient(key)
        val prompt =
            "An image rendered in a vintage etching or engraving style, featuring fine cross-hatching and a hand-drawn, textured appearance reminiscent of 19th-century botanical or scientific illustrations. The image depicts only ${ingredientName.toTitleCase()}, shown in a single, appropriate form for culinary use..."

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
