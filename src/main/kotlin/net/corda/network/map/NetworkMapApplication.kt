/*
 */
package net.corda.network.map

import net.corda.network.map.certificates.CertificateUtils
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import java.security.Security

/**
 * Starts the Network Map Service.
 *
 * @author Stephen Houston (steve) on 04/04/2018.
 */
@SpringBootApplication(scanBasePackages = ["net.corda.network.map"])
open class NetworkMapApplication

/**
 * Starts the Network Map Service.
 *
 * @param args Any args passed from the command line.
 */
fun main(args: Array<String>) {
    System.setProperty("logging.level.org.springframework.web", "DEBUG")
    Security.insertProviderAt(CertificateUtils.provider, 1);
    val app = SpringApplication(NetworkMapApplication::class.java)
    app.isWebEnvironment = true
    app.run(*args)
}

