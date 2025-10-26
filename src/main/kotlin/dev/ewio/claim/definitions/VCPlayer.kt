package dev.ewio.claim.definitions

import java.util.UUID

data class VCPlayer(
    val key: Int = -1,
    val mcUUID: UUID,
    val name: String,
    val resolvedNameAt: Long,
    val autoClaim: Boolean = false
) {
    companion object {
        fun dummy(): VCPlayer = VCPlayer(
            -1,
            UUID.fromString("00000000-0000-0000-0000-000000000000"),
            "dummy",
            0L,
            false
        )
    }
}
