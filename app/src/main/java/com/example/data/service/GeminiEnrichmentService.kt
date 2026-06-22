package com.example.data.service

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class ContentItem(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class ResponseFormatText(
    val mimeType: String
)

@JsonClass(generateAdapter = true)
data class ResponseFormat(
    val text: ResponseFormatText? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseFormat: ResponseFormat? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<ContentItem>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: ContentItem? = null
)

@JsonClass(generateAdapter = true)
data class GeminiPartResponse(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContentResponse(
    val parts: List<GeminiPartResponse>
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContentResponse
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null
)

// Target JSON structure from Gemini
@JsonClass(generateAdapter = true)
data class ProductSuggestion(
    @Json(name = "nombre_sugerido") val nombreSugerido: String,
    @Json(name = "material_sugerido") val materialSugerido: String,
    @Json(name = "presentacion_sugerida") val presentacionSugerida: String
)

class GeminiEnrichmentService {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeProduct(bitmap: Bitmap): ProductSuggestion? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e("GeminiService", "API Key is missing or default placeholder")
            return@withContext null
        }

        // Convert Bitmap to Base64 (JPEG)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

        val prompt = "Visualiza la imagen adjunta de este adorno, topper, empaque o producto artesanal físico de papelería. Propón una sugerencia en formato JSON para rellenar este formulario técnico de catálogo. El JSON debe contener exactamente tres propiedades string: 'nombre_sugerido' (nombre descriptivo), 'material_sugerido' (materiales detectados o recomendados) y 'presentacion_sugerida' (ej: Unidad, Set de 4, Paquete)."

        val requestData = GeminiRequest(
            contents = listOf(
                ContentItem(
                    parts = listOf(
                        Part(text = prompt),
                        Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
                    )
                )
            ),
            generationConfig = GenerationConfig(
                responseFormat = ResponseFormat(
                    text = ResponseFormatText(mimeType = "application/json")
                ),
                temperature = 0.2f
            ),
            systemInstruction = ContentItem(
                parts = listOf(
                    Part(text = "Eres un analista experto en manualidades, toppers, recuerditos personalizados, papelería fina y artesanía de diseño comercial. Produces JSON válido sin bloques markdown.")
                )
            )
        )

        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val adapter = moshi.adapter(GeminiRequest::class.java)
            val jsonRequest = adapter.toJson(requestData)

            val body = jsonRequest.toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(endpoint)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e("GeminiService", "API Request failed with code: ${response.code} message: ${response.message}")
                return@withContext null
            }

            val rawResponse = response.body?.string() ?: ""
            Log.d("GeminiService", "Raw API Response: $rawResponse")

            val responseAdapter = moshi.adapter(GeminiResponse::class.java)
            val parsedResponse = responseAdapter.fromJson(rawResponse)

            val rawText = parsedResponse?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (rawText != null) {
                Log.d("GeminiService", "Extracted JSON text: $rawText")
                // Parse proposal
                val suggestionAdapter = moshi.adapter(ProductSuggestion::class.java)
                return@withContext suggestionAdapter.fromJson(rawText)
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "Exception during analysis", e)
        }
        return@withContext null
    }
}
