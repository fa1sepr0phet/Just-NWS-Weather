package com.nwsweather.util

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class AwaitGeocodingTest {
    @Test fun successfulLookupReturnsResult() = runBlocking {
        assertEquals("Kansas City", awaitGeocoding<String> { it(Result.success("Kansas City")) })
    }
    @Test(expected = IOException::class) fun failedLookupCompletesExceptionally(): Unit = runBlocking {
        awaitGeocoding<String> { it(Result.failure(IOException("Unavailable"))) }
        Unit
    }
    @Test fun cancellationCompletesWithoutWaitingForCallback() = runBlocking {
        lateinit var complete: (Result<String>) -> Unit
        var delivered = false
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            awaitGeocoding<String> { complete = it }
            delivered = true
        }
        job.cancelAndJoin()
        complete(Result.success("Late result"))
        assertTrue(job.isCancelled)
        assertFalse(delivered)
    }
    @Test fun duplicateCallbacksCannotResumeTwice() = runBlocking {
        assertEquals("First", awaitGeocoding<String> {
            it(Result.success("First"))
            it(Result.failure(IOException("Late error")))
        })
    }
}
