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

class UnclaimCommand(
    private val preService: PrerequisiteService,
    private val coroutineScope: CoroutineScope,
    private val getStringFromConfig: (key: String) -> String,
) : TabExecutor {
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
                val chunk = PlainChunk.fromBukkitChunk(realPlayer.location.chunk)

                if (betterArgs.isEmpty()) {
                    preService.unclaimChunk(
                        context = context,
                        chunk = chunk
                    )
                } else {
                    val result = if(betterArgs[0] == "force") {
                        //we are forcing unclaim even if the player is not the owner
                        //fear not the wrath of permissions, for you shall be forgiven - Copilot

                        //no seriously, the preService will check permissions and ownership so no worries there
                        preService.unclaimChunk(
                            context = context,
                            chunk = chunk,
                            forceUnclaim = true
                        )
                    } else {
                        //invalid argument
                        realPlayer.sendMessage(
                            getStringFromConfig("usage.unclaim")
                        )
                        return@launch
                    }

                    when(result){
                        is VCResult.UnclaimChunk.UnclaimSuccessful -> {
                            realPlayer.sendMessage(
                                getStringFromConfig("messages.unclaim.success")
                                    .replace("<x>", chunk.x.toString())
                                    .replace("<z>", chunk.z.toString())
                                    .replace("<claim-name>", result.claimName)
                            )
                        }
                        is VCResult.UnclaimChunk.UnclaimAlreadyUnclaimed -> {
                            realPlayer.sendMessage(
                                getStringFromConfig("messages.unclaim.none")
                            )
                        }
                        is VCResult.UnclaimChunk.UnclaimFailedWrongOwner -> {
                            realPlayer.sendMessage(
                                getStringFromConfig("messages.unclaim.other-owner")
                                    .replace("<owner>", result.ownerName)
                            )
                        }
                        is VCResult.UnknownFailure -> {
                            realPlayer.sendMessage(
                                getStringFromConfig("messages.unknown-error")
                            )
                        }
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
        return mutableListOf()
    }
}


