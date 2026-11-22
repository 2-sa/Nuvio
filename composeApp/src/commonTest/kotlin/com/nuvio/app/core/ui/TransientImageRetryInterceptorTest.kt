package com.nuvio.app.core.ui

import coil3.request.NullRequestDataException
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TransientImageRetryInterceptorTest {

    @Test
    fun permanent_4xx_status_codes_are_not_retryable() {
        listOf(400, 401, 403, 404, 410).forEach { code ->
            assertFalse(isRetryableHttpStatusCode(code), "Expected $code not to be retryable")
            assertFalse(
                shouldRetryImageError(RuntimeException("HTTP $code Error"), attempt = 0),
                "Expected HTTP $code not to be retried",
            )
        }
    }

    @Test
    fun transient_http_status_codes_retry_up_to_maxRetries() {
        listOf(408, 429, 500, 502, 503, 504).forEach { code ->
            assertTrue(isRetryableHttpStatusCode(code), "Expected $code to be retryable")
            val error = RuntimeException("HTTP $code")
            assertTrue(shouldRetryImageError(error, attempt = 0), "Expected attempt 0 for $code to retry")
            assertTrue(shouldRetryImageError(error, attempt = 1), "Expected attempt 1 for $code to retry")
            assertFalse(
                shouldRetryImageError(error, attempt = DefaultImageMaxRetries),
                "Expected attempt >= maxRetries for $code not to retry",
            )
        }
    }

    @Test
    fun extractHttpStatusCode_parses_message_and_cause_chain() {
        assertEquals(503, extractHttpStatusCode(RuntimeException("HTTP 503 Service Unavailable")))
        assertEquals(
            404,
            extractHttpStatusCode(RuntimeException("Outer failure", IllegalStateException("http 404 not found"))),
        )
        assertNull(extractHttpStatusCode(IOException("Connection reset by peer")))
    }

    @Test
    fun cancellation_and_null_request_data_never_retry() {
        assertFalse(shouldRetryImageError(CancellationException("Cancelled"), attempt = 0))
        assertFalse(
            shouldRetryImageError(
                RuntimeException("Wrapped", CancellationException("Cancelled")),
                attempt = 0,
            ),
        )
        assertFalse(shouldRetryImageError(NullRequestDataException(), attempt = 0))
        assertFalse(
            shouldRetryImageError(
                RuntimeException("Wrapped", NullRequestDataException()),
                attempt = 0,
            ),
        )
    }

    @Test
    fun generic_io_and_timeout_errors_retry_while_invalid_argument_and_unsupported_do_not() {
        val ioError = IOException("Connection reset")
        assertTrue(shouldRetryImageError(ioError, attempt = 0))
        assertTrue(shouldRetryImageError(ioError, attempt = 1))
        assertFalse(shouldRetryImageError(ioError, attempt = DefaultImageMaxRetries))

        val timeoutError = RuntimeException("Socket timeout")
        assertTrue(shouldRetryImageError(timeoutError, attempt = 0))
        assertTrue(shouldRetryImageError(timeoutError, attempt = 1))
        assertFalse(shouldRetryImageError(timeoutError, attempt = DefaultImageMaxRetries))

        assertFalse(shouldRetryImageError(IllegalArgumentException("Invalid URL"), attempt = 0))
        assertFalse(shouldRetryImageError(UnsupportedOperationException("Unsupported image"), attempt = 0))
        assertFalse(shouldRetryImageError(Error("Out of memory"), attempt = 0))
    }

    @Test
    fun imageRetryDelayMillis_returns_ascending_backoff_delays() {
        val firstDelay = imageRetryDelayMillis(0)
        val secondDelay = imageRetryDelayMillis(1)
        assertEquals(350L, firstDelay)
        assertEquals(900L, secondDelay)
        assertTrue(firstDelay < secondDelay)
    }
}
