package com.digitalchopda.app.core.sound

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * Enterprise Retail Soundbox Audio Engine
 *
 * Provides immediate auditory confirmation for counter transactions (payments received,
 * invoices issued, cart actions) mimicking standard retail payment soundboxes.
 * Operates purely via Android's native ToneGenerator without external asset dependencies.
 */
object SoundEffectsManager {

    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 95)
        } catch (_: Exception) {
            toneGenerator = null
        }
    }

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
}
