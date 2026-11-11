package com.nwsweather.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.action.clickable
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.LocalSize
import androidx.glance.appwidget.SizeMode
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.DpSize
import com.nwsweather.MainActivity
import com.nwsweather.myapp.R
import com.nwsweather.data.local.AppDatabase
import com.nwsweather.data.local.WeatherSnapshotEntity
import com.nwsweather.data.local.SettingsManager
import com.nwsweather.ui.theme.WeatherMood
import com.nwsweather.ui.theme.mapWeatherMood
import com.nwsweather.util.convertTemperature
import com.nwsweather.presentation.TemperatureUnit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.glance.appwidget.lazy.LazyColumn
import com.nwsweather.data.model.NwsForecastPeriod
import com.nwsweather.util.WidgetDailyForecast
import com.nwsweather.util.widgetDailyForecast
import com.nwsweather.util.formatTemperature
import kotlinx.serialization.json.Json
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeatherAppWidget : GlanceAppWidget() {
    companion object {
        private val SIZE_2X1 = DpSize(140.dp, 70.dp)
        private val SIZE_4X1 = DpSize(280.dp, 70.dp)
        private val SIZE_2X2 = DpSize(140.dp, 140.dp)
        private val SIZE_4X2 = DpSize(280.dp, 140.dp)
        private val SIZE_4X4 = DpSize(280.dp, 280.dp)
    }

    override val sizeMode = SizeMode.Responsive(
        setOf(SIZE_2X1, SIZE_4X1, SIZE_2X2, SIZE_4X2, SIZE_4X4)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = withContext(Dispatchers.IO) {
            AppDatabase.getInstance(context).weatherSnapshotDao().getLatest()
        }
        val settings = SettingsManager(context)
        val unit = settings.unit.value

        provideContent {
            WeatherWidgetContent(snapshot = snapshot, preferredUnit = unit)
        }
    }
}

@Composable
private fun WeatherWidgetContent(snapshot: WeatherSnapshotEntity?, preferredUnit: TemperatureUnit) {
    val size = LocalSize.current
    val isPill = size.height < 100.dp
    val radius = if (isPill) 999.dp else 24.dp

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(radius)
            .background(backgroundColorFor(snapshot))
            .clickable(actionStartActivity<MainActivity>())
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (snapshot == null) {
            EmptyWeatherWidget()
        } else {
            when {
                size.width <= 160.dp && size.height <= 90.dp -> Loaded2x1Pill(snapshot, preferredUnit)
                size.width <= 160.dp -> Loaded2x2Square(snapshot, preferredUnit)
                size.height <= 90.dp -> Loaded4x1Pill(snapshot, preferredUnit)
                else -> Loaded4x2Rect(snapshot, preferredUnit)
            }
        }
    }
}

@Composable
private fun Loaded2x1Pill(snapshot: WeatherSnapshotEntity, preferredUnit: TemperatureUnit) {
    val displayTemp = convertTemperature(snapshot.temperature, snapshot.temperatureUnit, preferredUnit)
    Row(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ConditionIcon(snapshot)
        Spacer(modifier = GlanceModifier.width(8.dp))
        Column {
            Text(
                text = "$displayTemp°",
                style = TextStyle(color = widgetColor(Color.White), fontWeight = FontWeight.Bold, fontSize = 20.sp)
            )
            Text(
                text = snapshot.locationName,
                style = TextStyle(color = widgetColor(Color(0xE6FFFFFF)), fontSize = 10.sp),
                maxLines = 1
            )
            HighLow(snapshot, preferredUnit, 10)
        }
    }
}

@Composable
private fun Loaded2x2Square(snapshot: WeatherSnapshotEntity, preferredUnit: TemperatureUnit) {
    val displayTemp = convertTemperature(snapshot.temperature, snapshot.temperatureUnit, preferredUnit)
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ConditionIcon(snapshot)
        Text(
            text = "$displayTemp°",
            style = TextStyle(color = widgetColor(Color.White), fontWeight = FontWeight.Bold, fontSize = 32.sp)
        )
        Text(
            text = snapshot.locationName,
            style = TextStyle(color = widgetColor(Color.White), fontSize = 12.sp),
            maxLines = 1
        )
        HighLow(snapshot, preferredUnit, 11)
        Text(
            text = snapshot.shortForecast,
            style = TextStyle(color = widgetColor(Color(0xCCFFFFFF)), fontSize = 11.sp),
            maxLines = 1
        )
    }
}

@Composable
private fun Loaded4x1Pill(snapshot: WeatherSnapshotEntity, preferredUnit: TemperatureUnit) {
    val displayTemp = convertTemperature(snapshot.temperature, snapshot.temperatureUnit, preferredUnit)
    Row(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ConditionIcon(snapshot)
        Spacer(modifier = GlanceModifier.width(12.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = "$displayTemp° ${snapshot.locationName}",
                style = TextStyle(color = widgetColor(Color.White), fontWeight = FontWeight.Bold, fontSize = 18.sp),
                maxLines = 1
            )
            val details = listOfNotNull(
                highLowText(snapshot, preferredUnit),
                snapshot.shortForecast
            ).joinToString(" • ")
            Text(
                text = details,
                style = TextStyle(color = widgetColor(Color(0xE6FFFFFF)), fontSize = 12.sp),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun Loaded4x2Rect(snapshot: WeatherSnapshotEntity, preferredUnit: TemperatureUnit) {
    val daily = snapshot.dailyForecast()
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ConditionIcon(snapshot)
            Spacer(GlanceModifier.width(8.dp))
            Text(
                text = formatTemperature(snapshot.temperature, snapshot.temperatureUnit, preferredUnit),
                style = TextStyle(color = widgetColor(Color.White), fontWeight = FontWeight.Bold, fontSize = 26.sp)
            )
            Spacer(GlanceModifier.width(8.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(snapshot.locationName, style = TextStyle(color = widgetColor(Color.White), fontSize = 12.sp), maxLines = 1)
                val updateTime = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(snapshot.updatedAtEpochMs))
                Text("Reading $updateTime", style = TextStyle(color = widgetColor(Color(0xE6FFFFFF)), fontSize = 10.sp))
            }
        }
        HighLow(snapshot, preferredUnit, 13)
        LazyColumn(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
            item {
                Text(
                    text = daily?.date?.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
                        ?.let { "$it forecast" } ?: "Daily forecast unavailable",
                    style = TextStyle(color = widgetColor(Color.White), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                )
            }
            item {
                if (daily?.day != null) DailyPeriod("Day", daily.day)
                else Text("Daytime forecast unavailable", style = TextStyle(color = widgetColor(Color.White), fontSize = 12.sp))
            }
            item {
                if (daily?.night != null) DailyPeriod("Night", daily.night)
                else Text("Night forecast unavailable", style = TextStyle(color = widgetColor(Color.White), fontSize = 12.sp))
            }
            item { DetailRow("Current conditions", snapshot.shortForecast) }
            item { DetailRow("Wind", "${snapshot.windSpeed} ${snapshot.windDirection}") }
            item { snapshot.humidity?.let { DetailRow("Humidity", "$it%") } }
        }
    }
}

@Composable
private fun DailyPeriod(label: String, period: NwsForecastPeriod) {
    Column(modifier = GlanceModifier.padding(vertical = 4.dp)) {
        Text(label, style = TextStyle(color = widgetColor(Color.White), fontWeight = FontWeight.Bold, fontSize = 12.sp))
        Text(
            period.detailedForecast?.takeIf { it.isNotBlank() } ?: period.shortForecast ?: "Forecast unavailable",
            style = TextStyle(color = widgetColor(Color.White), fontSize = 12.sp)
        )
        val details = listOfNotNull(
            period.probabilityOfPrecipitation?.value?.toInt()?.let { "Precip $it%" },
            period.windSpeed?.let { "Wind $it ${period.windDirection.orEmpty()}" }
        ).joinToString(" \u00B7 ")
        if (details.isNotEmpty()) Text(details, style = TextStyle(color = widgetColor(Color(0xE6FFFFFF)), fontSize = 11.sp))
    }
}

private fun WeatherSnapshotEntity.dailyForecast(): WidgetDailyForecast? {
    val now = System.currentTimeMillis()
    val fetched = forecastFetchedAtEpochMs ?: return null
    if (now - fetched !in 0..86_400_000L) return null
    val periods = runCatching { Json.decodeFromString<List<NwsForecastPeriod>>(forecastPeriodsJson ?: "[]") }
        .getOrDefault(emptyList())
    return widgetDailyForecast(periods, forecastTimeZone, now)
}

private fun highLowText(snapshot: WeatherSnapshotEntity, unit: TemperatureUnit): String? {
    val daily = snapshot.dailyForecast() ?: return null
    fun temperature(period: NwsForecastPeriod?) = period?.let {
        "${convertTemperature(it.temperature, it.temperatureUnit, unit)}\u00B0"
    } ?: "--"
    return "H ${temperature(daily.day)} / L ${temperature(daily.night)}"
}

@Composable
private fun HighLow(snapshot: WeatherSnapshotEntity, unit: TemperatureUnit, fontSize: Int) {
    highLowText(snapshot, unit)?.let {
        Text(it, style = TextStyle(color = widgetColor(Color.White), fontSize = fontSize.sp), maxLines = 1)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = GlanceModifier.padding(vertical = 2.dp)) {
        Text(
            text = label,
            style = TextStyle(color = widgetColor(Color(0xCCFFFFFF)), fontSize = 10.sp)
        )
        Text(
            text = value,
            style = TextStyle(color = widgetColor(Color.White), fontSize = 12.sp, fontWeight = FontWeight.Medium),
            maxLines = 1
        )
    }
}

@Composable
private fun ConditionIcon(snapshot: WeatherSnapshotEntity) {
    val mood = mapWeatherMood(snapshot.shortForecast, snapshot.isDaytime)
    val resourceId = when (mood) {
        WeatherMood.SUNNY -> R.drawable.ic_sun
        WeatherMood.CLOUDY, WeatherMood.CLOUDY_NIGHT -> R.drawable.ic_cloud
        WeatherMood.RAIN -> R.drawable.ic_rain
        WeatherMood.STORM -> R.drawable.ic_storm
        WeatherMood.SNOW -> R.drawable.ic_snow
        WeatherMood.CLEAR_NIGHT -> R.drawable.ic_moon
    }

    Image(
        provider = ImageProvider(resourceId),
        contentDescription = snapshot.shortForecast,
        modifier = GlanceModifier.size(32.dp)
    )
}

@Composable
private fun EmptyWeatherWidget() {
    Text(text = "No data", style = TextStyle(color = widgetColor(Color.White)))
}

private fun backgroundColorFor(snapshot: WeatherSnapshotEntity?): ColorProvider {
    if (snapshot == null) return widgetColor(Color(0xFF31435F))
    
    val mood = mapWeatherMood(snapshot.shortForecast, snapshot.isDaytime)
    
    val color = when (mood) {
        WeatherMood.SUNNY -> Color(0xFF4F86C6)
        WeatherMood.CLOUDY -> Color(0xFF4D626F)
        WeatherMood.RAIN -> Color(0xFF355C7D)
        WeatherMood.STORM -> Color(0xFF16213E)
        WeatherMood.SNOW -> Color(0xFF425E78)
        WeatherMood.CLEAR_NIGHT -> Color(0xFF1F2A44)
        WeatherMood.CLOUDY_NIGHT -> Color(0xFF1A2633)
    }
    return widgetColor(color)
}

private fun widgetColor(color: Color): ColorProvider =
    androidx.glance.color.ColorProvider(day = color, night = color)
