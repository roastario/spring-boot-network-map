package net.corda.network.map.repository.sqllite

import net.corda.core.crypto.SecureHash
import net.corda.core.serialization.deserialize
import net.corda.core.serialization.serialize
import net.corda.network.map.repository.NodeInfoRepository
import net.corda.network.map.repository.SqlLiteBacked
import net.corda.nodeapi.internal.SignedNodeInfo
import org.springframework.stereotype.Component
import org.sqlite.SQLiteDataSource
import java.sql.Connection
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

@Component
@SqlLiteBacked
class SqlLiteRepository : NodeInfoRepository {


    companion object {
        internal val globalLock: ReentrantReadWriteLock = ReentrantReadWriteLock()
        val jdbcUrl = "jdbc:sqlite:/opt/node-storage/nm.db:"
        val dataSource: SQLiteDataSource = SQLiteDataSource().also { it.url = jdbcUrl }
    }

    init {
        dataSource.write {
            it.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS NODEINFOS_BY_HASH
                    (
                        hash text NOT NULL PRIMARY KEY,
                        value BLOB NOT NULL
                    )
                """.trimIndent()
                )
            }

            it.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS HASH_BY_X500
                    (
                        x500 text NOT NULL PRIMARY KEY,
                        hash text NOT NULL
                    )
                    """.trimIndent()
                )

            }
        }
    }


    fun <T> SQLiteDataSource.read(block: (ds: Connection) -> T): T {
        globalLock.read {
            return this.connection.use(block)
        }
    }

    fun <T> SQLiteDataSource.write(block: (ds: Connection) -> T): T {
        globalLock.write {
            return this.connection.use(block)
        }
    }

    override fun persistSignedNodeInfo(signedNodeInfo: SignedNodeInfo) {
        dataSource.write {
            it.autoCommit = false
            it.prepareStatement(
                """
                INSERT INTO NODEINFOS_BY_HASH (HASH, VALUE) 
                VALUES (?,?)
                ON CONFLICT(HASH)
                DO UPDATE SET VALUE=excluded.VALUE;
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, signedNodeInfo.raw.hash.toString())
                statement.setBytes(2, signedNodeInfo.serialize().bytes)
                statement.execute()
            }
            it.prepareStatement(
                """
                INSERT INTO HASH_BY_X500 (X500, HASH) 
                VALUES (?,?)
                ON CONFLICT(X500)
                DO UPDATE SET HASH=excluded.HASH;                       
                """.trimIndent()
            ).use {statement ->
                statement.setString(1, signedNodeInfo.verified().legalIdentities.first().name.toString())
                statement.setString(2, signedNodeInfo.raw.hash.toString())
                statement.execute()
            }
            it.commit()
        }
    }

    override fun getSignedNodeInfo(hash: String): Pair<SignedNodeInfo, ByteArray>? {
        return dataSource.read {
            it.prepareStatement("SELECT hash, value FROM NODEINFOS_BY_HASH where hash = ? ").use { statement ->
                statement.setString(1, hash)
                val rs = statement.executeQuery()
                if (rs.next()) {
                    val bytes = rs.getBytes(2)
                    val signedNodeInfo = bytes.deserialize<SignedNodeInfo>()
                    signedNodeInfo to bytes
                } else {
                    null
                }
            }
        }
    }

    override fun getAllHashes(): Collection<SecureHash> {
        return dataSource.read {
            it.prepareStatement("SELECT hash FROM HASH_BY_X500").use { statement ->
                val resultSet = statement.executeQuery()
                resultSet.use {
                    generateSequence {
                        if (resultSet.next()) SecureHash.parse(resultSet.getString(1)) else null
                    }.toList()
                }
            }
        }
    }

    override fun purgeAllPersistedSignedNodeInfos(): Int {
        return dataSource.write {
            it.createStatement().use { statement ->
                statement.executeUpdate("TRUNCATE TABLE NODEINFOS_BY_HASH")
                statement.executeUpdate("TRUNCATE TABLE HASH_BY_X500")
            }
        }
    }

}