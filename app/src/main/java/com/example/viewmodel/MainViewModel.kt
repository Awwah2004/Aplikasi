package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.TransactionEntity
import com.example.data.TransactionRepository
import com.example.api.GeminiApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TransactionRepository

    // --- Sync States ---
    var isSyncing by mutableStateOf(false)
        private set
    var syncResult by mutableStateOf<String?>(null)

    // Sync configuration states
    var sheetsUrlInput by mutableStateOf("")
    var sheetsSyncEnabled by mutableStateOf(false)
    var showSyncSettingsDialog by mutableStateOf(false)

    // --- Authentication State ---
    var isLoggedIn by mutableStateOf(false)
        private set
    var usernameInput by mutableStateOf("")
    var passwordInput by mutableStateOf("")
    var isProcessingLogin by mutableStateOf(false)
    var loginError by mutableStateOf<String?>(null)

    // --- Google Auth State ---
    var currentUserProfile by mutableStateOf<com.example.auth.UserProfile?>(null)
    var googleWebClientIdInput by mutableStateOf("")
    var showGoogleConfigDialog by mutableStateOf(false)

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TransactionRepository(database.transactionDao())

        // Load initial sync configurations from SyncManager SharedPreferences
        sheetsUrlInput = com.example.sync.SyncManager.getSheetsUrl(application)
        sheetsSyncEnabled = com.example.sync.SyncManager.isSheetsSyncEnabled(application)

        // Restore Google user session if previously logged in
        val savedUser = com.example.auth.GoogleAuthManager.getSavedUser(application)
        if (savedUser != null) {
            currentUserProfile = savedUser
            isLoggedIn = true
        }
        googleWebClientIdInput = com.example.auth.GoogleAuthManager.getWebClientId(application)
    }

    fun saveSyncSettings() {
        val app = getApplication<Application>()
        com.example.sync.SyncManager.setSheetsUrl(app, sheetsUrlInput)
        com.example.sync.SyncManager.setSheetsSyncEnabled(app, sheetsSyncEnabled)
        showToast("Pengaturan sinkronisasi berhasil disimpan")
    }

    fun startSync() {
        isSyncing = true
        syncResult = null
        viewModelScope.launch {
            val app = getApplication<Application>()
            // Ensure settings are saved first
            com.example.sync.SyncManager.setSheetsUrl(app, sheetsUrlInput)
            com.example.sync.SyncManager.setSheetsSyncEnabled(app, sheetsSyncEnabled)

            val res = com.example.sync.SyncManager.syncNow(app)
            isSyncing = false
            if (res.success) {
                showToast("Sinkronisasi Berhasil! Sheets: ${res.sheetsSyncedCount}")
                syncResult = "Berhasil disinkronisasi"
            } else {
                showToast("Gagal menyelaraskan: ${res.error}", isError = true)
                syncResult = "Gagal: ${res.error}"
            }
        }
    }

    // --- Transactions List Flow ---
    val transactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun login() {
        if (usernameInput.isBlank() || passwordInput.isBlank()) {
            loginError = "Username dan password tidak boleh kosong!"
            return
        }

        isProcessingLogin = true
        loginError = null

        viewModelScope.launch {
            kotlinx.coroutines.delay(600) // Simulate slight network processing
            if (usernameInput == "admin" && passwordInput == "123") {
                isLoggedIn = true
                loginError = null
                showToast("Login berhasil")
            } else {
                loginError = "Username atau password salah!"
                showToast("Login gagal: Username/password salah!", isError = true)
            }
            isProcessingLogin = false
        }
    }

    fun signInWithGoogle(context: Context) {
        val clientId = googleWebClientIdInput.ifBlank {
            com.example.auth.GoogleAuthManager.getWebClientId(context)
        }

        if (clientId.isBlank() || clientId == "YOUR_GOOGLE_WEB_CLIENT_ID") {
            showGoogleConfigDialog = true
            return
        }

        isProcessingLogin = true
        loginError = null

        viewModelScope.launch {
            val result = com.example.auth.GoogleAuthManager.signInWithGoogle(context, clientId)
            isProcessingLogin = false
            if (result.isSuccess) {
                currentUserProfile = result.getOrNull()
                isLoggedIn = true
                loginError = null
                showToast("Selamat datang, ${currentUserProfile?.displayName ?: "Pengguna Google"}!")
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Gagal login dengan Google"
                loginError = errorMsg
                showToast(errorMsg, isError = true)
            }
        }
    }

    fun saveGoogleWebClientId() {
        val app = getApplication<Application>()
        com.example.auth.GoogleAuthManager.saveWebClientId(app, googleWebClientIdInput)
        showToast("Web Client ID berhasil disimpan.")
    }

    fun logout() {
        isLoggedIn = false
        usernameInput = ""
        passwordInput = ""
        loginError = null
        currentUserProfile = null
        com.example.auth.GoogleAuthManager.clearUser(getApplication())
        showToast("Anda telah keluar.")
    }

    // --- Transaction Form Inputs ---
    var tanggalInput by mutableStateOf(currentDateString())
    var tipeInput by mutableStateOf("Masuk") // "Masuk" or "Keluar"
    var milikInput by mutableStateOf("Pak Hamzah") // "Pak Hamzah", "Klien", "Abyan"
    var jumlahInput by mutableStateOf("")
    var keteranganInput by mutableStateOf("")

    // --- Dialogs & Modal States ---
    var showDeleteConfirmationId by mutableStateOf<String?>(null)
    var showAiAnalysisModal by mutableStateOf(false)
    var aiAnalysisResult by mutableStateOf<String?>(null)
    var aiAnalysisLoading by mutableStateOf(false)
    var aiAnalysisError by mutableStateOf<String?>(null)

    // --- Toast Flow ---
    var toastMessage by mutableStateOf<String?>(null)
    var toastIsError by mutableStateOf(false)

    fun showToast(message: String, isError: Boolean = false) {
        toastMessage = message
        toastIsError = isError
    }

    fun clearToast() {
        toastMessage = null
    }

    // --- Database Operations ---
    fun saveTransaction() {
        val amount = jumlahInput.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            showToast("Jumlah transaksi harus berupa angka positif!", isError = true)
            return
        }
        if (keteranganInput.isBlank()) {
            showToast("Keterangan transaksi tidak boleh kosong!", isError = true)
            return
        }

        viewModelScope.launch {
            val newTx = TransactionEntity(
                id = UUID.randomUUID().toString(),
                tanggal = tanggalInput,
                tipe = tipeInput,
                milik = milikInput,
                keterangan = keteranganInput.trim(),
                jumlah = amount,
                timestamp = System.currentTimeMillis()
            )
            repository.insert(newTx)
            showToast("Transaksi berhasil ditambahkan.")
            resetForm()

            // Trigger background sync
            val app = getApplication<Application>()
            if (com.example.sync.SyncManager.isSheetsSyncEnabled(app)) {
                launch { com.example.sync.SyncManager.uploadToSheets(app, newTx) }
            }
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            repository.delete(id)
            showToast("Transaksi berhasil dihapus.")
            showDeleteConfirmationId = null

            // Trigger background delete
            val app = getApplication<Application>()
            if (com.example.sync.SyncManager.isSheetsSyncEnabled(app)) {
                launch { com.example.sync.SyncManager.deleteFromSheets(app, id) }
            }
        }
    }

    private fun resetForm() {
        tanggalInput = currentDateString()
        tipeInput = "Masuk"
        milikInput = "Pak Hamzah"
        jumlahInput = ""
        keteranganInput = ""
    }

    // --- AI Analysis via Gemini API ---
    fun runAiAnalysis() {
        val currentTxList = transactions.value
        if (currentTxList.isEmpty()) {
            showToast("Belum ada data transaksi untuk dianalisis.", isError = true)
            return
        }

        showAiAnalysisModal = true
        aiAnalysisLoading = true
        aiAnalysisResult = null
        aiAnalysisError = null

        viewModelScope.launch {
            // Build the string representation of transactions
            val cleanData = currentTxList.joinToString(separator = "\n") { tx ->
                "Tgl: ${formatDateDisplay(tx.tanggal)} | Tipe: ${tx.tipe} | Milik: ${tx.milik} | Jumlah: Rp${formatNumber(tx.jumlah)} | Ket: ${tx.keterangan}"
            }

            val result = GeminiApi.analyzeFinancials(cleanData)
            
            if (result.startsWith("Error")) {
                aiAnalysisError = result
            } else {
                aiAnalysisResult = result
            }
            aiAnalysisLoading = false
        }
    }

    // --- Helper Utilities ---
    private fun currentDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun formatRupiah(amount: Double): String {
        val numberFormat = java.text.NumberFormat.getNumberInstance(Locale("id", "ID"))
        return "Rp " + numberFormat.format(amount.toLong())
    }

    fun formatNumber(amount: Double): String {
        val numberFormat = java.text.NumberFormat.getNumberInstance(Locale("id", "ID"))
        return numberFormat.format(amount.toLong())
    }

    fun formatDateDisplay(dateStr: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val formatter = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
            val date = parser.parse(dateStr)
            if (date != null) formatter.format(date) else dateStr
        } catch (e: Exception) {
            dateStr
        }
    }

    fun formatTimeDisplay(timestamp: Long): String {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }
}
