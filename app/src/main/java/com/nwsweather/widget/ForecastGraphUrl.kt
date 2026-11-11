package com.nwsweather.widget

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import com.nwsweather.presentation.TemperatureUnit

fun forecastGraphPage(latitude: Double, longitude: Double, unit: TemperatureUnit): HttpUrl =
    "https://forecast.weather.gov/MapClick.php".toHttpUrl().newBuilder()
        .addQueryParameter("w0", "t").addQueryParameter("w4", "sky")
        .addQueryParameter("w5", "pop").addQueryParameter("w25", "psnw4")
        .addQueryParameter("pqpfhr", "6").addQueryParameter("psnwhr", "6")
        .addQueryParameter("AheadHour", "59").addQueryParameter("FcstType", "graphical")
        .addQueryParameter("textField1", latitude.toString()).addQueryParameter("textField2", longitude.toString())
        .addQueryParameter("site", "all").addQueryParameter("dd", "1").addQueryParameter("menu", "1")
        .addQueryParameter("unit", if (unit == TemperatureUnit.CELSIUS) "1" else "0")
        .build()

fun extractForecastGraphImage(html: String, page: HttpUrl): HttpUrl? {
    val images = Regex("""<img\b[^>]*\bsrc\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    return images.findAll(html).mapNotNull { match ->
        page.resolve(match.groupValues[1].replace("&amp;", "&").replace("&#38;", "&"))
    }.firstOrNull {
        it.scheme == "https" && it.host == "forecast.weather.gov" && it.encodedPath == "/meteograms/Plotter.php"
    }
}
