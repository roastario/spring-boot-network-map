/*
 */
package net.corda.network.map.notaries.filesystem

import net.corda.core.identity.Party
import net.corda.core.internal.readObject
import net.corda.core.node.NodeInfo
import net.corda.core.node.NotaryInfo
import net.corda.network.map.SerializationEngine
import net.corda.network.map.notaries.NotaryInfoLoader
import net.corda.nodeapi.internal.SignedNodeInfo
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.DirectoryFileFilter
import org.apache.commons.io.filefilter.RegexFileFilter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component


@Component
class FilesystemNotaryInfoLoader(
    @Autowired val context: ApplicationContext,
    @SuppressWarnings("unused") serializationEngine: SerializationEngine,
    @Value("\${nodesDirectoryUrl:classpath:nodes}") private val nodesDirectoryUrl: String
) : NotaryInfoLoader {

    override fun load(): List<NotaryInfo> {
        val directoryToLoadFrom = context.getResource(nodesDirectoryUrl).file
        log.info("Started scanning nodes directory ${directoryToLoadFrom.absolutePath} for notaries nodeInfo files")
        val nodeInfoFiles = FileUtils.listFiles(
            directoryToLoadFrom,
            RegexFileFilter("nodeInfo-.*"),
            DirectoryFileFilter.DIRECTORY
        )
        log.info("Found ${nodeInfoFiles.size} nodeInfo files")
        val notaryInfos = nodeInfoFiles.map {
            val nodeInfo = it.toPath().readObject<SignedNodeInfo>()
            log.info("Found nodeInfo for notary: ${nodeInfo.verified().legalIdentities.first().name}")
            NotaryInfo(nodeInfo.verified().notaryIdentity(), false)
        }
        log.info("Found ${notaryInfos.size} notaries in ${directoryToLoadFrom.absolutePath}")
        return notaryInfos.toSet().toList()
    }

    companion object {
        private val log = LoggerFactory.getLogger(FilesystemNotaryInfoLoader::class.java)
    }

}


private fun NodeInfo.notaryIdentity(): Party {
    return when (legalIdentities.size) {
        1 -> legalIdentities[0]
        else -> throw IllegalArgumentException("Not sure how to get the notary identity in this scenario: $this")
    }
}

