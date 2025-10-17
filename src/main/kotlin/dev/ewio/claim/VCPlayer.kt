package dev.ewio.claim

import dev.ewio.util.UKey
import java.util.UUID

data class VCPlayer(
    val key: UKey<VCPlayer>,
    val mcUUID: UUID,
    val name: String,
    val resolvedNameAt: Long,
    val autoClaim: Boolean = false
)
