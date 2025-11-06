package dev.ewio.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


/**
 * A generic cache that fetches values based on an object of interest (OOI).
 *
 * @param K The type of the key used for caching.
 * @param V The type of the value being cached.
 * @property fetch A suspend function that fetches the value based on the OOI.
 *
 * VCCache replaced the old ContextCache to provide a more flexible caching mechanism that can be used
 * across different types of objects and keys.
 */


class VCCache<K,V>(
    val fetch: suspend (key: K) -> V?,
) {
    private val cache = mutableMapOf<K, V>()

    suspend fun get(key: K): V? {
        val hit = cache[key]
        if(hit != null){
            return hit
        }else{
            fetch(key)?.let {
                cache[key] = it
            }
            return cache[key]
        }
    }

    fun getIfCached(key: K): V? {
        return cache[key]
    }

    fun put(key: K, value: V) {
        cache[key] = value
    }
}