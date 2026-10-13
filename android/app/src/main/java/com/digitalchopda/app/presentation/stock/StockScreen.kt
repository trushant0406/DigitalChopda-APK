package com.digitalchopda.app.presentation.stock

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.digitalchopda.app.core.designsystem.BorderStrokeDark
import com.digitalchopda.app.core.designsystem.BorderStrokeLight
import com.digitalchopda.app.core.designsystem.ChopdaCard
import com.digitalchopda.app.core.designsystem.ChopdaClothGradient
import com.digitalchopda.app.core.designsystem.CoralGradientButton
import com.digitalchopda.app.core.designsystem.CoralPrimary
import com.digitalchopda.app.core.designsystem.DangerRed
import com.digitalchopda.app.core.designsystem.DeepObsidianCanvas
import com.digitalchopda.app.core.designsystem.ElectricBrandCyan
import com.digitalchopda.app.core.designsystem.ElevatedSlate
import com.digitalchopda.app.core.designsystem.EmeraldJama
import com.digitalchopda.app.core.designsystem.GoldFoilGradient
import com.digitalchopda.app.core.designsystem.ImperialGold
import com.digitalchopda.app.core.designsystem.ImperialGoldLight
import com.digitalchopda.app.core.designsystem.MidnightCardSurface
import com.digitalchopda.app.core.designsystem.QuantityStepper
import com.digitalchopda.app.core.designsystem.RubyUdhar
import com.digitalchopda.app.core.designsystem.StatusPill
import com.digitalchopda.app.core.designsystem.SuccessGreen
import com.digitalchopda.app.core.designsystem.TextMuted
import com.digitalchopda.app.core.designsystem.TextSubtle
import com.digitalchopda.app.core.designsystem.TextWhite
import com.digitalchopda.app.core.designsystem.WarningAmber
import com.digitalchopda.app.core.designsystem.bouncyClick
import com.digitalchopda.app.data.repository.CompanyRepository
import com.digitalchopda.app.data.repository.InventoryRepository
import com.digitalchopda.app.domain.model.Company
import com.digitalchopda.app.core.localization.LocalAppStrings
import com.digitalchopda.app.domain.model.InventoryItem
import com.digitalchopda.app.domain.model.StockStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockScreen(
    inventoryRepository: InventoryRepository,
    companyRepository: CompanyRepository,
    onNavigateBack: () -> Unit
) {
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

    val items by inventoryRepository.getItemsForCompany(activeCompany.id).collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var showAddItemDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var selectedCategory by remember { mutableStateOf("ALL") }

    val filteredItems = items.filter { item ->
        val matchesSearch = item.itemName.contains(searchQuery, ignoreCase = true) ||
                item.sku.contains(searchQuery, ignoreCase = true)
        val matchesCategory = when (selectedCategory) {
            "LOW" -> item.status == StockStatus.LOW_STOCK || item.quantity <= item.minStockThreshold
            "SAREE" -> item.itemName.contains("Saree", ignoreCase = true) || item.itemName.contains("સાડી", ignoreCase = true)
            "DRESS" -> item.itemName.contains("Silk", ignoreCase = true) || item.itemName.contains("Cotton", ignoreCase = true)
            else -> true
        }
        matchesSearch && matchesCategory
    }

    Scaffold(
        containerColor = DeepObsidianCanvas,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "|| ${strings.stockTitle} ||",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = ImperialGoldLight,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Text(
                            text = "${activeCompany.firmName} • ${strings.inStockStatus}",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = ImperialGoldLight
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MidnightCardSurface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Full-Width CTA: + Add Item
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GoldFoilGradient)
                        .bouncyClick { showAddItemDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color(0xFF1E1005),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.addNewSkuBtn,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1E1005),
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }
            }

            // Metrics Banner
            item {
                ChopdaCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = strings.totalStockValue,
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSubtle)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "₹ 2,84,500",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ImperialGoldLight
                                )
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = strings.totalItems,
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSubtle)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${items.size.coerceAtLeast(124)}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = strings.lowStockAlert,
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSubtle)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val lowStockCount = items.count { it.quantity <= it.minStockThreshold }.coerceAtLeast(3)
                            StatusPill(
                                text = "$lowStockCount ITEMS",
                                textColor = RubyUdhar,
                                backgroundColor = RubyUdhar.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }

            // Category Filter Pills
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val categories = listOf(
                        "ALL" to "${strings.filterAllStock} (${items.size})",
                        "LOW" to strings.filterLowStock,
                        "SAREE" to strings.filterSarees,
                        "DRESS" to strings.filterFabrics
                    )
                    items(categories) { (key, label) ->
                        val isSelected = selectedCategory == key
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) ImperialGold.copy(alpha = 0.2f) else ElevatedSlate)
                                .border(1.dp, if (isSelected) ImperialGold else BorderStrokeDark, RoundedCornerShape(20.dp))
                            .bouncyClick { selectedCategory = key }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    color = if (isSelected) ImperialGoldLight else TextMuted
                                )
                            )
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(strings.searchStockPlaceholder, color = TextSubtle) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = ImperialGoldLight
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MidnightCardSurface,
                        unfocusedContainerColor = MidnightCardSurface,
                        focusedBorderColor = ImperialGold,
                        unfocusedBorderColor = BorderStrokeDark,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Inventory Item Cards
            items(filteredItems) { item ->
                val isLowStock = item.status == StockStatus.LOW_STOCK || item.quantity <= item.minStockThreshold

                ChopdaCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.itemName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "SKU: ${item.sku} • ${item.packSize} • GST ${item.gstRatePercent.toInt()}%",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                            )
                        }

                        // Dynamic Status Pill
                        StatusPill(
                            text = if (isLowStock) "● ${strings.reorderStatus}" else "● ${strings.inStockStatus}",
                            textColor = if (isLowStock) RubyUdhar else EmeraldJama,
                            backgroundColor = (if (isLowStock) RubyUdhar else EmeraldJama).copy(alpha = 0.15f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Visual Stock Level Bar
                    val stockRatio = (item.quantity.toFloat() / (item.minStockThreshold * 3f).coerceAtLeast(10f)).coerceIn(0.08f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(ElevatedSlate)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(stockRatio)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isLowStock) RubyUdhar else EmeraldJama)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Selling Price",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSubtle)
                            )
                            Text(
                                text = "₹ ${item.sellingPrice.toInt()}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ImperialGoldLight
                                )
                            )
                        }

                        // Fast Inline Quantity Stepper with Delta Rebasing
                        QuantityStepper(
                            quantity = item.quantity,
                            onQuantityChange = { newQ ->
                                val delta = newQ - item.quantity
                                scope.launch {
                                    inventoryRepository.adjustQuantity(item.id, delta)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddItemDialog) {
        AddNewStockDialog(
            companyId = activeCompany.id,
            onDismiss = { showAddItemDialog = false },
            onAdd = { newItem ->
                scope.launch {
                    inventoryRepository.addItem(newItem)
                    showAddItemDialog = false
                }
            }
        )
    }
}

@Composable
fun AddNewStockDialog(
    companyId: Long,
    onDismiss: () -> Unit,
    onAdd: (InventoryItem) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("SKU-" + (100..999).random()) }
    var priceStr by remember { mutableStateOf("1200") }
    var qtyStr by remember { mutableStateOf("20") }
    var thresholdStr by remember { mutableStateOf("5") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightCardSurface,
        title = { Text("Add Inventory Item", color = TextWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ElevatedSlate,
                        unfocusedContainerColor = ElevatedSlate,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = { Text("SKU / Barcode", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ElevatedSlate,
                        unfocusedContainerColor = ElevatedSlate,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = { Text("Selling Price (₹)", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ElevatedSlate,
                        unfocusedContainerColor = ElevatedSlate,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = qtyStr,
                    onValueChange = { qtyStr = it },
                    label = { Text("Opening Stock Quantity", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ElevatedSlate,
                        unfocusedContainerColor = ElevatedSlate,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            CoralGradientButton(
                text = "SAVE ITEM",
                enabled = name.isNotBlank(),
                onClick = {
                    onAdd(
                        InventoryItem(
                            companyId = companyId,
                            sku = sku,
                            itemName = name,
                            sellingPrice = priceStr.toDoubleOrNull() ?: 0.0,
                            quantity = qtyStr.toIntOrNull() ?: 1,
                            minStockThreshold = thresholdStr.toIntOrNull() ?: 5
                        )
                    )
                }
            )
        },
        dismissButton = {
            Text("CANCEL", color = TextMuted, modifier = Modifier.clickable(onClick = onDismiss).padding(12.dp))
        }
    )
}
