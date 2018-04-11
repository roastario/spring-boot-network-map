/*
 * Copyright (c) 2018. B3i Switzerland. All rights reserved.
 *
 * http://www.b3i.tech
 */
package net.corda.network.map.filesystem

import com.typesafe.config.ConfigFactory
import net.corda.core.identity.Party

import net.corda.core.node.NodeInfo
import net.corda.core.node.NotaryInfo
import net.corda.core.serialization.deserialize
import net.corda.network.map.NotaryInfoLoader
import net.corda.nodeapi.internal.SignedNodeInfo

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext

import org.springframework.stereotype.Component
import java.io.InputStreamReader


@Component
class FilesystemNotaryInfoLoader(@Autowired private val context: ApplicationContext,
                                 @Value("\${nodesDirectoryUrl:classpath:nodes}") private val nodesDirectoryUrl: String)
    : NotaryInfoLoader {

    override fun load(): List<NotaryInfo> {
        val nodeResources = context.getResources("$nodesDirectoryUrl/*.conf")
        log.info("Started scanning nodes directory $nodesDirectoryUrl for notaries in node.conf files")
        log.info("Found ${nodeResources.size} node.conf files")


        val notaries = nodeResources
                .mapNotNull {
                    val isr = InputStreamReader(it.inputStream)
                    ConfigFactory.parseReader(isr) to it
                }
                .filter {
                    it.first.hasPath("notary") }
                .map { (notaryNodeConf, notaryNodeConfFile) ->
                    val validating = notaryNodeConf.getConfig("notary").getBoolean("validating")
                    val legalName = notaryNodeConf.getString("myLegalName")
                    var thisOne : NotaryInfo? = null
                    context.getResources("$nodesDirectoryUrl/nodeInfo-*").forEach {
                        val nodeInfo = it.inputStream.readBytes(DEFAULT_BUFFER_SIZE).deserialize<SignedNodeInfo>().verified()
                        val organisation = nodeInfo.legalIdentities.first().name.organisation
                        if (legalName.contains(organisation)) {
                            thisOne = NotaryInfo(nodeInfo.notaryIdentity(), validating = validating)

                        }
                    }
                    thisOne!!
                }

        log.info("Found ${notaries.size} notaries in $nodesDirectoryUrl")

        return notaries
    }

    companion object {
        private val log = LoggerFactory.getLogger(FilesystemNotaryInfoLoader::class.java)
    }

}


private fun NodeInfo.notaryIdentity(): Party {
    return when (legalIdentities.size) {
    // Single node notaries have just one identity like all other nodes. This identity is the notary identity
        1 -> legalIdentities[0]
    // Nodes which are part of a distributed notary have a second identity which is the composite identity of the
    // cluster and is shared by all the other members. This is the notary identity.
        2 -> legalIdentities[1]
        else -> throw IllegalArgumentException("Not sure how to get the notary identity in this scenario: $this")
    }
}

