package dev.ewio.claim.service

import dev.ewio.claim.definitions.VCPlayerContext
import dev.ewio.util.VCCache
import dev.ewio.util.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.UUID

class CentralCache(
    val coroutineScope: CoroutineScope,
    val claimService: ClaimService,
    val permissionService: PermissionService
) {
    private val contextCache = VCCache<UUID, VCPlayerContext, Player>(
        fetch = { uuid ->
            this.getPlayerContext(uuid)
        },
        extractKey = { player ->
            player.uniqueId
        },
        coroutineScope = coroutineScope
    )

    private suspend fun getPlayerContextFromDB(player: Player): VCPlayerContext? {
        val context = claimService.registerPlayerContextByUUID(player.uniqueId)?: return null
        return VCPlayerContext(
            restrictions = permissionService.getRestrictionsForPlayer(
                player = context.player,
                bukkitPlayer = player
            ),
            dbContext = context,
        )
    }

    private suspend fun getFreshPlayerContextWithoutCaching(context: VCPlayerContext): VCPlayerContext? {
        val updatedContext = claimService.getPlayerContextByKey(context.player.key) ?: return null
        return VCPlayerContext(
            restrictions = context.restrictions,
            dbContext = updatedContext
        )
    }

    suspend fun <T> updatePlayerContextCache(context: VCPlayerContext, exFirst: suspend () -> T): T  {
        val result = exFirst()
        val updatedContext = this.getFreshPlayerContextWithoutCaching(context)?: return result
        contextCache.put(updatedContext.player.mcUUID, updatedContext)
        return result
    }

    suspend fun getPlayerContext(sender: CommandSender): Pair<VCPlayerContext, Player>? {
        val realPlayer = sender as? Player ?: return null
        log("Fetching player context for ${realPlayer.name} (${realPlayer.uniqueId})")
        getPlayerContextFromDB(realPlayer)?.let{
            contextCache.put(realPlayer.uniqueId, it)
            return Pair(it, realPlayer)
        }
        return null
    }

    suspend fun getFreshPlayerContext(context: VCPlayerContext): VCPlayerContext?{
        val updatedContext = claimService.getPlayerContextByKey(context.player.key) ?: return null
        val newContext = VCPlayerContext(
            restrictions = context.restrictions,
            dbContext = updatedContext
        )
        contextCache.put(updatedContext.player.mcUUID, newContext)
        return newContext
    }

    fun getCachedPlayerContext(player: Player): VCPlayerContext? {
        return contextCache.get(player)
    }

    fun getCachedPlayerContext(uuid: UUID): VCPlayerContext? {
        return contextCache.get(uuid)
    }



}