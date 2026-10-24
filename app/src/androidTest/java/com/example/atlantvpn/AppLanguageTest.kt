package com.example.atlantvpn

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

class AppLanguageTest {
    @Test fun languagePersistsAndLocalizesBothCatalogAndAndroidResources() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val original = AppLanguage.selected(context)
        try {
            for ((tag, title) in listOf("en" to "Settings", "ru" to "Настройки", "zh" to "设置")) {
                AppLanguage.save(context, tag)
                assertEquals(tag, AppLanguage.selected(context))
                val localized = AppLanguage.wrap(context)
                assertEquals(tag, localized.resources.configuration.locales[0].language)
                assertEquals(title, tr("Настройки"))
                assertEquals("name{0}", tr("{0}", "name{0}"))
                assertFalse(localized.getString(R.string.qr_camera_title).isBlank())
            }
            AppLanguage.save(context, "system")
            assertEquals("system", AppLanguage.selected(context))
            assertEquals(android.content.res.Resources.getSystem().configuration.locales[0].language,
                AppLanguage.wrap(context).resources.configuration.locales[0].language)
        } finally {
            AppLanguage.save(context, original)
            AppLanguage.wrap(context)
        }
    }
}
