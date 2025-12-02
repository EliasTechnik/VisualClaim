package dev.ewio.claim.command

import dev.ewio.claim.definitions.PlainChunk
import dev.ewio.claim.definitions.VCResult
import dev.ewio.claim.service.MessageService
import dev.ewio.claim.service.PrerequisiteService
import dev.ewio.util.getCorrectlySplitArgs
import dev.ewio.util.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor

class UnclaimCommand(
    private val preService: PrerequisiteService,
    private val coroutineScope: CoroutineScope,
    private val getStringFromConfig: (key: String) -> String,
    private val ms: MessageService
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

                val result = if (betterArgs.isEmpty()) {
                        preService.unclaimChunk(
                        context = context,
                        chunk = chunk
                    )
                } else {
                    if (betterArgs[0] == "force") {
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
                        ms.send(realPlayer, "usage.unclaim")
                        return@launch
                    }
                }

                when(result){
                    is VCResult.UnclaimChunk.UnclaimSuccessful -> {
                        ms.send(realPlayer, "unclaim.success", mapOf(
                            "chunk_x" to chunk.x.toString(),
                            "chunk_z" to chunk.z.toString(),
                            "claim_name" to result.claimName
                        ))
                    }
                    is VCResult.UnclaimChunk.UnclaimAlreadyUnclaimed -> {
                        ms.send(realPlayer, "unclaim.none")
                    }
                    is VCResult.UnclaimChunk.UnclaimFailedWrongOwner -> {
                        ms.send(realPlayer, "unclaim.other-owner", mapOf(
                            "owner" to result.ownerName
                        ))
                    }
                    is VCResult.UnknownFailure -> {
                        log("Unknown Failure")
                        ms.send(realPlayer, "unknown-error")
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


