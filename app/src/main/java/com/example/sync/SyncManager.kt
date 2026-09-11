package com.example.sync

import android.content.Context
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.TransactionEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object SyncManager {
    private const val TAG = "SyncManager"
    private const val PREFS_NAME = "sync_settings"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // --- SharedPreferences Helpers ---
    fun getSheetsUrl(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("sheets_url", "") ?: ""
    }

    fun setSheetsUrl(context: Context, url: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("sheets_url", url.trim())
            .apply()
    }

    fun isSheetsSyncEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean("sheets_sync", false)
    }

    fun setSheetsSyncEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("sheets_sync", enabled)
            .apply()
    }

    // --- Google Sheets Sync Operations (via Google Apps Script Web App API) ---
    suspend fun uploadToSheets(context: Context, transaction: TransactionEntity): Boolean = withContext(Dispatchers.IO) {
        if (!isSheetsSyncEnabled(context)) return@withContext false
        val url = getSheetsUrl(context)
        if (url.isEmpty()) return@withContext false

        val payload = mapOf(
            "tanggal" to transaction.tanggal,
            "tipe" to transaction.tipe,
            "milik" to transaction.milik,
            "jumlah" to transaction.jumlah,
            "keterangan" to transaction.keterangan
        )

        val requestMap = mapOf(
            "action" to "addTransaction",
            "payload" to payload
        )

        try {
            val jsonAdapter = moshi.adapter(Map::class.java)
            val jsonString = jsonAdapter.toJson(requestMap)
            
            val requestBody = jsonString.toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(url).post(requestBody).build()
            
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                Log.d(TAG, "Sheets upload response: $body")
                return@withContext response.isSuccessful && body?.contains("success\":true") == true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sheets upload failed", e)
            return@withContext false
        }
    }

    suspend fun deleteFromSheets(context: Context, id: String): Boolean = withContext(Dispatchers.IO) {
        if (!isSheetsSyncEnabled(context)) return@withContext false
        val url = getSheetsUrl(context)
        if (url.isEmpty()) return@withContext false

        val requestMap = mapOf(
            "action" to "deleteTransaction",
            "payload" to mapOf("id" to id)
        )

        try {
            val jsonAdapter = moshi.adapter(Map::class.java)
            val jsonString = jsonAdapter.toJson(requestMap)
            
            val requestBody = jsonString.toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(url).post(requestBody).build()
            
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                Log.d(TAG, "Sheets delete response: $body")
                return@withContext response.isSuccessful && body?.contains("success\":true") == true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sheets delete failed", e)
            return@withContext false
        }
    }

    // --- Bulk Sync Engine (Local Database <-> Remotes) ---
    suspend fun syncNow(context: Context): SyncResult = withContext(Dispatchers.IO) {
        var firestoreSynced = 0
        var sheetsSynced = 0
        val errorMessages = mutableListOf<String>()

        val db = AppDatabase.getDatabase(context)
        val dao = db.transactionDao()
        
        // Fetch all local transactions
        val localList = mutableListOf<TransactionEntity>()
        try {
            localList.addAll(dao.getAllTransactionsList())
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching local transactions", e)
        }

        // 1. Google Sheets Sync (Get full sheet data and merge)
        if (isSheetsSyncEnabled(context)) {
            val url = getSheetsUrl(context)
            if (url.isNotEmpty()) {
                try {
                    val requestMap = mapOf("action" to "getTransactions")
                    val jsonAdapter = moshi.adapter(Map::class.java)
                    val jsonString = jsonAdapter.toJson(requestMap)
                    
                    val requestBody = jsonString.toRequestBody("application/json; charset=utf-8".toMediaType())
                    val request = Request.Builder().url(url).post(requestBody).build()
                    
                    client.newCall(request).execute().use { response ->
                        val body = response.body?.string()
                        if (response.isSuccessful && body != null) {
                            // Parse list
                            val listAdapter = moshi.adapter(Map::class.java)
                            val responseMap = listAdapter.fromJson(body)
                            
                            val rawData = responseMap?.get("data") as? List<*>
                            rawData?.forEach { rowRaw ->
                                val row = rowRaw as? Map<*, *>
                                if (row != null) {
                                    val id = row["ID"] as? String ?: ""
                                    if (id.isNotEmpty()) {
                                        val tanggal = row["Tanggal"] as? String ?: ""
                                        val tipe = row["Tipe"] as? String ?: "Masuk"
                                        val milik = row["Milik"] as? String ?: "Pak Hamzah"
                                        val keterangan = row["Keterangan"] as? String ?: ""
                                        val jumlahRaw = row["Jumlah"]
                                        val jumlah = when (jumlahRaw) {
                                            is Number -> jumlahRaw.toDouble()
                                            is String -> jumlahRaw.toDoubleOrNull() ?: 0.0
                                            else -> 0.0
                                        }
                                        
                                        // Parse Timestamp String or Long
                                        val timestampStr = row["Timestamp"] as? String
                                        val timestamp = if (timestampStr != null) {
                                            try {
                                                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault()).parse(timestampStr)?.time ?: System.currentTimeMillis()
                                            } catch (e: Exception) {
                                                System.currentTimeMillis()
                                            }
                                        } else {
                                            System.currentTimeMillis()
                                        }

                                        val entity = TransactionEntity(id, tanggal, tipe, milik, keterangan, jumlah, timestamp)
                                        dao.insertTransaction(entity)
                                        sheetsSynced++
                                    }
                                }
                            }
                        } else {
                            errorMessages.add("Google Sheets: Gagal mengambil data (Response code ${response.code})")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Google Sheets sync failed", e)
                    errorMessages.add("Google Sheets: ${e.localizedMessage}")
                }
            } else {
                errorMessages.add("Google Sheets: URL Web App kosong.")
            }
        }

        return@withContext SyncResult(
            success = errorMessages.isEmpty(),
            firestoreSyncedCount = firestoreSynced,
            sheetsSyncedCount = sheetsSynced,
            error = if (errorMessages.isEmpty()) null else errorMessages.joinToString(", ")
        )
    }
}

data class SyncResult(
    val success: Boolean,
    val firestoreSyncedCount: Int,
    val sheetsSyncedCount: Int,
    val error: String?
)
