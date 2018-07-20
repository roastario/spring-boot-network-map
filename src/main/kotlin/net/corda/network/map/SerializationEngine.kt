/*
 */
package net.corda.network.map

import net.corda.core.serialization.SerializationContext
import net.corda.core.serialization.internal.SerializationEnvironmentImpl
import net.corda.core.serialization.internal.nodeSerializationEnv
import net.corda.serialization.internal.AMQP_P2P_CONTEXT
import net.corda.serialization.internal.CordaSerializationMagic
import net.corda.serialization.internal.SerializationFactoryImpl
import net.corda.serialization.internal.amqp.AbstractAMQPSerializationScheme
import net.corda.serialization.internal.amqp.SerializerFactory
import net.corda.serialization.internal.amqp.amqpMagic
import org.springframework.stereotype.Component

@Component
class SerializationEngine {
    init {
        if (nodeSerializationEnv == null) {
            val classloader = this.javaClass.classLoader
            nodeSerializationEnv = SerializationEnvironmentImpl(
                    SerializationFactoryImpl().apply {
                        registerScheme(object : AbstractAMQPSerializationScheme(emptyList()){
                            override fun canDeserializeVersion(magic: CordaSerializationMagic, target: SerializationContext.UseCase): Boolean {
                                return (magic == amqpMagic && target == SerializationContext.UseCase.P2P)
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