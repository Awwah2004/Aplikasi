package com.example.api

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object GeminiApi {
    private const val TAG = "GeminiApi"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    suspend fun analyzeFinancials(transactionsData: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Error: API Key is missing. Please configure GEMINI_API_KEY in the Secrets panel in AI Studio."
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        
        val systemPrompt = "Anda adalah Asisten Keuangan cerdas dan analitis untuk sebuah kantor advokat. Analisis riwayat transaksi berikut. Berikan ringkasan singkat dalam 2-3 paragraf. Fokus pada:\\n1. Kesimpulan total pemasukan vs pengeluaran.\\n2. Soroti pengeluaran/pemasukan terbesar.\\n3. Berikan saran singkat untuk menjaga atau meningkatkan stabilitas kas.\\n\\nAturan Format: Gunakan HTML tag dasar (seperti <p>, <b>, <ul>, <li>, <br>) agar rapi saat dirender di web. JANGAN GUNAKAN markdown (seperti **tebal** atau ```html). Kembalikan response murni dalam bentuk format HTML."
        
        // Escape special characters in data for valid JSON
        val escapedData = transactionsData
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")

        val jsonRequest = """
            {
              "contents": [
                {
                  "parts": [
                    {
                      "text": "Berikut adalah data riwayat transaksi terakhir:\n\n$escapedData"
                    }
                  ]
                }
              ],
              "systemInstruction": {
                "parts": [
                  {
                    "text": "$systemPrompt"
                  }
                ]
              }
            }
        """.trimIndent()

        Log.d(TAG, "Request payload constructed")

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonRequest.toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (!response.isSuccessful) {
                    Log.e(TAG, "Error response: ${response.code} - $body")
                    return@withContext "Error: Server returned code ${response.code}\n$body"
                }
                
                if (body == null) {
                    return@withContext "Error: Empty response body from Gemini API"
                }

                try {
                    val adapter = moshi.adapter(Map::class.java)
                    val map = adapter.fromJson(body)
                    val candidates = map?.get("candidates") as? List<*>
                    val candidate = candidates?.firstOrNull() as? Map<*, *>
                    val content = candidate?.get("content") as? Map<*, *>
                    val parts = content?.get("parts") as? List<*>
                    val part = parts?.firstOrNull() as? Map<*, *>
                    var text = part?.get("text") as? String
                    
                    if (text != null) {
                        text = text.replace("```html", "", ignoreCase = true)
                            .replace("```", "")
                            .trim()
                        return@withContext text
                    }
                    return@withContext "Error: No analysis text found in model response."
                } catch (e: Exception) {
                    Log.e(TAG, "Parsing failed", e)
                    val regex = """"text"\s*:\s*"([^"]*)"""".toRegex()
                    val match = regex.find(body)
                    if (match != null) {
                        return@withContext match.groupValues[1]
                            .replace("\\n", "\n")
                            .replace("\\\"", "\"")
                    }
                    return@withContext "Error parsing Gemini response: ${e.localizedMessage}"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Connection failed", e)
            return@withContext "Error connecting to Gemini API: ${e.localizedMessage}"
        }
    }
}
