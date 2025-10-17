package dev.ewio.claim.repository.adapter

import dev.ewio.claim.repository.interfaces.DBInterface
import dev.ewio.util.UKey

class InMemoryRepository<T> (val extractKey: (T) -> UKey<T>): DBInterface<T> {
    private val storage = mutableMapOf<UKey<T>, T>()

    override fun find(key: UKey<T>): T? {
        return storage[key]
    }

    override fun upsert(item: T) {
        val key = extractKey(item)
        storage[key] = item
    }

    override fun delete(key: UKey<T>): Boolean {
        return storage.remove(key) != null
    }

    override fun all(): List<T> {
        return storage.values.toList()
    }
}