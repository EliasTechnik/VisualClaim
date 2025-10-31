package dev.ewio.claim.definitions

open class VCResult {
    sealed class Success : VCResult()

    sealed class Failure : VCResult()

    object UnknownFailure : Failure()

    object MalformedCommand: Failure()

    object MissingPermission: Failure()

    sealed class CreateClaim {
        data class ClaimCreatedSuccessfully(val claim: VCClaim, val chunk: VCChunk) : Success()
        data class ChunkAddedToClaim(val claim: VCClaim, val chunk: VCChunk) : Success()
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
        object RenamedSuccessful : Success()
        object VCClaimNotFound : Failure()  //"No claim found with the given name."
        object ClaimNameAlreadyExists : Failure() //"You already have a claim with this name."
        data class ClaimNameTooLong(val maxLength: Int) : Failure() //"The claim name is too long."
    }

    sealed class ClaimInfo{
        data class chunkClaimed(val claimName: String, val ownerName: String) : Success()
        object ChunkNotClaimed : Success()
        object ClaimedButWithoutOwner : Failure() //"This claim has no owner."
    }
}