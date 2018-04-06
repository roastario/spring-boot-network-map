package net.corda.network.map.sqllite

import org.sqlite.SQLiteDataSource
import java.sql.Connection
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

abstract class SqlLiteRepository(dbLocation: String) {

    internal val jdbcUrl = "jdbc:sqlite:$dbLocation"
    internal val dataSource: SQLiteDataSource = SQLiteDataSource()

    init {
        dataSource.url = jdbcUrl
    }

    companion object {
        internal val globalLock: ReentrantReadWriteLock = ReentrantReadWriteLock()
    }


    fun <T> SQLiteDataSource.read(block: (ds: Connection) -> T): T {
        globalLock.read {
            return dataSource.connection.use(block)
        }
    }

    fun <T> SQLiteDataSource.write(block: (ds: Connection) -> T): T {
        globalLock.write {
            return dataSource.connection.use(block)
        }
    }

}