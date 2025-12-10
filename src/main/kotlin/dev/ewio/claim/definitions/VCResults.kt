package dev.ewio.claim.definitions

import dev.ewio.claim.service.MovementService

open class VCResult {
    sealed class Success : VCResult()

    sealed class Failure : VCResult()

    object UnknownFailure : Failure()

    object MalformedCommand: Failure()

    object MissingPermission: Failure()

    data class VCPlayerNotFound(val playerName: String) : Failure()

    sealed class CreateClaim {
        data class ClaimCreatedSuccessfully(val claim: VCClaim, val chunk: VCChunk) : Success()
        data class ChunkTransferredToClaim(val claim: VCClaim, val chunk: PlainChunk) : Success() //The chunk was claimed previously, but now added to a different claim of the same player
        object ChunkAlreadyClaimedBySameClaim : Failure()
        data class ChunkClaimedByOtherPlayer(val otherPlayer: String) : Failure()
        data class ChunkLimitReached(val maxChunks: Int) : Failure()
        data class ClaimLimitReached(val maxClaims: Int) : Failure()
        object ChunkCanNotBeClaimed: Failure()
        object NoExistingClaimFound : Failure()
        //object ClaimCouldNotBeCreated : Failure()
        data class ClaimNameTooLong(val maxLength: Int): Failure()
        object ClaimNameNotAllowed: Failure()
    }

    sealed class TransferChunk{
        object TransferSuccessful : Success()
        object VCChunkNotFound : Failure()
    }

    sealed class UnclaimChunk{
        data class UnclaimSuccessful(val claimName: String) : Success()
        object UnclaimAlreadyUnclaimed : Failure()  //"This chunk is already unclaimed."
        data class UnclaimFailedWrongOwner(val ownerName: String) : Failure() //"You do not own this chunk. It is owned by <ownerName>."

    }

    sealed class DeleteClaim{
        data class RemovedSuccessful(val claimName: String) : Success()
        data class VCClaimNotFound(val claimName: String) : Failure()  //"No claim found with the given name."
        data class NotOwnerOfClaim(val claimName: String) : Failure() //"You do not own this claim."
        data class ConfirmationRequired(val claimName: String) : Failure() //"You must confirm the deletion of this claim."
        data class ConfirmOtherPlayerClaimRequired(val claimName: String) : Failure() //"You must confirm the deletion of another player's claim."
    }

    sealed class RenameClaim{
        data class RenamedSuccessful(val oldName: String, val newName: String) : Success()
        data class MergeSuccessful(val oldName: String, val newName: String) : Success()
        data class OldNameNotFound(val oldName: String) : Failure()  //"No claim found with the given name."
        data class ConfirmMergeRequired(val oldName: String, val newName: String) : Failure() //"A claim with the new name already exists. You must confirm the merge."
        data class ConfirmMergeOtherPlayerClaimRequired(val oldName: String, val newName: String, val playerName: String) : Failure() //"A claim with the new name owned by another player already exists. You must confirm the merge."
        data class ClaimNameTooLong(val maxLength: Int) : Failure() //"The claim name is too long."
        object ClaimNameNotAllowed: Failure()
    }

    sealed class ClaimInfo{
        data class chunkClaimed(val claimName: String, val ownerName: String) : Success()
        object ChunkNotClaimed : Success()
        object ClaimedButWithoutOwner : Failure() //"This claim has no owner."
    }

    sealed class AutoClaim{
        data class AutoClaimEnabled(val forClaim: VCClaim, val movementService: MovementService) : Success()
        object AutoClaimDisabled : Success()
        data class ChunkClaimed(val claim: VCClaim, val chunk: VCChunk): Success()
        object ChunkAlreadyClaimed : Failure()
        data class ChunkClaimedByOtherPlayer(val otherPlayer: String) : Failure()
        data class ChunkLimitReached(val maxChunks: Int) : Failure()
        data class ClaimNeedsCreationFirst(val claimName: String) : Failure()
        data class StatusInfo(val isEnabled: Boolean, val claimingFor: VCClaim? = null) : Success()
        object AutoClaimFailedNoTargetClaimSet: Failure()
        object ChunkCanNotBeClaimed: Failure()
        data class ChunkBelongsToDifferentClaim(val chunk: PlainChunk, val otherClaimName: String): Failure()
    }

    sealed class AddChunkLoader{
        data class ChunkLoaderAdded(val cl: VCLoadedChunk): Success()
        data class ChunkAlreadyLoaded(val cl: VCLoadedChunk): Failure()
        data class ChunkLoadedByOtherPlayer(val other: VCPlayer): Failure()
        object ChunkCanNotBeLoaded: Failure()
        data class MaxChunkLoadersReached(val max: Int): Failure()
        object NameInvalid: Failure()
    }

    sealed class RemoveChunkLoader{
        data class ChunkLoaderRemoved(val cl: VCLoadedChunk): Success()
        object ChunkLoaderNotFound: Failure()
    }

    sealed class ListChunkLoaders{
        data class ChunkLoadersFound(val loaders: List<VCLoadedChunk>): Success()
        data class ChunkLoadersOtherFound(val loaders: List<VCLoadedChunk>, val owner: String ): Success()
        object NoChunkLoadersFound: Failure()
    }

    sealed class ClaimLore{
        data class LoreSet(val claim: VCClaim): Success()
        data class LoreGet(val claim: VCClaim): Success()
        object ClaimNotFound: Failure()
        object ContainsInvalidCharacters: Failure()
        data class LoreTooLong(val maxLength: Int): Failure()
    }

    sealed class ClaimColor{
        data class ColorSet(val claim: VCClaim, val color: VCColor): Success()
        object ColorNotFound: Failure()
        data class ClaimNotFound(val name: String): Failure()
    }
}