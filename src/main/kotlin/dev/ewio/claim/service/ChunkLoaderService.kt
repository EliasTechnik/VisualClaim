package dev.ewio.claim.service

import dev.ewio.claim.definitions.VCLoadedChunk
import org.bukkit.Bukkit

class ChunkLoaderService {

    fun loadChunk(cl: VCLoadedChunk){
        Bukkit.getServer().getWorld(cl.chunk.world)?.loadChunk(cl.chunk.x, cl.chunk.z)
        Bukkit.getServer().getWorld(cl.chunk.world)?.setChunkForceLoaded(cl.chunk.x, cl.chunk.z, true)
    }

    fun unloadChunk(cl: VCLoadedChunk){
        Bukkit.getServer().getWorld(cl.chunk.world)?.setChunkForceLoaded(cl.chunk.x, cl.chunk.z, false)
    }
}