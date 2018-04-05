/*
 * Copyright (c) 2018. B3i Switzerland. All rights reserved.
 *
 * http://www.b3i.tech
 */
package net.corda.network.map.sqllite

import net.corda.core.crypto.SecureHash
import net.corda.core.serialization.deserialize
import net.corda.core.serialization.serialize
import net.corda.nodeapi.internal.SignedNodeInfo
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import org.sqlite.SQLiteDataSource
import net.corda.network.map.NodeInfoRepository
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.annotation.PostConstruct
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * SQL Lite implementation of a Repository for storing NodeInfo.
 */
@Repository
class SqlLiteNodeInfoRepository(@Value("\${db.location:network_map.db}") dbLocation: String) : NodeInfoRepository {

    private val jdbcUrl = "jdbc:sqlite:$dbLocation"
    private val dataSource: SQLiteDataSource = SQLiteDataSource()

    init {
        dataSource.url = jdbcUrl
    }


    @PostConstruct
    fun connect() {
        globalLock.read {
            dataSource.connection.use {
                it.createStatement().execute(BUILD_TABLE_SQL)
                it.createStatement().execute(BUILD_INDEX_SQL)
            }

        }
    }

    override fun persistSignedNodeInfo(signedNodeInfo: SignedNodeInfo) {
        globalLock.write {
            dataSource.connection.use {
                it.prepareStatement(INSERT_SIGNED_NODE_INFO_SQL).use { preparedStatement ->
                    preparedStatement.setString(1, signedNodeInfo.raw.hash.toString())
                    preparedStatement.setBytes(2, (signedNodeInfo.serialize().bytes))
                    preparedStatement.execute()
                }
            }

        }
    }

    override fun getSignedNodeInfo(hash: String): Pair<SignedNodeInfo, ByteArray> {
        return globalLock.write {
            dataSource.connection.use {
                it.prepareStatement(GET_NODE_INFO_SQL).use({ preparedStatement ->
                    preparedStatement.setString(1, hash)
                    val executedResults = preparedStatement.executeQuery()
                    val bytes = executedResults.getBytes(1)
                    bytes.deserialize<SignedNodeInfo>() to bytes
                })
            }
        }
    }

    override fun getAllHashes(): List<SecureHash> {
        return globalLock.read {
            dataSource.connection.use {
                val resultSet = it.prepareStatement(GET_ALL_HASHES_SQL).executeQuery()
                val results = mutableListOf<SecureHash>()
                while (resultSet.next()) {
                    results.add(SecureHash.parse(resultSet.getString(1)))
                }
                results
            }
        }
    }


    companion object {

        private const val BUILD_TABLE_SQL = ("CREATE TABLE IF NOT EXISTS SIGNED_NODE_INFOS (\n"
                + "	hash TEXT PRIMARY KEY,\n"
                + "	data BLOB NOT NULL\n"
                + ");")

        private const val BUILD_INDEX_SQL = "CREATE INDEX IF NOT EXISTS SIGNED_NODE_INFOS_HASH_IDX\n" +
                "ON SIGNED_NODE_INFOS (hash);"

        private const val INSERT_SIGNED_NODE_INFO_SQL =
                "INSERT INTO SIGNED_NODE_INFOS (hash, data) VALUES ( ? , ? );"

        private const val GET_NODE_INFO_SQL =
                "SELECT data FROM SIGNED_NODE_INFOS WHERE hash=?;"

        private const val GET_ALL_HASHES_SQL = "SELECT hash FROM SIGNED_NODE_INFOS;"

        private val globalLock: ReentrantReadWriteLock = ReentrantReadWriteLock()

    }
}

