package com.digitalchopda.app.presentation.products

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalchopda.app.core.designsystem.BorderStrokeDark
import com.digitalchopda.app.core.designsystem.BorderStrokeLight
import com.digitalchopda.app.core.designsystem.BrandAccentSoft
import com.digitalchopda.app.core.designsystem.BrandPrimaryBlue
import com.digitalchopda.app.core.designsystem.ChopdaCard
import com.digitalchopda.app.core.designsystem.ChopdaModalDialog
import com.digitalchopda.app.core.designsystem.DangerRed
import com.digitalchopda.app.core.designsystem.DarkNavy
import com.digitalchopda.app.core.designsystem.DeepObsidianCanvas
import com.digitalchopda.app.core.designsystem.ElevatedSlate
import com.digitalchopda.app.core.designsystem.EmeraldJama
import com.digitalchopda.app.core.designsystem.EmeraldJamaBg
import com.digitalchopda.app.core.designsystem.ImperialGold
import com.digitalchopda.app.core.designsystem.MidnightCardSurface
import com.digitalchopda.app.core.designsystem.RubyUdhar
import com.digitalchopda.app.core.designsystem.StatusPill
import com.digitalchopda.app.core.designsystem.TextMuted
import com.digitalchopda.app.core.designsystem.TextSubtle
import com.digitalchopda.app.core.designsystem.TextWhite
import com.digitalchopda.app.core.designsystem.bouncyClick
import com.digitalchopda.app.core.localization.LocalAppStrings
import com.digitalchopda.app.data.repository.CompanyRepository
import com.digitalchopda.app.data.repository.ProductRepository
import com.digitalchopda.app.domain.model.Company
import com.digitalchopda.app.domain.model.Product
import com.digitalchopda.app.util.CurrencyFormatter
import kotlinx.coroutines.launch

/**
 * 🛍️ Products Catalog Screen (Replaces legacy Stock feature)
 * Direct parity with Web Platform's /api/products and ProductsPage.jsx
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    productRepository: ProductRepository,
    companyRepository: CompanyRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = LocalAppStrings.current

    var activeCompany by remember {
        mutableStateOf(
            Company(
                id = 1L,
                firmName = "Sharma Traders",
                ownerName = "Amit Sharma",
                gstin = "24AAACS1429B1Z8",
                phone = "+91 98765 43210",
                address = "Surat, Gujarat",
                isActive = true
            )
        )
    }

    LaunchedEffect(Unit) {
        activeCompany = companyRepository.getActiveCompany()
    }

    var isSyncing by remember { mutableStateOf(false) }

    fun triggerSync() {
        scope.launch {
            isSyncing = true
            val res = productRepository.syncProductsWithServer(activeCompany.id)
            isSyncing = false
            if (res.isSuccess) {
                Toast.makeText(context, "✓ Products synced with server (${res.getOrNull()} items)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(activeCompany.id) {
        if (activeCompany.id > 0) {
            triggerSync()
        }
    }

    val products by productRepository.getProducts(activeCompany.id).collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var selectedGstFilter by remember { mutableStateOf<Double?>(null) } // null = All
    var stockFilter by remember { mutableStateOf("ALL") } // "ALL", "LOW_STOCK", "OUT_OF_STOCK"

    var showAddDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }
    var productToAdjustStock by remember { mutableStateOf<Product?>(null) }

    val filteredProducts = remember(products, searchQuery, selectedGstFilter, stockFilter) {
        products.filter { p ->
            val matchesSearch = searchQuery.isBlank() ||
                    p.name.contains(searchQuery, ignoreCase = true) ||
                    (p.hsnCode?.contains(searchQuery, ignoreCase = true) == true)
            val matchesGst = selectedGstFilter == null || p.gstRate == selectedGstFilter
            val matchesStock = when (stockFilter) {
                "LOW_STOCK" -> p.stockQuantity <= p.minStockAlert && p.stockQuantity > 0.0
                "OUT_OF_STOCK" -> p.stockQuantity <= 0.0
                else -> true
            }
            matchesSearch && matchesGst && matchesStock
        }
    }

    val displayProducts = filteredProducts

    // Dual Valuation Metrics (Retail / Cost Parity)
    val totalRetailValuation = remember(products) { products.sumOf { it.stockQuantity * it.defaultPrice } }
    val totalCostValuation = remember(products) { products.sumOf { it.stockQuantity * it.purchasePrice } }
    val totalStockUnits = remember(products) { products.sumOf { it.stockQuantity } }
    val potentialMargin = totalRetailValuation - totalCostValuation

    Scaffold(
        containerColor = DeepObsidianCanvas,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = strings.productsTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = DarkNavy,
                                letterSpacing = 0.5.sp,
                                fontSize = 16.sp
                            )
                        )
                        Text(
                            text = "${activeCompany.firmName.ifBlank { "ABC E-COMMERCE" }} • Catalog",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { triggerSync() },
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                color = BrandPrimaryBlue,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync Products",
                                tint = DarkNavy
                            )
                        }
                    }

                    // Total Products Count Badge
                    Row(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MidnightCardSurface)
                            .border(1.dp, BorderStrokeDark, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = BrandPrimaryBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${displayProducts.size} Items",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = DarkNavy,
                                fontSize = 11.sp
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightCardSurface)
            )
            HorizontalDivider(color = BorderStrokeDark, thickness = 0.8.dp)
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = BrandPrimaryBlue,
                contentColor = Color.White,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Product",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                },
                text = {
                    Text(
                        text = "Add Product",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.5.sp
                        )
                    )
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .semantics(mergeDescendants = true) { contentDescription = "Add Product" }
                    .padding(bottom = 8.dp)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ─── DUAL VALUATION BENTO CARD ───
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MidnightCardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStrokeDark)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(BrandPrimaryBlue.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingBag,
                                        contentDescription = null,
                                        tint = BrandPrimaryBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "STOCK VALUATION PARITY",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            color = TextSubtle,
                                            letterSpacing = 0.8.sp,
                                            fontSize = 10.5.sp
                                        )
                                    )
                                    Text(
                                        text = "${totalStockUnits.toInt()} Units • ${products.size} Catalog Items",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(EmeraldJamaBg)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Margin: ${CurrencyFormatter.formatINR(potentialMargin)}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldJama,
                                        fontSize = 10.5.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = ElevatedSlate, thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Retail Valuation
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Retail / Selling Value",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextSubtle,
                                        fontSize = 11.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = CurrencyFormatter.formatINR(totalRetailValuation),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = BrandPrimaryBlue,
                                        fontSize = 16.sp
                                    )
                                )
                            }

                            // Divider
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(36.dp)
                                    .background(BorderStrokeDark)
                            )

                            // Cost Valuation
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 14.dp)
                            ) {
                                Text(
                                    text = "Asset / Cost Value",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextSubtle,
                                        fontSize = 11.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = CurrencyFormatter.formatINR(totalCostValuation),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = ImperialGold,
                                        fontSize = 16.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(strings.searchProductsPlaceholder, color = TextSubtle, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TextSubtle,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandPrimaryBlue,
                        unfocusedBorderColor = BorderStrokeDark,
                        focusedContainerColor = MidnightCardSurface,
                        unfocusedContainerColor = MidnightCardSurface,
                        focusedTextColor = DarkNavy,
                        unfocusedTextColor = DarkNavy
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            // Stock Availability Filter Chips
            item {
                val stockChips = listOf(
                    Pair("All Items", "ALL"),
                    Pair("⚠️ Low Stock", "LOW_STOCK"),
                    Pair("❌ Out of Stock", "OUT_OF_STOCK")
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(stockChips) { (label, key) ->
                        val isSelected = stockFilter == key
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) BrandPrimaryBlue else MidnightCardSurface)
                                .border(
                                    1.dp,
                                    if (isSelected) BrandPrimaryBlue else BorderStrokeDark,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { stockFilter = key }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isSelected) Color.White else DarkNavy,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }

            // GST Rate Filter Chips
            item {
                val gstChips = listOf(
                    Pair("All Slabs", null),
                    Pair("0% GST", 0.0),
                    Pair("5% GST", 5.0),
                    Pair("12% GST", 12.0),
                    Pair("18% GST", 18.0),
                    Pair("28% GST", 28.0)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(gstChips) { (label, rate) ->
                        val isSelected = selectedGstFilter == rate
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) BrandPrimaryBlue else MidnightCardSurface)
                                .border(
                                    1.dp,
                                    if (isSelected) BrandPrimaryBlue else BorderStrokeDark,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedGstFilter = rate }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isSelected) Color.White else DarkNavy,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }

            // Empty state
            if (displayProducts.isEmpty()) {
                item {
                    ChopdaCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        cornerRadius = 16.dp,
                        borderColor = BorderStrokeDark,
                        backgroundColor = MidnightCardSurface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = TextSubtle,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = strings.noProductsText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextMuted,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                ),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandPrimaryBlue,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(strings.addProductBtn, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Products Catalog List
            items(
                items = displayProducts,
                key = { it.id }
            ) { product ->
                ChopdaCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 14.dp,
                    borderColor = BorderStrokeDark,
                    backgroundColor = MidnightCardSurface,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Product Icon / Initial Avatar
                        val avatarColors = remember(product.name) {
                            val hash = product.name.hashCode()
                            when (Math.abs(hash) % 4) {
                                0 -> Pair(Color(0xFFEFF6FF), BrandPrimaryBlue)
                                1 -> Pair(Color(0xFFECFDF5), EmeraldJama)
                                2 -> Pair(Color(0xFFFFF7ED), Color(0xFFEA580C))
                                else -> Pair(Color(0xFFF5F3FF), Color(0xFF7C3AED))
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(avatarColors.first)
                                .border(1.dp, avatarColors.second.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = product.name.trim().take(2).uppercase().ifBlank { "PR" },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = avatarColors.second,
                                    fontSize = 13.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Product Title, Price & Responsive Metadata
                        Column(modifier = Modifier.weight(1f)) {
                            // Row 1: Product Name & Formatted Price
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = product.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = DarkNavy,
                                        fontSize = 15.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = CurrencyFormatter.formatCurrencyExact(product.defaultPrice),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = DarkNavy,
                                        fontSize = 15.sp
                                    ),
                                    maxLines = 1
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Row 2: Badges (GST, Stock, HSN) and Unit Pricing
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    if (!product.hsnCode.isNullOrBlank()) {
                                        StatusPill(
                                            text = "HSN: ${product.hsnCode}",
                                            textColor = BrandPrimaryBlue,
                                            backgroundColor = BrandAccentSoft
                                        )
                                    }
                                    StatusPill(
                                        text = "${product.gstRate.toInt()}% GST",
                                        textColor = if (product.gstRate >= 18.0) RubyUdhar else EmeraldJama,
                                        backgroundColor = if (product.gstRate >= 18.0) RubyUdhar.copy(alpha = 0.12f) else EmeraldJama.copy(alpha = 0.12f)
                                    )
                                    if (product.stockQuantity <= 0.0) {
                                        StatusPill(
                                            text = "Out of Stock",
                                            textColor = RubyUdhar,
                                            backgroundColor = RubyUdhar.copy(alpha = 0.12f)
                                        )
                                    } else if (product.stockQuantity <= product.minStockAlert) {
                                        StatusPill(
                                            text = "Low: ${product.stockQuantity.toInt()}",
                                            textColor = Color(0xFFD97706),
                                            backgroundColor = Color(0xFFFEF3C7)
                                        )
                                    } else {
                                        StatusPill(
                                            text = "${product.stockQuantity.toInt()} in stock",
                                            textColor = EmeraldJama,
                                            backgroundColor = EmeraldJama.copy(alpha = 0.12f)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "per ${product.unit.lowercase()}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextSubtle,
                                        fontSize = 10.sp
                                    ),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = BorderStrokeDark.copy(alpha = 0.7f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(6.dp))

                    // Actions Row: Item Code & Tactile Pill Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Item Code: #${product.id}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSubtle,
                                fontSize = 11.sp
                            )
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Adjust Stock Pill Button
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(EmeraldJama.copy(alpha = 0.12f))
                                    .border(1.dp, EmeraldJama.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .clickable { productToAdjustStock = product }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📦 Stock",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = EmeraldJama,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                            }

                            // Edit Pill Button
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BrandAccentSoft)
                                    .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(8.dp))
                                    .clickable { productToEdit = product }
                                    .padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = BrandPrimaryBlue,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Edit",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = BrandPrimaryBlue,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                            }

                            // Delete Pill Button
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFEF2F2))
                                    .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(8.dp))
                                    .clickable { productToDelete = product }
                                    .padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = DangerRed,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Delete",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = DangerRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Product Modal
    if (showAddDialog) {
        AddEditProductDialog(
            companyId = activeCompany.id,
            onDismiss = { showAddDialog = false },
            onSave = { newProd ->
                scope.launch {
                    val result = productRepository.addProduct(newProd)
                    result.fold(
                        onSuccess = {
                            Toast.makeText(context, "Product added to catalog", Toast.LENGTH_SHORT).show()
                            showAddDialog = false
                        },
                        onFailure = { err ->
                            Toast.makeText(context, err.message ?: "Failed to add product", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }
        )
    }

    // Edit Product Modal
    if (productToEdit != null) {
        AddEditProductDialog(
            companyId = activeCompany.id,
            product = productToEdit,
            onDismiss = { productToEdit = null },
            onSave = { updatedProd ->
                scope.launch {
                    val result = productRepository.updateProduct(updatedProd)
                    result.fold(
                        onSuccess = {
                            Toast.makeText(context, "Product updated", Toast.LENGTH_SHORT).show()
                            productToEdit = null
                        },
                        onFailure = { err ->
                            Toast.makeText(context, err.message ?: "Failed to update product", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (productToDelete != null) {
        val prod = productToDelete!!
        ChopdaModalDialog(
            onDismissRequest = { productToDelete = null },
            title = "Delete Product?",
            subtitle = "Permanent catalog removal",
            icon = Icons.Default.Delete,
            iconTint = DangerRed,
            widthFraction = 0.92f,
            maxHeightFraction = 0.40f,
            scrollable = false,
            buttons = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { productToDelete = null },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderStrokeDark)
                    ) {
                        Text("CANCEL", color = TextMuted, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                productRepository.deleteProduct(prod.id)
                                Toast.makeText(context, "Product deleted", Toast.LENGTH_SHORT).show()
                                productToDelete = null
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                    ) {
                        Text("DELETE", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        ) {
            Text(
                text = "Are you sure you want to remove \"${prod.name}\" from the catalog? Past tax invoices and quotations will keep their own records.",
                color = TextMuted,
                fontSize = 13.5.sp,
                lineHeight = 20.sp
            )
        }
    }

    if (productToAdjustStock != null) {
        AdjustStockDialog(
            product = productToAdjustStock!!,
            productRepository = productRepository,
            onDismiss = { productToAdjustStock = null },
            onAdjusted = { productToAdjustStock = null }
        )
    }
}
