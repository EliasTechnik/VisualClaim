package dev.ewio.claim

import dev.ewio.util.UKey
import java.util.UUID

data class VCPlayer(
    val key: UKey<VCPlayer>,
    val mcUUID: UUID,
    val name: String,
    val resolvedNameAt: Long,
    val autoClaim: Boolean = false
) {
    companion object {
        fun dummy(): VCPlayer = VCPlayer(
            UKey.dummy(),
            UUID.fromString("00000000-0000-0000-0000-000000000000"),
            "dummy",
            0L,
            false
        )
    }
}
