package dev.ewio.command

import dev.ewio.VisualClaim
import dev.ewio.util.VCExceptionType
import dev.ewio.util.VCRenameResultType
import dev.ewio.util.getCorrectlySplitArgs
import dev.ewio.util.getQuotedStrings
import dev.ewio.util.registerAndGetVCPlayer
import dev.ewio.util.registerAndGetVCPlayerAndRealPlayer
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor

class RenameclaimCommand(
    val plugin: VisualClaim
): TabExecutor {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        val betterArgs = getCorrectlySplitArgs(args.toList(), 0)

        registerAndGetVCPlayerAndRealPlayer(sender, plugin.claimService)?.let {
            val (vcPlayer, realPlayer) = it

            plugin.logger.info("Renameclaim command executed by ${vcPlayer.name} with args: ${betterArgs.joinToString("|")}")

            //check if there is anything to rename
            val claims = plugin.claimService.getClaimsOfPlayer(vcPlayer)
            if(claims.isEmpty()) {
                realPlayer.sendMessage(
                    plugin.cfg.get("messages.renameclaim.none").toString()
                )
                return true
            }

            //on empty args show usage
            if(betterArgs.isEmpty()){
                realPlayer.sendMessage(
                    plugin.cfg.get("usage.renameclaim").toString()
                )
                return true
            }

            //evaluate args
            when(betterArgs.size){
                1 -> {
                    realPlayer.sendMessage(
                        plugin.cfg.get("usage.renameclaim").toString()
                    )
                    return true
                }
                2 -> {
                    //get claim by name
                    val claim = claims.firstOrNull { it.displayName == betterArgs[0] }

                    if (claim == null) {
                        //error: no claim found
                        realPlayer.sendMessage(
                            plugin.cfg.get("messages.renameclaim.not-found")
                                .toString()
                                .replace("<claim-name>", betterArgs[0])
                        )
                        return true
                    } else {
                        //rename claim
                        val result = plugin.claimService.renameAndMergeClaim(vcPlayer, claim.displayName, betterArgs[1])

                        when (result.first) {
                            VCRenameResultType.SUCCESS -> {
                                realPlayer.sendMessage(
                                    plugin.cfg.get("messages.renameclaim.success")
                                        .toString()
                                        .replace("<claim-name>", result.second)
                                        .replace("<claim-new-name>", result.third)
                                )
                                return true
                            }

                            VCRenameResultType.MERGED -> {
                                //This code should not be reachable here, but just in case
                                realPlayer.sendMessage(
                                    plugin.cfg.get("messages.renameclaim.merge-success")
                                        .toString()
                                        .replace("<claim-name>", result.second)
                                        .replace("<claim-new-name>", result.third)
                                )
                                return true
                            }

                            VCRenameResultType.NAME_ALREADY_EXISTS -> {
                                realPlayer.sendMessage(
                                    plugin.cfg.get("messages.renameclaim.merge-confirm")
                                        .toString()
                                        .replace("<claim-name>", "\"${result.second}\"")
                                        .replace("<claim-new-name>", "\"${result.third}\"")
                                        .replace(
                                            "<renameclaim-confirm>",
                                            plugin.cfg.get("trigger-words.renameclaim-confirm").toString()
                                        )
                                )
                                return true
                            }

                            VCRenameResultType.OLD_CLAIM_NOT_FOUND -> {
                                realPlayer.sendMessage(
                                    plugin.cfg.get("messages.renameclaim.not-found")
                                        .toString()
                                        .replace("<claim-name>", betterArgs[0])
                                )
                                return true
                            }

                            VCRenameResultType.FAILED -> {
                                realPlayer.sendMessage(
                                    plugin.cfg.get("messages.unknown-error")
                                        .toString()
                                )
                                return true
                            }
                        }
                    }
                }
                3 -> {
                    //a merge was confirmed or the user mistyped
                    if(betterArgs[2].lowercase() == plugin.cfg.get("trigger-words.renameclaim-confirm").toString()) {
                        //get claim by name
                        val claim = claims.firstOrNull { it.displayName == betterArgs[0] }

                        if (claim == null) {
                            //error: no claim found
                            realPlayer.sendMessage(
                                plugin.cfg.get("messages.renameclaim.not-found")
                                    .toString()
                                    .replace("<claim-name>", betterArgs[0])
                            )
                            return true
                        } else {
                            //rename claim with merge
                            val result = plugin.claimService.renameAndMergeClaim(
                                vcPlayer,
                                claim.displayName,
                                betterArgs[1],
                                true
                            )

                            when (result.first) {
                                VCRenameResultType.MERGED -> {
                                    realPlayer.sendMessage(
                                        plugin.cfg.get("messages.renameclaim.merge-success")
                                            .toString()
                                            .replace("<claim-name>", result.second)
                                            .replace("<claim-new-name>", result.third)
                                    )
                                    return true
                                }

                                else -> {
                                    realPlayer.sendMessage(
                                        plugin.cfg.get("messages.unknown-error")
                                            .toString()
                                    )
                                    return true
                                }
                            }
                        }
                    }
                }

                else -> {
                    sender.sendMessage(
                        plugin.cfg.get("messages.usage.renameclaim")
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
        val betterArgs = getCorrectlySplitArgs(args.toList(), 0)

        plugin.logger.info("Renameclaim tabcomplete executed by ${sender.name} with args: ${betterArgs.joinToString("|")}")

        registerAndGetVCPlayer(sender, plugin.claimService)?.let {
            //get available claims
            val claims = plugin.claimService.getClaimsOfPlayer(it)

            when (betterArgs.size) {
                0 -> {
                    return claims.map { "\"" + it.displayName + "\"" }.toMutableList()
                }

                1 -> {
                    return claims.map {name ->  "\"" + name.displayName + "\"" }.toMutableList()
                }
                2 -> {
                    return claims.filterNot { name -> name.displayName == betterArgs[0] }
                        .map {name ->  "\"" + name.displayName + "\"" }
                        .toMutableList()
                }

                else -> return mutableListOf()
            }
        }
        return mutableListOf()
    }
}