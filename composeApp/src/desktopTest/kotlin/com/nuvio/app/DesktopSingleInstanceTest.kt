package com.nuvio.app

import java.nio.file.Files
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopSingleInstanceTest {
    @Test
    fun `second instance fails acquisition and forwards focus and url to primary`() {
        val lockDir = Files.createTempDirectory("nuvio-single-instance-test").toFile()
        val primary = DesktopSingleInstanceGuard(lockDir)
        val secondary = DesktopSingleInstanceGuard(lockDir)
        val receivedMessages = CopyOnWriteArrayList<String>()
        val latch = CountDownLatch(2)

        try {
            val acquiredPrimary = primary.tryAcquire { message ->
                receivedMessages.add(message)
                latch.countDown()
            }
            assertTrue(acquiredPrimary, "Primary guard should acquire the lock")

            val acquiredSecondary = secondary.tryAcquire { }
            assertFalse(acquiredSecondary, "Second instance should fail lock acquisition")

            val forwarded = secondary.forwardToPrimary(
                arrayOf("--ignored-flag", "  nuvio://detail/movie/tt1234567  "),
            )
            assertTrue(forwarded, "Secondary should forward focus and URL to primary")

            assertTrue(
                latch.await(5, TimeUnit.SECONDS),
                "Primary should receive forwarded FOCUS and URL messages",
            )
            assertEquals(
                listOf("FOCUS", "nuvio://detail/movie/tt1234567"),
                receivedMessages.toList(),
            )
        } finally {
            secondary.close()
            primary.close()
            lockDir.deleteRecursively()
        }
    }

    @Test
    fun `stale port file after crash does not block new primary acquisition`() {
        val lockDir = Files.createTempDirectory("nuvio-single-instance-stale-test").toFile()
        val stalePortFile = lockDir.resolve("nuvio-desktop.port")
        val primary = DesktopSingleInstanceGuard(lockDir)
        val receivedMessages = CopyOnWriteArrayList<String>()
        val latch = CountDownLatch(1)

        try {
            stalePortFile.writeText("59999")

            val acquired = primary.tryAcquire { message ->
                receivedMessages.add(message)
                latch.countDown()
            }
            assertTrue(acquired, "New primary should acquire lock even when stale port file exists")

            val updatedPort = stalePortFile.readText().trim().toIntOrNull()
            assertTrue(
                updatedPort != null && updatedPort != 59999,
                "Stale port file should be overwritten with the active listener port",
            )

            val client = DesktopSingleInstanceGuard(lockDir)
            try {
                assertTrue(client.forwardToPrimary(emptyArray()))
                assertTrue(latch.await(5, TimeUnit.SECONDS))
                assertEquals(listOf("FOCUS"), receivedMessages.toList())
            } finally {
                client.close()
            }
        } finally {
            primary.close()
            lockDir.deleteRecursively()
        }
    }
}
