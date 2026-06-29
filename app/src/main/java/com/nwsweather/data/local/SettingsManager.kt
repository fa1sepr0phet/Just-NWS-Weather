package com.nwsweather.data.local

import android.content.Context
import androidx.core.content.edit
import com.nwsweather.presentation.AppTheme
import com.nwsweather.presentation.TemperatureUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("just_weather_settings", Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(
        safeValueOf(prefs.getString(KEY_THEME, null), AppTheme.SYSTEM)
    )
    val theme: StateFlow<AppTheme> = _theme.asStateFlow()

    private val _unit = MutableStateFlow(
        safeValueOf(prefs.getString(KEY_UNIT, null), TemperatureUnit.FAHRENHEIT)
    )
    val unit: StateFlow<TemperatureUnit> = _unit.asStateFlow()

    private inline fun <reified T : Enum<T>> safeValueOf(name: String?, default: T): T {
        if (name == null) return default
        return try {
            java.lang.Enum.valueOf(T::class.java, name)
        } catch (e: IllegalArgumentException) {
            default
        }
    }

    private val _notificationsEnabled = MutableStateFlow(prefs.getBoolean(KEY_NOTIFICATIONS, false))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _statusBarTempEnabled = MutableStateFlow(prefs.getBoolean(KEY_STATUS_BAR_TEMP, false))
    val statusBarTempEnabled: StateFlow<Boolean> = _statusBarTempEnabled.asStateFlow()

    private val _showTutorial = MutableStateFlow(prefs.getBoolean(KEY_SHOW_TUTORIAL, true))
    val showTutorial: StateFlow<Boolean> = _showTutorial.asStateFlow()

    private val _hasSeenSearchHelp = MutableStateFlow(prefs.getBoolean(KEY_HAS_SEEN_SEARCH_HELP, false))
    val hasSeenSearchHelp: StateFlow<Boolean> = _hasSeenSearchHelp.asStateFlow()

    private val _hasSeenFavoritesHelp = MutableStateFlow(prefs.getBoolean(KEY_HAS_SEEN_FAVORITES_HELP, false))
    val hasSeenFavoritesHelp: StateFlow<Boolean> = _hasSeenFavoritesHelp.asStateFlow()

    fun getLastNotifiedAlertId(): String? = prefs.getString(KEY_LAST_NOTIFIED_ALERT_ID, null)

    fun setLastNotifiedAlertId(id: String) {
        prefs.edit { putString(KEY_LAST_NOTIFIED_ALERT_ID, id) }
    }

    private val _installTime = prefs.getLong(KEY_INSTALL_TIME, 0L).let {
        if (it == 0L) {
            val now = System.currentTimeMillis()
            prefs.edit { putLong(KEY_INSTALL_TIME, now) }
            now
        } else it
    }

    fun shouldPromptForRating(): Boolean {
        val weekInMs = 7 * 24 * 60 * 60 * 1000L
        val hasRated = prefs.getBoolean(KEY_HAS_RATED, false)
        return !hasRated && (System.currentTimeMillis() - _installTime > weekInMs)
    }

    fun markRated() {
        prefs.edit { putBoolean(KEY_HAS_RATED, true) }
    }

    fun setTheme(theme: AppTheme) {
        prefs.edit { putString(KEY_THEME, theme.name) }
        _theme.value = theme
    }

    fun setTemperatureUnit(unit: TemperatureUnit) {
        prefs.edit { putString(KEY_UNIT, unit.name) }
        _unit.value = unit
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_NOTIFICATIONS, enabled) }
        _notificationsEnabled.value = enabled
    }

    fun setStatusBarTempEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_STATUS_BAR_TEMP, enabled) }
        _statusBarTempEnabled.value = enabled
    }

    fun setShowTutorial(show: Boolean) {
        prefs.edit { putBoolean(KEY_SHOW_TUTORIAL, show) }
        _showTutorial.value = show
    }

    fun setHasSeenSearchHelp(seen: Boolean) {
        prefs.edit { putBoolean(KEY_HAS_SEEN_SEARCH_HELP, seen) }
        _hasSeenSearchHelp.value = seen
    }

    fun setHasSeenFavoritesHelp(seen: Boolean) {
        prefs.edit { putBoolean(KEY_HAS_SEEN_FAVORITES_HELP, seen) }
        _hasSeenFavoritesHelp.value = seen
    }

    companion object {
        private const val KEY_THEME = "theme"
        private const val KEY_UNIT = "unit"
        private const val KEY_NOTIFICATIONS = "notifications"
        private const val KEY_STATUS_BAR_TEMP = "status_bar_temp"
        private const val KEY_SHOW_TUTORIAL = "show_tutorial"
        private const val KEY_HAS_SEEN_SEARCH_HELP = "has_seen_search_help"
        private const val KEY_HAS_SEEN_FAVORITES_HELP = "has_seen_favorites_help"
        private const val KEY_INSTALL_TIME = "install_time"
        private const val KEY_HAS_RATED = "has_rated"
        private const val KEY_LAST_NOTIFIED_ALERT_ID = "last_notified_alert_id"
    }
}
