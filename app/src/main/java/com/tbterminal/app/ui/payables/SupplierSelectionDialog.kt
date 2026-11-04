package com.tbterminal.app.ui.payables

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
import com.tbterminal.app.data.model.Supplier
import com.tbterminal.app.data.repository.PurchasingRepository
import com.tbterminal.app.data.repository.RepositoryResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    if (trimmed.isEmpty()) return "SP"
    val parts = trimmed.split("\\s+".toRegex()).filter { it.isNotBlank() }
    return if (parts.size >= 2) {
        "${parts[0].first()}${parts[1].first()}".uppercase()
    } else {
        trimmed.take(2).uppercase()
    }
}

private const val SUPPLIER_PAGE_LIMIT = 10

@Composable
fun SupplierSelectionDialog(
    purchasingRepository: PurchasingRepository,
    selectedSupplierId: String,
    onSupplierSelected: (Supplier) -> Unit,
    onAddSupplierClick: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var suppliers by remember { mutableStateOf<List<Supplier>>(emptyList()) }
    var currentPage by remember { mutableIntStateOf(1) }
    var totalPages by remember { mutableIntStateOf(1) }
    var totalCount by remember { mutableLongStateOf(0L) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var tempSelectedId by remember { mutableStateOf(selectedSupplierId) }
    var showAddSupplierDialog by remember { mutableStateOf(false) }
    var userHasScrolled by remember { mutableStateOf(false) }

    val handleQuickAdd = {
        if (onAddSupplierClick != null) {
            onDismiss()
            onAddSupplierClick()
        } else {
            showAddSupplierDialog = true
        }
    }

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
            delay(350)
        }
        isLoading = true
        loadError = null
        currentPage = 1
        userHasScrolled = false
        when (val result = purchasingRepository.getSuppliers(
            page = 1,
            limit = SUPPLIER_PAGE_LIMIT,
            search = searchQuery.trim().takeIf(String::isNotBlank)
        )) {
            is RepositoryResult.Success -> {
                suppliers = result.data.data
                totalCount = result.data.total
                totalPages = result.data.totalPages
                isLoading = false
            }
            is RepositoryResult.Error -> {
                loadError = result.message
                isLoading = false
            }
            is RepositoryResult.Exception -> {
                loadError = "Koneksi bermasalah. Gagal memuat supplier."
                isLoading = false
            }
        }
    }

    // Load more when scrolled near the end
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
            when (val result = purchasingRepository.getSuppliers(
                page = nextPage,
                limit = SUPPLIER_PAGE_LIMIT,
                search = searchQuery.trim().takeIf(String::isNotBlank)
            )) {
                is RepositoryResult.Success -> {
                    val existingIds = suppliers.map { it.id }.toSet()
                    val newItems = result.data.data.filterNot { it.id in existingIds }
                    suppliers = suppliers + newItems
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

    // Quick Add Supplier Dialog
    if (showAddSupplierDialog) {
        QuickAddSupplierDialog(
            purchasingRepository = purchasingRepository,
            onSupplierCreated = { newSupplier ->
                suppliers = listOf(newSupplier) + suppliers
                tempSelectedId = newSupplier.id
                totalCount += 1
                showAddSupplierDialog = false
                coroutineScope.launch {
                    listState.animateScrollToItem(0)
                }
            },
            onDismiss = { showAddSupplierDialog = false }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Slate200),
                elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Modal Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Pilih Supplier",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900,
                                letterSpacing = (-0.3).sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Pilih supplier untuk pencatatan hutang",
                                fontSize = 12.sp,
                                color = Slate500
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Slate100)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = Slate500,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = Slate100, thickness = 1.dp)

                    // Search & Add Bar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Search Box
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Slate100)
                                    .border(1.dp, Slate200, RoundedCornerShape(14.dp))
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = "Cari",
                                        tint = Slate400,
                                        modifier = Modifier.size(18.dp)
                                    )

                                    Box(modifier = Modifier.weight(1f)) {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = "Cari nama atau telepon supplier...",
                                                color = Slate400,
                                                fontSize = 13.sp
                                            )
                                        }
                                        BasicTextField(
                                            value = searchQuery,
                                            onValueChange = { searchQuery = it },
                                            singleLine = true,
                                            textStyle = LocalTextStyle.current.copy(
                                                color = Slate900,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                            keyboardActions = KeyboardActions(onSearch = { /* Handled by LaunchedEffect */ }),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    if (searchQuery.isNotEmpty()) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Hapus",
                                            tint = Slate400,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .clickable { searchQuery = "" }
                                        )
                                    }
                                }
                            }

                            // Add Supplier Button
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Sage50)
                                    .border(1.dp, Sage200, RoundedCornerShape(14.dp))
                                    .clickable { showAddSupplierDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Tambah Supplier",
                                    tint = Sage700,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Total count subtitle
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Menampilkan ${suppliers.size} dari $totalCount supplier",
                                fontSize = 11.sp,
                                color = Slate400,
                                fontWeight = FontWeight.Medium
                            )
                            if (isLoading) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 2.dp,
                                        color = Sage700
                                    )
                                    Text(
                                        text = "Memuat...",
                                        fontSize = 11.sp,
                                        color = Sage700
                                    )
                                }
                            }
                        }
                    }

                    // Content Area (List / Loading / Error / Empty)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        if (isLoading && suppliers.isEmpty()) {
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
                                    text = "Memuat daftar supplier...",
                                    fontSize = 12.sp,
                                    color = Slate500
                                )
                            }
                        } else if (loadError != null && suppliers.isEmpty()) {
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
                                            when (val result = purchasingRepository.getSuppliers(
                                                page = 1,
                                                limit = SUPPLIER_PAGE_LIMIT,
                                                search = searchQuery.trim().takeIf(String::isNotBlank)
                                            )) {
                                                is RepositoryResult.Success -> {
                                                    suppliers = result.data.data
                                                    totalCount = result.data.total
                                                    totalPages = result.data.totalPages
                                                    isLoading = false
                                                }
                                                is RepositoryResult.Error -> {
                                                    loadError = result.message
                                                    isLoading = false
                                                }
                                                is RepositoryResult.Exception -> {
                                                    loadError = "Koneksi bermasalah. Gagal memuat supplier."
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
                        } else if (suppliers.isEmpty()) {
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
                                    text = "Supplier tidak ditemukan",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate700
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (searchQuery.isNotBlank()) "Tidak ada hasil untuk \"$searchQuery\""
                                    else "Belum ada data supplier yang terdaftar",
                                    fontSize = 12.sp,
                                    color = Slate500
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                QuickAddSupplierButton(onClick = handleQuickAdd)
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(suppliers, key = { it.id }) { supplier ->
                                    val isSelected = supplier.id == tempSelectedId
                                    SupplierCardItem(
                                        supplier = supplier,
                                        isSelected = isSelected,
                                        onClick = { tempSelectedId = supplier.id }
                                    )
                                }

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
                                                text = "Memuat supplier lainnya...",
                                                fontSize = 11.sp,
                                                color = Slate500
                                            )
                                        }
                                    }
                                }

                                item(key = "quick_add_row") {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    QuickAddSupplierButton(onClick = handleQuickAdd)
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                    }

                    // Sticky Bottom Actions
                    HorizontalDivider(color = Slate100, thickness = 1.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Slate200),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate600)
                        ) {
                            Text(
                                text = "Batal",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        val selectedSupplier = suppliers.find { it.id == tempSelectedId }
                        Button(
                            onClick = {
                                selectedSupplier?.let { onSupplierSelected(it) }
                            },
                            enabled = selectedSupplier != null,
                            modifier = Modifier
                                .weight(1.4f)
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Sage700,
                                disabledContainerColor = Sage200
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Pilih Supplier",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SupplierCardItem(
    supplier: Supplier,
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
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) Sage100 else Slate100)
                    .border(1.dp, if (isSelected) Sage200 else Slate200, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getInitials(supplier.name),
                    color = if (isSelected) Sage800 else Slate700,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = supplier.name,
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
                    val phone = supplier.phone?.takeIf(String::isNotBlank)
                    val address = supplier.address?.takeIf(String::isNotBlank)

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
private fun QuickAddSupplierButton(
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
                text = "Tambah Supplier Baru",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Sage700
            )
        }
    }
}

@Composable
private fun QuickAddSupplierDialog(
    purchasingRepository: PurchasingRepository,
    onSupplierCreated: (Supplier) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .widthIn(max = 480.dp)
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tambah Supplier Baru",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Text(
                                text = "Masukkan data supplier dengan cepat",
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

                    Text(
                        text = "NAMA SUPPLIER *",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate700
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("Contoh: PT Semen Gresik", fontSize = 12.sp, color = Slate400) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Sage700,
                            unfocusedBorderColor = Slate300
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "NOMOR TELEPON / WA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate700
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        placeholder = { Text("Contoh: 081234567890", fontSize = 12.sp, color = Slate400) },
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

                    Text(
                        text = "ALAMAT (OPSIONAL)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate700
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        placeholder = { Text("Alamat kantor atau gudang supplier", fontSize = 12.sp, color = Slate400) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Sage700,
                            unfocusedBorderColor = Slate300
                        )
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFDC2626),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isSaving
                        ) {
                            Text("Batal", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val trimmedName = name.trim()
                                if (trimmedName.isBlank()) {
                                    errorMessage = "Nama supplier wajib diisi."
                                    return@Button
                                }
                                isSaving = true
                                errorMessage = null
                                coroutineScope.launch {
                                    when (
                                        val result = purchasingRepository.createSupplier(
                                            name = trimmedName,
                                            phone = phone.trim().takeIf(String::isNotBlank),
                                            address = address.trim().takeIf(String::isNotBlank),
                                            paymentTermDays = 30
                                        )
                                    ) {
                                        is RepositoryResult.Success -> {
                                            isSaving = false
                                            onSupplierCreated(result.data)
                                        }
                                        is RepositoryResult.Error -> {
                                            isSaving = false
                                            errorMessage = result.message
                                        }
                                        is RepositoryResult.Exception -> {
                                            isSaving = false
                                            errorMessage = "Gagal menyimpan supplier. Periksa koneksi."
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isSaving,
                            colors = ButtonDefaults.buttonColors(containerColor = Sage700)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
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
}
