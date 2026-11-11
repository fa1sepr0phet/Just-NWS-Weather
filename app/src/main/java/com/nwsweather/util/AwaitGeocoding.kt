package com.nwsweather.util

import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean

/** Android geocoding cannot be cancelled, but cancelled searches must ignore late callbacks. */
internal suspend fun <T> awaitGeocoding(register: ((Result<T>) -> Unit) -> Unit): T =
    suspendCancellableCoroutine { continuation ->
        val completed = AtomicBoolean(false)
        register { result ->
            if (completed.compareAndSet(false, true) && continuation.isActive) {
                continuation.resumeWith(result)
            }
        }
    }
