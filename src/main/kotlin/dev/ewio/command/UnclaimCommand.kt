package dev.ewio.command

import dev.ewio.VisualClaim
import dev.ewio.claim.PlainChunk
import dev.ewio.util.VCExceptionType
import dev.ewio.util.registerAndGetVCPlayerAndRealPlayer
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.plugin.Plugin

class UnclaimCommand(
    private val plugin: VisualClaim
) : TabExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {

        if(args.isNotEmpty()){
            sender.sendMessage(
                plugin.cfg.get("messages.unclaim-usage")
                    .toString()
            )
            return true
        }

        //get Player
        registerAndGetVCPlayerAndRealPlayer(sender, plugin.claimService)?.let {
            val (vcPlayer, realPlayer) = it
            //get chunk
            val chunk = PlainChunk.fromBukkitChunk(realPlayer.location.chunk)
            //get claim
            val claim = plugin.claimService.getClaimAtChunk(chunk)

            if(claim == null){
                realPlayer.sendMessage(
                    plugin.cfg.get("messages.unclaim-none")
                        .toString()
                )
                return true
            }

            val result = plugin.claimService.removeChunkFromClaim(vcPlayer, claim, chunk)

            if(result == VCExceptionType.NONE){
                realPlayer.sendMessage(
                    plugin.cfg.get("messages.unclaim-success")
                        .toString()
                        .replace("<x>", chunk.x.toString())
                        .replace("<z>", chunk.z.toString())
                        .replace("<claim-name>", claim.displayName)
                )
                return true
            }else{
                realPlayer.sendMessage(
                    plugin.strings.writeOutVCException(result,vcPlayer,claim)
                )
                return true
            }
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
        return mutableListOf()
    }
}

