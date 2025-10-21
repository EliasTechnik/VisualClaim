package dev.ewio.claim.repository.adapter

import dev.ewio.claim.repository.interfaces.DBInterface
import dev.ewio.util.UKey
import kotlin.collections.filter

class InMemoryRepository<T> (val extractKey: (T) -> UKey<T>, val isDeleted: (T) -> Boolean): DBInterface<T> {
    private val storage = mutableMapOf<UKey<T>, T>()

    override fun find(key: UKey<T>): T? {
        return storage.filter{isDeleted(it.value)}[key]
    }

    override fun findAll(method: (T) -> Boolean): List<T> {
        return storage.filter { isDeleted(it.value) }.values.toList().filter { method(it) }
    }

    override fun upsert(item: T) {
        val key = extractKey(item)
        storage[key] = item
    }

    override fun delete(key: UKey<T>, deleteFunction: (T) -> T): Boolean {
        val item = storage[key]
        item?.let {
            storage[key] = deleteFunction(it)
            return true
        }
        return false
    }

    override fun forceDelete(key: UKey<T>): Boolean {
        return storage.remove(key) != null
    }

    override fun all(): List<T> {
        return storage.filter{isDeleted(it.value)}.values.toList()
    }

}