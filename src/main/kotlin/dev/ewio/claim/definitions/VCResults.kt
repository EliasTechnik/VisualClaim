package dev.ewio.claim.definitions

open class VCResult {
    sealed class Success : VCResult()

    sealed class Failure : VCResult()

    object UnknownFailure : Failure()

    object MalformedCommand: Failure()

    object MissingPermission: Failure()

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
    }

    sealed class ClaimInfo{
        data class chunkClaimed(val claimName: String, val ownerName: String) : Success()
        object ChunkNotClaimed : Success()
        object ClaimedButWithoutOwner : Failure() //"This claim has no owner."
    }
}