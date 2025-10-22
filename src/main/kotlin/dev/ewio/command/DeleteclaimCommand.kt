package dev.ewio.command

import dev.ewio.VisualClaim
import dev.ewio.util.VCExceptionType
import dev.ewio.util.getCorrectlySplitArgs
import dev.ewio.util.getQuotedStrings
import dev.ewio.util.registerAndGetVCPlayer
import dev.ewio.util.registerAndGetVCPlayerAndRealPlayer
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import kotlin.collections.mutableListOf

class DeleteclaimCommand(
    val plugin: VisualClaim
): TabExecutor {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        //get Player

        val betterArgs = getCorrectlySplitArgs(args.toList(), 0)

        registerAndGetVCPlayerAndRealPlayer(sender, plugin.claimService)?.let {
            val (vcPlayer, realPlayer) = it

            //check if there is anything to delete
            val claims = plugin.claimService.getClaimsOfPlayer(vcPlayer)
            if(claims.isEmpty()) {
                realPlayer.sendMessage(
                    plugin.cfg.get("messages.deleteclaim-none").toString()
                )
                return true
            }

            //on empty args show usage
            if(betterArgs.isEmpty()){
                realPlayer.sendMessage(
                    plugin.cfg.get("messages.deleteclaim-usage").toString()
                )
                return true
            }

            //evaluate args
            when(betterArgs.size){
                1 -> {
                    //claim name given
                    //check if claim exists

                    val claim = claims.firstOrNull{ it.displayName == betterArgs[0] }

                    if(claim == null){
                        //error: no claim found
                        realPlayer.sendMessage(
                            plugin.cfg.get("messages.deleteclaim-not-found")
                                .toString()
                                .replace("<claim-name>", betterArgs[0])
                        )
                        return true
                    }else{
                        //prompt for confirmation
                        sender.sendMessage(
                            plugin.cfg.get("messages.deleteclaim-confirm")
                                .toString()
                                .replace("<claim-name>", "\"${claim.displayName}\"")
                                .replace("<deleteclaim-confirm>", plugin.cfg.get("trigger-words.deleteclaim-confirm").toString())
                        )
                        return true
                    }
                }
                2 -> {
                    //two args. check again for claimname and confirmation
                    val claim = claims.firstOrNull{ it.displayName == betterArgs[0] }

                    if(claim == null){
                        //error: no claim found
                        realPlayer.sendMessage(
                            plugin.cfg.get("messages.deleteclaim-not-found")
                                .toString()
                                .replace("<claim-name>", betterArgs[0])
                        )
                        return true
                    }else{
                        //there is a claim. Is it confirmed right?
                        if(betterArgs[1].lowercase() == plugin.cfg.get("trigger-words.deleteclaim-confirm").toString()){
                            //right, delete claim
                            val result = plugin.claimService.deleteClaim(vcPlayer, claim)

                            if(result == VCExceptionType.NONE) {
                                realPlayer.sendMessage(
                                    plugin.cfg.get("messages.deleteclaim-success")
                                        .toString()
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
                }
                else ->
                {
                    sender.sendMessage(
                        plugin.cfg.get("messages.deleteclaim-usage")
                            .toString()
                    )
                    return true
                }
            }
        }
        return false
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
        // Recommendations für <arg>
        registerAndGetVCPlayer(sender, plugin.claimService)?.let {
            //get available claims
            val claims = plugin.claimService.getClaimsOfPlayer(it)

            val names = getQuotedStrings(claims.map { it.displayName }).toMutableList()

            if (names.isEmpty()) {
                return mutableListOf()
            }

            return when (args.size) {
                1 -> {
                    val recommendations = names.toMutableList()
                        recommendations.add("")
                    recommendations
                }

                else -> mutableListOf<String>()
            }
        }
        return mutableListOf()
    }
}