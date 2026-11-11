package com.nwsweather.util

import com.nwsweather.data.model.*
import com.nwsweather.data.network.EpaUvApi
import com.nwsweather.data.repository.EpaUvRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class EpaUvTest {
    private val row = EpaUvHour("Kansas City", "MO", "Nov/11/2026 11 AM", 2)
    private fun parse(rows: List<EpaUvHour> = listOf(row), zone: String = "America/Chicago") =
        parseEpaUvHours(rows, "Kansas City", "MO", zone)
    private fun epoch(time: String) = Instant.parse(time).toEpochMilli()

    @Test fun decodesEpaResponseAndConvertsLocationTime() {
        val rows = Json { ignoreUnknownKeys = true }.decodeFromString<List<EpaUvHour>>(
            """[{"ORDER":7,"CITY":"Kansas City","STATE":"MO","DATE_TIME":"Nov/11/2026 11 AM","UV_VALUE":2}]"""
        )
        val hour = parse(rows).single()
        assertEquals(2, hour.value)
        assertEquals(epoch("2026-11-11T17:00:00Z"), hour.startEpochMs)
    }
    @Test fun matchesOnlyTheValidHourIncludingZero() {
        val hour = parse(listOf(row.copy(value = 0))).single()
        assertEquals(0, hour.value)
        assertFalse(hour.covers(epoch("2026-11-11T16:59:59Z")))
        assertTrue(hour.covers(epoch("2026-11-11T17:00:00Z")))
        assertTrue(hour.covers(epoch("2026-11-11T17:59:59Z")))
        assertFalse(hour.covers(epoch("2026-11-11T18:00:00Z")))
        assertFalse(hour.covers(epoch("2026-11-12T17:00:00Z")))
    }
    @Test fun usesSummerOffsetAndTwelveHourClock() {
        assertEquals(epoch("2026-07-11T17:00:00Z"), parse(listOf(row.copy(dateTime = "Jul/11/2026 12 PM"))).single().startEpochMs)
        assertEquals(epoch("2026-07-11T05:00:00Z"), parse(listOf(row.copy(dateTime = "Jul/11/2026 12 AM"))).single().startEpochMs)
    }
    @Test fun rejectsWrongLocationInvalidValuesAndInvalidDates() {
        assertTrue(parse(listOf(row.copy(city = "St. Louis"), row.copy(state = "KS"),
            row.copy(value = -1), row.copy(value = null), row.copy(dateTime = "Feb/30/2026 11 AM"),
            row.copy(dateTime = null))).isEmpty())
        assertTrue(parse(zone = "invalid").isEmpty())
    }
    @Test fun skipsAmbiguousAndNonexistentDstHours() {
        assertTrue(parse(listOf(row.copy(dateTime = "Nov/01/2026 01 AM"),
            row.copy(dateTime = "Mar/08/2026 02 AM"))).isEmpty())
    }
    @Test fun acceptsCaseDifferencesAndHighUv() {
        assertEquals(12, parse(listOf(row.copy(city = "KANSAS CITY", state = "mo", value = 12))).single().value)
    }
    @Test fun repositoryUsesMetadataAndPreservesParsedHours() = runBlocking {
        val api = object : EpaUvApi {
            override suspend fun getHourlyUv(city: String, state: String): List<EpaUvHour> {
                assertEquals("Kansas City", city)
                assertEquals("MO", state)
                return listOf(row)
            }
        }
        assertEquals(2, EpaUvRepository(api).fetch("Kansas City", "MO", "America/Chicago").single().value)
    }
    @Test fun missingMetadataDoesNotRequestAndErrorsAreOptional() = runBlocking {
        var calls = 0
        val api = object : EpaUvApi {
            override suspend fun getHourlyUv(city: String, state: String): List<EpaUvHour> {
                calls++
                throw java.io.IOException("Unavailable")
            }
        }
        val repository = EpaUvRepository(api)
        assertTrue(repository.fetch(null, "MO", "America/Chicago").isEmpty())
        assertTrue(repository.fetch("Kansas City", "MO", null).isEmpty())
        assertEquals(0, calls)
        assertTrue(repository.fetch("Kansas City", "MO", "America/Chicago").isEmpty())
        assertEquals(1, calls)
    }
    @Test(expected = CancellationException::class) fun cancellationPropagates() { runBlocking {
        val api = object : EpaUvApi {
            override suspend fun getHourlyUv(city: String, state: String): List<EpaUvHour> =
                throw CancellationException("Location changed")
        }
        EpaUvRepository(api).fetch("Kansas City", "MO", "America/Chicago")
    } }
}
