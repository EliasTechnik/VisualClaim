package dev.ewio.listener

import dev.ewio.util.log
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

class LeaveListener: Listener {

    @EventHandler
    fun onPlayerLeave(event: PlayerQuitEvent ) {
        // Handle player leaving the server
        log("VC: Player ${event.player.name} has left the server.")
    }

}