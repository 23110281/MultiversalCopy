package com.multiversalcopy

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    val DEFAULT_OCR_PARAMS = mapOf(
        "temperature" to "0.0",
        "top_p" to "1.0",
        "repetition_penalty" to "1.0",
        "layout_threshold" to "0.2",
        "layout_nms" to "true",
        "max_new_tokens" to "2048",
        "use_doc_orientation_classify" to "false",
        "use_doc_unwarping" to "false",
        "use_layout_detection" to "true",
        "use_chart_recognition" to "false",
        "use_seal_recognition" to "false",
        "use_ocr_for_image_block" to "false",
        "format_block_content" to "true",
        "merge_layout_blocks" to "true"
    )

    suspend fun uploadScreenshot(baseUrl: String, imageBytes: ByteArray, params: Map<String, String> = DEFAULT_OCR_PARAMS): OcrResponse = withContext(Dispatchers.IO) {
        try {
            val cleanBaseUrl = baseUrl.trimEnd('/')
            val url = "$cleanBaseUrl/api/ocr"

            val requestBodyBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    "screenshot.jpg",
                    imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                )

            for ((key, value) in params) {
                requestBodyBuilder.addFormDataPart(key, value)
            }

            val request = Request.Builder()
                .url(url)
                .post(requestBodyBuilder.build())
                .build()

            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (response.isSuccessful) {
                try {
                    json.decodeFromString<OcrResponse>(bodyString)
                } catch (e: Exception) {
                    OcrResponse(success = false, error = "Failed to parse JSON: ${e.message}")
                }
            } else {
                OcrResponse(success = false, error = "HTTP ${response.code}: $bodyString")
            }
        } catch (e: Exception) {
            OcrResponse(success = false, error = "Network Error: ${e.message}")
        }
    }
}
