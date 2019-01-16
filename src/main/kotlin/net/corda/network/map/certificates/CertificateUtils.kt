package net.corda.network.map.certificates

import net.corda.core.identity.CordaX500Name
import net.corda.core.internal.toX500Name
import net.corda.network.map.NetworkMapApi
import net.corda.nodeapi.internal.DEV_CA_TRUST_STORE_PASS
import net.corda.nodeapi.internal.DEV_ROOT_CA
import net.corda.nodeapi.internal.crypto.*
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralSubtree
import org.bouncycastle.asn1.x509.NameConstraints
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.pkcs.PKCS10CertificationRequest
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest
import java.io.ByteArrayOutputStream
import java.security.cert.X509Certificate
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import javax.security.auth.x500.X500Principal

class CertificateUtils {

    companion object {

        val provider = BouncyCastleProvider()

        fun createDevNetworkMapCa(rootCa: CertificateAndKeyPair): CertificateAndKeyPair {
            val keyPair = NetworkMapApi.generateKeyPair()
            val cert = X509Utilities.createCertificate(
                    CertificateType.NETWORK_MAP,
                    rootCa.certificate,
                    rootCa.keyPair,
                    X500Principal("CN=BasicNetworkMap,O=R3 Ltd,L=London,C=GB"),
                    keyPair.public
            )
            return CertificateAndKeyPair(cert, keyPair)
        }

        fun createDevDoormanCa(rootCa: CertificateAndKeyPair): CertificateAndKeyPair {
            val keyPair = NetworkMapApi.generateKeyPair()
            val cert = X509Utilities.createCertificate(
                    CertificateType.INTERMEDIATE_CA,
                    rootCa.certificate,
                    rootCa.keyPair,
                    X500Principal("CN=BasicDoorman,O=R3 Ltd,L=London,C=GB"),
                    keyPair.public
            )
            return CertificateAndKeyPair(cert, keyPair)
        }

        fun generateTrustStore(): ByteArray = ByteArrayOutputStream().apply {
            X509KeyStore(DEV_CA_TRUST_STORE_PASS).apply {
                setCertificate("cordarootca", DEV_ROOT_CA.certificate)
            }.internal.store(this, DEV_CA_TRUST_STORE_PASS.toCharArray())
        }.toByteArray()


        fun createAndSignNodeCACerts(caCertAndKey: CertificateAndKeyPair,
                                     request: PKCS10CertificationRequest): X509Certificate {
            val jcaRequest = JcaPKCS10CertificationRequest(request)
            val type = CertificateType.NODE_CA
            val nameConstraints = NameConstraints(
                    arrayOf (GeneralSubtree(GeneralName(
                            GeneralName.directoryName,
                            CordaX500Name.parse(jcaRequest.subject.toString()).copy(commonName = null).toX500Name()))),
                    arrayOf()
            )
            val issuerCertificate = caCertAndKey.certificate
            val issuerKeyPair = caCertAndKey.keyPair
            val validityWindow = Date.from(Instant.now()) to Date.from(Instant.now().plus(500, ChronoUnit.DAYS))
            val subject = X500Principal(CordaX500Name.parse(jcaRequest.subject.toString()).toX500Name().encoded)
            val builder = X509Utilities.createPartialCertificate(type, issuerCertificate.subjectX500Principal, issuerKeyPair.public, subject, jcaRequest.publicKey, validityWindow, nameConstraints)
            val signer = JcaContentSignerBuilder("SHA256withECDSA").setProvider(provider).build(issuerKeyPair.private)
            val certificate = builder.build(signer).toJca()
            certificate.checkValidity(Date())
            certificate.verify(issuerKeyPair.public)
            return certificate
        }

    }

}