package dev.ewio.command

import dev.ewio.VisualClaim
import dev.ewio.claim.ClaimService
import dev.ewio.claim.PlainChunk
import dev.ewio.util.CMDStringWrapper
import dev.ewio.util.VCExceptionType
import dev.ewio.util.getCorrectlySplitArgs
import dev.ewio.util.registerAndGetVCPlayer
import dev.ewio.util.registerAndGetVCPlayerAndRealPlayer
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor

class ClaimCommand(
    private val plugin: VisualClaim,
): TabExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        // TODO: check permissions and limits
        //plugin.logger.info("Args: ${args.asList().toString()}")
        val betterArgs = getCorrectlySplitArgs(args.toList(),0)

        registerAndGetVCPlayerAndRealPlayer(sender, plugin.claimService)?.let{
            val (vcPlayer, realPlayer) = it
            val chunk = PlainChunk.fromBukkitChunk(realPlayer.location.chunk)

            if(betterArgs.isEmpty()) {
                val result = plugin.claimService.createClaim(vcPlayer, listOf(chunk))
                when(result.first) {
                    VCExceptionType.NONE -> {
                        realPlayer.sendMessage(
                            plugin.config.get("messages.claim-success")
                                .toString()
                                .replace("<x>", chunk.x.toString())
                                .replace("<z>", chunk.z.toString())
                                .replace("<player>",vcPlayer.name)
                                .replace("<claim-name>",result.second!!.displayName)
                        )
                        plugin.mapService.writeClaimMarker(result.second!!)
                        return true
                    }
                    else -> {
                        realPlayer.sendMessage(plugin.strings.writeOutVCException(result.first, vcPlayer, result.second!!))
                        return true
                    }
                }
            }
            else{
                val result = plugin.claimService.createClaim(
                    player = vcPlayer,
                    chunks = listOf(chunk),
                    name = betterArgs[0],
                    useDefault = false)
                when(result.first) {
                    VCExceptionType.NONE -> {
                        realPlayer.sendMessage(
                            plugin.config.get("messages.claim-success")
                                .toString()
                                .replace("<x>", chunk.x.toString())
                                .replace("<z>", chunk.z.toString())
                                .replace("<player>",vcPlayer.name)
                                .replace("<claim-name>",result.second!!.displayName)
                        )
                        plugin.mapService.writeClaimMarker(result.second!!)
                        return true
                    }
                    else -> {
                        realPlayer.sendMessage(plugin.strings.writeOutVCException(result.first, vcPlayer, result.second!!))
                        return true
                    }
                }
            }
        }
        //this point should never be reached
        return false
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String>? {
        // Recommendations für <arg>
        registerAndGetVCPlayer(sender, plugin.claimService)?.let {
            val (vcPlayer, realPlayer) = it
            //get available claims
            val claims = plugin.claimService.getClaimsOfPlayer(it).filterNot { it.isDefaultClaim } //we must remove the default one

            val names = claims.map {"\"" + it.displayName +"\"" }

            if (names.isEmpty()) {
                return mutableListOf()
            }

            return when (args.size) {
                1 -> {
                    names.toMutableList()
                }

                else -> mutableListOf()
            }
        }
        return mutableListOf()
    }
}