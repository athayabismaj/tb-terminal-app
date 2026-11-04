package com.tbterminal.app.ui.receivables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
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

@Composable
internal fun InlineCustomerPicker(
    customerRepository: CustomerRepository,
    selectedCustomerId: String,
    initialCustomer: Customer? = null,
    onCustomerSelected: (Customer) -> Unit,
    onClearSelection: () -> Unit = {}
) {
    var selectedCustomer by remember(selectedCustomerId, initialCustomer) {
        mutableStateOf(initialCustomer)
    }

    // Search query & suggestions
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Customer>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var isDropdownOpen by remember { mutableStateOf(false) }

    // Quick add inline accordion state
    var isAddingNew by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var newAddress by remember { mutableStateOf("") }
    var isSavingNew by remember { mutableStateOf(false) }
    var newCustomerError by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Fetch suggestions on search query change (debounced)
    LaunchedEffect(searchQuery, isDropdownOpen) {
        if (!isDropdownOpen && searchQuery.isBlank()) {
            searchResults = emptyList()
            return@LaunchedEffect
        }
        if (searchQuery.isNotBlank()) {
            delay(250) // Debounce
        }
        isSearching = true
        val result = customerRepository.getCustomers(
            page = 1,
            limit = 6,
            search = searchQuery.trim().takeIf { it.isNotBlank() }
        )
        if (result is RepositoryResult.Success) {
            searchResults = result.data.data
        }
        isSearching = false
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // STATE 1: Customer is Selected
        if (selectedCustomer != null && !isAddingNew) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("inline-customer-selected-card"),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF9FAFB),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE0ECE7))
                                .border(1.dp, Color(0xFF256B57).copy(alpha = 0.25f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = selectedCustomer?.name?.take(2)?.uppercase() ?: "PL",
                                color = Color(0xFF256B57),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text(
                                text = selectedCustomer?.name.orEmpty(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111827),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val detail = selectedCustomer?.phone?.takeIf(String::isNotBlank)
                                ?: selectedCustomer?.address?.takeIf(String::isNotBlank)
                                ?: "ID: ${selectedCustomer?.id?.take(8)}"
                            Text(
                                text = detail,
                                fontSize = 11.sp,
                                color = Color(0xFF6B7280),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // "Ganti" Button
                    Surface(
                        onClick = {
                            selectedCustomer = null
                            onClearSelection()
                            isDropdownOpen = true
                            searchQuery = ""
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Ganti",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF256B57)
                            )
                        }
                    }
                }
            }
        } else if (!isAddingNew) {
            // STATE 2: Search Input & Autocomplete Dropdown
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Search Input Field
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(
                        1.dp,
                        if (isDropdownOpen) Color(0xFF256B57) else Color(0xFFD1D5DB)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = if (isDropdownOpen) Color(0xFF256B57) else Color(0xFF9CA3AF),
                            modifier = Modifier.size(18.dp)
                        )

                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Cari nama atau nomor telepon...",
                                    fontSize = 13.sp,
                                    color = Color(0xFF9CA3AF)
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = {
                                    searchQuery = it
                                    isDropdownOpen = true
                                },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 13.sp,
                                    color = Color(0xFF111827),
                                    fontWeight = FontWeight.Medium
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("inline-customer-search-input")
                            )
                        }

                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Hapus pencarian",
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable {
                                        searchQuery = ""
                                    }
                            )
                        }
                    }
                }

                // Autocomplete Results Card
                AnimatedVisibility(
                    visible = isDropdownOpen,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                        shadowElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 4.dp)
                        ) {
                            if (isSearching) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF256B57)
                                    )
                                }
                            } else {
                                if (searchResults.isNotEmpty()) {
                                    searchResults.forEach { customer ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedCustomer = customer
                                                    onCustomerSelected(customer)
                                                    isDropdownOpen = false
                                                    focusManager.clearFocus()
                                                }
                                                .padding(horizontal = 14.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFF1F5F9)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = customer.name.take(2).uppercase(),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF475569)
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = customer.name,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF1E293B)
                                                )
                                                val sub = customer.phone?.takeIf(String::isNotBlank)
                                                    ?: customer.address?.takeIf(String::isNotBlank)
                                                    ?: "Pelanggan Terdaftar"
                                                Text(
                                                    text = sub,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                        }
                                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.5.dp)
                                    }
                                } else if (searchQuery.isNotBlank()) {
                                    Text(
                                        text = "Pelanggan \"$searchQuery\" tidak ditemukan",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B),
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                    )
                                }

                                // Quick-Add Row Option
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            isAddingNew = true
                                            newName = searchQuery.trim()
                                            newPhone = ""
                                            newAddress = ""
                                            newCustomerError = null
                                            isDropdownOpen = false
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color(0xFF256B57),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (searchQuery.isNotBlank()) "Tambah \"$searchQuery\" sebagai pelanggan baru"
                                               else "Tambah pelanggan baru",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF256B57)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // STATE 3: Inline Quick-Add Accordion
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("inline-add-customer-panel"),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFF256B57).copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header of inline quick add
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = Color(0xFF256B57),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Tambah Pelanggan Baru",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Batal",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier
                                .size(18.dp)
                                .clickable {
                                    if (!isSavingNew) isAddingNew = false
                                }
                        )
                    }

                    // Field 1: Nama
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "NAMA LENGKAP *",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                        ) {
                            BasicTextField(
                                value = newName,
                                onValueChange = { newName = it },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 12.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Medium),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                    .testTag("inline-add-customer-name")
                            )
                        }
                    }

                    // Field 2: Telepon / WhatsApp
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "NO. TELEPON / WA (OPSIONAL)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                        ) {
                            BasicTextField(
                                value = newPhone,
                                onValueChange = { newPhone = it },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                textStyle = TextStyle(fontSize = 12.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Medium),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                    .testTag("inline-add-customer-phone")
                            )
                        }
                    }

                    // Field 3: Alamat (Opsional)
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "ALAMAT (OPSIONAL)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                        ) {
                            BasicTextField(
                                value = newAddress,
                                onValueChange = { newAddress = it },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 12.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Medium),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                    .testTag("inline-add-customer-address")
                            )
                        }
                    }

                    // Error text
                    if (newCustomerError != null) {
                        Text(
                            text = newCustomerError.orEmpty(),
                            fontSize = 11.sp,
                            color = Color(0xFFDC2626)
                        )
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = { if (!isSavingNew) isAddingNew = false },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE2E8F0),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Batal",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF475569)
                                )
                            }
                        }

                        Surface(
                            onClick = {
                                if (newName.isBlank()) {
                                    newCustomerError = "Nama pelanggan wajib diisi."
                                    return@Surface
                                }
                                isSavingNew = true
                                newCustomerError = null
                                coroutineScope.launch {
                                    val command = CustomerCommand(
                                        name = newName.trim(),
                                        phone = newPhone.trim().takeIf(String::isNotBlank),
                                        address = newAddress.trim().takeIf(String::isNotBlank),
                                        isContractor = false,
                                        creditLimit = BigDecimal.ZERO,
                                        paymentTermDays = 0
                                    )
                                    when (val result = customerRepository.createCustomer(command)) {
                                        is RepositoryResult.Success -> {
                                            val created = result.data
                                            selectedCustomer = created
                                            onCustomerSelected(created)
                                            isSavingNew = false
                                            isAddingNew = false
                                        }
                                        is RepositoryResult.Error -> {
                                            isSavingNew = false
                                            newCustomerError = result.message
                                        }
                                        is RepositoryResult.Exception -> {
                                            isSavingNew = false
                                            newCustomerError = "Koneksi bermasalah. Pelanggan gagal disimpan."
                                        }
                                    }
                                }
                            },
                            enabled = !isSavingNew,
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF256B57),
                            modifier = Modifier
                                .weight(1.5f)
                                .height(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isSavingNew) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = "Simpan & Pilih",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
