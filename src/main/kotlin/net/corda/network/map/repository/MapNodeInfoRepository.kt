package net.corda.network.map.repository

import net.corda.core.crypto.SecureHash
import net.corda.core.serialization.serialize
import net.corda.nodeapi.internal.SignedNodeInfo
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

@Component
@MapBacked
class MapNodeInfoRepository : NodeInfoRepository {

    private val map: ConcurrentMap<SecureHash, Pair<SignedNodeInfo, ByteArray>> = ConcurrentHashMap()

    override fun persistSignedNodeInfo(signedNodeInfo: SignedNodeInfo) {
        map.put(signedNodeInfo.raw.hash, signedNodeInfo to signedNodeInfo.serialize().bytes)
    }

    override fun getSignedNodeInfo(hash: String): Pair<SignedNodeInfo, ByteArray>? {
        return map.get(SecureHash.parse(hash))
    }

    override fun getAllHashes(): Collection<SecureHash> {
        return Collections.unmodifiableSet(map.keys);
    }

    override fun purgeAllPersistedSignedNodeInfos(): Int {
        return map.size.also { map.clear() }
    }
}