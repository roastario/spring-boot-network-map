package tech.b3i.networkmap

import net.corda.core.internal.mapNotNull
import net.corda.core.node.NotaryInfo
import net.corda.core.serialization.deserialize
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.toList


@Component
class FolderNotaryLoader(@Value("\${notaryFolder}") val notaryFolder: String) : NotaryLoader {
    override fun load(): List<NotaryInfo> {
        return invoke();
    }


    override fun invoke(): List<NotaryInfo> {
        return Files.list(Paths.get(notaryFolder))
                .map { it to FileInputStream(it.toFile()) }
                .map {
                    val bytes = it.second.use { it.readBytes() }
                    try {
                        bytes.deserialize<NotaryInfo>()
                    } catch (error: Throwable) {
                        null
                    }
                }.mapNotNull { it }.toList()

    }
}

interface NotaryLoader {

    fun load(): List<NotaryInfo>
    fun invoke(): List<NotaryInfo>
}
