/*
 * Copyright (c) 2018. B3i Switzerland. All rights reserved.
 *
 * http://www.b3i.tech
 */
package net.corda.network.map

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

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
    val app = SpringApplication(NetworkMapApplication::class.java)
    app.isWebEnvironment = true
    app.run(*args)
}

