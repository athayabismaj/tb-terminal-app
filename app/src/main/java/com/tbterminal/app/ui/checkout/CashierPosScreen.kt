package com.tbterminal.app.ui.checkout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.Customer
import com.tbterminal.app.ui.common.UiText
import com.tbterminal.app.ui.checkout.components.ProductCardGrid
import com.tbterminal.app.ui.checkout.components.ProductErrorState
import com.tbterminal.app.ui.checkout.components.ProductPaginationBar
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

// ==========================================
// TEMA & WARNA
// ==========================================
val SurfaceBright = Color(0xFFF8F9FF)
val SurfaceContainerLow = Color(0xFFEFF4FF)
val OnSurface = Color(0xFF121C2A)
val OnSurfaceVariant = Color(0xFF3D4A42)
val OutlineVariant = Color(0xBCCAC0)
val Outline = Color(0xFF6D7A72)

val Primary = Color(0xFF006948)
val PrimaryContainer = Color(0xFF00855D)
val OnPrimary = Color(0xFFFFFFFF)

val SecondaryContainer = Color(0xFFFEA619)
val OnSecondaryContainer = Color(0xFF684000)

val Error = Color(0xFFBA1A1A)
val ErrorContainer = Color(0xFFFFDAD6)
val OnError = Color(0xFFFFFFFF)
val WarningOrange = Color(0xFFFEA619)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierPosScreen(
    state: CheckoutUiState,
    onSearchChanged: (String) -> Unit,
    onRefreshProducts: () -> Unit,
    onProductPageChanged: (Int) -> Unit,
    onProductClick: (Product) -> Unit,
    onCustomerSearchChanged: (String) -> Unit,
    onSelectCustomer: (Customer) -> Unit,
    onClearCustomer: () -> Unit,
    onUseCustomerName: () -> Unit,
    onRefreshCustomers: () -> Unit,
    onUpdateQty: (String, Int) -> Unit,
    onSelectPayment: (PaymentMethod) -> Unit,
    onAmountPaidChanged: (String) -> Unit,
    onStartingCashChanged: (String) -> Unit,
    onOpenCashSession: () -> Unit,
    onRefreshCashSession: () -> Unit,
    onCheckout: () -> Unit,
    onNavigateToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allCategory = "Semua kategori"
    val categories = listOf(allCategory) + state.products.map { it.categoryName }.filter { it.isNotBlank() }.distinct()
    var selectedCategory by remember { mutableStateOf(allCategory) }
    LaunchedEffect(categories) {
        if (selectedCategory !in categories) {
            selectedCategory = allCategory
        }
    }
    
    val filteredProducts = if (selectedCategory == allCategory) state.products else state.products.filter { it.categoryName == selectedCategory }
    
    val cartItemCount = state.cartItems.sumOf { it.quantity }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = SurfaceBright,
        contentWindowInsets = WindowInsets(0.dp),
        floatingActionButton = {
            // Floating Action Button Keranjang Belanja
            Box(
                contentAlignment = Alignment.TopEnd,
                modifier = Modifier.offset(y = (-60).dp)
            ) {
                FloatingActionButton(
                    onClick = onNavigateToCart,
                    containerColor = Primary,
                    contentColor = OnPrimary,
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(Icons.Outlined.ShoppingCart, contentDescription = "Keranjang", modifier = Modifier.size(32.dp))
                }
                
                // Badge Notifikasi
                if (cartItemCount > 0) {
                    Box(
                        modifier = Modifier
                            .offset(x = (-4).dp, y = 4.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Error)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cartItemCount.toString(),
                            color = OnError,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // ── Page Header ──
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kasir POS",
                    color = OnSurface,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // 1. Search & Filter
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchChanged,
                    placeholder = { Text("Cari produk, SKU, atau scan barcode...", color = Outline) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Outline) },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        unfocusedBorderColor = OutlineVariant,
                        focusedBorderColor = Primary
                    ),
                    singleLine = true
                )
                CashierCategoryDropdown(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                )
            }

            // 2. Kartu Produk
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.isProductLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    state.productError != null -> ProductErrorState(
                        message = state.productError.message(),
                        onRetry = onRefreshProducts,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    state.products.isEmpty() -> Text(
                        text = "No products found.",
                        color = Outline,
                        fontSize = 16.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    else -> {
                        ProductCardGrid(
                            products = filteredProducts,
                            cartItems = state.cartItems,
                            onProductClick = onProductClick,
                            onUpdateQty = onUpdateQty,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            ProductPaginationBar(
                page = state.productPage,
                totalPages = state.productTotalPages,
                totalItems = state.productTotal,
                limit = state.productLimit,
                isLoading = state.isProductLoading,
                onPrevious = { onProductPageChanged(state.productPage - 1) },
                onNext = { onProductPageChanged(state.productPage + 1) },
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun CashierCategoryDropdown(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = BorderStroke(1.dp, OutlineVariant),
            modifier = Modifier
                .width(220.dp)
                .height(56.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedCategory,
                    color = OnSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Outlined.ExpandMore,
                    contentDescription = "Pilih kategori",
                    tint = Outline,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(220.dp)
                .background(Color.White)
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = category,
                            color = if (category == selectedCategory) Primary else OnSurface,
                            fontWeight = if (category == selectedCategory) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {
                        expanded = false
                        onCategorySelected(category)
                    }
                )
            }
        }
    }
}

@Composable
fun ProductGlassCard(
    product: Product,
    cartQty: Int,
    onAddClick: () -> Unit,
    onDecreaseQty: () -> Unit,
    onIncreaseQty: () -> Unit
) {
    val isOutOfStock = product.stockQty <= BigDecimal.ZERO
    
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.clickable(enabled = !isOutOfStock, onClick = {
            if (cartQty == 0) onAddClick()
        })
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Gambar & Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .background(SurfaceContainerLow, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = product.name.firstOrNull()?.uppercase() ?: "P",
                    color = Primary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp
                )
                
                // Konfigurasi Badge Peringatan Stok
                val badgeColor = if (isOutOfStock) Error else if (product.stockQty <= BigDecimal(5)) SecondaryContainer else Primary
                val badgeBg = if (isOutOfStock) ErrorContainer else if (product.stockQty <= BigDecimal(5)) SecondaryContainer.copy(alpha=0.2f) else Primary.copy(alpha=0.1f)
                val badgeText = if (isOutOfStock) "Out of Stock" else if (product.stockQty <= BigDecimal(5)) "Low Stock: ${product.stockQty.qtyText()}" else "In Stock: ${product.stockQty.qtyText()}"
                
                Surface(
                    color = badgeBg, 
                    shape = RoundedCornerShape(4.dp), 
                    modifier = Modifier.padding(6.dp).align(Alignment.TopStart)
                ) {
                    Text(
                        text = badgeText, 
                        color = badgeColor, 
                        fontSize = 9.sp, 
                        fontWeight = FontWeight.Bold, 
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Teks Informasi
            Text(product.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("SKU: ${product.sku.ifBlank { "-" }}", fontSize = 11.sp, color = OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Area Bawah: Harga & Tombol Kuantitas
            Column {
                Text(product.unitPrice.moneyText(), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Primary)
                Spacer(modifier = Modifier.height(8.dp))
                
                if (cartQty > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceContainerLow, RoundedCornerShape(8.dp))
                            .border(1.dp, OutlineVariant.copy(alpha=0.3f), RoundedCornerShape(8.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDecreaseQty, modifier = Modifier.size(30.dp)) { 
                            Icon(Icons.Outlined.Remove, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp)) 
                        }
                        Text(cartQty.toString(), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = OnSurface)
                        IconButton(onClick = onIncreaseQty, modifier = Modifier.size(30.dp).background(Primary.copy(alpha = 0.1f), RoundedCornerShape(6.dp))) { 
                            Icon(Icons.Outlined.Add, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp)) 
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable(enabled = !isOutOfStock, onClick = onAddClick),
                        color = if (isOutOfStock) SurfaceContainerLow else Primary,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, OutlineVariant.copy(alpha=0.3f))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isOutOfStock) "Habis" else "+ Keranjang",
                                color = if (isOutOfStock) Outline else OnPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CartSection(
    cartItems: List<CartItem>,
    customers: List<Customer>,
    customerSearchQuery: String,
    selectedCustomer: Customer?,
    isCustomerLoading: Boolean,
    customerError: UiText?,
    subtotal: BigDecimal,
    totalDiscount: BigDecimal,
    finalTotal: BigDecimal,
    selectedPaymentMethod: PaymentMethod,
    amountPaidInput: String,
    hasActiveCashSession: Boolean,
    isCashSessionLoading: Boolean,
    isOpeningCashSession: Boolean,
    startingCashInput: String,
    cashSessionError: UiText?,
    isLoading: Boolean,
    onCustomerSearchChanged: (String) -> Unit,
    onSelectCustomer: (Customer) -> Unit,
    onClearCustomer: () -> Unit,
    onUseCustomerName: () -> Unit,
    onRefreshCustomers: () -> Unit,
    onUpdateQty: (String, Int) -> Unit,
    onSelectPayment: (PaymentMethod) -> Unit,
    onAmountPaidChanged: (String) -> Unit,
    onStartingCashChanged: (String) -> Unit,
    onOpenCashSession: () -> Unit,
    onRefreshCashSession: () -> Unit,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.background(Color.White)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.ShoppingCart, contentDescription = null, tint = Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Current Order", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = OnSurface)
            }
            Surface(color = SurfaceContainerLow, shape = RoundedCornerShape(16.dp)) {
                Text(
                    text = "${cartItems.sumOf { it.quantity }} Items", 
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.Bold, 
                    color = OnSurfaceVariant, 
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        HorizontalDivider(color = OutlineVariant.copy(alpha = 0.3f))

        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CustomerPicker(
                    customers = customers,
                    query = customerSearchQuery,
                    selectedCustomer = selectedCustomer,
                    isLoading = isCustomerLoading,
                    error = customerError,
                    onQueryChanged = onCustomerSearchChanged,
                    onSelectCustomer = onSelectCustomer,
                    onClearCustomer = onClearCustomer,
                    onUseCustomerName = onUseCustomerName,
                    onRefreshCustomers = onRefreshCustomers
                )
            }

            if (cartItems.isEmpty()) {
                item {
                    EmptyCartState(modifier = Modifier.fillMaxWidth().height(180.dp))
                }
            }

            items(cartItems, key = { it.cartItemId }) { item ->
                CartItemCard(item, onUpdateQty = onUpdateQty)
            }

            item {
                HorizontalDivider(color = OutlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 8.dp))
            }

            item {
                CashSessionGate(
                    hasActiveCashSession = hasActiveCashSession,
                    isCashSessionLoading = isCashSessionLoading,
                    isOpeningCashSession = isOpeningCashSession,
                    startingCashInput = startingCashInput,
                    error = cashSessionError,
                    onStartingCashChanged = onStartingCashChanged,
                    onOpenCashSession = onOpenCashSession,
                    onRefresh = onRefreshCashSession
                )
            }

            item {
                PaymentMethodPicker(
                    selectedPaymentMethod = selectedPaymentMethod,
                    onSelectPayment = onSelectPayment
                )
            }

            if (selectedPaymentMethod.requiresReceivable()) {
                item {
                    ReceivablePaymentInput(
                        paymentMethod = selectedPaymentMethod,
                        amountPaidInput = amountPaidInput,
                        finalTotal = finalTotal,
                        onAmountPaidChanged = onAmountPaidChanged
                    )
                }
            }
        }

        // Footer
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 16.dp,
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                CalculationRow("Subtotal", subtotal.moneyText())
                if (totalDiscount > BigDecimal.ZERO) {
                    CalculationRow("Discount", "- ${totalDiscount.moneyText()}", color = WarningOrange)
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = OutlineVariant.copy(alpha = 0.5f))

                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Text("Total", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                    Text(finalTotal.moneyText(), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Primary, letterSpacing = (-1).sp)
                }

                Button(
                    onClick = onCheckout,
                    enabled = cartItems.isNotEmpty() && hasActiveCashSession && !isLoading && selectedPaymentMethod.canSubmit(amountPaidInput, finalTotal),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = checkoutButtonText(hasActiveCashSession, selectedPaymentMethod, amountPaidInput, finalTotal),
                            fontSize = 18.sp, 
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
fun CartItemCard(item: CartItem, onUpdateQty: (String, Int) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.productName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                    Text(item.unitPrice.moneyText(), fontSize = 12.sp, color = OnSurfaceVariant)
                }
                IconButton(onClick = { onUpdateQty(item.cartItemId, 0) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Remove", tint = Error, modifier = Modifier.size(16.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                // Quantity Control
                Row(
                    modifier = Modifier.background(SurfaceContainerLow, RoundedCornerShape(8.dp)).border(1.dp, OutlineVariant.copy(alpha=0.3f), RoundedCornerShape(8.dp)).padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { if(item.quantity > 1) onUpdateQty(item.cartItemId, item.quantity - 1) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Outlined.Remove, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp)) }
                    Text(
                        text = item.quantity.toString(),
                        fontWeight = FontWeight.Bold, 
                        fontSize = 14.sp, 
                        textAlign = TextAlign.Center, 
                        color = OnSurface,
                        modifier = Modifier.widthIn(min = 24.dp)
                    )
                    IconButton(onClick = { onUpdateQty(item.cartItemId, item.quantity + 1) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Outlined.Add, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp)) }
                }
                
                // Unit & Price
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text((item.unitPrice * item.quantity.toBigDecimal()).moneyText(), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = OnSurface, modifier = Modifier.widthIn(min = 80.dp), textAlign = TextAlign.End)
                }
            }
        }
    }
}

@Composable
fun CalculationRow(label: String, value: String, color: Color = OnSurfaceVariant) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = color)
        Text(value, fontSize = 14.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerPicker(
    customers: List<Customer>,
    query: String,
    selectedCustomer: Customer?,
    isLoading: Boolean,
    error: UiText?,
    onQueryChanged: (String) -> Unit,
    onSelectCustomer: (Customer) -> Unit,
    onClearCustomer: () -> Unit,
    onUseCustomerName: () -> Unit,
    onRefreshCustomers: () -> Unit
) {
    val trimmedQuery = query.trim()
    val exactMatch = customers.any { customer ->
        customer.name.equals(trimmedQuery, ignoreCase = true)
    }

    Surface(
        color = SurfaceContainerLow,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Nama Pelanggan", color = OnSurface, fontWeight = FontWeight.Bold)
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                }
            }

            if (selectedCustomer == null) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChanged,
                    placeholder = { Text("Ketik nama pelanggan...", color = Outline) },
                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = Outline) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        unfocusedBorderColor = OutlineVariant,
                        focusedBorderColor = Primary
                    )
                )

                if (trimmedQuery.isNotBlank() && !exactMatch) {
                    Text(
                        text = "Hanya dipakai di nota transaksi.",
                        color = Outline,
                        fontSize = 12.sp
                    )
                }

                error?.let {
                    Text(it.message(), color = Error, fontSize = 12.sp)
                }

                customers
                    .filter(Customer::isActive)
                    .take(3)
                    .forEach { customer ->
                        CustomerSuggestionRow(
                            customer = customer,
                            onClick = { onSelectCustomer(customer) }
                        )
                    }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onUseCustomerName,
                        enabled = trimmedQuery.isNotBlank() && !isLoading && !exactMatch,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Pakai nama ini", fontWeight = FontWeight.Bold, color = Primary)
                    }
                    IconButton(
                        onClick = onRefreshCustomers,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Muat ulang", tint = Primary)
                    }
                }
            } else {
                SelectedCustomerCard(
                    customer = selectedCustomer,
                    onClear = onClearCustomer
                )
            }
        }
    }
}

@Composable
private fun CustomerSuggestionRow(
    customer: Customer,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(PrimaryContainer.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Person, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(customer.name, color = OnSurface, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(
                text = customer.phone?.takeIf(String::isNotBlank) ?: "Tersimpan",
                color = OnSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SelectedCustomerCard(
    customer: Customer,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(PrimaryContainer.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Person, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(customer.name, color = OnSurface, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(
                text = customer.phone?.takeIf(String::isNotBlank) ?: "Dipakai untuk nota",
                color = Primary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onClear) {
            Icon(Icons.Outlined.Close, contentDescription = "Hapus", tint = OnSurfaceVariant)
        }
    }
}

@Composable
private fun CashSessionGate(
    hasActiveCashSession: Boolean,
    isCashSessionLoading: Boolean,
    isOpeningCashSession: Boolean,
    startingCashInput: String,
    error: UiText?,
    onStartingCashChanged: (String) -> Unit,
    onOpenCashSession: () -> Unit,
    onRefresh: () -> Unit
) {
    Surface(
        color = if (hasActiveCashSession) Primary.copy(alpha = 0.1f) else SurfaceBright,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (hasActiveCashSession) Primary.copy(alpha = 0.2f) else OutlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Primary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Wallet, contentDescription = null, tint = Primary)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (hasActiveCashSession) "Sesi kasir aktif" else "Sesi kasir ditutup",
                            color = OnSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                IconButton(
                    onClick = onRefresh,
                    enabled = !isCashSessionLoading,
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color.White)
                ) {
                    if (isCashSessionLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Cek sesi", tint = Primary)
                    }
                }
            }

            if (!hasActiveCashSession) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = startingCashInput,
                        onValueChange = onStartingCashChanged,
                        placeholder = { Text("Modal awal", color = Outline, fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).height(48.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = OutlineVariant,
                            focusedBorderColor = Primary
                        )
                    )
                    Button(
                        onClick = onOpenCashSession,
                        enabled = startingCashInput.isNotBlank() && !isOpeningCashSession && !isCashSessionLoading,
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        if (isOpeningCashSession) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Text("Buka Sesi", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                error?.let {
                    Text(it.message(), color = Error, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodPicker(
    selectedPaymentMethod: PaymentMethod,
    onSelectPayment: (PaymentMethod) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Payment Method", color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(PaymentMethod.entries.toList()) { method ->
                PaymentMethodCard(
                    method = method,
                    isSelected = selectedPaymentMethod == method,
                    onClick = { onSelectPayment(method) }
                )
            }
        }
    }
}

@Composable
private fun PaymentMethodCard(
    method: PaymentMethod,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = if (isSelected) Primary else OnSurfaceVariant
    val icon = when (method) {
        PaymentMethod.TUNAI -> Icons.Outlined.Payments
        PaymentMethod.TRANSFER -> Icons.Outlined.Wallet
        PaymentMethod.QRIS -> Icons.Outlined.QrCodeScanner
        PaymentMethod.HUTANG -> Icons.Outlined.History
        PaymentMethod.DP -> Icons.Outlined.PointOfSale
    }

    Surface(
        modifier = modifier.height(64.dp).width(100.dp),
        color = if (isSelected) Primary.copy(alpha = 0.1f) else Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isSelected) Primary else OutlineVariant.copy(alpha = 0.5f)),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Text(
                text = method.displayName(),
                color = tint,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ReceivablePaymentInput(
    paymentMethod: PaymentMethod,
    amountPaidInput: String,
    finalTotal: BigDecimal,
    onAmountPaidChanged: (String) -> Unit
) {
    Surface(
        color = SurfaceBright,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (paymentMethod == PaymentMethod.HUTANG) "Hutang Pelanggan" else "Nominal DP",
                color = OnSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            if (paymentMethod == PaymentMethod.DP) {
                OutlinedTextField(
                    value = amountPaidInput,
                    onValueChange = onAmountPaidChanged,
                    placeholder = { Text("Masukkan DP", color = Outline) },
                    leadingIcon = { Text("Rp", color = Outline, fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        unfocusedBorderColor = OutlineVariant,
                        focusedBorderColor = Primary
                    )
                )
            }
        }
    }
}

@Composable
private fun EmptyCartState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.ShoppingCart, contentDescription = null, tint = Outline, modifier = Modifier.size(48.dp))
            Text("Keranjang Kosong", color = Outline, fontWeight = FontWeight.SemiBold)
            Text("Silakan tambah produk", color = Outline, fontSize = 12.sp)
        }
    }
}

private fun checkoutButtonText(
    hasActiveCashSession: Boolean,
    paymentMethod: PaymentMethod,
    amountPaidInput: String,
    finalTotal: BigDecimal
): String {
    if (!hasActiveCashSession) return "Buka Sesi Kasir"
    if (!paymentMethod.canSubmit(amountPaidInput, finalTotal)) return "Data Belum Lengkap"
    return "Proses Bayar"
}

private fun BigDecimal.moneyText(): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"))
    formatter.maximumFractionDigits = 0
    return formatter.format(this)
}

private fun BigDecimal.qtyText(): String {
    return stripTrailingZeros().toPlainString()
}

private fun PaymentMethod.requiresReceivable(): Boolean {
    return this == PaymentMethod.HUTANG || this == PaymentMethod.DP
}

private fun PaymentMethod.paidAmount(amountPaidInput: String, finalTotal: BigDecimal): BigDecimal {
    return when (this) {
        PaymentMethod.HUTANG -> BigDecimal.ZERO
        PaymentMethod.DP -> amountPaidInput.toBigDecimalOrNull() ?: BigDecimal.ZERO
        else -> finalTotal
    }
}

private fun PaymentMethod.outstandingAmount(amountPaidInput: String, finalTotal: BigDecimal): BigDecimal {
    return finalTotal.subtract(paidAmount(amountPaidInput, finalTotal)).coerceAtLeast(BigDecimal.ZERO)
}

private fun PaymentMethod.canSubmit(amountPaidInput: String, finalTotal: BigDecimal): Boolean {
    val paid = paidAmount(amountPaidInput, finalTotal)
    return when (this) {
        PaymentMethod.HUTANG -> finalTotal > BigDecimal.ZERO
        PaymentMethod.DP -> paid > BigDecimal.ZERO && paid < finalTotal
        else -> true
    }
}

private fun UiText.message(): String {
    return when (this) {
        is UiText.DynamicString -> value
    }
}
