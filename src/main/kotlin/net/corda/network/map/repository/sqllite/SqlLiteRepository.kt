package net.corda.network.map.repository.sqllite

import org.sqlite.SQLiteDataSource
import java.sql.Connection
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

abstract class SqlLiteRepository {


    companion object {
        internal val globalLock: ReentrantReadWriteLock = ReentrantReadWriteLock()
        val jdbcUrl = "jdbc:sqlite::memory:"
        val dataSource: SQLiteDataSource = SQLiteDataSource().also { it.url = jdbcUrl }
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