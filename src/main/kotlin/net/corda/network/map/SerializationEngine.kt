/*
 * Copyright (c) 2018. B3i Switzerland. All rights reserved.
 *
 * http://www.b3i.tech
 */
package net.corda.network.map

import net.corda.core.serialization.SerializationContext
import net.corda.core.serialization.internal.SerializationEnvironmentImpl
import net.corda.core.serialization.internal.nodeSerializationEnv
import net.corda.core.utilities.ByteSequence
import net.corda.nodeapi.internal.serialization.AMQP_P2P_CONTEXT
import net.corda.nodeapi.internal.serialization.SerializationFactoryImpl
import net.corda.nodeapi.internal.serialization.amqp.AbstractAMQPSerializationScheme
import net.corda.nodeapi.internal.serialization.amqp.SerializerFactory
import org.springframework.stereotype.Component

@Component
class SerializationEngine {
    init {
        if (nodeSerializationEnv == null) {
            val classloader = this.javaClass.classLoader
            nodeSerializationEnv = SerializationEnvironmentImpl(
                    SerializationFactoryImpl().apply {
                        registerScheme(object : AbstractAMQPSerializationScheme(emptyList()){
                            override fun canDeserializeVersion(byteSequence: ByteSequence, target: SerializationContext.UseCase): Boolean {
                                return target == SerializationContext.UseCase.P2P
                            }
                            override fun rpcClientSerializerFactory(context: SerializationContext): SerializerFactory {
                                throw UnsupportedOperationException()
                            }
                            override fun rpcServerSerializerFactory(context: SerializationContext): SerializerFactory {
                                throw UnsupportedOperationException()
                            }
                        })
                    },
                    p2pContext = AMQP_P2P_CONTEXT.withClassLoader(classloader)
            )
        }
    }
}