package dev.ewio.command

import dev.ewio.VisualClaim
import dev.ewio.claim.PlainChunk
import dev.ewio.util.registerAndGetVCPlayerAndRealPlayer
import org.bukkit.command.TabExecutor

class ClaiminfoCommand(
    private val plugin: VisualClaim
): TabExecutor {
    override fun onTabComplete(
        sender: org.bukkit.command.CommandSender,
        command: org.bukkit.command.Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
        return mutableListOf()
    }

    override fun onCommand(
        sender: org.bukkit.command.CommandSender,
        command: org.bukkit.command.Command,
        label: String,
        args: Array<out String>
    ): Boolean {

        //get Player
        registerAndGetVCPlayerAndRealPlayer(sender, plugin.claimService)?.let {
            val (vcPlayer, realPlayer) = it

            val chunk = PlainChunk.fromBukkitChunk((realPlayer.location.chunk))
            val claim = plugin.claimService.getClaimAtChunk(chunk)

            if(claim != null){
                val owner = plugin.claimService.getPlayerByKey(claim.playerKey)
                if(owner != null){
                    realPlayer.sendMessage(
                        plugin.config.get("messages.claiminfo-claimed")
                            .toString()
                            .replace("<owner>", owner.name)
                            .replace("<claim-name>", claim.displayName)
                    )
                }
                else{
                    realPlayer.sendMessage(
                        plugin.config.get("messages.unknown-owner")
                            .toString()
                    )
                    plugin.logger.warning("Claim ${claim.key.value} has unknown owner with key ${claim.playerKey.value}. " +
                            "This is an plugin internal error and might hint to a corrupted database.")
                }

            } else {
                realPlayer.sendMessage(
                    plugin.config.get("messages.claiminfo-free")
                        .toString()
                )

            }
        }
        return true
    }
}
