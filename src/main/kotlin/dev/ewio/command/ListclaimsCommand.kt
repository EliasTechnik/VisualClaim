package dev.ewio.command

import dev.ewio.VisualClaim
import dev.ewio.claim.ClaimService
import dev.ewio.util.registerAndGetVCPlayerAndRealPlayer
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor

class ListclaimsCommand(
    private val plugin: VisualClaim
): TabExecutor {

    private val service: ClaimService = plugin.claimService

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
        //TODO: add tab completions (PlayerNames) for admins
        return mutableListOf()
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        //TODO: add admin functionality to list other players claims
        registerAndGetVCPlayerAndRealPlayer(sender, plugin.claimService)?.let{
            val (vcPlayer, realPlayer) = it

            //get all claims from the service
            val claims = plugin.claimService.getClaimsOfPlayer(vcPlayer)

            if(claims.isEmpty()){
                realPlayer.sendMessage(plugin.cfg.get("messages.no-claims").toString())
            } else {
                realPlayer.sendMessage(plugin.cfg.get("messages.list-claims").toString())
                for(claim in claims){
                    if(plugin.cfg.get("plugin-insights.enabled") == true){
                        realPlayer.sendMessage("§6- ${claim.displayName} (ID: ${claim.key.value})")
                    }else{
                        realPlayer.sendMessage("§6- ${claim.displayName}")
                    }
                }
                realPlayer.sendMessage(
                    plugin.cfg.get("messages.list-claims-summary")
                        .toString()
                        .replace(
                            "<count>",
                            plugin.claimService.getChunkCountOfPlayer(vcPlayer).toString())
                        .replace(
                            "<maxchunks>",
                            plugin.permissionService.getMaxClaimableChunksOfPlayer(realPlayer).toString())
                )
            }
        }
        return true
    }
}