package com.tbterminal.app.ui.receivables

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.Customer
import com.tbterminal.app.data.model.CustomerCommand
import com.tbterminal.app.data.repository.CustomerRepository
import com.tbterminal.app.data.repository.RepositoryResult
import java.math.BigDecimal
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Brand = Color(0xFF256B57)
private val Brand50 = Color(0xFFF0F7F5)
private val Brand100 = Color(0xFFE1EFEA)
private val Brand200 = Color(0xFFC4DFD6)
private val Brand600 = Color(0xFF256B57)
private val Brand700 = Color(0xFF1D5344)
private val Slate50 = Color(0xFFF8FAFC)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate300 = Color(0xFFCBD5E1)
private val Slate400 = Color(0xFF94A3B8)
private val Slate500 = Color(0xFF64748B)
private val Slate600 = Color(0xFF475569)
private val Slate700 = Color(0xFF334155)
private val Slate800 = Color(0xFF1E293B)
private val Slate900 = Color(0xFF0F172A)

/**
 * Step navigation for bottom sheets requiring customer selection without modal stacking.
 */
enum class SheetCustomerStep {
    FORM,
    SELECT_CUSTOMER,
    ADD_CUSTOMER
}

private fun getInitials(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return "PL"
    val parts = trimmed.split("\\s+".toRegex()).filter { it.isNotBlank() }
    return if (parts.size >= 2) {
        "${parts[0].first()}${parts[1].first()}".uppercase()
    } else {
        trimmed.take(2).uppercase()
    }
}

/**
 * Customer card selector displayed in the main form (Step.FORM).
 * Clicking it transitions to Step.SELECT_CUSTOMER.
 */
@Composable
fun CustomerCardSelector(
    selectedCustomer: Customer?,
    onOpenPicker: () -> Unit,
    onClearCustomer: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selectedCustomer != null) {
        // Selected Customer Card
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(Brand50, RoundedCornerShape(14.dp))
                .border(1.5.dp, Brand200, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Initials Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Brand, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getInitials(selectedCustomer.name),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            // Customer Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenPicker() },
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = selectedCustomer.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (selectedCustomer.isContractor) {
                        Surface(
                            color = Brand100,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "KONTRAKTOR",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Brand700,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                val subInfo = buildList {
                    selectedCustomer.phone?.takeIf { it.isNotBlank() }?.let { add(it) }
                    selectedCustomer.address?.takeIf { it.isNotBlank() }?.let { add(it) }
                }.joinToString(" • ")

                if (subInfo.isNotBlank()) {
                    Text(
                        text = subInfo,
                        fontSize = 11.sp,
                        color = Slate500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Change Pill Button & Clear Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    onClick = onOpenPicker,
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Brand200)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Ganti",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Brand700
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Brand700,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onClearCustomer,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Hapus Pilihan",
                        tint = Slate400,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    } else {
        // Empty State: Tap to select customer
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Brand50.copy(alpha = 0.6f))
                .border(1.dp, Brand200, RoundedCornerShape(14.dp))
                .clickable { onOpenPicker() }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Color.White, CircleShape)
                    .border(1.dp, Brand200, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = null,
                    tint = Brand,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Pilih Pelanggan",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate800
                )
                Text(
                    text = "Ketuk untuk memilih dari kontak atau buat baru",
                    fontSize = 11.sp,
                    color = Slate500
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Brand,
                contentColor = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Pilih",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

/**
 * In-sheet screen for searching and choosing a customer (Step.SELECT_CUSTOMER).
 */
@Composable
fun CustomerPickerSheetView(
    customerRepository: CustomerRepository,
    selectedCustomerId: String,
    onBackToForm: () -> Unit,
    onClose: () -> Unit,
    onNavigateToAddCustomer: () -> Unit,
    onCustomerSelected: (Customer) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var customers by remember { mutableStateOf<List<Customer>>(emptyList()) }
    var currentPage by remember { mutableIntStateOf(1) }
    var totalPages by remember { mutableIntStateOf(1) }
    var totalCount by remember { mutableLongStateOf(0L) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var userHasScrolled by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            userHasScrolled = true
        }
    }

    // Search and initial fetch
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            delay(300)
        }
        isLoading = true
        loadError = null
        currentPage = 1
        userHasScrolled = false
        when (val result = customerRepository.getCustomers(
            page = 1,
            limit = 12,
            search = searchQuery.trim().takeIf { it.isNotBlank() }
        )) {
            is RepositoryResult.Success -> {
                customers = result.data.data
                totalCount = result.data.total
                totalPages = result.data.totalPages
                isLoading = false
            }
            is RepositoryResult.Error -> {
                loadError = result.message
                isLoading = false
            }
            is RepositoryResult.Exception -> {
                loadError = "Koneksi bermasalah. Gagal memuat daftar pelanggan."
                isLoading = false
            }
        }
    }

    // Pagination on scroll
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            userHasScrolled && !isLoading && !isLoadingMore && currentPage < totalPages && totalItems > 0 && lastVisibleIndex >= (totalItems - 2)
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            isLoadingMore = true
            val nextPage = currentPage + 1
            when (val result = customerRepository.getCustomers(
                page = nextPage,
                limit = 12,
                search = searchQuery.trim().takeIf { it.isNotBlank() }
            )) {
                is RepositoryResult.Success -> {
                    val existingIds = customers.map { it.id }.toSet()
                    val newItems = result.data.data.filterNot { it.id in existingIds }
                    customers = customers + newItems
                    currentPage = nextPage
                    totalPages = result.data.totalPages
                    totalCount = result.data.total
                }
                else -> Unit
            }
            isLoadingMore = false
            userHasScrolled = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
    ) {
        // In-Sheet Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onBackToForm,
                    modifier = Modifier
                        .size(34.dp)
                        .background(Slate100, CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali ke Formulir",
                        tint = Slate700,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = "Pilih Pelanggan",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        letterSpacing = (-0.3).sp
                    )
                    Text(
                        text = if (isLoading) "Memuat..." else "$totalCount kontak tersedia",
                        fontSize = 11.sp,
                        color = Slate500
                    )
                }
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(34.dp)
                    .background(Slate100, CircleShape)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Tutup",
                    tint = Slate500,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        HorizontalDivider(color = Slate100)

        // Search Bar & Quick Add Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Search Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = Slate400,
                    modifier = Modifier.size(18.dp)
                )
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate900
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Cari nama, telepon, atau alamat...",
                                fontSize = 13.sp,
                                color = Slate400
                            )
                        }
                        innerTextField()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Slate200)
                            .clickable { searchQuery = "" },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Hapus teks",
                            tint = Slate600,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // Quick Add New Customer Action Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brand50)
                    .border(1.dp, Brand200, RoundedCornerShape(12.dp))
                    .clickable { onNavigateToAddCustomer() }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Brand, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "+ Tambah Pelanggan Baru",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Brand700
                    )
                    Text(
                        text = "Daftarkan pelanggan baru ke kontak toko",
                        fontSize = 11.sp,
                        color = Slate500
                    )
                }

                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Brand600,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Customer List Body
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (isLoading && customers.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp), color = Brand, strokeWidth = 2.5.dp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "Memuat daftar pelanggan...", fontSize = 12.sp, color = Slate500)
                }
            } else if (loadError != null && customers.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = loadError ?: "Terjadi kesalahan",
                        fontSize = 12.sp,
                        color = Color(0xFFDC2626),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            isLoading = true
                            loadError = null
                            coroutineScope.launch {
                                when (val result = customerRepository.getCustomers(page = 1, limit = 12, search = searchQuery.trim().takeIf { it.isNotBlank() })) {
                                    is RepositoryResult.Success -> {
                                        customers = result.data.data
                                        totalCount = result.data.total
                                        totalPages = result.data.totalPages
                                        isLoading = false
                                    }
                                    is RepositoryResult.Error -> {
                                        loadError = result.message
                                        isLoading = false
                                    }
                                    is RepositoryResult.Exception -> {
                                        loadError = "Koneksi bermasalah."
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Brand),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Coba Lagi", fontSize = 12.sp)
                    }
                }
            } else if (customers.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Slate100, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Slate400, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Pelanggan tidak ditemukan",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate800
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "Tidak ada hasil untuk \"$searchQuery\"" else "Belum ada data pelanggan",
                        fontSize = 11.sp,
                        color = Slate500
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(customers, key = { it.id }) { customer ->
                        val isSelected = customer.id == selectedCustomerId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Brand50 else Color.White)
                                .border(1.dp, if (isSelected) Brand else Slate200, RoundedCornerShape(12.dp))
                                .clickable { onCustomerSelected(customer) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Initials Avatar
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(if (isSelected) Brand else Brand100, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = getInitials(customer.name),
                                    color = if (isSelected) Color.White else Brand700,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            // Customer Details
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = customer.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate900,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (customer.isContractor) {
                                        Surface(color = Brand100, shape = RoundedCornerShape(4.dp)) {
                                            Text(
                                                text = "KONTRAKTOR",
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Brand700,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }

                                val details = buildList {
                                    customer.phone?.takeIf { it.isNotBlank() }?.let { add(it) }
                                    customer.address?.takeIf { it.isNotBlank() }?.let { add(it) }
                                }.joinToString(" • ")

                                if (details.isNotBlank()) {
                                    Text(
                                        text = details,
                                        fontSize = 11.sp,
                                        color = Slate500,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Selection Indicator
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(Brand, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Terpilih",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .border(1.5.dp, Slate300, CircleShape)
                                )
                            }
                        }
                    }

                    if (isLoadingMore) {
                        item(key = "load_more") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Brand)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Memuat data lainnya...", fontSize = 11.sp, color = Slate500)
                            }
                        }
                    }

                    item(key = "bottom_space") {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

/**
 * In-sheet screen for quickly adding a new customer (Step.ADD_CUSTOMER).
 */
@Composable
fun AddCustomerSheetView(
    customerRepository: CustomerRepository,
    initialName: String = "",
    onBackToPicker: () -> Unit,
    onClose: () -> Unit,
    onCustomerCreated: (Customer) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onBackToPicker,
                    modifier = Modifier
                        .size(34.dp)
                        .background(Slate100, CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali ke Daftar",
                        tint = Slate700,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = "Tambah Pelanggan Baru",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        letterSpacing = (-0.3).sp
                    )
                    Text(
                        text = "Lengkapi profil pelanggan untuk pencatatan piutang",
                        fontSize = 11.sp,
                        color = Slate500
                    )
                }
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(34.dp)
                    .background(Slate100, CircleShape)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Tutup",
                    tint = Slate500,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        HorizontalDivider(color = Slate100)

        // Form Inputs
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Error banner if any
            if (errorMessage != null) {
                Surface(
                    color = Color(0xFFFEF2F2),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            fontSize = 12.sp,
                            color = Color(0xFFDC2626),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Nama Pelanggan *
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "NAMA LENGKAP PELANGGAN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate700,
                        letterSpacing = 0.5.sp
                    )
                    Text("*", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Brand)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            if (errorMessage != null) errorMessage = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Slate900),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (name.isEmpty()) {
                                Text("Contoh: H. Ahmad Subarjo", fontSize = 13.sp, color = Slate400)
                            }
                            innerTextField()
                        }
                    )
                }
            }

            // Nomor HP / WA
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = "NOMOR TELEPON / WHATSAPP",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate700,
                    letterSpacing = 0.5.sp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Outlined.Phone, contentDescription = null, tint = Slate400, modifier = Modifier.size(16.dp))
                    BasicTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Slate900),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        decorationBox = { innerTextField ->
                            if (phone.isEmpty()) {
                                Text("Contoh: 081234567890", fontSize = 13.sp, color = Slate400)
                            }
                            innerTextField()
                        }
                    )
                }
            }

            // Alamat
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = "ALAMAT LENGKAP (OPSIONAL)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate700,
                    letterSpacing = 0.5.sp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    BasicTextField(
                        value = address,
                        onValueChange = { address = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Slate900),
                        decorationBox = { innerTextField ->
                            if (address.isEmpty()) {
                                Text("Contoh: Jl. Diponegoro No. 12, RT 02/RW 04", fontSize = 13.sp, color = Slate400)
                            }
                            innerTextField()
                        }
                    )
                }
            }
        }

        // Sticky Footer Actions
        HorizontalDivider(color = Slate100)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onBackToPicker,
                enabled = !isSaving,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Slate100, contentColor = Slate700)
            ) {
                Text("Batal", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Nama pelanggan wajib diisi."
                        return@Button
                    }
                    isSaving = true
                    errorMessage = null
                    coroutineScope.launch {
                        val command = CustomerCommand(
                            name = name.trim(),
                            phone = phone.trim().takeIf(String::isNotBlank),
                            address = address.trim().takeIf(String::isNotBlank),
                            isContractor = false,
                            creditLimit = BigDecimal.ZERO,
                            paymentTermDays = 0
                        )
                        when (val result = customerRepository.createCustomer(command)) {
                            is RepositoryResult.Success -> {
                                isSaving = false
                                onCustomerCreated(result.data)
                            }
                            is RepositoryResult.Error -> {
                                isSaving = false
                                errorMessage = result.message
                            }
                            is RepositoryResult.Exception -> {
                                isSaving = false
                                errorMessage = "Koneksi bermasalah. Gagal menyimpan pelanggan."
                            }
                        }
                    }
                },
                enabled = !isSaving,
                modifier = Modifier
                    .weight(2f)
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Brand)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("SIMPAN & PILIH", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
