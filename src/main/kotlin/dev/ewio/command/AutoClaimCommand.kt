package dev.ewio.command

import dev.ewio.claim.definitions.PlainChunk
import dev.ewio.claim.definitions.VCResult
import dev.ewio.claim.service.PrerequisiteService
import dev.ewio.util.getCorrectlySplitArgs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class AutoclaimCommand(
    private val preService: PrerequisiteService,
    private val coroutineScope: CoroutineScope,
    private val getStringFromConfig: (key: String) -> String
): TabExecutor {

    /**
     *  /autoclaim <claim-name>
     *  /autoclaim off
     *
     */


    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        coroutineScope.launch {
            val betterArgs = getCorrectlySplitArgs(args.toList(), 0)

            preService.getPlayerContext(sender)?.let {
                val (context, realPlayer) = it

                val result = when (betterArgs.size) {
                    0 -> {
                        VCResult.AutoClaim.StatusInfo(context.player.autoClaim)
                    }
                    1 -> {
                        // /autoclaim <claim-name> or  /autoclaim off
                        if(betterArgs[0].equals(getStringFromConfig("trigger-words.autoclaim-off"), ignoreCase = true)) {
                            //turn off autoclaim
                            preService.disableAutoclaim(context)
                        }else{
                            //might be a claim name

                            if(context.claims.none { it.displayName.equals(betterArgs[0], ignoreCase = true) }){
                                //no claim with that name
                                VCResult.MalformedCommand
                            }else{
                                //enable autoclaim
                                preService.enableAutoclaim(context, betterArgs[0])
                            }
                        }
                    }

                    else -> {
                        VCResult.MalformedCommand
                    }
                }

                when(result){
                    is VCResult.AutoClaim.StatusInfo -> {
                        if(result.isEnabled){
                            realPlayer.sendMessage(
                                getStringFromConfig("messages.autoclaim.enabled")
                                    .replace("<claim-name>", context.getAutoClaimTarget()?.displayName?: "unknown")
                            )
                        }else{
                            realPlayer.sendMessage(
                                getStringFromConfig("messages.autoclaim.disabled")
                                    .replace("<claim-name>", context.getAutoClaimTarget()?.displayName?: "unknown")
                            )
                        }
                    }
                    is VCResult.MalformedCommand -> {
                        realPlayer.sendMessage(
                            getStringFromConfig("usage.autoclaim")
                        )
                    }
                    is VCResult.AutoClaim.AutoClaimEnabled -> {
                        realPlayer.sendMessage(
                            getStringFromConfig("messages.autoclaim.enabled")
                                .replace("<claim-name>", result.forClaim.displayName)
                        )
                        //trigger an autoclaim for the current chunk right away
                        //do not forget to fetch the context again!
                        preService.getCachedPlayerContext(realPlayer)?.let{newContext ->
                            result.movementService.autoclaimActivated(realPlayer, newContext, PlainChunk.fromBukkitChunk(realPlayer.location.chunk))
                        }

                    }
                    is VCResult.AutoClaim.AutoClaimDisabled -> {
                        realPlayer.sendMessage(
                            getStringFromConfig("messages.autoclaim.disabled")
                        )
                    }
                    is VCResult.AutoClaim.ClaimNeedsCreationFirst -> {
                        realPlayer.sendMessage(
                            getStringFromConfig("messages.no-target-claim-set")
                                .replace("<claim-name>", result.claimName)
                        )
                    }
                    is VCResult.MissingPermission -> {
                        realPlayer.sendMessage(
                            getStringFromConfig("messages.missing-permission")
                        )
                    }
                    else -> {
                        //if this is reached I have forgotten to handle a case
                        realPlayer.sendMessage(
                            getStringFromConfig("messages.unknown-error")
                        )
                    }
                }
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

        val betterArgs = getCorrectlySplitArgs(args.toList(), 0)
        val player = sender as? Player ?: return mutableListOf()

        preService.getCachedPlayerContext(player)?.let { context ->
            when (betterArgs.size) {
                1 -> {
                    //claim names
                    val names = context.claims.map { "\"" + it.displayName + "\"" }.toMutableList()
                    names.add(getStringFromConfig("trigger-words.autoclaim-off"))
                    return names
                }
            }
        }

        return mutableListOf()
    }
}