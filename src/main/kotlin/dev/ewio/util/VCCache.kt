package dev.ewio.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

//OOI = Object Of Interest

/**
 * A generic cache that fetches values based on an object of interest (OOI).
 *
 * @param K The type of the key used for caching.
 * @param V The type of the value being cached.
 * @param OOI The type of the object used to extract the key and fetch the value.
 * @property fetch A suspend function that fetches the value based on the OOI.
 * @property extractKey A function that extracts the key from the OOI.
 * @property coroutineScope The CoroutineScope used for launching fetch operations.
 *
 * VCCache replaced the old ContextCache to provide a more flexible caching mechanism that can be used
 * across different types of objects and keys.
 */


class VCCache<K,V,OOI>(
    val fetch: suspend (obj: OOI) -> V?,
    val extractKey: (obj: OOI) -> K,
    val coroutineScope: CoroutineScope
) {
    private val cache = mutableMapOf<K, V>()

    fun get(obj: OOI): V? {
        val hit = cache[extractKey(obj)]
        if(hit != null){
            return hit
        }else{
            coroutineScope.launch {
                fetch(obj)?.let {
                    cache[extractKey(obj)] = it
                }
            }
            return null
        }
    }

    fun put(key: K, value: V) {
        cache[key] = value
    }
}