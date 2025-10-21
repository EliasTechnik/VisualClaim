package dev.ewio.claim.repository.interfaces

import dev.ewio.util.UKey

interface DBInterface<T> {
    fun find(key: UKey<T>): T?
    fun findAll(method: (T) -> Boolean): List<T>
    fun upsert(item: T) // insert / update
    fun forceDelete(key: UKey<T>): Boolean
    fun delete(key: UKey<T>, deleteFunction: (T) -> T): Boolean
    fun purge(test: (T) -> Boolean, selector: (T) -> UKey<T>){
        all().forEach {
            if(test(it)){
                forceDelete(selector(it))
            }
        }
    }
    fun all(): List<T>
}