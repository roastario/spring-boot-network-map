package net.corda.network.map.repository

import net.corda.core.crypto.SecureHash
import net.corda.nodeapi.internal.SignedNodeInfo
import org.springframework.beans.factory.annotation.Qualifier

/**
 * Represents a Repository for storing Node Info.
 */
interface NodeInfoRepository {
    fun persistSignedNodeInfo(signedNodeInfo: SignedNodeInfo)
    fun getSignedNodeInfo(hash: String): Pair<SignedNodeInfo, ByteArray>?
    fun getAllHashes(): Collection<SecureHash>
    fun purgeAllPersistedSignedNodeInfos(): Int
}

@Target(AnnotationTarget.CLASS, AnnotationTarget.VALUE_PARAMETER)
@Qualifier
annotation class MapBacked

@Target(AnnotationTarget.CLASS, AnnotationTarget.VALUE_PARAMETER)
@Qualifier
annotation class SqlLiteBacked