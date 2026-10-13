package com.digitalchopda.app.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CurrencyFormatter {
    private val indianFormat: DecimalFormat by lazy {
        val symbols = DecimalFormatSymbols(Locale("en", "IN"))
        symbols.currencySymbol = "₹"
        DecimalFormat("₹ #,##,##0.00", symbols)
    }

    private val wholeIndianFormat: DecimalFormat by lazy {
        val symbols = DecimalFormatSymbols(Locale("en", "IN"))
        symbols.currencySymbol = "₹"
        DecimalFormat("₹ #,##,##0", symbols)
    }

    fun format(amount: Double, includeDecimals: Boolean = false): String {
        return if (includeDecimals) {
            indianFormat.format(amount)
        } else {
            wholeIndianFormat.format(amount)
        }
    }

    fun formatBalance(balance: Double): String {
        val absAmount = kotlin.math.abs(balance)
        return format(absAmount)
    }
}

object DateTimeUtils {
    private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale("en", "IN"))
    private val timeFormatter = SimpleDateFormat("hh:mm a", Locale("en", "IN"))
    private val dateTimeFormatter = SimpleDateFormat("dd MMM, hh:mm a", Locale("en", "IN"))

    fun formatDate(timestamp: Long): String = dateFormatter.format(Date(timestamp))
    fun formatTime(timestamp: Long): String = timeFormatter.format(Date(timestamp))
    fun formatDateTime(timestamp: Long): String = dateTimeFormatter.format(Date(timestamp))
}
