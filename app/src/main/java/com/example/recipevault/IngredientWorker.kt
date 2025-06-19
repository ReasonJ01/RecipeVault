package com.example.recipevault

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.File
import java.io.FileOutputStream

val sampleBase64 = """
    iVBORw0KGgoAAAANSUhEUgAAAIAAAACACAIAAABMXPacAAACtElEQVR4nOzd32vNfwDH8e/5OmlczI8rapm0CKntbouUm0VbyYWSpJVwo41SRItCW3Gj0cmwyQUXVrMr1kSbJqXWKVeaFmVOrEymofz6G16lnjfPx/XrffX8vG/OxXkXZ288+S/xYt2raD9dey/at54+Fu3bzjZH+8rWyWjf82A+2m9Z9Cna/x+t9c8ZAGYAmAFgBoAZAGYAmAFgBoAZAGYAmAFgBoAZAGYAWOHd+NLowLKpmmhf3XEq2h8//D3aX/tQiPajA5uj/a6Glmjf//NttPcGwAwAMwDMADADwAwAMwDMADADwAwAMwDMADADwAwAMwCs0LP+QHSg5U24v5D9nn6yfjba3+1fHe3nnx2J9l87F0b790eHo703AGYAmAFgBoAZAGYAmAFgBoAZAGYAmAFgBoAZAGYAmAFgxUrnpejAlX17o3255nG0n6o5Ee2frl0T7Rc/yv5f6FfD5Wjf+7sv2nsDYAaAGQBmAJgBYAaAGQBmAJgBYAaAGQBmAJgBYAaAGQBWHFneFR143f4j2nc13or22+fGo/3DDa3R/uCmPdG+3FEV7f+UP0d7bwDMADADwAwAMwDMADADwAwAMwDMADADwAwAMwDMADADwIrbJuqiAx1N36L9nYnsPeEvvTej/f322mi/e2ZjtG+6+Dzar+g7E+29ATADwAwAMwDMADADwAwAMwDMADADwAwAMwDMADADwAwAK5SqBqIDO5qz93tX1S2I9iPVQ9F+ZakS7aeXXI32Y2PZN3r+9qFo7w2AGQBmAJgBYAaAGQBmAJgBYAaAGQBmAJgBYAaAGQBmAFjh4+i56MDQ9ew9gP313dG+fbAx2s91T0b70uDOaD/zMnsvoW04+6a9ATADwAwAMwDMADADwAwAMwDMADADwAwAMwDMADADwAwA+xsAAP//9ItklNvhF8gAAAAASUVORK5CYII=
""".trimIndent()


class IngredientWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d("IngredientWorker", "Worker started")

        val ingredientId = inputData.getInt("ingredientId", -1)
        val ingredientName = inputData.getString("ingredientName")

        if (ingredientId == -1 || ingredientName.isNullOrEmpty()) {
            Log.d("IngredientWorker", "ig '$ingredientId' or '$ingredientName'")
            return Result.failure()
        }

        val imageB64 = generateImage(ingredientName, applicationContext) ?: return Result.retry()
        //val imageB64 = sampleBase64

        val savedPath =
            saveBase64ImageToStorage(applicationContext, imageB64, ingredientId.toString())
        Log.d("IngredientWorker", "Saved to $savedPath")


        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "app_db"
        ).build()
        db.ingredientDao().updateImageUrl(ingredientId, savedPath)
        Log.d("IngredientWorker", "Assocaited in db")

        return Result.success()
    }

    private suspend fun generateImage(ingredientName: String, context: Context): String? {
        val key = PrefsManager.getApiKey(context) ?: return null
        Log.e("Worker", "key $key")


        val client = RetrofitClient.getClient(key)
        val prompt =
            "An image rendered in a vintage etching or engraving style, featuring fine cross-hatching and a hand-drawn, textured appearance reminiscent of 19th-century botanical or scientific illustrations. The image depicts only ${ingredientName.toTitleCase()}, presented appropriately for culinary use (e.g., whole, chopped, ground, or in a suitable container such as a jar, bowl, or glass if needed). No additional objects are included. The composition is centered, minimal, in color, and text-free. The background is plain and free of cross-hatching. The ingredient’s color should be vibrant, not sepia or monochrome."
        val response = try {
            client.generateImage(ImageGenerationRequest(prompt = prompt))
        } catch (e: Exception) {
            Log.e("Worker", "API error:", e)
            return null
        }

        Log.d("Worker", "Response: $response")

        return response.data.firstOrNull()?.b64_json
    }

    private fun saveBase64ImageToStorage(
        context: Context,
        base64: String,
        filename: String
    ): String {
        val decodedBytes = Base64.decode(base64, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

        val dir = context.getExternalFilesDir("ingredient_images") ?: context.filesDir
        val file = File(dir, "$filename.png")

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        return file.absolutePath
    }
}
