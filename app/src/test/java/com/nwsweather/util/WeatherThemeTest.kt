package com.nwsweather.util

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.nwsweather.presentation.AppTheme
import com.nwsweather.ui.theme.*
import org.junit.Assert.*
import org.junit.Test

class WeatherThemeTest {
    @Test fun systemFollowsDeviceAppearance() {
        assertEquals(AppTheme.LIGHT, resolveAppTheme(AppTheme.SYSTEM, false))
        assertEquals(AppTheme.DARK, resolveAppTheme(AppTheme.SYSTEM, true))
    }
    @Test fun explicitThemesOverrideDeviceAppearance() {
        listOf(AppTheme.LIGHT, AppTheme.DARK, AppTheme.MIDNIGHT).forEach { theme ->
            assertEquals(theme, resolveAppTheme(theme, false))
            assertEquals(theme, resolveAppTheme(theme, true))
        }
    }
    @Test fun weatherNeverOverridesSelectedCardPalette() {
        val light = lightColorScheme(surface = Color.White, onSurface = Color(0xFF1A1C1E))
        val dark = darkColorScheme(surface = Color(0xFF1A1C1E), onSurface = Color(0xFFE2E2E6))
        val midnight = darkColorScheme(surface = Color(0xFF121212), onSurface = Color(0xFFE2E2E6))
        WeatherMood.entries.forEach { mood ->
            listOf(AppTheme.LIGHT to light, AppTheme.DARK to dark, AppTheme.MIDNIGHT to midnight).forEach { (theme, colors) ->
                val visuals = visualsForMood(mood, theme, colors)
                assertEquals(colors.surface, visuals.cardColor)
                assertEquals(colors.onSurface, visuals.onCardTextColor)
                val luminances = listOf(visuals.cardColor.luminance(), visuals.onCardTextColor.luminance())
                assertTrue((luminances.max() + 0.05f) / (luminances.min() + 0.05f) >= 4.5f)
                assertEquals(1f, visuals.cardColor.alpha, 0f)
            }
            assertNotEquals(visualsForMood(mood, AppTheme.LIGHT, light).background,
                visualsForMood(mood, AppTheme.DARK, dark).background)
        }
    }
    @Test fun systemPaletteColorsReachWeatherCards() {
        val dynamic = lightColorScheme(surface = Color(0xFFF0EDF6), onSurface = Color(0xFF201A28))
        val visuals = visualsForMood(WeatherMood.SUNNY, resolveAppTheme(AppTheme.SYSTEM, false), dynamic)
        assertEquals(dynamic.surface, visuals.cardColor)
        assertEquals(dynamic.onSurface, visuals.onCardTextColor)
    }
}
