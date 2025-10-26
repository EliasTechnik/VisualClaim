package dev.ewio.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class SimpleCache<T>(
    val fetchAll: suspend () -> List<T>,
    val invalidAfter: Long = 180 * 1000L,
    val coroutineScope: CoroutineScope
) {
    private var cache: List<T>? = null
    private var lastFetchTime: Long = 0L

    fun getAll(): List<T> {
        if(System.currentTimeMillis() > lastFetchTime + invalidAfter){
            cache = null
        }
        if(cache != null){
            return cache!!
        }else{
            coroutineScope.launch {
                cache = fetchAll()
                lastFetchTime = System.currentTimeMillis()
            }
            return listOf<T>()
        }
    }
}