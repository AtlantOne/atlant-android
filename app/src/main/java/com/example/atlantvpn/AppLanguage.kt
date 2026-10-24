package com.example.atlantvpn

import android.content.Context
import android.content.res.Configuration
import org.json.JSONObject
import java.util.Locale

/** UI language only: protocol fields, server names and credentials stay untouched. */
internal object AppLanguage {
    val choices = listOf("system", "en", "ru", "zh")
    private var tag = "ru"
    private var catalog: JSONObject? = null

    fun selected(context: Context): String = context.getSharedPreferences("ui_preferences", Context.MODE_PRIVATE)
        .getString("language", "system").let { if (it in choices) it!! else "system" }

    fun save(context: Context, value: String) {
        require(value in choices)
        context.getSharedPreferences("ui_preferences", Context.MODE_PRIVATE).edit().putString("language", value).apply()
    }

    fun wrap(base: Context): Context {
        val selected = selected(base)
        val locale = if (selected == "system") android.content.res.Resources.getSystem().configuration.locales[0] else Locale.forLanguageTag(selected)
        tag = locale.language.takeIf { it in choices } ?: "en"
        val configuration = Configuration(base.resources.configuration).apply { setLocale(locale) }
        val localized = base.createConfigurationContext(configuration)
        catalog = JSONObject(base.assets.open("client-languages.json").bufferedReader().use { it.readText() })
        return localized
    }

    fun text(source: String, vararg values: Any?): String {
        val row = catalog?.optJSONObject(source)
        val template = if (tag == "ru") source else row?.optString(tag, source) ?: source
        // Replace placeholders once, so imported names containing {0} remain literal.
        return Regex("\\{(\\d+)\\}").replace(template) { match ->
            values.getOrNull(match.groupValues[1].toInt())?.toString() ?: match.value
        }
    }
}

internal fun tr(source: String, vararg values: Any?): String = AppLanguage.text(source, *values)
