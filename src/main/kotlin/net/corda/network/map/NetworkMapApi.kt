/*
 */
package net.corda.network.map

import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.sha256
import net.corda.core.internal.SignedDataWithCert
import net.corda.core.internal.signWithCert
import net.corda.core.node.NetworkParameters
import net.corda.core.serialization.SerializedBytes
import net.corda.core.serialization.deserialize
import net.corda.core.serialization.serialize
import net.corda.core.utilities.loggerFor
import net.corda.network.map.bootstrapping.UbuntuBootstapper
import net.corda.network.map.certificates.CertificateUtils
import net.corda.network.map.notaries.NotaryInfoLoader
import net.corda.network.map.repository.MapBacked
import net.corda.network.map.repository.NodeInfoRepository
import net.corda.network.map.whitelist.JarLoader
import net.corda.nodeapi.internal.DEV_ROOT_CA
import net.corda.nodeapi.internal.SignedNodeInfo
import net.corda.nodeapi.internal.crypto.CertificateAndKeyPair
import net.corda.nodeapi.internal.network.NetworkMap
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.spec.ECParameterSpec
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.request.async.DeferredResult
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.collections.HashMap
import kotlin.collections.set
import kotlin.streams.toList


val rootCa = DEV_ROOT_CA

/**
 * API for serving the Network Map over HTTP to clients.
 *
 * Note that serializationEngine must remain as an Autowired parameter, even though its not explicitly used
 * by this class. Its needed to initialize the serialization engines in Corda.
 */
@RestController
class NetworkMapApi(
    @Autowired @MapBacked private val nodeInfoRepository: NodeInfoRepository,
    @Autowired private val notaryInfoLoader: NotaryInfoLoader,
    @Autowired private val jarLoader: JarLoader,
    @Autowired private val ubuntuBootstapper: UbuntuBootstapper,
    @Value("\${minimumPlatformVersion:1}") private val minPlatformVersion: String,
    @Value("\${doormanCN:BasicDoorman}") private val doormanCommonName: String,
    @Value("\${networkMapCN:BasicNetworkMap}") private val networkMapCommonName: String,
    @Suppress("unused") @Autowired private val serializationEngine: SerializationEngine
) {

    private val doormanCa = CertificateUtils.createDevDoormanCa(rootCa, doormanCommonName)
    private val networkMapCa = CertificateUtils.createDevNetworkMapCa(rootCa, networkMapCommonName)

    private val networkMapCert: X509Certificate = networkMapCa.certificate
    private val networkMapKeyPair = networkMapCa.keyPair

    private val doormanCert: X509Certificate = doormanCa.certificate
    private val doormanKeyPair = doormanCa.keyPair

    private val executorService = Executors.newSingleThreadExecutor()
    private val networkMap: AtomicReference<SerializedBytes<SignedDataWithCert<NetworkMap>>> = AtomicReference()
    private val signedNetworkParams: AtomicReference<SignedDataWithCert<NetworkParameters>> = AtomicReference()

    private val trustRoot = CertificateUtils.generateTrustStore()


    companion object {
        val logger: Logger = loggerFor<NetworkMapApi>()

        fun generateKeyPair(): KeyPair {
            val ecSpec: ECParameterSpec = ECNamedCurveTable.getParameterSpec("prime256v1");
            val generator: KeyPairGenerator = KeyPairGenerator.getInstance("ECDSA", "BC");
            generator.initialize(ecSpec, SecureRandom())
            return generator.generateKeyPair();
        }
    }

    private val csrHolder: MutableMap<String, JcaPKCS10CertificationRequest> = Collections.synchronizedMap(HashMap())
    private val signedCSRHolder: ConcurrentHashMap<String, ByteArray> = ConcurrentHashMap()

    init {
        val whiteList = jarLoader.generateWhitelist()
        whiteList.forEach { entry ->
            entry.value.forEach {
                logger.info("found hash: " + it + " for contractClass: " + entry.key)
            }
        }

        logger.info(
            "Starting NMS with Parameters = NetworkParameters(\n" +
                    "            minimumPlatformVersion = ${minPlatformVersion.toInt()},\n" +
                    "            notaries = ${notaryInfoLoader.load()},\n" +
                    "            maxMessageSize = 10485760 * 10,\n" +
                    "            maxTransactionSize = 10485760 * 5,\n" +
                    "            modifiedTime = Instant.MIN,\n" +
                    "            epoch = 10,\n" +
                    "            whitelistedContractImplementations = $whiteList\n" +
                    "        )"
        )

        logger.info("using: ${networkMapCert.subjectX500Principal.name} as network map certName issued by ${networkMapCert.issuerX500Principal.name}")
        logger.info("using: ${doormanCert.subjectX500Principal.name} as doorman certName issued by ${networkMapCert.issuerX500Principal.name}")

        val networkParams = NetworkParameters(
            minimumPlatformVersion = minPlatformVersion.toInt(),
            notaries = notaryInfoLoader.load().sortedBy { it.identity.name.toString() },
            maxMessageSize = 10485760 * 10,
            maxTransactionSize = 10485760 * 5,
            modifiedTime = Instant.MIN,
            epoch = 10,
            whitelistedContractImplementations = whiteList
        )
        signedNetworkParams.set(networkParams.signWithCert(networkMapKeyPair.private, networkMapCert))
        networkMap.set(buildNetworkMap())
    }

    @RequestMapping(path = ["/ping"], method = [RequestMethod.GET])
    fun ping(): ByteArray {
        return "OK".toByteArray()
    }

    @RequestMapping(path = ["network-map/bumpMPV"], method = [RequestMethod.GET])
    fun bumpMPVInNetParams() {
        val currentSignedParams = this.signedNetworkParams.get()
        val currentParams = currentSignedParams.verified()
        val newParams = currentParams.copy(
            minimumPlatformVersion = currentParams.minimumPlatformVersion + 1,
            epoch = currentParams.epoch + 1,
            notaries = notaryInfoLoader.load().sortedBy { it.identity.name.toString() })
        signedNetworkParams.set(newParams.signWithCert(networkMapKeyPair.private, networkMapCert))
        networkMap.set(buildNetworkMap())
    }

    @RequestMapping(path = ["network-map/bumpEpoch"], method = [RequestMethod.GET])
    fun bumpEpoch() {
        val currentSignedParams = this.signedNetworkParams.get()
        val currentParams = currentSignedParams.verified()
        val newParams = currentParams.copy(epoch = currentParams.epoch + 1, notaries = notaryInfoLoader.load().sortedBy { it.identity.name.toString() })
        signedNetworkParams.set(newParams.signWithCert(networkMapKeyPair.private, networkMapCert))
        networkMap.set(buildNetworkMap())
    }

    @RequestMapping(path = ["network-map/publish"], method = [RequestMethod.POST])
    fun postNodeInfo(@RequestBody input: ByteArray): DeferredResult<ResponseEntity<String>> {
        logger.debug("Processing network-map/publish")
        val deserializedSignedNodeInfo = input.deserialize<SignedNodeInfo>()
        logger.info("Processing network-map/publish for " + deserializedSignedNodeInfo.verified().legalIdentities)
        deserializedSignedNodeInfo.verified()
        nodeInfoRepository.persistSignedNodeInfo(deserializedSignedNodeInfo)
        val result = DeferredResult<ResponseEntity<String>>()
        executorService.submit {
            networkMap.set(buildNetworkMap())
            result.setResult(ResponseEntity.ok().body("OK"))
        }
        return result
    }

    @RequestMapping(path = ["truststore", "trustStore"], method = [RequestMethod.GET])
    fun getTrustStore(): ResponseEntity<ByteArray> {
        return ResponseEntity.ok()
            .header("Content-Type", "application/octet-stream")
            .header("Content-Disposition", "attachment; filename=\"network-root-truststore.jks\"")
            .body(trustRoot)
    }

    @RequestMapping(path = ["certificate"], method = [RequestMethod.POST])
    fun submitCSR(@RequestBody input: ByteArray): DeferredResult<ResponseEntity<String>> {
        val csr = JcaPKCS10CertificationRequest(input)
        logger.info("Received A CSR: ${csr.publicKey}")
        val id = csr.subject.toString().toByteArray().sha256().toString()
        val result = DeferredResult<ResponseEntity<String>>()
        if (csrHolder[id] != null) {
            if (csr != (csrHolder[id])){
                throw IllegalStateException("Duplicated CSR for ${csr.subject}")
            }
        }
        executorService.submit {
            csrHolder[id] = csr
            result.setResult(ResponseEntity.ok().body(id))
        }
        return result
    }

    @RequestMapping(path = ["/certificate/{id}"], method = [RequestMethod.GET])
    fun getSignedCSR(@PathVariable("id") id: String): ResponseEntity<ByteArray> {
        val csr = csrHolder[id]

        val issuerCert = doormanCert
        val issuerKeyPair = doormanKeyPair

        val zipBytes = csr?.let {
            val nodeCaCertificate = CertificateUtils.createAndSignNodeCACerts(CertificateAndKeyPair(issuerCert, issuerKeyPair), csr)
            val backingStream = ByteArrayOutputStream()
            backingStream.use {
                val zipOutputStream = ZipOutputStream(backingStream);
                zipOutputStream.use {
                    listOf(nodeCaCertificate, doormanCert, rootCa.certificate).forEach { certificate ->
                        zipOutputStream.putNextEntry(ZipEntry(certificate.subjectX500Principal.name))
                        zipOutputStream.write(certificate.encoded)
                        zipOutputStream.closeEntry()
                    }
                }
            }
            val zipBytes = backingStream.toByteArray()
            zipBytes
        }

        return zipBytes?.let {
            ResponseEntity.ok()
                .header("Content-Type", "application/octet-stream")
                .body(zipBytes)
        } ?: ResponseEntity.notFound().build()

    }

    @RequestMapping(path = ["network-map/node-info/{hash}"], method = [RequestMethod.GET])
    fun getNodeInfo(@PathVariable("hash") input: String): ResponseEntity<ByteArray>? {
        logger.info("Processing retrieval of nodeInfo for {$input}.")
        val foundNodeInfo = nodeInfoRepository.getSignedNodeInfo(input)
        return if (foundNodeInfo == null) {
            ResponseEntity.notFound().build()
        } else {
            return ResponseEntity.ok()
                .contentLength(foundNodeInfo.second.size.toLong())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(foundNodeInfo.second)
        }

    }

    @RequestMapping(path = ["network-map"], method = [RequestMethod.GET])
    fun getNetworkMap(): ResponseEntity<ByteArray> {
        logger.debug("Processing method to obtain network map.")
        return if (networkMap.get() != null) {
            val networkMapBytes = networkMap.get().bytes
            ResponseEntity.ok()
                .contentLength(networkMapBytes.size.toLong())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Cache-Control", "max-age=${ThreadLocalRandom.current().nextInt(10, 30)}")
                .body(networkMapBytes)
        } else {
            ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    @RequestMapping(
        method = [RequestMethod.GET],
        path = ["network-map/network-parameters/{hash}"],
        produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE]
    )
    fun getNetworkParams(@PathVariable("hash") h: String): ResponseEntity<ByteArray> {
        logger.info("Processing retrieval of network params for {$h}.")
        return if (SecureHash.parse(h) == signedNetworkParams.get().raw.hash) {
            ResponseEntity.ok().header("Cache-Control", "max-age=${ThreadLocalRandom.current().nextInt(10, 30)}")
                .body(signedNetworkParams.get().serialize().bytes)
        } else {
            ResponseEntity.notFound().build()
        }
    }


    @ResponseBody
    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["network-map/reset-persisted-nodes"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun resetPersistedNodes(): ResponseEntity<String> {
        val result = nodeInfoRepository.purgeAllPersistedSignedNodeInfos()
        val resultMsg = "Deleted : {$result} rows."
        logger.info(resultMsg)
        return ResponseEntity(resultMsg, HttpStatus.ACCEPTED)
    }

    @ResponseBody
    @RequestMapping(
        method = [RequestMethod.GET],
        value = ["network-map/map-stats"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun fetchMapState(): SimpleMapState {
        val stats = SimpleMapState()
        signedNetworkParams.get().verified().notaries.forEach {
            stats.notaryNames.add("organisationUnit=" + it.identity.name.organisationUnit + " organisation=" + it.identity.name.organisation + " locality=" + it.identity.name.locality + " country=" + it.identity.name.country)
        }
        val allNodes = nodeInfoRepository.getAllHashes()
        allNodes.forEach {
            val pair: Pair<SignedNodeInfo, ByteArray>? = nodeInfoRepository.getSignedNodeInfo(it.toString())
            pair?.let {
                val orgName = pair.first.verified().legalIdentities[0].name.organisation
                stats.nodeNames.add(orgName)
            }
        }
        return stats
    }

    @GetMapping("install.sh")
    fun installScript(@RequestHeader(value = "Host") host: String): ResponseEntity<String> =
        ResponseEntity(ubuntuBootstapper.installScript(host), HttpStatus.OK)

    class SimpleMapState {
        val nodeNames: MutableList<String> = emptyList<String>().toMutableList()
        val notaryNames: MutableList<String> = emptyList<String>().toMutableList()
        val notaryCount get() = notaryNames.size
        val nodeCount get() = nodeNames.size
    }

    private fun buildNetworkMap(): SerializedBytes<SignedDataWithCert<NetworkMap>> {
        val allNodes = nodeInfoRepository.getAllHashes()
        logger.info("Processing retrieval of allNodes from the db and found {${allNodes.size}}.")
        return NetworkMap(allNodes.toList(), signedNetworkParams.get().raw.hash, null).signWithCert(
            networkMapKeyPair.private,
            networkMapCert
        ).serialize()
    }


}
