package com.example.viewmodel

import android.app.Application
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

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TransactionRepository(database.transactionDao())
    }

    // --- Transactions List Flow ---
    val transactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Authentication State ---
    var isLoggedIn by mutableStateOf(false)
        private set

    var usernameInput by mutableStateOf("")
    var passwordInput by mutableStateOf("")
    var isProcessingLogin by mutableStateOf(false)
    var loginError by mutableStateOf<String?>(null)

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

    fun logout() {
        isLoggedIn = false
        usernameInput = ""
        passwordInput = ""
        loginError = null
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
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            repository.delete(id)
            showToast("Transaksi berhasil dihapus.")
            showDeleteConfirmationId = null
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
