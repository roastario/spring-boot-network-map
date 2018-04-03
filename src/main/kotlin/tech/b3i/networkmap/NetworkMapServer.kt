package tech.b3i.networkmap

import net.corda.core.crypto.Crypto
import net.corda.core.internal.SignedDataWithCert
import net.corda.core.internal.signWithCert
import net.corda.core.node.NetworkParameters
import net.corda.core.serialization.SerializedBytes
import net.corda.core.serialization.deserialize
import net.corda.core.serialization.internal.SerializationEnvironmentImpl
import net.corda.core.serialization.internal.nodeSerializationEnv
import net.corda.core.serialization.serialize
import net.corda.nodeapi.internal.DEV_ROOT_CA
import net.corda.nodeapi.internal.SignedNodeInfo
import net.corda.nodeapi.internal.crypto.CertificateAndKeyPair
import net.corda.nodeapi.internal.crypto.CertificateType
import net.corda.nodeapi.internal.crypto.X509Utilities
import net.corda.nodeapi.internal.network.NetworkMap
import net.corda.nodeapi.internal.serialization.AMQP_P2P_CONTEXT
import net.corda.nodeapi.internal.serialization.AMQP_STORAGE_CONTEXT
import net.corda.nodeapi.internal.serialization.SerializationFactoryImpl
import net.corda.nodeapi.internal.serialization.amqp.AMQPServerSerializationScheme
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.request.async.DeferredResult
import java.security.cert.X509Certificate
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import javax.security.auth.x500.X500Principal


@RestController
class NetworkMapServer(
        @Autowired val networkMapPersistance: NetworkMapPersistance,
        @Autowired val notaryLoader: NotaryLoader
) {

    val networkMapCa = createDevNetworkMapCa2(DEV_ROOT_CA)
    val networkMapCert: X509Certificate = networkMapCa.certificate
    val keyPair = networkMapCa.keyPair

    private val networkParams = NetworkParameters(1, notaryLoader.invoke(), 10485760, Int.MAX_VALUE, Instant.now(), 10, emptyMap())
    private val executorService = Executors.newSingleThreadExecutor()
    private val networkMap: AtomicReference<SerializedBytes<SignedDataWithCert<NetworkMap>>> = AtomicReference()


    init {
        if (nodeSerializationEnv == null) {
            val classloader = this.javaClass.classLoader
            nodeSerializationEnv = SerializationEnvironmentImpl(
                    SerializationFactoryImpl().apply {
                        registerScheme(AMQPServerSerializationScheme(emptyList()))
                    },
                    p2pContext = AMQP_P2P_CONTEXT.withClassLoader(classloader),
                    rpcServerContext = AMQP_P2P_CONTEXT.withClassLoader(classloader),
                    storageContext = AMQP_STORAGE_CONTEXT.withClassLoader(classloader),
                    checkpointContext = AMQP_P2P_CONTEXT.withClassLoader(classloader)
            )
        }
        networkMap.set(buildNetworkMap())
    }


    @RequestMapping(path = ["/ping"], method = [RequestMethod.GET])
    fun ping(): ByteArray {
        return "OK".toByteArray()
    }


    @RequestMapping(path = ["network-map/publish"], method = [RequestMethod.POST])
    fun postNodeInfo(@RequestBody input: ByteArray): DeferredResult<ResponseEntity<String>> {
        val deserializedSignedNodeInfo = input.deserialize<SignedNodeInfo>()
        val deserializedNodeInfo = deserializedSignedNodeInfo.raw.deserialize()
        deserializedSignedNodeInfo.verified()
        networkMapPersistance.persistSignedNodeInfo(deserializedSignedNodeInfo)
        val result = DeferredResult<ResponseEntity<String>>()
        executorService.submit({
            networkMap.set(buildNetworkMap())
            result.setResult(ResponseEntity.ok("OK"))
        })
        return result;
    }

    @RequestMapping(path = ["network-map/node-info/{hash}"], method = [RequestMethod.GET])
    fun getNodeInfo(@PathVariable("hash") input: String): ResponseEntity<ByteArray>? {
        val (signed, bytes) = networkMapPersistance.getSignedNodeInfo(input)
        return ResponseEntity.ok()
                .contentLength(bytes.size.toLong())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @RequestMapping(path = ["network-map"], method = [RequestMethod.GET])
    fun getNetworkMap(): ResponseEntity<ByteArray> {
        if (networkMap.get() != null) {
            val networkMapBytes = networkMap.get().bytes
            return ResponseEntity.ok()
                    .contentLength(networkMapBytes.size.toLong())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(networkMapBytes);
        } else {
            return ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    fun buildNetworkMap(): SerializedBytes<SignedDataWithCert<NetworkMap>> {
        val allNodes = networkMapPersistance.getAllHashes()
        val signedNetworkParams = networkParams.signWithCert(keyPair.private, networkMapCert)
        return NetworkMap(allNodes, signedNetworkParams.raw.hash, null).signWithCert(keyPair.private, networkMapCert).serialize()
    }


    fun createDevNetworkMapCa2(rootCa: CertificateAndKeyPair): CertificateAndKeyPair {
        val keyPair = Crypto.generateKeyPair()
        val cert = X509Utilities.createCertificate(
                CertificateType.NETWORK_MAP,
                rootCa.certificate,
                rootCa.keyPair,
                X500Principal("CN=Network Map,O=R3 Ltd,L=London,C=GB"),
                keyPair.public)
        return CertificateAndKeyPair(cert, keyPair)
    }

}