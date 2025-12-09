package dev.ewio.claim.listener

import dev.ewio.util.log
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerEditBookEvent



class BookEditListener(
    val onBookEdit: (event: PlayerEditBookEvent) -> Unit
): Listener {
    @EventHandler
    fun onEditBook(event: PlayerEditBookEvent){
        log("Book edited by ${event.player.name}, title: ${event.newBookMeta.title}, pages: ${event.newBookMeta.pages.size}")
        onBookEdit(event)
    }
}