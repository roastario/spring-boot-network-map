package tech.b3i.networkmap

import net.corda.core.crypto.SecureHash
import net.corda.nodeapi.internal.SignedNodeInfo

interface NetworkMapPersistance {
    fun persistSignedNodeInfo(signedNodeInfo: SignedNodeInfo)
    fun getSignedNodeInfo(hash: String): Pair<SignedNodeInfo, ByteArray>
    fun getAllHashes(): List<SecureHash>
}