package com.example.recipevault

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import java.util.concurrent.TimeUnit


data class ImageGenerationRequest(
    val model: String = "gpt-image-1",
    val prompt: String,
    val n: Int = 1,
    val size: String = "1024x1024",
    val quality: String = "low",
)

data class ImageGenerationResponse(
    val data: List<ImageData>
)

data class ImageData(
    val b64_json: String
)

interface OpenAIApi {
    @Headers("Content-Type: application/json")
    @POST("v1/images/generations")
    suspend fun generateImage(
        @Body request: ImageGenerationRequest,
    ): ImageGenerationResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://api.openai.com/"


    fun getClient(apiKey: String): OpenAIApi {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }


        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $apiKey")
                    .build()
                chain.proceed(request)
            }.addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS) // Timeout for establishing a connection
            .readTimeout(30, TimeUnit.SECONDS)    // Timeout for reading data from the server
            .writeTimeout(30, TimeUnit.SECONDS)   // Timeout for writing data to the server
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenAIApi::class.java)
    }
}
