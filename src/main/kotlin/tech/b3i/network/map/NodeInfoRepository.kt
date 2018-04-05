/*
 * Copyright (c) 2018. B3i Switzerland. All rights reserved.
 *
 * http://www.b3i.tech
 */
package tech.b3i.network.map

import net.corda.core.crypto.SecureHash
import net.corda.nodeapi.internal.SignedNodeInfo

/**
 * Represents a Repository for storing Node Info.
 */
interface NodeInfoRepository {
    fun persistSignedNodeInfo(signedNodeInfo: SignedNodeInfo)
    fun getSignedNodeInfo(hash: String): Pair<SignedNodeInfo, ByteArray>
    fun getAllHashes(): List<SecureHash>
}