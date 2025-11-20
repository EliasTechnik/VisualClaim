package dev.ewio.claim.service

import dev.ewio.claim.definitions.VCPlayerContext
import dev.ewio.claim.definitions.VCPlayerDBContext
import dev.ewio.util.VCCache
import dev.ewio.util.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.UUID

class CentralCache(
    val coroutineScope: CoroutineScope,
    val claimService: ClaimService,
    val permissionService: PermissionService
) {
    private val contextCache = VCCache<UUID, VCPlayerContext>(
        fetch = { uuid ->
            val player = Bukkit.getPlayer(uuid)?: return@VCCache null
            buildPlayerContext(player)
        }
    )

    /**
     * Throws a uuid at the claim service which puts it in its database. If the uuid is already known the context around that player is retrieved.
     */
    private suspend fun getPlayerContextFromDB(uuid: UUID): VCPlayerDBContext? = claimService.registerPlayerContextByUUID(uuid)

    /**
     * Builds a player context. IT DOES NOT CACHE!
     */
    private suspend fun buildPlayerContext(player: Player): VCPlayerContext? {
        val dbContext = getPlayerContextFromDB(player.uniqueId)?: return null
        return VCPlayerContext(
                dbContext = dbContext,
                restrictions = permissionService.getRestrictionsForPlayer(
                    dbContext.player,
                    player
                )
            )
    }

    /**
     * This gets the player Context. Cached or fresh
     */
    suspend fun getPlayerContext(player: Player): VCPlayerContext? {
        return contextCache.get(player.uniqueId)
    }
    /**
     * This gets the player Context. Cached or fresh
     */
    suspend fun getPlayerContext(uuid: UUID): VCPlayerContext? {
        val player = Bukkit.getPlayer(uuid) ?: return null
        return getPlayerContext(player)
    }

    /**
     * Convenient function which casts a player object and returns also the context
     */
    suspend fun getPlayerContextFromSender(sender: CommandSender): Pair<VCPlayerContext, Player>? {
        val realPlayer = sender as? Player ?: return null
        //log("Fetching player context for ${realPlayer.name} (${realPlayer.uniqueId})")
        getPlayerContext(realPlayer)?.let{
            return Pair(it, realPlayer)
        }
        return null
    }

    /**
     * This updates the cache. This needs to be called everytime a significant change is made.
     */
    suspend fun <T> updatePlayerContextCache(uuid: UUID, exFirst: suspend () -> T): T  {
        val result = exFirst()
        val player = Bukkit.getPlayer(uuid)?: return result
        val updatedContext = this.buildPlayerContext(player)?: return result
        contextCache.put(updatedContext.player.mcUUID, updatedContext)
        //log("Updated cache for player ${player.name} to: ${getPlayerContext(uuid).toString()}")
        return result
    }

    fun getCachedPlayerContext(player: Player): VCPlayerContext? {
        return contextCache.getIfCached(player.uniqueId)
    }
}