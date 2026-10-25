package `in`.caffeinelabs.archie.data.repository

import android.content.Context
import android.content.SharedPreferences

interface SettingsRepository {
    var isGlobalShieldEnabled: Boolean
    var isForceDarkEnabled: Boolean
    var defaultSearchEngine: String
}

class SharedPreferencesSettingsRepository(context: Context) : SettingsRepository {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "archie_settings"
        private const val KEY_SHIELD_ENABLED = "global_shield_enabled"
        private const val KEY_FORCE_DARK = "global_force_dark"
        private const val KEY_SEARCH_ENGINE = "default_search_engine"
        private const val DEFAULT_SEARCH_ENGINE = "Google"
    }

    override var isGlobalShieldEnabled: Boolean
        get() = prefs.getBoolean(KEY_SHIELD_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SHIELD_ENABLED, value).apply()

    override var isForceDarkEnabled: Boolean
        get() = prefs.getBoolean(KEY_FORCE_DARK, false)
        set(value) = prefs.edit().putBoolean(KEY_FORCE_DARK, value).apply()

    override var defaultSearchEngine: String
        get() = prefs.getString(KEY_SEARCH_ENGINE, DEFAULT_SEARCH_ENGINE) ?: DEFAULT_SEARCH_ENGINE
        set(value) = prefs.edit().putString(KEY_SEARCH_ENGINE, value).apply()
}
