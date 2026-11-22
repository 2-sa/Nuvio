package com.nuvio.app.core.cache

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * Thread-safe, KMP-compatible LRU cache backed by insertion-ordered [LinkedHashMap].
 *
 * Moves accessed entries to the tail so the head is always the least-recently-used key.
 */
internal class BoundedLruCache<K : Any, V : Any>(
    private val maxEntries: Int,
) {
    init {
        require(maxEntries > 0) { "maxEntries must be positive, was $maxEntries" }
    }

    private val lock = SynchronizedObject()
    private val entries = LinkedHashMap<K, V>()

    val size: Int
        get() = synchronized(lock) { entries.size }

    operator fun get(key: K): V? = synchronized(lock) {
        val value = entries.remove(key) ?: return@synchronized null
        entries[key] = value
        value
    }

    operator fun set(key: K, value: V) {
        synchronized(lock) {
            entries.remove(key)
            entries[key] = value
            evictToLocked(maxEntries, preserveKey = null)
        }
    }

    fun remove(key: K): V? = synchronized(lock) {
        entries.remove(key)
    }

    fun clear() {
        synchronized(lock) {
            entries.clear()
        }
    }

    fun firstValueOrNull(predicate: (K, V) -> Boolean): V? = synchronized(lock) {
        entries.entries.firstOrNull { (key, value) -> predicate(key, value) }?.value
    }

    /**
     * Evicts least-recently-used entries until at most [targetSize] remain.
     * When [preserveKey] is present in the cache, it is retained even if [targetSize] is 0.
     */
    fun trimToSize(targetSize: Int, preserveKey: K? = null) {
        val clamped = targetSize.coerceAtLeast(0)
        synchronized(lock) {
            evictToLocked(clamped, preserveKey = preserveKey)
        }
    }

    private fun evictToLocked(limit: Int, preserveKey: K?) {
        if (entries.size <= limit) return
        val iterator = entries.entries.iterator()
        while (entries.size > limit && iterator.hasNext()) {
            val next = iterator.next()
            if (preserveKey != null && next.key == preserveKey) continue
            iterator.remove()
        }
    }
}
