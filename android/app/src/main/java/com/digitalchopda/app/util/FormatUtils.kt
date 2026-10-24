package com.digitalchopda.app.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CurrencyFormatter {
    private val decimalSymbols = DecimalFormatSymbols(Locale("en", "IN"))

    private val indianFormatWithDecimals: DecimalFormat by lazy {
        DecimalFormat("#,##,##0.00", decimalSymbols)
    }

    private val indianFormatWhole: DecimalFormat by lazy {
        DecimalFormat("#,##,##0", decimalSymbols)
    }

    fun format(amount: Double, includeDecimals: Boolean = false): String {
        val isNegative = amount < 0
        val absAmount = kotlin.math.abs(amount)
        val formatted = if (includeDecimals) {
            indianFormatWithDecimals.format(absAmount)
        } else {
            indianFormatWhole.format(absAmount)
        }
        return if (isNegative) "- ₹ $formatted" else "₹ $formatted"
    }

    fun format(amount: Int, includeDecimals: Boolean = false): String =
        format(amount.toDouble(), includeDecimals)

    fun format(amount: Long, includeDecimals: Boolean = false): String =
        format(amount.toDouble(), includeDecimals)

    fun formatBalance(balance: Double): String {
        val absAmount = kotlin.math.abs(balance)
        return format(absAmount, includeDecimals = false)
    }

    fun formatCurrency(amount: Double, includeDecimals: Boolean = false): String =
        format(amount, includeDecimals)

    fun formatCurrency(amount: Int): String =
        format(amount.toDouble(), includeDecimals = false)

    fun formatCurrencyExact(amount: Double): String =
        format(amount, includeDecimals = true)

    fun formatINR(amount: Double): String = format(amount, includeDecimals = false)
    fun formatINR(amount: Long): String = format(amount.toDouble(), includeDecimals = false)
    fun formatINR(amount: Int): String = format(amount.toDouble(), includeDecimals = false)
}

fun formatCurrency(amount: Double, includeDecimals: Boolean = false): String =
    CurrencyFormatter.format(amount, includeDecimals)

fun formatCurrency(amount: Int): String =
    CurrencyFormatter.format(amount.toDouble(), includeDecimals = false)

fun formatCurrencyExact(amount: Double): String =
    CurrencyFormatter.format(amount, includeDecimals = true)

fun formatINR(amount: Double): String = CurrencyFormatter.formatINR(amount)
fun formatINR(amount: Long): String = CurrencyFormatter.formatINR(amount)
fun formatINR(amount: Int): String = CurrencyFormatter.formatINR(amount)

object DateTimeUtils {
    private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale("en", "IN"))
    private val timeFormatter = SimpleDateFormat("hh:mm a", Locale("en", "IN"))
    private val dateTimeFormatter = SimpleDateFormat("dd MMM, hh:mm a", Locale("en", "IN"))

    fun formatDate(timestamp: Long): String = dateFormatter.format(Date(timestamp))
    fun formatTime(timestamp: Long): String = timeFormatter.format(Date(timestamp))
    fun formatDateTime(timestamp: Long): String = dateTimeFormatter.format(Date(timestamp))

    fun parseIsoDateToMillis(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            val clean = if (dateStr.length >= 19) dateStr.substring(0, 19) else dateStr
            sdf.parse(clean)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }
}

object PhoneSanitizer {
    fun normalize(phone: String?): String {
        if (phone.isNullOrBlank()) return ""
        val digitsOnly = phone.filter { it.isDigit() }
        return if (digitsOnly.length > 10) digitsOnly.takeLast(10) else digitsOnly
    }
}
