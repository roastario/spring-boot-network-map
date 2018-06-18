package net.corda.network.map.certificates

data class RpcUser(val password: String, val user: String, val permissions: List<Permission> = listOf(Permission.ALL))


enum class Permission {
    ALL
}
