package dev.ewio.claim.service

import dev.ewio.claim.definitions.VCPlayerContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bukkit.entity.Player
import java.util.UUID

class ContextCache(
    val fetch: suspend (player: Player) -> VCPlayerContext?,
    val coroutineScope: CoroutineScope
) {
    private val cache = mutableMapOf<UUID, VCPlayerContext>()

    fun get(player: Player): VCPlayerContext? {
        val hit = cache[player.uniqueId]
        if(hit != null){
            return hit
        }else{
            coroutineScope.launch {
                fetch(player)?.let {
                    cache[player.uniqueId] = it
                }
            }
            return null
        }
    }

    fun put(uuid: UUID, context: VCPlayerContext) {
        cache[uuid] = context
    }
}