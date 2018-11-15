package net.corda.network.map.whitelist

import net.corda.core.contracts.ContractClassName
import net.corda.core.node.services.AttachmentId
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component()
class JarLoader(@Value("\${jars.location:/jars}") jarDir: String?) {
    fun generateWhitelist(): Map<ContractClassName, List<AttachmentId>> {
        return emptyMap()
    }
}
