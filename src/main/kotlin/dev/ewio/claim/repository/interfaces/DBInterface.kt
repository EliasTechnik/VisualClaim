package dev.ewio.claim.repository.interfaces

import dev.ewio.util.UKey

interface DBInterface<T> {
    fun find(key: UKey<T>): T?
    fun findAll(method: (T) -> Boolean): List<T> {
        return all().filter(method)
    }
    fun upsert(item: T): T // insert / update
    fun delete(key: UKey<T>): Boolean
    fun all(): List<T>
}