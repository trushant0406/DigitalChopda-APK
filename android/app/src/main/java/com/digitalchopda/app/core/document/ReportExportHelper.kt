package com.digitalchopda.app.core.document

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream

/**
 * Native FileProvider and Sharing Engine for DigitalChopda Business & Tax Reports.
 * Handles streaming ResponseBody saving to cacheDir and seamless Intent dispatch
 * to WhatsApp (Regular + Business) and Android Sharesheet.
 */
object ReportExportHelper {

    const val MIME_EXCEL = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    const val MIME_JSON = "application/json"
    const val MIME_ZIP = "application/zip"
    const val MIME_PDF = "application/pdf"

    /**
     * Writes okhttp3.ResponseBody byteStream directly to File in context.cacheDir.
     */
    suspend fun writeStreamToFile(
        context: Context,
        body: ResponseBody,
        filename: String
    ): File = withContext(Dispatchers.IO) {
        val targetFile = File(context.cacheDir, filename)
        body.byteStream().use { inputStream ->
            FileOutputStream(targetFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        targetFile
    }

    /**
     * Writes raw String content to File in context.cacheDir (used for offline export fallbacks).
     */
    suspend fun writeTextToFile(
        context: Context,
        content: String,
        filename: String
    ): File = withContext(Dispatchers.IO) {
        val targetFile = File(context.cacheDir, filename)
        targetFile.writeText(content, Charsets.UTF_8)
        targetFile
    }

    /**
     * Resolves Content URI via FileProvider using the configured provider authority.
     */
    fun getFileUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    }

    /**
     * Constructs an Intent(Intent.ACTION_SEND) configured with FileProvider URI,
     * read permission flags, and designated MIME type.
     */
    fun createShareIntent(
        context: Context,
        file: File,
        mimeType: String,
        messageText: String? = null,
        targetWhatsApp: Boolean = false,
        recipientPhone: String? = null
    ): Intent {
        val contentUri = getFileUri(context, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            if (!messageText.isNullOrBlank()) {
                putExtra(Intent.EXTRA_TEXT, messageText)
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        if (targetWhatsApp) {
            val cleanPhone = recipientPhone?.filter { it.isDigit() }.orEmpty()
            if (cleanPhone.isNotBlank()) {
                intent.putExtra("jid", "$cleanPhone@s.whatsapp.net")
            }
            intent.setPackage("com.whatsapp")
        }

        return intent
    }

    /**
     * Launches the sharing intent with WhatsApp prioritization and chooser fallback.
     */
    fun launchShare(
        context: Context,
        file: File,
        mimeType: String,
        messageText: String? = null,
        chooserTitle: String = "Share Report",
        targetWhatsApp: Boolean = false,
        recipientPhone: String? = null
    ) {
        val intent = createShareIntent(context, file, mimeType, messageText, targetWhatsApp, recipientPhone)
        if (targetWhatsApp) {
            try {
                context.startActivity(intent)
            } catch (e1: Exception) {
                try {
                    intent.setPackage("com.whatsapp.w4b")
                    context.startActivity(intent)
                } catch (e2: Exception) {
                    intent.setPackage(null)
                    val chooser = Intent.createChooser(intent, chooserTitle).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(chooser)
                }
            }
        } else {
            intent.setPackage(null)
            val chooser = Intent.createChooser(intent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }

    /**
     * End-to-end convenience method: streams response to cacheDir and immediately triggers sharing.
     */
    suspend fun saveAndShare(
        context: Context,
        body: ResponseBody,
        filename: String,
        mimeType: String,
        messageText: String? = null,
        chooserTitle: String = "Share Report",
        targetWhatsApp: Boolean = false,
        recipientPhone: String? = null
    ): File {
        val file = writeStreamToFile(context, body, filename)
        withContext(Dispatchers.Main) {
            launchShare(context, file, mimeType, messageText, chooserTitle, targetWhatsApp, recipientPhone)
        }
        return file
    }
}
