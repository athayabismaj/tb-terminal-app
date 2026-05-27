package com.tbterminal.app.ui.checkout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tbterminal.app.ui.common.UiText
import java.math.BigDecimal
import kotlinx.coroutines.flow.filterNotNull

@Composable
fun CheckoutScreen(
    viewModel: CheckoutViewModel,
    onNavigateToReceipt: (receiptId: String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val onProductClick = remember(viewModel) {
        { product: Product -> viewModel.addProduct(product) }
    }
    val onUpdateQty = remember(viewModel) {
        { cartItemId: String, quantity: Int ->
            viewModel.updateCartQuantity(cartItemId, quantity)
        }
    }
    val onCheckout = remember(viewModel, uiState.finalTotal) {
        {
            viewModel.submitCheckout(
                paymentMethod = PaymentMethod.TUNAI,
                amountPaid = uiState.finalTotal
            )
        }
    }

    LaunchedEffect(viewModel.errorEvent, snackbarHostState) {
        viewModel.errorEvent
            .filterNotNull()
            .collect { error ->
                snackbarHostState.showSnackbar(error.message())
                viewModel.clearErrorEvent()
            }
    }

    LaunchedEffect(viewModel.checkoutEvents, onNavigateToReceipt) {
        viewModel.checkoutEvents.collect { event ->
            when (event) {
                is CheckoutEvent.NavigateToReceipt -> onNavigateToReceipt(event.transactionId)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { contentPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.65f)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Produk",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                ProductGrid(
                    products = uiState.products,
                    onProductClick = onProductClick,
                    modifier = Modifier.fillMaxSize()
                )
            }

            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 12.dp)
            )

            CartPanel(
                cartItems = uiState.cartItems,
                subtotal = uiState.subtotal,
                totalDiscount = uiState.totalDiscount,
                finalTotal = uiState.finalTotal,
                onUpdateQty = onUpdateQty,
                onCheckout = onCheckout,
                isCheckoutLoading = uiState.isLoading,
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.35f)
            )
        }
    }
}

@Composable
fun ProductGrid(
    products: List<Product>,
    onProductClick: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = products,
            key = { product -> product.productId }
        ) { product ->
            ProductTile(
                product = product,
                onClick = { onProductClick(product) }
            )
        }
    }
}

@Composable
fun CartPanel(
    cartItems: List<CartItem>,
    subtotal: BigDecimal,
    totalDiscount: BigDecimal,
    finalTotal: BigDecimal,
    onUpdateQty: (String, Int) -> Unit,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier,
    isCheckoutLoading: Boolean = false
) {
    Column(
        modifier = modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Keranjang",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                items = cartItems,
                key = { item -> item.cartItemId }
            ) { item ->
                CartLine(
                    item = item,
                    onUpdateQty = onUpdateQty
                )
            }
        }

        HorizontalDivider()

        AmountRow(label = "Subtotal", amount = subtotal)
        AmountRow(label = "Diskon", amount = totalDiscount)
        AmountRow(
            label = "Total",
            amount = finalTotal,
            emphasize = true
        )

        Button(
            onClick = onCheckout,
            enabled = cartItems.isNotEmpty() && !isCheckoutLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            if (isCheckoutLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Checkout")
            }
        }
    }
}

@Composable
private fun ProductTile(
    product: Product,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = product.unitPrice.moneyText(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun CartLine(
    item: CartItem,
    onUpdateQty: (String, Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.productName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.unitPrice.moneyText(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = { onUpdateQty(item.cartItemId, item.quantity - 1) }
                ) {
                    Text(
                        text = "-",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                Box(
                    modifier = Modifier.size(width = 36.dp, height = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.quantity.toString(),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                IconButton(
                    onClick = { onUpdateQty(item.cartItemId, item.quantity + 1) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tambah jumlah"
                    )
                }
            }
        }
    }
}

@Composable
private fun AmountRow(
    label: String,
    amount: BigDecimal,
    emphasize: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (emphasize) {
                MaterialTheme.typography.titleMedium
            } else {
                MaterialTheme.typography.bodyLarge
            }
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = amount.moneyText(),
            style = if (emphasize) {
                MaterialTheme.typography.titleLarge
            } else {
                MaterialTheme.typography.bodyLarge
            },
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private fun BigDecimal.moneyText(): String {
    return "Rp ${stripTrailingZeros().toPlainString()}"
}

private fun UiText.message(): String {
    return when (this) {
        is UiText.DynamicString -> value
    }
}
