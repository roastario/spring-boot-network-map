/*
 * Copyright (c) 2018. B3i Switzerland. All rights reserved.
 *
 * http://www.b3i.tech
 */
package net.corda.network.map.notaries

import net.corda.core.node.NotaryInfo

interface NotaryInfoLoader {
    fun load(): List<NotaryInfo>
}