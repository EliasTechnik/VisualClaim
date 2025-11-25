package dev.ewio.util

/**
 *  The VCWrappedLookupTable class is a specialized data structure that maintains a
 *  bidirectional mapping between keys of type K and lists of items of type I.
 *
 *  K -> I1, I2, I3
 *
 *  I1 -> K
 *  I2 -> K
 *  I3 -> K
 *
 */
class VCWrappedLookupTable<K, I>(
    private val keyWrapped: MutableMap<K, List<I>> = mutableMapOf(),
    private val valueKey: MutableMap<I, K> = mutableMapOf(),
    val wrap: (item: I, oldWrap: List<I>?) -> List<I>,
) {
    fun put(key: K, item: I) {
        keyWrapped[key] = wrap(item, keyWrapped[key])
        valueKey[item] = key
    }

    fun getByKey(key: K): List<I>? {
        return keyWrapped[key]
    }

    fun getByItem(item: I): K? {
        return valueKey[item]
    }

    /**
     * Removes all items associated with the given key.
     */
    fun removeByKey(key: K) {
        val wrapped = keyWrapped.remove(key)
        wrapped?.forEach {
            valueKey.remove(it)
        }
    }

    /**
     * Removes a specific item associated with the given key.
     * If the resulting list is empty, the key is also removed from the map.
     */
    fun removeItem(key: K, itemToRemove: I) {
        val wrapped = keyWrapped[key]
        if (wrapped != null) {
            val newWrapped = wrapped.filter { it != itemToRemove }
            if (newWrapped.isEmpty()) {
                keyWrapped.remove(key)
            } else {
                keyWrapped[key] = newWrapped
            }
        }
    }
}