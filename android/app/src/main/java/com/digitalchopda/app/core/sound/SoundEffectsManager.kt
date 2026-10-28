package com.digitalchopda.app.core.sound

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Enterprise Retail Soundbox Audio & Voice Announcement Engine
 *
 * Provides immediate auditory confirmation for counter transactions (payments received,
 * invoices issued, cart actions) mimicking standard retail payment soundboxes (Paytm, PhonePe).
 * Combines Android's native ToneGenerator chimes with TextToSpeech (TTS) voice announcements.
 */
object SoundEffectsManager {

    private var toneGenerator: ToneGenerator? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false
    private var isVoiceEnabled = true
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 95)
        } catch (_: Exception) {
            toneGenerator = null
        }
    }

    /**
     * Initializes the native TextToSpeech engine using Application context.
     */
    fun init(context: Context) {
        try {
            if (textToSpeech != null) return
            textToSpeech = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = textToSpeech?.setLanguage(Locale("en", "IN"))
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        textToSpeech?.setLanguage(Locale.US)
                    }
                    textToSpeech?.setSpeechRate(0.95f)
                    textToSpeech?.setPitch(1.0f)
                    isTtsReady = true
                }
            }
        } catch (_: Exception) {
            isTtsReady = false
        }
    }

    fun setVoiceEnabled(enabled: Boolean) {
        isVoiceEnabled = enabled
    }

    fun isVoiceEnabled(): Boolean = isVoiceEnabled

    /**
     * Plays a crisp high-frequency dual chime resembling a retail Soundbox payment confirmation.
     */
    fun playPaymentSuccessSound() {
        try {
            val tg = toneGenerator ?: ToneGenerator(AudioManager.STREAM_NOTIFICATION, 95).also { toneGenerator = it }
            tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 220)
        } catch (_: Exception) {
            // Fail gracefully if audio hardware is unavailable or in silent profile
        }
    }

    /**
     * Full Soundbox Announcement: Plays chime tone then announces amount and customer.
     * Example: "Payment of 500 rupees received from Amitabh Patel"
     */
    fun announcePaymentReceived(
        amount: Double,
        customerName: String = "",
        isHindi: Boolean = false
    ) {
        playPaymentSuccessSound()

        if (!isVoiceEnabled || !isTtsReady) return

        mainHandler.postDelayed({
            try {
                val formattedAmt = if (amount % 1.0 == 0.0) {
                    amount.toLong().toString()
                } else {
                    String.format(Locale.US, "%.2f", amount)
                }

                val speechText = if (isHindi) {
                    if (customerName.isNotBlank()) {
                        "DigitalChopda par $formattedAmt rupaye prapt hue. $customerName se"
                    } else {
                        "DigitalChopda par $formattedAmt rupaye prapt hue"
                    }
                } else {
                    if (customerName.isNotBlank()) {
                        "Payment of $formattedAmt rupees received from $customerName on DigitalChopda"
                    } else {
                        "Payment of $formattedAmt rupees received on DigitalChopda"
                    }
                }

                textToSpeech?.speak(
                    speechText,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "payment_${System.currentTimeMillis()}"
                )
            } catch (_: Exception) {}
        }, 320L)
    }

    /**
     * Plays an affirmative single chime for invoice creation or cart confirmation.
     */
    fun playInvoiceCreatedSound() {
        try {
            val tg = toneGenerator ?: ToneGenerator(AudioManager.STREAM_NOTIFICATION, 95).also { toneGenerator = it }
            tg.startTone(ToneGenerator.TONE_PROP_ACK, 160)
        } catch (_: Exception) {
            // Fail gracefully
        }
    }

    /**
     * Announces invoice issuance over speaker.
     */
    fun announceInvoiceCreated(invoiceNumber: String, amount: Double) {
        playInvoiceCreatedSound()

        if (!isVoiceEnabled || !isTtsReady) return

        mainHandler.postDelayed({
            try {
                val formattedAmt = if (amount % 1.0 == 0.0) {
                    amount.toLong().toString()
                } else {
                    String.format(Locale.US, "%.2f", amount)
                }
                val speechText = "Tax invoice $invoiceNumber issued for $formattedAmt rupees"
                textToSpeech?.speak(
                    speechText,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "invoice_${System.currentTimeMillis()}"
                )
            } catch (_: Exception) {}
        }, 280L)
    }

    /**
     * Plays a subtle micro-tone for quick actions (e.g. barcode scan / quick add).
     */
    fun playQuickChirp() {
        try {
            val tg = toneGenerator ?: ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80).also { toneGenerator = it }
            tg.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
        } catch (_: Exception) {
            // Fail gracefully
        }
    }

    fun shutdown() {
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
            isTtsReady = false
        } catch (_: Exception) {}
    }
}

