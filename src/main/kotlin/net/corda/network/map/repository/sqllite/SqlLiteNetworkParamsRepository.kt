/*
 * Copyright (c) 2018. B3i Switzerland. All rights reserved.
 *
 * http://www.b3i.tech
 */
package net.corda.network.map.repository.sqllite

import net.corda.core.crypto.SecureHash
import net.corda.core.node.NetworkParameters
import net.corda.core.serialization.deserialize
import net.corda.core.serialization.serialize
import net.corda.network.map.repository.NetworkParamsRepository
import net.corda.network.map.SerializationEngine
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import javax.annotation.PostConstruct

/**
 * SQL Lite implementation of a Repository for storing NetworkParams.
 */
@Repository
class SqlLiteNetworkParamsRepository(@Value("\${db.location:network_map.db}") dbLocation: String,
                                     @SuppressWarnings("unused") @Autowired ignored: SerializationEngine) : SqlLiteRepository(dbLocation), NetworkParamsRepository {


    @PostConstruct
    fun connect() {
        dataSource.write {
            it.createStatement().execute(BUILD_TABLE_SQL)
            it.createStatement().execute(BUILD_INDEX_SQL)
        }
    }

    override fun persistNetworkParams(networkParams: NetworkParameters, hash: SecureHash) {
        dataSource.write {
            it.prepareStatement(INSERT_NETWORK_PARAMETERS_SQL).use { preparedStatement ->
                preparedStatement.setString(1, hash.toString())
                preparedStatement.setBytes(2, networkParams.serialize().bytes)
                val rowsInserted = if (preparedStatement.execute()) 0 else preparedStatement.updateCount
            }
        }
    }

    override fun getNetworkParams(hash: SecureHash): Pair<NetworkParameters, ByteArray> {
        return dataSource.read {
            it.prepareStatement(GET_NETWORK_PARAMETERS_SQL).use({ preparedStatement ->
                preparedStatement.setString(1, hash.toString())
                val executedResults = preparedStatement.executeQuery()
                val bytes = executedResults.getBytes(1)
                bytes.deserialize<NetworkParameters>() to bytes
            })
        }
    }

    override fun getLatestNetworkParams(): Pair<NetworkParameters, SecureHash>? {
        return dataSource.read {
            it.prepareStatement(GET_LATEST_NETWORK_SQL).use({ preparedStatement ->
                val executedResults = preparedStatement.executeQuery()
                if (executedResults.next()) {
                    val hash = executedResults.getString(1)
                    val bytes = executedResults.getBytes(2)
                    bytes.deserialize<NetworkParameters>() to SecureHash.parse(hash)
                } else {
                    null
                }
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


    companion object {
        private const val BUILD_TABLE_SQL = "CREATE TABLE IF NOT EXISTS NETWORK_PARAMETERS (\n" +
                "  hash           TEXT UNIQUE,\n" +
                "  data           BLOB NOT NULL\n" +
                ");"

        private const val BUILD_INDEX_SQL = "CREATE INDEX IF NOT EXISTS NETWORK_PARAMETERS_HASH_IDX\n" +
                "ON NETWORK_PARAMETERS (hash);"

        private const val INSERT_NETWORK_PARAMETERS_SQL =
                "INSERT or REPLACE INTO NETWORK_PARAMETERS (hash, data) VALUES ( ? , ? );"

        private const val GET_NETWORK_PARAMETERS_SQL =
                "SELECT data FROM NETWORK_PARAMETERS WHERE hash=?;"

        private const val GET_ALL_HASHES_SQL = "SELECT hash FROM NETWORK_PARAMETERS;"

        private const val GET_LATEST_NETWORK_SQL = "SELECT hash,data FROM NETWORK_PARAMETERS ORDER BY ROWID DESC LIMIT 1"
    }
}

