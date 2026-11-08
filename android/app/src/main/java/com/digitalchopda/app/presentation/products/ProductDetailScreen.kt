package com.digitalchopda.app.presentation.products

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalchopda.app.core.designsystem.BorderStrokeDark
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
import com.digitalchopda.app.core.designsystem.WhatsAppGreen
import com.digitalchopda.app.core.designsystem.bouncyClick
import com.digitalchopda.app.core.document.UniversalShareEngine
import com.digitalchopda.app.data.repository.CompanyRepository
import com.digitalchopda.app.data.repository.ProductRepository
import com.digitalchopda.app.domain.model.Company
import com.digitalchopda.app.domain.model.Product
import com.digitalchopda.app.domain.model.ProductCustomerPurchase
import com.digitalchopda.app.domain.model.ProductSupplierReceipt
import com.digitalchopda.app.domain.model.StockAdjustment
import com.digitalchopda.app.util.CurrencyFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 📦 360° Unified Product Detail Hub
 *
 * Provides complete inventory transparency:
 * 1. Live Stock Vault & Dual Valuation (Cost vs Retail & Margin)
 * 2. Instant Tactile Action Bar (Adjust Stock, New Invoice, Inward Purchase, WhatsApp Share)
 * 3. 4 Organized Perspectives:
 *    - 📜 Stock Movement Timeline (Passbook with running balance)
 *    - 👥 Customer Breakdown ("Who Purchased How Much")
 *    - 🏭 Supplier Inward Receipts
 *    - 📋 Specs & Online Store Configuration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: Long,
    productRepository: ProductRepository,
    companyRepository: CompanyRepository,
    onNavigateBack: () -> Unit,
    onNavigateToCreateInvoice: (productId: Long, productName: String) -> Unit = { _, _ -> },
    onNavigateToDocumentDetail: (docType: String, docId: Long) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val activeCompanyFlowVal by companyRepository.getActiveCompanyFlow().collectAsState(initial = null)
    val activeCompany = activeCompanyFlowVal ?: remember {
        Company(id = 1L, firmName = "My Business", ownerName = "Admin User", isActive = true)
    }

    val product by productRepository.observeProductById(productId).collectAsState(initial = null)

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAdjustStockDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val stockHistory by productRepository.getStockHistory(activeCompany.id, productId)
        .collectAsState(initial = emptyList())

    val customerPurchases by productRepository.getProductCustomerPurchases(activeCompany.id, product?.name ?: "")
        .collectAsState(initial = emptyList())

    val supplierReceipts by productRepository.getProductSupplierReceipts(activeCompany.id, product?.name ?: "")
        .collectAsState(initial = emptyList())

    fun shareProductWhatsApp(p: Product) {
        val stockTxt = if (p.stockQuantity > 0.0) "\nStock: ${p.stockQuantity.toInt()} ${p.unit.lowercase()}" else "\nStock: Out of Stock"
        val hsnTxt = if (!p.hsnCode.isNullOrBlank()) "\nHSN: ${p.hsnCode}" else ""
        val gstTxt = "\nGST: ${p.gstRate.toInt()}%"
        val priceFormatted = CurrencyFormatter.format(p.defaultPrice)
        val shareText = "📦 *${p.name}*\nPrice: $priceFormatted$hsnTxt$gstTxt$stockTxt\n\nShared via DigitalChopda"
        UniversalShareEngine.shareTextMessage(
            context = context,
            message = shareText,
            chooserTitle = "Share Product via"
        )
    }

    Scaffold(
        containerColor = DeepObsidianCanvas,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = product?.name ?: "Product Details",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = DarkNavy,
                                fontSize = 16.sp
                            ),
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                        )
                        Text(
                            text = "${activeCompany.firmName} • Item Code #${productId}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSubtle,
                                fontSize = 11.sp
                            ),
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DarkNavy
                        )
                    }
                },
                actions = {
                    product?.let { p ->
                        IconButton(onClick = { shareProductWhatsApp(p) }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = BrandPrimaryBlue
                            )
                        }
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = DarkNavy
                            )
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = DangerRed
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MidnightCardSurface
                )
            )
        }
    ) { paddingValues ->
        if (product == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BrandPrimaryBlue)
            }
        } else {
            val p = product!!
            val costPrice = if (p.purchasePrice > 0.0) p.purchasePrice else p.defaultPrice * 0.70
            val unitMargin = p.defaultPrice - costPrice
            val marginPercent = if (p.defaultPrice > 0.0) (unitMargin / p.defaultPrice) * 100.0 else 0.0
            val totalRetailVal = p.stockQuantity * p.defaultPrice
            val totalCostVal = p.stockQuantity * costPrice

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ─── 1. HERO STOCK & VALUATION COCKPIT CARD ───
                item {
                    ChopdaCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        borderColor = BorderStrokeDark,
                        backgroundColor = MidnightCardSurface,
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Header Row: Current Stock & Status Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "CURRENT PHYSICAL STOCK",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextSubtle,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            letterSpacing = 0.8.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = if (p.stockQuantity % 1.0 == 0.0) "${p.stockQuantity.toInt()}" else "%.1f".format(p.stockQuantity),
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.Black,
                                                color = if (p.stockQuantity <= 0.0) RubyUdhar else if (p.stockQuantity <= p.minStockAlert) Color(0xFFD97706) else EmeraldJama,
                                                fontSize = 28.sp
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = p.unit.uppercase(),
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                color = TextMuted,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            ),
                                            modifier = Modifier.padding(bottom = 3.dp)
                                        )
                                    }
                                }

                                if (p.stockQuantity <= 0.0) {
                                    StatusPill(
                                        text = "Out of Stock",
                                        textColor = RubyUdhar,
                                        backgroundColor = RubyUdhar.copy(alpha = 0.12f)
                                    )
                                } else if (p.stockQuantity <= p.minStockAlert) {
                                    StatusPill(
                                        text = "Low Stock Alert",
                                        textColor = Color(0xFFD97706),
                                        backgroundColor = Color(0xFFFEF3C7)
                                    )
                                } else {
                                    StatusPill(
                                        text = "In Stock (Healthy)",
                                        textColor = EmeraldJama,
                                        backgroundColor = EmeraldJamaBg
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = ElevatedSlate, thickness = 0.8.dp)
                            Spacer(modifier = Modifier.height(12.dp))

                            // Pricing & Margin Row (Selling vs Cost vs Margin)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Selling Price",
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextSubtle, fontSize = 10.5.sp)
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatCurrencyExact(p.defaultPrice),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = DarkNavy,
                                            fontSize = 15.sp
                                        )
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Cost Price",
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextSubtle, fontSize = 10.5.sp)
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatCurrencyExact(costPrice),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = ImperialGold,
                                            fontSize = 15.sp
                                        )
                                    )
                                }

                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Unit Margin",
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextSubtle, fontSize = 10.5.sp)
                                    )
                                    Text(
                                        text = "+${CurrencyFormatter.formatINR(unitMargin)} (${"%.1f".format(marginPercent)}%)",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldJama,
                                            fontSize = 13.5.sp
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Total Valuation Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ElevatedSlate)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Retail Value: ",
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextSubtle, fontSize = 11.sp)
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatINR(totalRetailVal),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = BrandPrimaryBlue,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Asset Value: ",
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextSubtle, fontSize = 11.sp)
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatINR(totalCostVal),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = ImperialGold,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Badges Row: HSN & GST
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!p.hsnCode.isNullOrBlank()) {
                                    StatusPill(
                                        text = "HSN: ${p.hsnCode}",
                                        textColor = BrandPrimaryBlue,
                                        backgroundColor = BrandAccentSoft
                                    )
                                }
                                StatusPill(
                                    text = "${p.gstRate.toInt()}% GST Slab",
                                    textColor = if (p.gstRate >= 18.0) RubyUdhar else EmeraldJama,
                                    backgroundColor = if (p.gstRate >= 18.0) RubyUdhar.copy(alpha = 0.12f) else EmeraldJamaBg
                                )
                                if (!p.sku.isNullOrBlank()) {
                                    StatusPill(
                                        text = "SKU: ${p.sku}",
                                        textColor = DarkNavy,
                                        backgroundColor = ElevatedSlate
                                    )
                                }
                            }
                        }
                    }
                }

                // ─── 2. TACTILE QUICK ACTION ROW ───
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Action 1: Adjust Stock
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(74.dp)
                                .bouncyClick { showAdjustStockDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            color = MidnightCardSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderStrokeDark)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldJamaBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = "Adjust Stock",
                                        tint = EmeraldJama,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Adjust Stock",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = DarkNavy,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                                )
                            }
                        }

                        // Action 2: New Sale Invoice
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(74.dp)
                                .bouncyClick { onNavigateToCreateInvoice(p.id, p.name) },
                            shape = RoundedCornerShape(12.dp),
                            color = MidnightCardSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderStrokeDark)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(BrandAccentSoft),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Receipt,
                                        contentDescription = "New Bill",
                                        tint = BrandPrimaryBlue,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "New Bill",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = DarkNavy,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                                )
                            }
                        }

                        // Action 3: Share WhatsApp
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(74.dp)
                                .bouncyClick { shareProductWhatsApp(p) },
                            shape = RoundedCornerShape(12.dp),
                            color = MidnightCardSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderStrokeDark)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(WhatsAppGreen.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Chat,
                                        contentDescription = "Share",
                                        tint = WhatsAppGreen,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Share Card",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = DarkNavy,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                                )
                            }
                        }
                    }
                }

                // ─── 3. PERSPECTIVE TABS (Stock Movement vs Who Purchased vs Inward vs Specs) ───
                item {
                    val tabs = listOf(
                        "Stock Movement",
                        "Who Purchased (${customerPurchases.size})",
                        "Inward (${supplierReceipts.size})",
                        "Specs"
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = ElevatedSlate,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderStrokeDark)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            tabs.forEachIndexed { index, title ->
                                val isSelected = selectedTab == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(if (isSelected) BrandPrimaryBlue else Color.Transparent)
                                        .bouncyClick { selectedTab = index }
                                        .padding(horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isSelected) Color.White else TextMuted,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 11.sp
                                        ),
                                        maxLines = 1,
                                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                                    )
                                }
                            }
                        }
                    }
                }

                // ─── 4. TAB CONTENTS ───
                when (selectedTab) {
                    0 -> {
                        // ─── TAB 0: STOCK MOVEMENT TIMELINE (PASSBOOK) ───
                        if (stockHistory.isEmpty()) {
                            item {
                                EmptyTabState(
                                    icon = Icons.Default.History,
                                    message = "No manual stock adjustments recorded yet for this item.\nTap 'Adjust Stock' above to record audits."
                                )
                            }
                        } else {
                            items(
                                items = stockHistory,
                                key = { it.id }
                            ) { adj ->
                                StockMovementCard(adj = adj, unit = p.unit)
                            }
                        }
                    }

                    1 -> {
                        // ─── TAB 1: WHO PURCHASED (CUSTOMER BREAKDOWN) ───
                        if (customerPurchases.isEmpty()) {
                            item {
                                EmptyTabState(
                                    icon = Icons.Default.People,
                                    message = "No customer purchases recorded for \"${p.name}\" yet.\nInvoices containing this item will appear here automatically."
                                )
                            }
                        } else {
                            items(
                                items = customerPurchases,
                                key = { "${it.invoiceId}-${it.invoiceNumber}" }
                            ) { purchase ->
                                CustomerPurchaseCard(
                                    purchase = purchase,
                                    unit = p.unit,
                                    onCall = { phone ->
                                        if (phone.isNotBlank()) {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                            context.startActivity(intent)
                                        }
                                    },
                                    onWhatsApp = { phone, name ->
                                        if (phone.isNotBlank()) {
                                            val cleanPhone = phone.replace("+", "").replace(" ", "").replace("-", "")
                                            val url = "https://wa.me/$cleanPhone?text=${Uri.encode("Hello $name, regarding your purchase of ${p.name} from ${activeCompany.firmName}...")}"
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            context.startActivity(intent)
                                        }
                                    },
                                    onClick = {
                                        onNavigateToDocumentDetail("invoice", purchase.invoiceId)
                                    }
                                )
                            }
                        }
                    }

                    2 -> {
                        // ─── TAB 2: SUPPLIER INWARD RECEIPTS ───
                        if (supplierReceipts.isEmpty()) {
                            item {
                                EmptyTabState(
                                    icon = Icons.Default.LocalShipping,
                                    message = "No inward supplier receipts found for \"${p.name}\".\nInward purchase bills from vendors will appear here."
                                )
                            }
                        } else {
                            items(
                                items = supplierReceipts,
                                key = { it.purchaseId }
                            ) { receipt ->
                                SupplierReceiptCard(receipt = receipt, unit = p.unit)
                            }
                        }
                    }

                    3 -> {
                        // ─── TAB 3: SPECIFICATIONS & ONLINE STORE ───
                        item {
                            ProductSpecificationsCard(product = p, costPrice = costPrice)
                        }
                    }
                }
            }
        }
    }

    // ─── ADJUST STOCK DIALOG ───
    if (showAdjustStockDialog && product != null) {
        AdjustStockDialog(
            product = product!!,
            productRepository = productRepository,
            onDismiss = { showAdjustStockDialog = false },
            onAdjusted = { showAdjustStockDialog = false }
        )
    }

    // ─── EDIT PRODUCT DIALOG ───
    if (showEditDialog && product != null) {
        AddEditProductDialog(
            companyId = activeCompany.id,
            product = product,
            onDismiss = { showEditDialog = false },
            onSave = { updatedProd ->
                scope.launch {
                    val res = productRepository.updateProduct(updatedProd)
                    res.fold(
                        onSuccess = {
                            Toast.makeText(context, "Product updated", Toast.LENGTH_SHORT).show()
                            showEditDialog = false
                        },
                        onFailure = { err ->
                            Toast.makeText(context, err.message ?: "Failed to update product", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }
        )
    }

    // ─── DELETE CONFIRMATION DIALOG ───
    if (showDeleteDialog && product != null) {
        val prod = product!!
        ChopdaModalDialog(
            onDismissRequest = { showDeleteDialog = false },
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
                        onClick = { showDeleteDialog = false },
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
                                showDeleteDialog = false
                                onNavigateBack()
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
}

/**
 * 👤 Customer Purchase Row Card ("Who Purchased How Much")
 */
@Composable
fun CustomerPurchaseCard(
    purchase: ProductCustomerPurchase,
    unit: String,
    onCall: (String) -> Unit,
    onWhatsApp: (String, String) -> Unit,
    onClick: () -> Unit
) {
    val dateStr = remember(purchase.invoiceDate) {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        sdf.format(Date(purchase.invoiceDate))
    }

    ChopdaCard(
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClick { onClick() },
        cornerRadius = 14.dp,
        borderColor = BorderStrokeDark,
        backgroundColor = MidnightCardSurface,
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Customer Name & Contact
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = purchase.customerName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = DarkNavy,
                            fontSize = 14.5.sp
                        ),
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (purchase.customerPhone.isNotBlank()) "${purchase.customerPhone} • ${purchase.invoiceNumber}" else purchase.invoiceNumber,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSubtle,
                            fontSize = 11.5.sp
                        ),
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Amount Billed & Status
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyFormatter.formatCurrencyExact(purchase.totalAmount),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = DarkNavy,
                            fontSize = 15.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val isPaid = purchase.status.equals("PAID", ignoreCase = true)
                    StatusPill(
                        text = if (isPaid) "PAID" else "UNPAID",
                        textColor = if (isPaid) EmeraldJama else RubyUdhar,
                        backgroundColor = if (isPaid) EmeraldJamaBg else RubyUdhar.copy(alpha = 0.12f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = ElevatedSlate, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(6.dp))

            // Line Item Details: Quantity, Unit Price & Quick Call/WhatsApp Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Purchased: ${purchase.quantity} ${unit.lowercase()} @ ${CurrencyFormatter.formatINR(purchase.unitPrice)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BrandPrimaryBlue,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp
                    ),
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .basicMarquee(iterations = Int.MAX_VALUE)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (purchase.customerPhone.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(WhatsAppGreen.copy(alpha = 0.12f))
                                .clickable { onWhatsApp(purchase.customerPhone, purchase.customerName) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "WhatsApp",
                                tint = WhatsAppGreen,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(BrandAccentSoft)
                                .clickable { onCall(purchase.customerPhone) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Call",
                                tint = BrandPrimaryBlue,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 📜 Stock Movement Item Card
 */
@Composable
fun StockMovementCard(
    adj: StockAdjustment,
    unit: String
) {
    val dateStr = remember(adj.createdAt) {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        sdf.format(Date(adj.createdAt))
    }

    val isAddition = adj.adjustmentType.equals("add", ignoreCase = true)
    val isReduction = adj.adjustmentType.equals("reduce", ignoreCase = true)

    ChopdaCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp,
        borderColor = BorderStrokeDark,
        backgroundColor = MidnightCardSurface,
        contentPadding = PaddingValues(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isAddition) EmeraldJamaBg else if (isReduction) RubyUdhar.copy(alpha = 0.12f) else BrandAccentSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isAddition) Icons.Default.Add else Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = if (isAddition) EmeraldJama else if (isReduction) RubyUdhar else BrandPrimaryBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = adj.reason.ifBlank { "Physical Stock Audit" },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = DarkNavy,
                            fontSize = 13.5.sp
                        ),
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "$dateStr • Prior: ${adj.previousStock.toInt()}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSubtle,
                            fontSize = 11.sp
                        ),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                val sign = if (isAddition) "+" else if (isReduction) "-" else "="
                Text(
                    text = "$sign${adj.quantity.toInt()} ${unit.lowercase()}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = if (isAddition) EmeraldJama else if (isReduction) RubyUdhar else BrandPrimaryBlue,
                        fontSize = 14.sp
                    )
                )
                Text(
                    text = "Bal: ${adj.newStock.toInt()}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextMuted,
                        fontSize = 10.5.sp
                    )
                )
            }
        }
    }
}

/**
 * 🏭 Supplier Inward Item Card
 */
@Composable
fun SupplierReceiptCard(
    receipt: ProductSupplierReceipt,
    unit: String
) {
    val dateStr = remember(receipt.purchaseDate) {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        sdf.format(Date(receipt.purchaseDate))
    }

    ChopdaCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp,
        borderColor = BorderStrokeDark,
        backgroundColor = MidnightCardSurface,
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = receipt.supplierName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = DarkNavy,
                            fontSize = 14.sp
                        ),
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                    )
                    Text(
                        text = "Bill: ${receipt.purchaseNumber} • $dateStr",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSubtle,
                            fontSize = 11.sp
                        ),
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "+${receipt.quantity.toInt()} ${unit.lowercase()}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = EmeraldJama,
                            fontSize = 14.sp
                        )
                    )
                    Text(
                        text = CurrencyFormatter.formatINR(receipt.totalAmount),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = ImperialGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}

/**
 * 📋 Product Specifications & E-Store Card
 */
@Composable
fun ProductSpecificationsCard(
    product: Product,
    costPrice: Double
) {
    ChopdaCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        borderColor = BorderStrokeDark,
        backgroundColor = MidnightCardSurface,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "PRODUCT SPECIFICATIONS & STORE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextSubtle,
                    letterSpacing = 0.8.sp,
                    fontSize = 10.5.sp
                )
            )

            SpecRow("Full Product Name", product.name)
            SpecRow("Measurement Unit", product.unit)
            SpecRow("HSN / SAC Code", product.hsnCode ?: "Not configured")
            SpecRow("GST Tax Slab", "${product.gstRate.toInt()}%")
            SpecRow("Retail Selling Price", CurrencyFormatter.formatCurrencyExact(product.defaultPrice))
            SpecRow("Estimated Cost Price", CurrencyFormatter.formatCurrencyExact(costPrice))
            SpecRow("SKU / Item Identifier", product.sku ?: "Item Code #${product.id}")
            SpecRow("Barcode", product.barcode ?: "None")
            SpecRow("Min Stock Warning Threshold", "${product.minStockAlert.toInt()} ${product.unit.lowercase()}")
            SpecRow("Batch Number", product.batchNumber ?: "None")
            SpecRow("Expiry Date", product.expiryDate ?: "None")
            SpecRow("Online Store Visibility", if (product.showInOnlineStore) "Visible in E-Store" else "Hidden")
        }
    }
}

@Composable
fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextSubtle,
                fontSize = 12.sp
            ),
            modifier = Modifier.weight(0.38f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = DarkNavy,
                fontSize = 12.5.sp
            ),
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier
                .weight(0.62f)
                .basicMarquee(iterations = Int.MAX_VALUE)
        )
    }
}

@Composable
fun EmptyTabState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String
) {
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
                imageVector = icon,
                contentDescription = null,
                tint = TextSubtle,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextMuted,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}
