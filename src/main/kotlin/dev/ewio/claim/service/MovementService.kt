package dev.ewio.claim.service

import dev.ewio.claim.definitions.VCClaim
import dev.ewio.claim.definitions.VCMovementContext
import dev.ewio.claim.definitions.VCPlayerContext
import dev.ewio.listener.MoveListener
import dev.ewio.util.VCCache
import dev.ewio.util.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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
    val cc: CentralCache
) {
    var moveListener: MoveListener

    /*
    val movementCache: VCCache<UUID, VCMovementContext, UUID> = VCCache(
        fetch = { uuid ->
            this.getMovementContext(uuid)
        },
        extractKey = { it },
        coroutineScope = coroutineScope
    )

     */

    init {
        moveListener = MoveListener(
            onMoveChunk = { event ->
                onPlayerMoveChunk(event)
            }
        )
        registerListener(moveListener)
        preService.registerMovementService(this)
    }

    private suspend fun getMovementContext(uuid: UUID): VCMovementContext {
        preService.getCachedPlayerContext(uuid)?.let{
            VCMovementContext(
                uuid = it.player.mcUUID,
            usesAutoclaim = it.player.autoClaim,
            usesBossbar = it.player.bossbar
            )
        }?: preService.get^
    }


    private fun initCache(){
        //val onlinePlayers = preService.getOnlinePlayers()

    }




    private fun onPlayerMoveChunk(event: PlayerMoveEvent) {
        // Handle player moving between chunks
        log("Player ${event.player.name} moved from ${event.from.chunk.x},${event.from.chunk.z} to chunk: ${event.to.chunk.x},${event.to.chunk.z}")

        coroutineScope.launch {
            val context = cc.getCachedPlayerContext(event.player)

            if(context != null) {
                if(context.player.autoClaim){
                    preService.handleAutoClaimOnMove(context, event)
                }
            }
        }
    }

    suspend fun activateAutoClaimForPlayer(playerContext: VCPlayerContext, claim: VCClaim) {

        var mc = movementCache.get(playerContext.player.mcUUID)

        movementCache.put(playerContext.player.uuid, VCMovementContext()
            uuid = playerContext.player.uuid,
            usesAutoclaim = true,
            usesBossbar = playerContext.player.bossbar
        ))
    }


}