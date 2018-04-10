package net.corda.network.map.certificates

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigParseOptions
import net.corda.nodeapi.internal.crypto.X509KeyStore
import java.io.ByteArrayInputStream
import java.io.OutputStream

fun X509KeyStore.store(outputStream: OutputStream, password: String) {
    internal.store(outputStream, password.toCharArray())
}


fun loadConfig(input: ByteArray): Config {
    val parseOptions = ConfigParseOptions.defaults()
    val defaultConfig = ConfigFactory.parseResources("reference.conf", parseOptions.setAllowMissing(false))
    val appConfig = ConfigFactory.parseReader(ByteArrayInputStream(input).reader())
    val finalConfig = ConfigFactory.parseMap(mapOf("baseDirectory" to "/tmp/"))
            .withFallback(appConfig)
            .withFallback(defaultConfig)
            .resolve()
    return finalConfig
}