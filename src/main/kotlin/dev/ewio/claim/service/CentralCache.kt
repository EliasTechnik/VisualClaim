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
            this.getFreshPlayerContextWithoutCaching(uuid)
        }
    )



    suspend fun getPlayerContextFromSender(sender: CommandSender): Pair<VCPlayerContext, Player>? {
        val realPlayer = sender as? Player ?: return null
        log("Fetching player context for ${realPlayer.name} (${realPlayer.uniqueId})")
        getPlayerContextFromDB(realPlayer)?.let{
            contextCache.put(realPlayer.uniqueId, it)
            return Pair(it, realPlayer)
        }
        return null
    }

    suspend fun getPlayerContextForUUID(uuid: UUID): Pair<VCPlayerContext, Player>? {
        val realPlayer = Bukkit.getPlayer(uuid) ?: return null
        getPlayerContextFromDB(uuid)?.let{
            contextCache.put(uuid, it)
            return it
        }
        return null
    }


    suspend fun getPlayerContext(uuid: UUID): VCPlayerContext? {
        //register player if not exists
        val dbContext = registerPlayer(uuid) ?: return null
        val player = Bukkit.getPlayer(uuid) ?: return null
        val restrictions = permissionService.getRestrictionsForPlayer(
            player = dbContext.player,
            bukkitPlayer = player
        )

        contextCache.put(uuid, VCPlayerContext(
            restrictions = restrictions,
            dbContext = dbContext
            )
        )


        return contextCache.get(uuid)
    }


    /**
     * Registers a player by their UUID in the DB and returns their database context.
     */
    private suspend fun registerPlayer(uuid: UUID): VCPlayerDBContext? {
        return claimService.registerPlayerContextByUUID(uuid)
    }


    /*















    /**
     * Fetches a fresh player context from the database without using the cache.
     */
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

    /**
     * Fetches a fresh player context from the database without using the cache.
     */
    private suspend fun getPlayerContextFromDB(uuid: UUID): VCPlayerContext? {
        Bukkit.getPlayer(uuid)?.let { player ->
            return getPlayerContextFromDB(player)
        }
        return null
    }

    /**
     * Fetches a fresh player context from the database without using the cache.
     */
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

    */

}