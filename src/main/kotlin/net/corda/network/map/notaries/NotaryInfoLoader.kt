/*
 */
package net.corda.network.map.notaries

import net.corda.core.node.NotaryInfo

interface NotaryInfoLoader {
    fun load(): List<NotaryInfo>
}