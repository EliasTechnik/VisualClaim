package dev.ewio.claim.definitions

open class VCResult {
    sealed class Success : VCResult()

    sealed class Failure : VCResult()

    sealed class CreateClaim {
        object ClaimCreatedSuccessfully : Success()
        object ChunkClaimedSucessfully : Success()
        object ChunkAlreadyClaimedBySameClaim : Failure()
        data class ChunkClaimedByOtherPlayer(val otherPlayer: String) : Failure()
        data class ChunkLimitReached(val maxChunks: Int) : Failure()
        data class ClaimLimitReached(val maxClaims: Int) : Failure()
        object ChunkCouldNotBeClaimed: Failure()
        object NoExistingClaimFound : Failure()
        object ClaimCouldNotBeCreated : Failure()
        data class ClaimNameTooLong(val maxLength: Int): Failure()
        object UNKNOWN : Failure()
    }

    sealed class TransferChunk{
        object TransferSuccessful : Success()
        object VCChunkNotFound : Failure()
    }

    sealed class RemoveChunk{
        object RemovedSuccessful : Success()
        object VCChunkNotFound : Failure()  //"This chunk is not found in any of your claims."
    }

    sealed class DeleteClaim{
        object RemovedSuccessful : Success()
        object VCClaimNotFound : Failure()  //"No claim found with the given name."
    }

    sealed class RenameClaim{
        object RenamedSuccessful : Success()
        object VCClaimNotFound : Failure()  //"No claim found with the given name."
        object ClaimNameAlreadyExists : Failure() //"You already have a claim with this name."
        data class ClaimNameTooLong(val maxLength: Int) : Failure() //"The claim name is too long."
    }
}