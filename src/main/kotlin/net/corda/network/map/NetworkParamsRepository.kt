/*
 * Copyright (c) 2018. B3i Switzerland. All rights reserved.
 *
 * http://www.b3i.tech
 */
package net.corda.network.map

import net.corda.core.crypto.SecureHash
import net.corda.core.node.NetworkParameters

/**
 * Represents a Repository for storing Node Info.
 */
interface NetworkParamsRepository {
    fun persistNetworkParams(networkParams: NetworkParameters, hash: SecureHash)
    fun getNetworkParams(hash: SecureHash): Pair<NetworkParameters, ByteArray>
    fun getAllHashes(): List<SecureHash>
    fun getLatestNetworkParams(): Pair<NetworkParameters, SecureHash>?
}