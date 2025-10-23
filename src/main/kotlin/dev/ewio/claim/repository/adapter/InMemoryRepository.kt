package dev.ewio.claim.repository.adapter

import dev.ewio.claim.repository.interfaces.DBInterface
import dev.ewio.util.UKey

/*
This is a simple in-memory implementation of the DBInterface for testing purposes.
It uses a mutable map to store items by their unique keys. The Keys have to be generated externally.
In past this was done by the uKeyProvider, but for compatibility reasons it has been removed in favor
of external key management by the LiteSQL database.
 */

class InMemoryRepository<T> (val extractKey: (T) -> UKey<T>): DBInterface<T> {
    private val storage = mutableMapOf<UKey<T>, T>()

    override fun find(key: UKey<T>): T? {
        return storage[key]
    }

    override fun upsert(item: T):T {
        val key = extractKey(item)
        storage[key] = item
        return storage[key]!!
    }

    override fun delete(key: UKey<T>): Boolean {
        return storage.remove(key) != null
    }

    override fun all(): List<T> {
        return storage.values.toList()
    }
}