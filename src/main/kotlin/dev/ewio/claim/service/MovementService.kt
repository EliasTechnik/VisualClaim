package dev.ewio.claim.service

import dev.ewio.claim.definitions.VCMovementContext
import dev.ewio.listener.MoveListener
import dev.ewio.util.VCCache
import dev.ewio.util.log
import kotlinx.coroutines.CoroutineScope
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerMoveEvent
import java.util.UUID

/**
 * Service for handling player movement related to claims.
 *
 */

class MovementService(
    val registerListener: (listener: MoveListener) -> Unit,
    val preService: PrerequisiteService,
    val coroutineScope: CoroutineScope,
) {
    lateinit var moveListener: MoveListener

    val movementCache: VCCache<UUID, VCMovementContext, PlayerMoveEvent> = VCCache(
        fetch = { event ->
            this.getMovementContext(event.player)
        },
        extractKey = { event -> event.player.uniqueId },
        coroutineScope = coroutineScope
    )

    init {
        moveListener = MoveListener(
            onMoveChunk = { event ->
                onPlayerMoveChunk(event)
            }
        )
        registerListener(moveListener)
    }

    private suspend fun getMovementContext(player: Player) = preService.getPlayerContext(player)?.let{
        VCMovementContext(
            uuid = player.uniqueId,
            usesAutoclaim = it.first.player.autoClaim,
            usesBossbar = it.first.player.bossbar
        )
    }


    private fun initCache(){
        //val onlinePlayers = preService.getOnlinePlayers()

    }

    private fun onPlayerMoveChunk(event: PlayerMoveEvent) {
        // Handle player moving between chunks
        log("Player ${event.player.name} moved from ${event.from.chunk.x},${event.from.chunk.z} to chunk: ${event.to.chunk.x},${event.to.chunk.z}")
    }



}