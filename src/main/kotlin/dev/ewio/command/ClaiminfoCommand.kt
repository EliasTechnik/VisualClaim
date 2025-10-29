package dev.ewio.command

import dev.ewio.VisualClaim
import dev.ewio.claim.definitions.PlainChunk
import dev.ewio.claim.definitions.VCResult
import dev.ewio.claim.service.PrerequisiteService
import dev.ewio.util.getCorrectlySplitArgs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor


class ClaiminfoCommand(
    private val preService: PrerequisiteService,
    private val coroutineScope: CoroutineScope,
    private val getStringFromConfig: (key: String) -> String
): TabExecutor {
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
        return mutableListOf()
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {

        coroutineScope.launch{
            val betterArgs = getCorrectlySplitArgs(args.toList(),0)

            if(betterArgs.isNotEmpty()){
                //invalid usage
                preService.getPlayerContext(sender)?.let {
                    val (_, realPlayer) = it
                    realPlayer.sendMessage(
                        getStringFromConfig("usage.claiminfo-usage")
                    )
                }
                return@launch
            }

            preService.getPlayerContext(sender)?.let {
                val (context, realPlayer) = it

                val chunk = PlainChunk.fromBukkitChunk((realPlayer.location.chunk))

                when (val claimResult = preService.getClaimAtChunk(chunk)) {
                    is VCResult.ClaimInfo.chunkClaimed -> {
                        realPlayer.sendMessage(
                            getStringFromConfig("messages.claiminfo.claimed")
                                .replace("<owner>", claimResult.ownerName)
                                .replace("<claim-name>", claimResult.claimName)
                        )
                    }

                    is VCResult.ClaimInfo.ChunkNotClaimed -> {
                        realPlayer.sendMessage(
                            getStringFromConfig("messages.claiminfo.free")
                        )
                    }

                    else -> {
                        realPlayer.sendMessage(
                            getStringFromConfig("messages.unknown-owner")
                        )

                    }
                }
            }
        }
        return true
    }
}
