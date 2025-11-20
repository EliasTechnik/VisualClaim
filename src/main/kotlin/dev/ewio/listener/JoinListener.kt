package dev.ewio.listener

import dev.ewio.util.log
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class JoinListener(
    val onJoin: (event: PlayerJoinEvent) -> Unit = {}
): Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        log("VC: Player ${event.player.name} has joined the server.")
        onJoin(event)
    }
}