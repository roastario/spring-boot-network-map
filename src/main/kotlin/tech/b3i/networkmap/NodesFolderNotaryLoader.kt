package tech.b3i.networkmap

import com.typesafe.config.ConfigFactory
import net.corda.core.identity.Party
import net.corda.core.internal.readObject
import net.corda.core.node.NodeInfo
import net.corda.core.node.NotaryInfo
import net.corda.nodeapi.internal.SignedNodeInfo
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.DirectoryFileFilter
import org.apache.commons.io.filefilter.RegexFileFilter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File


@Component
class NodesFolderNotaryLoader(@Value("\${nodesFolder}") val nodesFolder: String,
                              @Autowired val serializationEngine: SerializationEngine) : NotaryLoader {

    override fun load(): List<NotaryInfo> {
        println("Started scanning nodes folder for notaries")
        val configFiles = FileUtils.listFiles(
                File(nodesFolder),
                RegexFileFilter("node.conf"),
                DirectoryFileFilter.DIRECTORY
        )

        val notaries =  configFiles.map {
            try {
                ConfigFactory.parseFile(it) to it
            } catch (error: Throwable) {
                println(error.stackTrace)
                null;
            }
        }.filterNotNull()
                .filter { it.first.hasPath("notary") }
                .map { (notaryNodeConf, notaryNodeConfFile) ->
                    val validating = notaryNodeConf.getConfig("notary").getBoolean("validating")
                    FileUtils.listFiles(notaryNodeConfFile.parentFile,
                            RegexFileFilter("nodeInfo-.*"),
                            null).firstOrNull() to validating
                }.filter { it.first != null }
                .map {
                    val nodeInfo = it.first!!.toPath()!!.readObject<SignedNodeInfo>().verified()
                    print("found notary: " + nodeInfo.legalIdentities +" @ " + nodeInfo.addresses)
                    NotaryInfo(nodeInfo.notaryIdentity(), validating = it.second)
                }
        println("Finished scanning nodes folder")
        return notaries
    }
}


private fun NodeInfo.notaryIdentity(): Party {
    return when (legalIdentities.size) {
    // Single node notaries have just one identity like all other nodes. This identity is the notary identity
        1 -> legalIdentities[0]
    // Nodes which are part of a distributed notary have a second identity which is the composite identity of the
    // cluster and is shared by all the other members. This is the notary identity.
        2 -> legalIdentities[1]
        else -> throw IllegalArgumentException("Not sure how to get the notary identity in this scenerio: $this")
    }
}

interface NotaryLoader {
    fun load(): List<NotaryInfo>
}
