package com.andrecoura.homemonitor.support

import kotlinx.coroutines.CancellationException

/** Like assertThrows, but the block may suspend. */
suspend inline fun <reified T : Throwable> assertFailsWith(block: () -> Unit) {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        if (e is T) return
        throw AssertionError("Expected ${T::class.simpleName} but got $e", e)
    }
    throw AssertionError("Expected ${T::class.simpleName} but nothing was thrown")
}
