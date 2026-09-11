package com.example

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Html
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.text.HtmlCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.TransactionEntity
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MainViewModel
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val viewModel: MainViewModel = viewModel()
    
    // Smooth transition
    AnimatedVisibility(
        visible = !viewModel.isLoggedIn,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        LoginScreen(viewModel)
    }

    AnimatedVisibility(
        visible = viewModel.isLoggedIn,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        DashboardScreen(viewModel)
    }

    // Toast notifications
    viewModel.toastMessage?.let { message ->
        LaunchedEffect(message) {
            // Dismiss after 3 seconds
            kotlinx.coroutines.delay(3000)
            viewModel.clearToast()
        }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp, start = 16.dp, end = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (viewModel.toastIsError) Color(0xFFFDE8E8) else Color(0xFFE1F5FE)
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .widthIn(max = 450.dp)
                    .border(
                        1.dp,
                        if (viewModel.toastIsError) Color(0xFFF05252) else Color(0xFF0288D1),
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (viewModel.toastIsError) Icons.Default.Error else Icons.Default.CheckCircle,
                        contentDescription = "Status",
                        tint = if (viewModel.toastIsError) Color(0xFFE53935) else Color(0xFF039BE5),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = message,
                        color = Color(0xFF2C3E50),
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun LoginScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 420.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Logo representation
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .border(3.dp, Color(0xFFD4AF37), CircleShape) // Gold border
                        .background(Color(0xFF0F172A))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_app_icon),
                        contentDescription = "Logo HAM Law & Justice",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "HAM LAW AND JUSTICE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "ADVOKAT DAN KANTOR HUKUM",
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = Color(0xFFD4AF37), // Gold accent
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Sistem Kas Administrator",
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(top = 4.dp),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Username input
                OutlinedTextField(
                    value = viewModel.usernameInput,
                    onValueChange = { viewModel.usernameInput = it },
                    label = { Text("Username") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF64748B)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2C5364),
                        focusedLabelColor = Color(0xFF2C5364)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("username_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password input
                OutlinedTextField(
                    value = viewModel.passwordInput,
                    onValueChange = { viewModel.passwordInput = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF64748B)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2C5364),
                        focusedLabelColor = Color(0xFF2C5364)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("password_input")
                )

                if (viewModel.loginError != null) {
                    Text(
                        text = viewModel.loginError ?: "",
                        color = Color(0xFFE53935),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        textAlign = TextAlign.Start
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { viewModel.login() },
                    enabled = !viewModel.isProcessingLogin,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("login_button")
                ) {
                    if (viewModel.isProcessingLogin) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("LOGIN", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Login, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 720
    val transactionsList by viewModel.transactions.collectAsState()

    // Financial calculations
    val totalMasuk = transactionsList.filter { it.tipe == "Masuk" }.sumOf { it.jumlah }
    val totalKeluar = transactionsList.filter { it.tipe == "Keluar" }.sumOf { it.jumlah }
    val totalSaldo = totalMasuk - totalKeluar

    // Balances per owner
    val milikBalances = remember(transactionsList) {
        val map = mutableMapOf("Pak Hamzah" to 0.0, "Klien" to 0.0, "Abyan" to 0.0)
        transactionsList.forEach { tx ->
            val factor = if (tx.tipe == "Masuk") 1.0 else -1.0
            val current = map[tx.milik] ?: 0.0
            map[tx.milik] = current + (tx.jumlah * factor)
        }
        map
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .background(Color(0xFF0F172A))
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, Color(0xFFD4AF37), CircleShape)
                                .background(Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            val userPhoto = viewModel.currentUserProfile?.photoUrl
                            if (!userPhoto.isNullOrBlank()) {
                                AsyncImage(
                                    model = userPhoto,
                                    contentDescription = "Avatar Pengguna",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.img_app_icon),
                                    contentDescription = "Logo HAM Law & Justice",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "HAM LAW AND JUSTICE",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                            val profile = viewModel.currentUserProfile
                            if (profile != null) {
                                Text(
                                    text = "${profile.displayName} (${profile.email})",
                                    fontSize = 11.sp,
                                    color = Color(0xFFD4AF37),
                                    maxLines = 1
                                )
                            } else {
                                Text(
                                    text = "Sistem Pencatatan Keuangan",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.logout() },
                            modifier = Modifier
                                .testTag("logout_button")
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E293B))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Keluar",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF1F5F9)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cards Summary Section (Grid / Stack)
            item {
                if (isTablet) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SummaryCard(
                            title = "Total Uang Masuk",
                            value = viewModel.formatRupiah(totalMasuk),
                            icon = Icons.Default.TrendingUp,
                            accentColor = Color(0xFF10B981),
                            bgColor = Color(0xFFECFDF5),
                            modifier = Modifier.weight(1f)
                        )
                        SummaryCard(
                            title = "Total Uang Keluar",
                            value = viewModel.formatRupiah(totalKeluar),
                            icon = Icons.Default.TrendingDown,
                            accentColor = Color(0xFFEF4444),
                            bgColor = Color(0xFFFEF2F2),
                            modifier = Modifier.weight(1f)
                        )
                        SummaryCard(
                            title = "Total Saldo Keseluruhan",
                            value = viewModel.formatRupiah(totalSaldo),
                            icon = Icons.Default.AccountBalanceWallet,
                            accentColor = Color(0xFF3B82F6),
                            bgColor = Color(0xFFEFF6FF),
                            modifier = Modifier.weight(1.2f)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SummaryCard(
                            title = "Total Uang Masuk",
                            value = viewModel.formatRupiah(totalMasuk),
                            icon = Icons.Default.TrendingUp,
                            accentColor = Color(0xFF10B981),
                            bgColor = Color(0xFFECFDF5),
                            modifier = Modifier.fillMaxWidth()
                        )
                        SummaryCard(
                            title = "Total Uang Keluar",
                            value = viewModel.formatRupiah(totalKeluar),
                            icon = Icons.Default.TrendingDown,
                            accentColor = Color(0xFFEF4444),
                            bgColor = Color(0xFFFEF2F2),
                            modifier = Modifier.fillMaxWidth()
                        )
                        SummaryCard(
                            title = "Total Saldo Keseluruhan",
                            value = viewModel.formatRupiah(totalSaldo),
                            icon = Icons.Default.AccountBalanceWallet,
                            accentColor = Color(0xFF3B82F6),
                            bgColor = Color(0xFFEFF6FF),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Atas Nama Individual Breakdown Section
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Rincian Saldo",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF1E293B)
                            )
                        }

                        // AI Analysis Button
                        Button(
                            onClick = { viewModel.runAiAnalysis() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)), // Royal purple
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .testTag("ai_analysis_button")
                                .height(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Analisis AI", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MilikBalanceCard(
                            name = "Pak Hamzah",
                            balance = viewModel.formatRupiah(milikBalances["Pak Hamzah"] ?: 0.0),
                            icon = Icons.Default.Work,
                            accentColor = Color(0xFF6366F1), // Indigo
                            modifier = Modifier.weight(1f)
                        )
                        MilikBalanceCard(
                            name = "Klien",
                            balance = viewModel.formatRupiah(milikBalances["Klien"] ?: 0.0),
                            icon = Icons.Default.Groups,
                            accentColor = Color(0xFFF59E0B), // Amber
                            modifier = Modifier.weight(1f)
                        )
                        MilikBalanceCard(
                            name = "Abyan",
                            balance = viewModel.formatRupiah(milikBalances["Abyan"] ?: 0.0),
                            icon = Icons.Default.Person,
                            accentColor = Color(0xFF14B8A6), // Teal
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Main Content Area (Form on Left / Top, History List on Right / Bottom)
            item {
                if (isTablet) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            TransactionFormCard(viewModel)
                        }
                        Box(modifier = Modifier.weight(1.8f)) {
                            TransactionHistoryCard(viewModel, transactionsList)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TransactionFormCard(viewModel)
                        TransactionHistoryCard(viewModel, transactionsList)
                    }
                }
            }
        }
    }

    // --- Dialogs ---

    // 1. Delete confirmation Dialog
    viewModel.showDeleteConfirmationId?.let { deleteId ->
        AlertDialog(
            onDismissRequest = { viewModel.showDeleteConfirmationId = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(40.dp)) },
            title = { Text("Hapus Transaksi?", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) },
            text = {
                Text(
                    "Apakah Anda yakin ingin menghapus data ini? Saldo akan langsung terhitung ulang, dan tindakan ini tidak dapat dibatalkan.",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF64748B)
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteTransaction(deleteId) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Ya, Hapus", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.showDeleteConfirmationId = null },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Batal")
                }
            },
            properties = DialogProperties(usePlatformDefaultWidth = true)
        )
    }

    // 2. AI Analysis Dialog
    if (viewModel.showAiAnalysisModal) {
        Dialog(
            onDismissRequest = { viewModel.showAiAnalysisModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.8f)
                    .padding(vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF7C3AED),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Analisis Keuangan AI",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF1E293B)
                            )
                        }
                        IconButton(onClick = { viewModel.showAiAnalysisModal = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color(0xFF64748B))
                        }
                    }
                    
                    Divider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (viewModel.aiAnalysisLoading) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF7C3AED),
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Gemini AI sedang membaca dan\nmenganalisis riwayat transaksi Anda...",
                                    textAlign = TextAlign.Center,
                                    color = Color(0xFF64748B),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        } else if (viewModel.aiAnalysisError != null) {
                            Text(
                                text = viewModel.aiAnalysisError ?: "Terjadi kesalahan",
                                color = Color(0xFFEF4444),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        } else {
                            // HTML formatted rendering inside Android TextView for true formatting
                            val resultHtml = viewModel.aiAnalysisResult ?: "Tidak ada data."
                            AndroidView(
                                factory = { context ->
                                    TextView(context).apply {
                                        textSize = 15f
                                        setTextColor(android.graphics.Color.parseColor("#334155")) // slate-700
                                        setPadding(8, 8, 8, 8)
                                    }
                                },
                                update = { textView ->
                                    textView.text = HtmlCompat.fromHtml(
                                        resultHtml,
                                        HtmlCompat.FROM_HTML_MODE_LEGACY
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll()
                            )
                        }
                    }
                }
            }
        }
    }

}

// Custom simple vertical scroll modifier for AndroidView wrapper
fun Modifier.verticalScroll(): Modifier = this // Wrapped native view handles scrolling internally or via android Layout parameters, but in our case, TextView scroll is fine.

@Composable
fun SummaryCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    fontSize = 20.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun MilikBalanceCard(
    name: String,
    balance: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = balance,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
        }
    }
}

@Composable
fun TransactionFormCard(viewModel: MainViewModel) {
    val context = LocalContext.current
    var isMilikDropdownExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = null,
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Catat Transaksi Baru",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF1E293B)
                )
            }
            Divider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 12.dp))

            // Date picker
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            
            val datePickerDialog = remember {
                DatePickerDialog(
                    context,
                    { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                        val formattedMonth = String.format("%02d", selectedMonth + 1)
                        val formattedDay = String.format("%02d", selectedDayOfMonth)
                        viewModel.tanggalInput = "$selectedYear-$formattedMonth-$formattedDay"
                    },
                    year, month, day
                )
            }

            OutlinedTextField(
                value = viewModel.formatDateDisplay(viewModel.tanggalInput),
                onValueChange = {},
                readOnly = true,
                label = { Text("Tanggal") },
                trailingIcon = {
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Pilih tanggal")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { datePickerDialog.show() }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Transaction Type (Masuk / Keluar Selector)
            Text(
                text = "Jenis Transaksi",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isMasuk = viewModel.tipeInput == "Masuk"
                val isKeluar = viewModel.tipeInput == "Keluar"

                Button(
                    onClick = { viewModel.tipeInput = "Masuk" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMasuk) Color(0xFFD1FAE5) else Color(0xFFF1F5F9),
                        contentColor = if (isMasuk) Color(0xFF065F46) else Color(0xFF475569)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    elevation = null,
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            1.dp,
                            if (isMasuk) Color(0xFF10B981) else Color(0xFFE2E8F0),
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    Text("Masuk", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = { viewModel.tipeInput = "Keluar" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isKeluar) Color(0xFFFEE2E2) else Color(0xFFF1F5F9),
                        contentColor = if (isKeluar) Color(0xFF991B1B) else Color(0xFF475569)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    elevation = null,
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            1.dp,
                            if (isKeluar) Color(0xFFEF4444) else Color(0xFFE2E8F0),
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    Text("Keluar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Amount Input
            OutlinedTextField(
                value = viewModel.jumlahInput,
                onValueChange = { viewModel.jumlahInput = it },
                label = { Text("Jumlah (Rp)") },
                prefix = { Text("Rp ") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Dropdown Owner (Milik / Atas Nama)
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = viewModel.milikInput,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Milik / Atas Nama") },
                    trailingIcon = {
                        IconButton(onClick = { isMilikDropdownExpanded = !isMilikDropdownExpanded }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Pilih")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isMilikDropdownExpanded = !isMilikDropdownExpanded }
                )
                
                DropdownMenu(
                    expanded = isMilikDropdownExpanded,
                    onDismissRequest = { isMilikDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    listOf("Pak Hamzah", "Klien", "Abyan").forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option, fontWeight = FontWeight.Medium) },
                            onClick = {
                                viewModel.milikInput = option
                                isMilikDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Description input
            OutlinedTextField(
                value = viewModel.keteranganInput,
                onValueChange = { viewModel.keteranganInput = it },
                label = { Text("Keterangan") },
                placeholder = { Text("Cth: Pembayaran listrik...") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = { viewModel.saveTransaction() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("add_tx_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simpan Data", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun TransactionHistoryCard(viewModel: MainViewModel, transactions: List<TransactionEntity>) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 450.dp, max = 800.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Riwayat Transaksi",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF1E293B)
                    )
                }
                
                // Pulsing indicator / refresh representation
                IconButton(
                    onClick = { viewModel.showToast("Data dimuat dari database lokal.") },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                }
            }
            Divider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 12.dp))

            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.FolderOpen, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Belum ada transaksi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF475569)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Silakan tambahkan data transaksi baru melalui form di samping.",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(transactions, key = { it.id }) { tx ->
                        TransactionItem(tx, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItem(tx: TransactionEntity, viewModel: MainViewModel) {
    val isMasuk = tx.tipe == "Masuk"
    
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = viewModel.formatDateDisplay(tx.tanggal),
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE2E8F0), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = tx.milik,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tx.keterangan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = viewModel.formatTimeDisplay(tx.timestamp),
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (isMasuk) Color(0xFFD1FAE5) else Color(0xFFFEE2E2),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isMasuk) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = if (isMasuk) Color(0xFF065F46) else Color(0xFF991B1B),
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = tx.tipe,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isMasuk) Color(0xFF065F46) else Color(0xFF991B1B)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = viewModel.formatRupiah(tx.jumlah),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isMasuk) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                }

                IconButton(
                    onClick = { viewModel.showDeleteConfirmationId = tx.id },
                    modifier = Modifier
                        .testTag("delete_tx_button")
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFEF2F2))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

