package com.nuvio.app.core.cache

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BoundedLruCacheTest {

    @Test
    fun evictsLeastRecentlyUsedEntryWhenCapacityExceeded() {
        val cache = BoundedLruCache<String, Int>(maxEntries = 3)
        cache["a"] = 1
        cache["b"] = 2
        cache["c"] = 3

        // Access "a" so "b" becomes the least recently used entry.
        assertEquals(1, cache["a"])

        cache["d"] = 4

        assertEquals(3, cache.size)
        assertNull(cache["b"])
        assertEquals(1, cache["a"])
        assertEquals(3, cache["c"])
        assertEquals(4, cache["d"])
    }

    @Test
    fun updatingExistingKeyRefreshesRecencyWithoutGrowingSize() {
        val cache = BoundedLruCache<String, Int>(maxEntries = 2)
        cache["a"] = 1
        cache["b"] = 2
        cache["a"] = 10
        cache["c"] = 3

        assertEquals(2, cache.size)
        assertEquals(10, cache["a"])
        assertNull(cache["b"])
        assertEquals(3, cache["c"])
    }

    @Test
    fun trimToSizePreservesSpecifiedKey() {
        val cache = BoundedLruCache<String, Int>(maxEntries = 4)
        cache["a"] = 1
        cache["b"] = 2
        cache["c"] = 3
        cache["d"] = 4

        cache.trimToSize(targetSize = 0, preserveKey = "a")

        assertEquals(1, cache.size)
        assertEquals(1, cache["a"])
        assertNull(cache["b"])
        assertNull(cache["c"])
        assertNull(cache["d"])
    }

    @Test
    fun firstValueOrNullMatchesPredicate() {
        val cache = BoundedLruCache<String, String>(maxEntries = 3)
        cache["100:movie:en"] = "Original Title"
        cache["200:tv:en"] = "Other Show"

        assertEquals(
            "Original Title",
            cache.firstValueOrNull { key, _ -> key.startsWith("100:movie:") },
        )
        assertNull(cache.firstValueOrNull { key, _ -> key.startsWith("999:") })
    }
}
