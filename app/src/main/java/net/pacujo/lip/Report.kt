@file:Suppress("unused")

package net.pacujo.lip

private const val tag = "net.pacujo.lip"

fun logInfo(text: String) {
    android.util.Log.i(tag, text)
}

fun logWarning(text: String, e: Throwable? = null) {
    android.util.Log.w(tag, text, e)
}

fun logError(text: String, e: Throwable? = null) {
    android.util.Log.e(tag, text, e)
}

fun logDebug(enabled: Boolean, text: String, e: Throwable? = null) {
    if (enabled)
        android.util.Log.i(tag, "[DEBUG] $text", e)
}

inline fun <T, U> monitor(
    enabled: Boolean,
    text: String,
    translate: (T) -> U,
    block: () -> T): T
{
    logDebug(enabled, "begin $text")
    val result = try {
        block()
    } catch (e: Throwable) {
        logDebug(enabled, "exception $text", e)
        throw(e)
    }
    logDebug(enabled, "end $text -> ${translate(result)}")
    return result
}

inline fun <T> monitor(enabled: Boolean, text: String, block: () -> T) =
    monitor(enabled, text, { it }, block)

fun <T> objId(x: T) = System.identityHashCode(x)
