package net.corda.network.map.whitelist

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractClassName
import net.corda.core.crypto.SecureHash
import net.corda.core.internal.*
import net.corda.core.node.services.AttachmentId
import net.corda.nodeapi.internal.ContractsJar
import net.corda.nodeapi.internal.coreContractClasses
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File
import java.net.URLClassLoader
import java.nio.file.LinkOption
import java.nio.file.Path
import java.util.*
import kotlin.streams.toList

@Component()
class JarLoader(@Value("\${jars.location:/jars}") jarDir: String?) {
    private val directory = File(jarDir).toPath()

    private fun loadJars(): List<Path> {
        return directory.ifExists {
            val cordappJars = directory.list { paths ->
                paths.filter { it.toString().endsWith(".jar") && it.fileName.toString() != "corda.jar" }.toList()
            }
            cordappJars;
        } ?: emptyList()
    }

    fun generateWhitelist(): Map<ContractClassName, List<AttachmentId>> {

        val newWhiteList = loadJars().map { it -> ContractsJarFile(it) }
                .flatMap { jar -> (jar.scan()).map { it to jar.hash } }
                .toMultiMap()

        return (newWhiteList.keys).associateBy({ it }) {
            val newHashes = newWhiteList[it] ?: emptyList()
            newHashes.distinct()
        }
    }

    class ContractsJarFile(private val file: Path) : ContractsJar {
        override val hash: SecureHash by lazy(LazyThreadSafetyMode.NONE, file::hash)

        override fun scan(): List<ContractClassName> {
            val scanResult = FastClasspathScanner()
                    // A set of a single element may look odd, but if this is removed "Path" which itself is an `Iterable`
                    // is getting broken into pieces to scan individually, which doesn't yield desired effect.
                    .overrideClasspath(Collections.singleton(file))
                    .scan()

            val contractClassNames = coreContractClasses
                    .flatMap { scanResult.getNamesOfClassesImplementing(it.qualifiedName) }
                    .toSet()

            return URLClassLoader(arrayOf(file.toUri().toURL()), Contract::class.java.classLoader).use { cl ->
                contractClassNames.mapNotNull {
                    val contractClass = cl.loadClass(it)
                    // Only keep instantiable contracts
                    if (contractClass.isConcreteClass) contractClass.name else null
                }
            }
        }
    }

}

fun <R> Path.ifExists(block: (Path) -> R): R? {
    if (this.exists()) {
        return block.invoke(this);
    } else {
        return null
    }
}
