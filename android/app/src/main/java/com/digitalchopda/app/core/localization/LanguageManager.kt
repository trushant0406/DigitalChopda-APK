package com.digitalchopda.app.core.localization

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LanguageManager(
    context: Context? = null,
    customPrefs: SharedPreferences? = null
) {

    private val prefs: SharedPreferences? = customPrefs ?: context?.getSharedPreferences("digital_chopda_lang_prefs", Context.MODE_PRIVATE)

    private val _currentLanguage = MutableStateFlow(loadInitialLanguage())
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    private val _strings = MutableStateFlow(_currentLanguage.value.getStrings())
    val strings: StateFlow<AppStrings> = _strings.asStateFlow()

    private fun loadInitialLanguage(): AppLanguage {
        val savedCode = prefs?.getString(KEY_LANGUAGE_CODE, AppLanguage.GUJARATI.code) ?: AppLanguage.GUJARATI.code
        return AppLanguage.entries.find { it.code == savedCode } ?: AppLanguage.GUJARATI
    }

    fun setLanguage(language: AppLanguage) {
        prefs?.edit()?.putString(KEY_LANGUAGE_CODE, language.code)?.apply()
        _currentLanguage.value = language
        _strings.value = language.getStrings()
    }

    fun cycleNextLanguage(): AppLanguage {
        val next = when (_currentLanguage.value) {
            AppLanguage.GUJARATI -> AppLanguage.HINDI
            AppLanguage.HINDI -> AppLanguage.ENGLISH
            AppLanguage.ENGLISH -> AppLanguage.GUJARATI
        }
        setLanguage(next)
        return next
    }

    companion object {
        const val KEY_LANGUAGE_CODE = "selected_app_language_code"
    }
}
