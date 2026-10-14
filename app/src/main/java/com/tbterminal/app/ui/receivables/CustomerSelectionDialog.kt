package com.tbterminal.app.ui.receivables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tbterminal.app.data.model.Customer
import com.tbterminal.app.data.model.CustomerCommand
import com.tbterminal.app.data.repository.CustomerRepository
import com.tbterminal.app.data.repository.RepositoryResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal

private val Sage50 = Color(0xFFF2F8F5)
private val Sage100 = Color(0xFFE2EFE9)
private val Sage200 = Color(0xFFC7E0D5)
private val Sage500 = Color(0xFF3D8C73)
private val Sage600 = Color(0xFF2C735D)
private val Sage700 = Color(0xFF256B57)
private val Sage800 = Color(0xFF1F5444)
private val Slate50 = Color(0xFFF8FAFC)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate300 = Color(0xFFCBD5E1)
private val Slate400 = Color(0xFF94A3B8)
private val Slate500 = Color(0xFF64748B)
private val Slate600 = Color(0xFF475569)
private val Slate700 = Color(0xFF334155)
private val Slate900 = Color(0xFF0F172A)

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

private const val CUSTOMER_PAGE_LIMIT = 10

@Composable
fun CustomerSelectionDialog(
    customerRepository: CustomerRepository,
    selectedCustomerId: String,
    onCustomerSelected: (Customer) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var customers by remember { mutableStateOf<List<Customer>>(emptyList()) }
    var currentPage by remember { mutableIntStateOf(1) }
    var totalPages by remember { mutableIntStateOf(1) }
    var totalCount by remember { mutableLongStateOf(0L) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var tempSelectedId by remember { mutableStateOf(selectedCustomerId) }
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var userHasScrolled by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            userHasScrolled = true
        }
    }

    // Load initial page or search query with debounce
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            delay(350) // Debounce searching
        }
        isLoading = true
        loadError = null
        currentPage = 1
        userHasScrolled = false
        when (val result = customerRepository.getCustomers(
            page = 1,
            limit = CUSTOMER_PAGE_LIMIT,
            search = searchQuery.trim().takeIf(String::isNotBlank)
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
                loadError = "Koneksi bermasalah. Gagal memuat pelanggan."
                isLoading = false
            }
        }
    }

    // Load more when scrolled near the end (load on scroll)
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            userHasScrolled && !isLoading && !isLoadingMore && currentPage < totalPages && totalItems > 0 && lastVisibleIndex >= (totalItems - 1)
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            isLoadingMore = true
            val nextPage = currentPage + 1
            when (val result = customerRepository.getCustomers(
                page = nextPage,
                limit = CUSTOMER_PAGE_LIMIT,
                search = searchQuery.trim().takeIf(String::isNotBlank)
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

    // Quick Add Customer Dialog
    if (showAddCustomerDialog) {
        QuickAddCustomerDialog(
            customerRepository = customerRepository,
            onCustomerCreated = { newCustomer ->
                customers = listOf(newCustomer) + customers
                tempSelectedId = newCustomer.id
                totalCount += 1
                showAddCustomerDialog = false
                coroutineScope.launch {
                    listState.animateScrollToItem(0)
                }
            },
            onDismiss = { showAddCustomerDialog = false }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Slate200),
            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // BEGIN: ModalHeader
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Pilih Pelanggan",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            letterSpacing = (-0.4).sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Pilih pelanggan untuk mencatat piutang",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = Slate500,
                            lineHeight = 16.sp
                        )
                    }

                    // Close Button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Slate100)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Slate500,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                // END: ModalHeader

                // BEGIN: SearchSection
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(14.dp))
                            .border(1.dp, Slate200, RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
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
                            textStyle = androidx.compose.ui.text.TextStyle(
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
                                    contentDescription = "Hapus pencarian",
                                    tint = Slate600,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
                // END: SearchSection

                // List Section Subheader (Daftar Pelanggan • X Data)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daftar Pelanggan",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate500
                    )
                    Text(
                        text = if (isLoading && customers.isEmpty()) "..." else "$totalCount Data",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate500
                    )
                }

                // BEGIN: CustomerListSection
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (isLoading && customers.isEmpty()) {
                        // Loading State
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = Sage700,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Memuat daftar pelanggan...",
                                fontSize = 12.sp,
                                color = Slate500
                            )
                        }
                    } else if (loadError != null && customers.isEmpty()) {
                        // Error State
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = loadError ?: "Terjadi kesalahan",
                                fontSize = 13.sp,
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    isLoading = true
                                    loadError = null
                                    coroutineScope.launch {
                                        when (val result = customerRepository.getCustomers(
                                            page = 1,
                                            limit = CUSTOMER_PAGE_LIMIT,
                                            search = searchQuery.trim().takeIf(String::isNotBlank)
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
                                                loadError = "Koneksi bermasalah. Gagal memuat pelanggan."
                                                isLoading = false
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Sage700),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Coba Lagi", fontSize = 12.sp)
                            }
                        }
                    } else if (customers.isEmpty()) {
                        // Empty State
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(Slate100, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Slate400,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Pelanggan tidak ditemukan",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate700
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "Tidak ada hasil untuk \"$searchQuery\""
                                else "Belum ada data pelanggan yang terdaftar",
                                fontSize = 12.sp,
                                color = Slate500
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            // Quick Add Button inside empty state
                            QuickAddButton(onClick = { showAddCustomerDialog = true })
                        }
                    } else {
                        // LazyColumn with load on scroll
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(customers, key = { it.id }) { customer ->
                                val isSelected = customer.id == tempSelectedId
                                CustomerCardItem(
                                    customer = customer,
                                    isSelected = isSelected,
                                    onClick = { tempSelectedId = customer.id }
                                )
                            }

                            // Load More Spinner
                            if (isLoadingMore) {
                                item(key = "loading_more") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = Sage700
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Memuat pelanggan lainnya...",
                                            fontSize = 11.sp,
                                            color = Slate500
                                        )
                                    }
                                }
                            }

                            // Quick Add Button at bottom of list
                            item(key = "quick_add_button") {
                                QuickAddButton(
                                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                                    onClick = { showAddCustomerDialog = true }
                                )
                            }
                        }
                    }
                }
                // END: CustomerListSection

                // BEGIN: ModalFooter
                HorizontalDivider(color = Slate100, thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Batal Button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(0.35f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Slate100,
                            contentColor = Slate600
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text(
                            text = "Batal",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Pilih Pelanggan Button
                    val selectedCustomerObj = customers.firstOrNull { it.id == tempSelectedId }
                    val isEnabled = selectedCustomerObj != null
                    Button(
                        onClick = {
                            selectedCustomerObj?.let {
                                onCustomerSelected(it)
                                onDismiss()
                            }
                        },
                        enabled = isEnabled,
                        modifier = Modifier
                            .weight(0.65f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEnabled) Sage700 else Slate300,
                            contentColor = Color.White,
                            disabledContainerColor = Slate300,
                            disabledContentColor = Color.White.copy(alpha = 0.8f)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = if (isEnabled) 2.dp else 0.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Pilih Pelanggan",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                // END: ModalFooter
            }
        }
    }
}

@Composable
private fun CustomerCardItem(
    customer: Customer,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Sage50.copy(alpha = 0.5f) else Color.White)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Sage700 else Slate200,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Squircle Avatar
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) Sage100 else Slate100)
                    .border(1.dp, if (isSelected) Sage200 else Slate200, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getInitials(customer.name),
                    color = if (isSelected) Sage800 else Slate700,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Customer Name & Contact Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val phone = customer.phone?.takeIf(String::isNotBlank)
                    val address = customer.address?.takeIf(String::isNotBlank)

                    Icon(
                        Icons.Outlined.Phone,
                        contentDescription = null,
                        tint = Slate400,
                        modifier = Modifier.size(12.dp)
                    )

                    Text(
                        text = phone ?: "-",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate600
                    )

                    if (address != null) {
                        Text(
                            text = "•",
                            fontSize = 11.sp,
                            color = Slate300
                        )
                        Text(
                            text = address,
                            fontSize = 11.sp,
                            color = Slate500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Right Selection Indicator
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Sage700, CircleShape),
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
                    .size(22.dp)
                    .border(1.5.dp, Slate300, CircleShape)
            )
        }
    }
}

@Composable
private fun QuickAddButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val stroke = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                )
                drawRoundRect(
                    color = Sage500.copy(alpha = 0.5f),
                    style = stroke,
                    cornerRadius = CornerRadius(12.dp.toPx())
                )
            }
            .clip(RoundedCornerShape(12.dp))
            .background(Sage50.copy(alpha = 0.7f))
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp, horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                tint = Sage700,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Tambah Pelanggan Baru",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Sage700
            )
        }
    }
}

@Composable
private fun QuickAddCustomerDialog(
    customerRepository: CustomerRepository,
    onCustomerCreated: (Customer) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Dialog(onDismissRequest = { if (!isSaving) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Slate200),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tambah Pelanggan Baru",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "Masukkan data pelanggan dengan cepat",
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Slate100)
                            .clickable { if (!isSaving) onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Slate500,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Input Nama
                Text(
                    text = "NAMA PELANGGAN *",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate700
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Contoh: Budi Pratama", fontSize = 12.sp, color = Slate400) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Sage700,
                        unfocusedBorderColor = Slate300
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Input Telepon
                Text(
                    text = "NOMOR TELEPON",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate700
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    placeholder = { Text("Contoh: 08123456789", fontSize = 12.sp, color = Slate400) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Sage700,
                        unfocusedBorderColor = Slate300
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Input Alamat
                Text(
                    text = "ALAMAT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate700
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    placeholder = { Text("Contoh: Jl. Merdeka No. 10", fontSize = 12.sp, color = Slate400) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Sage700,
                        unfocusedBorderColor = Slate300
                    )
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        fontSize = 11.sp,
                        color = Color(0xFFDC2626)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        enabled = !isSaving,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Slate100,
                            contentColor = Slate600
                        )
                    ) {
                        Text("Batal", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
                                        errorMessage = "Koneksi bermasalah. Pelanggan gagal disimpan."
                                    }
                                }
                            }
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Sage700,
                            contentColor = Color.White
                        )
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Simpan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
