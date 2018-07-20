/*
 */
package net.corda.network.map.repository

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