package net.corda.network.map.repository

import net.corda.core.crypto.SecureHash
import net.corda.core.identity.CordaX500Name
import net.corda.core.serialization.serialize
import net.corda.nodeapi.internal.SignedNodeInfo
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

@Component
@MapBacked
class MapNodeInfoRepository : NodeInfoRepository {

    private val lock = ReentrantReadWriteLock()

    private val mapByName: ConcurrentMap<CordaX500Name, NodeInfoHolder> = ConcurrentHashMap()
    private val mapByHash: ConcurrentHashMap<SecureHash, NodeInfoHolder> = ConcurrentHashMap()

    override fun persistSignedNodeInfo(signedNodeInfo: SignedNodeInfo) {
        lock.write {
            signedNodeInfo.verified().legalIdentities.forEach {
                val name = it.name
                val nodeInfoHolder = NodeInfoHolder(signedNodeInfo.raw.hash, signedNodeInfo, signedNodeInfo.serialize().bytes)
                mapByName[name] = nodeInfoHolder
                mapByHash[nodeInfoHolder.hash] = nodeInfoHolder
            }
        }
    }

    override fun getSignedNodeInfo(hash: String): Pair<SignedNodeInfo, ByteArray>? {
        val parsedHash = SecureHash.parse(hash)
        return lock.read { mapByHash[parsedHash]?.let { it.signedNodeInfo to it.byteRepresentation } }
    }

    override fun getAllHashes(): Collection<SecureHash> {
        return lock.read { mapByName.values.map { it.hash } }
    }

    override fun purgeAllPersistedSignedNodeInfos(): Int {
        return lock.write {
            mapByHash.clear()
            mapByName.size.also { mapByName.clear() }
        }
    }
}

data class NodeInfoHolder(val hash: SecureHash, val signedNodeInfo: SignedNodeInfo, val byteRepresentation: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NodeInfoHolder

        if (hash != other.hash) return false
        if (signedNodeInfo != other.signedNodeInfo) return false
        if (!byteRepresentation.contentEquals(other.byteRepresentation)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = hash.hashCode()
        result = 31 * result + signedNodeInfo.hashCode()
        result = 31 * result + byteRepresentation.contentHashCode()
        return result
    }
}