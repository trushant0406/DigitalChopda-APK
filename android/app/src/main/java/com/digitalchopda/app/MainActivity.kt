package com.digitalchopda.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.digitalchopda.app.core.designsystem.DigitalChopdaTheme
import com.digitalchopda.app.core.localization.LocalAppStrings
import com.digitalchopda.app.presentation.navigation.DigitalChopdaNavGraph

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        val app = application as DigitalChopdaApplication

        setContent {
            val strings by app.languageManager.strings.collectAsState()

            CompositionLocalProvider(LocalAppStrings provides strings) {
                DigitalChopdaTheme {
                    val navController = rememberNavController()
                    DigitalChopdaNavGraph(
                        navController = navController,
                        app = app
                    )
                }
            }
        }
    }
}
