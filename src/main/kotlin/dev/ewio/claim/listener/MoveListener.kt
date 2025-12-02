package dev.ewio.claim.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent


class MoveListener(
    val onMoveChunk: (event: PlayerMoveEvent) -> Unit
): Listener {

    @EventHandler(ignoreCancelled = true)
    fun onMove(event: PlayerMoveEvent) {
        if(event.from.chunk != event.to.chunk) onMoveChunk(event)
    }
}
