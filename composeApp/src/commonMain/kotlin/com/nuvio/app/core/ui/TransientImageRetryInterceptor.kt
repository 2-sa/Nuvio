package com.nuvio.app.core.ui

import coil3.intercept.Interceptor
import coil3.network.HttpException
import coil3.request.ErrorResult
import coil3.request.ImageResult
import coil3.request.NullRequestDataException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

internal const val DefaultImageMaxRetries = 2

private const val MaxCauseChainDepth = 16
private val HttpStatusCodeRegex = Regex("""\bHTTP\s+(\d{3})\b""", RegexOption.IGNORE_CASE)

internal fun isRetryableHttpStatusCode(statusCode: Int): Boolean =
    statusCode == 408 || statusCode == 429 || statusCode in 500..599

internal fun extractHttpStatusCode(throwable: Throwable): Int? {
    val causes = collectCauseChain(throwable)
    for (cause in causes) {
        if (cause is HttpException) {
            return cause.response.code
        }
    }
    for (cause in causes) {
        val message = cause.message ?: continue
        val match = HttpStatusCodeRegex.find(message) ?: continue
        val code = match.groupValues[1].toIntOrNull() ?: continue
        if (code in 100..599) {
            return code
        }
    }
    return null
}

internal fun shouldRetryImageError(
    throwable: Throwable,
    attempt: Int,
    maxRetries: Int = DefaultImageMaxRetries,
): Boolean {
    if (attempt < 0 || attempt >= maxRetries) return false

    val causes = collectCauseChain(throwable)
    if (causes.any { it is CancellationException || it is NullRequestDataException }) {
        return false
    }

    extractHttpStatusCode(throwable)?.let { statusCode ->
        return isRetryableHttpStatusCode(statusCode)
    }

    if (causes.any(::isNonRetryableCause)) {
        return false
    }

    return true
}

internal fun imageRetryDelayMillis(attempt: Int): Long = when (attempt) {
    0 -> 350L
    else -> 900L
}

private fun collectCauseChain(throwable: Throwable): List<Throwable> {
    val causes = mutableListOf<Throwable>()
    var current: Throwable? = throwable
    while (current != null && causes.size < MaxCauseChainDepth && causes.none { it === current }) {
        causes += current
        current = current.cause
    }
    return causes
}

private fun isNonRetryableCause(cause: Throwable): Boolean {
    if (cause is IllegalArgumentException || cause is UnsupportedOperationException || cause is Error) {
        return true
    }
    val message = cause.message?.lowercase() ?: return false
    return message.contains("unable to create a fetcher") ||
        message.contains("unable to create a decoder") ||
        message.contains("invalid url") ||
        message.contains("malformed") ||
        message.contains("unsupported scheme")
}

/**
 * Retries transient image fetch failures (HTTP 408/429/5xx and socket/timeout I/O errors)
 * with short backoff before propagating an error to downstream fallbacks.
 */
class TransientImageRetryInterceptor(
    private val maxRetries: Int = DefaultImageMaxRetries,
) : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        var attempt = 0
        while (true) {
            val result = chain.proceed()
            if (result !is ErrorResult || !shouldRetryImageError(result.throwable, attempt, maxRetries)) {
                return result
            }
            delay(imageRetryDelayMillis(attempt))
            attempt++
        }
    }
}
