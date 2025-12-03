package dev.ewio.claim.command

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
import org.bukkit.entity.Player
import kotlin.math.roundToInt

/**
 *  /chunkloader add <name>
 *  /chunkloader remove <name> [<player_name>]
 *  /chunkloader list [<player_name>]
 */


class ChunkLoaderCommand(
    private val preService: PrerequisiteService,
    private val coroutineScope: CoroutineScope,
    private val ms: MessageService
): TabExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        coroutineScope.launch {
            val betterArgs = getCorrectlySplitArgs(args.toList(), 0)
            preService.getPlayerContext(sender)?.let {
                var (context, realPlayer) = it

                if(betterArgs.isEmpty()){
                    ms.send(realPlayer, "usage.chunkloader")
                    return@launch
                }else{
                    val result = when(betterArgs[0]){
                        "add" -> {
                            log("${realPlayer.name} issued /chunkloader add")
                            if(betterArgs.size >= 2){
                                preService.addChunkLoader(context, betterArgs[1], realPlayer)
                            }else{
                                ms.send(realPlayer, "usage.chunkloader")
                                return@launch
                            }
                        }
                        "remove" -> {
                            log("${realPlayer.name} issued /chunkloader remove")
                            if(betterArgs.size == 2){
                                preService.removeChunkLoader(context, betterArgs[1])
                            }else{
                                if(betterArgs.size == 3) {
                                    preService.removeChunkLoader(context, betterArgs[1], betterArgs[2])
                                }else{
                                    ms.send(realPlayer, "usage.chunkloader")
                                    return@launch
                                }
                            }

                        }
                        "list" -> {
                            log("${realPlayer.name} issued /chunkloader list")
                            if(betterArgs.size == 2){
                                preService.listChunkLoader(context, betterArgs[1])
                            }else{
                                preService.listChunkLoader(context)
                            }

                        }
                        else -> {
                            ms.send(realPlayer, "usage.chunkloader")
                            return@launch
                        }
                    }

                    context = preService.getCachedPlayerContext(realPlayer)?: context

                    when(result){
                        //add
                        is VCResult.AddChunkLoader.ChunkLoaderAdded -> {
                            ms.send(realPlayer, "chunkloader.enabled", mapOf(
                                "chunkloader_name" to result.cl.name,
                                "current_chunk_loaders" to context.chunkLoader.size.toString(),
                                "max_chunk_loaders" to context.restrictions.maxChunkLoaders.toString()
                            ))
                        }
                        is VCResult.AddChunkLoader.ChunkAlreadyLoaded -> {
                            ms.send(realPlayer, "chunkloader.already-enabled", mapOf(
                                "chunkloader_name" to result.cl.name
                            ))
                        }
                        is VCResult.AddChunkLoader.ChunkLoadedByOtherPlayer -> {
                            ms.send(realPlayer, "chunkloader.already-enabled-other", mapOf(
                                "owner" to result.other.name
                            ))
                        }
                        is VCResult.AddChunkLoader.ChunkCanNotBeLoaded -> {
                            ms.send(realPlayer, "chunkloader.can-not-be-loaded")
                        }
                        is VCResult.AddChunkLoader.MaxChunkLoadersReached -> {
                            ms.send(realPlayer, "chunkloader.max-reached", mapOf(
                                "max_chunk_loaders" to result.max.toString()
                            ))
                        }
                        is VCResult.AddChunkLoader.NameInvalid -> {
                            ms.send(realPlayer, "chunkloader.name-invalid")
                        }

                        //remove
                        is VCResult.RemoveChunkLoader.ChunkLoaderRemoved -> {
                            ms.send(realPlayer, "chunkloader.disabled", mapOf(
                                "chunkloader_name" to result.cl.name,
                                "current_chunk_loaders" to context.chunkLoader.size.toString(),
                                "max_chunk_loaders" to context.restrictions.maxChunkLoaders.toString()
                            ))
                        }
                        is VCResult.RemoveChunkLoader.ChunkLoaderNotFound -> {
                            ms.send(realPlayer, "chunkloader.not-found")
                        }
                        is VCResult.RemoveChunkLoader.OtherPlayerNotFound -> {
                            ms.send(realPlayer, "player-not-found", mapOf(
                                "player_name" to result.other
                            ))
                        }

                        //list
                        is VCResult.ListChunkLoaders.ChunkLoadersFound -> {
                            if (result.loaders.isEmpty()) {
                                ms.send(realPlayer, "chunkloaderlist.no-chunkloaders")
                            } else {
                                ms.send(
                                    realPlayer, "chunkloaderlist.header", mapOf(
                                        "current_chunk_loaders" to context.chunkLoader.size.toString(),
                                        "max_chunk_loaders" to context.restrictions.maxChunkLoaders.toString()
                                    )
                                )
                                result.loaders.forEach { cl ->
                                    ms.send(
                                        realPlayer, "chunkloaderlist.entry", mapOf(
                                            "chunkloader_name" to cl.name,
                                            "world" to cl.chunk.world,
                                            "x" to cl.playerLocation.x.roundToInt().toString(),
                                            "y" to cl.playerLocation.y.roundToInt().toString(),
                                            "z" to cl.playerLocation.z.roundToInt().toString()
                                        )
                                    )
                                }
                            }
                        }
                        is VCResult.ListChunkLoaders.ChunkLoadersOtherFound -> {
                            if (result.loaders.isEmpty()) {
                                ms.send(realPlayer, "chunkloaderlist.no-chunkloaders-other", mapOf(
                                    "owner" to result.owner
                                ))
                            } else {
                                ms.send(
                                    realPlayer, "chunkloaderlist.header-other", mapOf(
                                        "owner" to result.owner
                                    )
                                )
                                result.loaders.forEach { cl ->
                                    ms.send(
                                        realPlayer, "chunkloaderlist.entry", mapOf(
                                            "chunkloader_name" to cl.name,
                                            "world" to cl.chunk.world,
                                            "x" to cl.playerLocation.x.roundToInt().toString(),
                                            "y" to cl.playerLocation.y.roundToInt().toString(),
                                            "z" to cl.playerLocation.z.roundToInt().toString()
                                        )
                                    )
                                }
                            }
                        }
                        is VCResult.ListChunkLoaders.NoChunkLoadersFound -> {
                            ms.send(realPlayer, "chunkloaderlist.no-chunkloaders")
                        }
                        is VCResult.ListChunkLoaders.VCPlayerNotFound -> {
                            ms.send(
                                realPlayer, "player-not-found", mapOf(
                                    "player_name" to betterArgs[1]
                                )
                            )
                        }

                        is VCResult.MissingPermission -> {
                            ms.send(realPlayer, "missing-permission")
                        }
                        else -> {
                            log("VCResult not catched")
                            ms.send(realPlayer, "unknown-error")}
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
    ): MutableList<String>? {
        val betterArgs = getCorrectlySplitArgs(args.toList(),0)
        val player = sender as? Player?: return mutableListOf()

        preService.getCachedPlayerContext(player)?.let{ context ->

            when (betterArgs.size) {
                1 -> {
                    val subcommands = listOf("add", "remove", "list")
                    return subcommands.filter { it.startsWith(betterArgs[0]) }.toMutableList()
                }
                2 -> {
                    if(betterArgs[0] == "remove"){
                        //get available chunkloaders for removal
                        val names = context.chunkLoader.map {"\"" + it.name +"\"" }

                        if (names.isEmpty()) {
                            return mutableListOf()
                        }

                        return names.toMutableList()
                    }
                }
                else -> {
                    return mutableListOf()
                }
            }
        }
        return mutableListOf()
    }
}