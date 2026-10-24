package com.example.atlantvpn

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.atlantvpn.ui.theme.AtlantVPNTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

// Local x86 emulator only: -PatlantUiTest=true. No MainActivity, native engine or network.
@RunWith(Parameterized::class)
class SettingsLayoutTest(private val screen: String, private val dark: Boolean, private val large: Boolean) {
    @get:Rule val compose = createComposeRule()

    @Test fun rendersAndScrollsWithLongNames() {
        val apps = (1..12).map { InstalledAppInfo("org.example.long.package.application$it", "Приложение с очень длинным названием $it") }
        var settings by mutableStateOf(AppSettings())
        var routing by mutableStateOf(RoutingSettings(mode = RouteMode.BYPASS, bypassApps = setOf(apps.first().packageName)))
        var saves = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (large) 1.5f else 1f)) {
                AtlantVPNTheme(darkTheme = dark) {
                    Surface(Modifier.requiredSize(320.dp, 568.dp)) {
                        if (screen.startsWith("ROUTING")) RoutingSubScreen(routing, apps, false, {}, { routing = it; saves++ })
                        else SettingsContent(settings, routing, SettingsPage.valueOf(screen), { settings = it; saves++ }, {}, {}, {})
                    }
                }
            }
        }
        compose.waitForIdle()
        assertEquals("Opening a screen must not rewrite settings", 0, saves)
        if (screen == "ROUTING_APPS") {
            compose.onNodeWithText("Приложения", useUnmergedTree = true).performScrollTo().performClick()
            compose.onNodeWithText("Найти приложение").performScrollTo().assertIsDisplayed()
            screenshot("top")
            compose.onNodeWithText(apps.last().appName).performScrollTo().assertIsDisplayed()
        } else {
            screenshot("top")
            val bottomText = when (screen) {
                "HOME" -> "Диагностика"
                "APPEARANCE" -> "Тёмная"
                "NETWORK" -> "Открыть настройки VPN"
                "TRANSPORT" -> "Смена порта сервера"
                "PRIVACY" -> "Фоновые запросы"
                "DIAGNOSTICS" -> "Открыть журнал"
                else -> "Список адресов"
            }
            compose.onNodeWithText(bottomText).performScrollTo().assertIsDisplayed()
        }
        screenshot("bottom")
    }

    private fun screenshot(position: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val folder = File(context.getExternalFilesDir(null), "ui-proof").apply { mkdirs() }
        val file = File(folder, "$screen-${if (dark) "dark" else "light"}-${if (large) "large" else "normal"}-$position.png")
        file.outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}-dark={1}-large={2}")
        fun cases(): List<Array<Any>> = listOf("HOME", "APPEARANCE", "NETWORK", "TRANSPORT", "PRIVACY", "DIAGNOSTICS", "ROUTING_DOMAINS", "ROUTING_APPS")
            .flatMap { screen -> listOf(false, true).flatMap { dark -> listOf(false, true).map { large -> arrayOf<Any>(screen, dark, large) } } }
    }
}
