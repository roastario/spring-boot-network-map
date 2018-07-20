package net.corda.network.map.repository

import net.corda.core.crypto.SecureHash
import net.corda.nodeapi.internal.SignedNodeInfo

/**
 * Represents a Repository for storing Node Info.
 */
interface NodeInfoRepository {
    fun persistSignedNodeInfo(signedNodeInfo: SignedNodeInfo)
    fun getSignedNodeInfo(hash: String): Pair<SignedNodeInfo, ByteArray>
    fun getAllHashes(): List<SecureHash>
    fun purgeAllPersistedSignedNodeInfos() : Int
}