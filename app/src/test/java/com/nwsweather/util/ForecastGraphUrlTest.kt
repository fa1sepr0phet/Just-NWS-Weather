package com.nwsweather.util

import com.nwsweather.widget.forecastGraphPage
import com.nwsweather.widget.extractForecastGraphImage
import com.nwsweather.presentation.TemperatureUnit
import org.junit.Assert.*
import org.junit.Test

class ForecastGraphUrlTest {
    private val page = forecastGraphPage(38.983, -94.585, TemperatureUnit.FAHRENHEIT)
    @Test fun preservesRequestedGraphSettings() {
        assertEquals("59", page.queryParameter("AheadHour"))
        assertEquals("t", page.queryParameter("w0"))
        assertEquals("sky", page.queryParameter("w4"))
        assertEquals("pop", page.queryParameter("w5"))
        assertEquals("psnw4", page.queryParameter("w25"))
        assertEquals("6", page.queryParameter("psnwhr"))
        assertEquals("graphical", page.queryParameter("FcstType"))
        assertEquals("38.983", page.queryParameter("textField1"))
        assertEquals("-94.585", page.queryParameter("textField2"))
    }
    @Test fun usesSelectedLocationAndUnits() {
        val url = forecastGraphPage(40.0, -90.0, TemperatureUnit.CELSIUS)
        assertEquals("40.0", url.queryParameter("textField1"))
        assertEquals("-90.0", url.queryParameter("textField2"))
        assertEquals("1", url.queryParameter("unit"))
        assertEquals("0", page.queryParameter("unit"))
    }
    @Test fun extractsImageFromNwsPageAndDecodesAmpersands() {
        val html = """<img src="/logo.png"><a><img src="meteograms/Plotter.php?lat=38.983&amp;lon=-94.585&amp;ahour=59" usemap="#MouseVal"></a>"""
        val url = extractForecastGraphImage(html, page)!!
        assertEquals("forecast.weather.gov", url.host)
        assertEquals("/meteograms/Plotter.php", url.encodedPath)
        assertEquals("-94.585", url.queryParameter("lon"))
    }
    @Test fun handlesSingleQuotesAndRejectsUnrelatedImages() {
        assertNotNull(extractForecastGraphImage("<IMG SRC='/meteograms/Plotter.php?lat=1'>", page))
        assertNull(extractForecastGraphImage("<img src='https://example.com/meteograms/Plotter.php'>", page))
        assertNull(extractForecastGraphImage("<img src='/logo.png'>", page))
        assertNull(extractForecastGraphImage("<img src='http://forecast.weather.gov/meteograms/Plotter.php'>", page))
        assertNull(extractForecastGraphImage("Service unavailable", page))
    }
}
