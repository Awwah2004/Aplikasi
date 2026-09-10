package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val tanggal: String, // YYYY-MM-DD
    val tipe: String,    // "Masuk" or "Keluar"
    val milik: String,   // "Pak Hamzah", "Klien", "Abyan"
    val keterangan: String,
    val jumlah: Double,
    val timestamp: Long
)
