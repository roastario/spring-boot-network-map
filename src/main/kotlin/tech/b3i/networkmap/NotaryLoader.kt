package tech.b3i.networkmap

import net.corda.core.node.NotaryInfo

interface NotaryLoader {
    fun load(): List<NotaryInfo>
}