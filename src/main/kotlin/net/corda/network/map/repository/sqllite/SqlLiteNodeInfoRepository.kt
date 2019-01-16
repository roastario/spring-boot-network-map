/*
 */
package net.corda.network.map.repository.sqllite

import net.corda.core.crypto.SecureHash
import net.corda.core.serialization.deserialize
import net.corda.core.serialization.serialize
import net.corda.network.map.SerializationEngine
import net.corda.network.map.repository.NodeInfoRepository
import net.corda.nodeapi.internal.SignedNodeInfo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import javax.annotation.PostConstruct

/**
 * SQL Lite implementation of a Repository for storing NodeInfo.
 */
@Repository
class SqlLiteNodeInfoRepository(@SuppressWarnings("unused") @Autowired ignored: SerializationEngine) : SqlLiteRepository(), NodeInfoRepository {

    @PostConstruct
    fun connect() {
        dataSource.write {
            it.createStatement().execute(BUILD_TABLE_SQL)
            it.createStatement().execute(BUILD_INDEX_SQL)
        }
    }

    override fun persistSignedNodeInfo(signedNodeInfo: SignedNodeInfo) {
        dataSource.write {
            it.prepareStatement(INSERT_SIGNED_NODE_INFO_SQL).use { preparedStatement ->
                preparedStatement.setString(1, signedNodeInfo.raw.hash.toString())
                preparedStatement.setBytes(2, (signedNodeInfo.serialize().bytes))
                preparedStatement.execute()
            }
        }

    }

    override fun getSignedNodeInfo(hash: String): Pair<SignedNodeInfo, ByteArray> {
        return dataSource.read {
            it.prepareStatement(GET_NODE_INFO_SQL).use({ preparedStatement ->
                preparedStatement.setString(1, hash)
                val executedResults = preparedStatement.executeQuery()
                val bytes = executedResults.getBytes(1)
                bytes.deserialize<SignedNodeInfo>() to bytes
            })
        }
    }

    override fun getAllHashes(): List<SecureHash> {
        return dataSource.read {
            val resultSet = it.prepareStatement(GET_ALL_HASHES_SQL).executeQuery()
            val results = mutableListOf<SecureHash>()
            while (resultSet.next()) {
                results.add(SecureHash.parse(resultSet.getString(1)))
            }
            results
        }
    }

    override fun purgeAllPersistedSignedNodeInfos(): Int {
        return dataSource.write {
            it.prepareStatement(DELETE_ALL_NODE_INFO_SQL).executeUpdate()
        }
    }

    companion object {

        private const val BUILD_TABLE_SQL = ("CREATE TABLE IF NOT EXISTS SIGNED_NODE_INFOS (\n"
                + "	hash TEXT PRIMARY KEY,\n"
                + "	data BLOB NOT NULL\n"
                + ");")

        private const val BUILD_INDEX_SQL = "CREATE INDEX IF NOT EXISTS SIGNED_NODE_INFOS_HASH_IDX\n" +
                "ON SIGNED_NODE_INFOS (hash);"

        private const val INSERT_SIGNED_NODE_INFO_SQL = "INSERT OR REPLACE INTO SIGNED_NODE_INFOS (hash, data) VALUES ( ?, ?) ;"

        private const val GET_NODE_INFO_SQL =
                "SELECT data FROM SIGNED_NODE_INFOS WHERE hash=?;"

        private const val GET_ALL_HASHES_SQL = "SELECT hash FROM SIGNED_NODE_INFOS;"

        private const val DELETE_ALL_NODE_INFO_SQL =
                "DELETE FROM SIGNED_NODE_INFOS;"
    }
}

