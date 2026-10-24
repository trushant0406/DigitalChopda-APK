package com.digitalchopda.app.core.document

import com.digitalchopda.app.domain.model.LineItem
import com.digitalchopda.app.domain.model.Product
import com.google.gson.Gson

object DocumentItemParser {

    private val gson = Gson()

    fun toJson(items: List<LineItem>): String {
        return try {
            gson.toJson(items)
        } catch (e: Exception) {
            ""
        }
    }

    fun parseLineItems(
        itemsJson: String?,
        itemsSummary: String? = null,
        defaultSubtotal: Double = 0.0,
        catalogProducts: List<Product> = emptyList()
    ): List<LineItem> {
        // 1. Try structured JSON first
        if (!itemsJson.isNullOrBlank()) {
            try {
                val parsed: Array<LineItem>? = gson.fromJson(itemsJson, Array<LineItem>::class.java)
                if (!parsed.isNullOrEmpty()) {
                    return parsed.filter { it.title.isNotBlank() || it.quantity > 0 }
                }
            } catch (_: Exception) {
                // Fallback to legacy summary parser
            }
        }

        // 2. Fallback to parsing legacy human-readable itemsSummary
        if (itemsSummary.isNullOrBlank()) {
            return if (defaultSubtotal > 0.0) {
                listOf(
                    LineItem(
                        title = "Invoice Goods / Services",
                        unitPrice = defaultSubtotal,
                        quantity = 1,
                        gstRatePercent = 18.0
                    )
                )
            } else {
                emptyList()
            }
        }

        val tokens = itemsSummary.split(",").map { it.trim() }.filter { it.isNotBlank() }
        if (tokens.isEmpty()) {
            return emptyList()
        }

        // Regex patterns to match: "Item Title (x2)", "Item Title (x2 @ ₹1,850)", "Item Title (2 PCS)"
        val qtyRegex = Regex("""\((?:x\s*(\d+)|(\d+)\s*(?:pcs|units?|items?)?)(?:\s*@\s*₹?\s*([0-9.,]+))?(?:\s*\|\s*([0-9.]+)%?)?\)""", RegexOption.IGNORE_CASE)

        val rawItems = tokens.mapIndexed { index, token ->
            var title = token
            var quantity = 1
            var unitPrice = 0.0
            var gstRate = 18.0

            val match = qtyRegex.find(token)
            if (match != null) {
                title = token.substring(0, match.range.first).trim()
                val q1 = match.groupValues[1]
                val q2 = match.groupValues[2]
                val q = (q1.ifBlank { q2 }).toIntOrNull() ?: 1
                quantity = q.coerceAtLeast(1)

                val priceStr = match.groupValues[3].replace(",", "")
                if (priceStr.isNotBlank()) {
                    unitPrice = priceStr.toDoubleOrNull() ?: 0.0
                }

                val gstStr = match.groupValues[4]
                if (gstStr.isNotBlank()) {
                    gstRate = gstStr.toDoubleOrNull() ?: 18.0
                }
            } else {
                // If parentheses without standard match, remove trailing parens for clean title
                title = token.replace(Regex("""\(.*?\)"""), "").trim()
            }

            // If unit price not in string, attempt catalog lookup
            if (unitPrice == 0.0 && catalogProducts.isNotEmpty()) {
                val matchedProduct = catalogProducts.firstOrNull {
                    it.name.equals(title, ignoreCase = true) || title.contains(it.name, ignoreCase = true)
                }
                if (matchedProduct != null) {
                    unitPrice = matchedProduct.defaultPrice
                    gstRate = matchedProduct.gstRate
                }
            }

            LineItem(
                itemId = (index + 1).toLong(),
                sku = "",
                title = title.ifBlank { "Item ${index + 1}" },
                unitPrice = unitPrice,
                quantity = quantity,
                gstRatePercent = gstRate
            )
        }

        // 3. If unit prices could not be resolved from catalog, derive from defaultSubtotal
        val totalExplicitSubtotal = rawItems.sumOf { it.unitPrice * it.quantity }
        if (totalExplicitSubtotal == 0.0 && defaultSubtotal > 0.0) {
            val totalQty = rawItems.sumOf { it.quantity }.coerceAtLeast(1)
            val avgPrice = defaultSubtotal / totalQty
            return rawItems.map { it.copy(unitPrice = avgPrice) }
        }

        return rawItems
    }
}
