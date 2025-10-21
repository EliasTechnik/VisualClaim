package dev.ewio.util

import dev.ewio.VisualClaim
import dev.ewio.claim.VCChunk
import dev.ewio.claim.VCClaim
import dev.ewio.claim.VCPlayer

class StringHelper(
    val plugin: VisualClaim
) {
    fun writeOutVCException(
        ex: VCExceptionType,
        player: VCPlayer = VCPlayer.dummy(),
        claim: VCClaim = VCClaim.dummy(),
        chunk: VCChunk = VCChunk.dummy()
    ): String {
        return when (ex) {
            VCExceptionType.CHUNK_ALREADY_CLAIMED_BY_SAME_CLAIM -> {
                plugin.cfg.getString("messages.claimed-already")
                    .toString()
                    .replace("<claim-name>", claim.displayName)
            }
            VCExceptionType.CHUNK_CLAIMED_BY_OTHER_PLAYER -> {
                plugin.cfg.getString("messages.claimed-by-other")
                    .toString()
                    .replace("<owner>", player.name)
                    .replace("<claim-name>", claim.displayName)
            }
            VCExceptionType.CLAIM_BELONGS_TO_OTHER_PLAYER -> {
                plugin.cfg.getString("messages.claim-belongs-to-other")
                    .toString()
                    .replace("<claim-name>", claim.displayName)
            }
            else -> VCExceptionType.toReadableString(ex)
            //TODO: Extend with more detailed messages if needed
        }
    }

}