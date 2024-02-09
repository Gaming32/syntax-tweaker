package io.github.gaming32.syntaxtweaker.tweakerscript

import io.github.gaming32.syntaxtweaker.TweakTarget
import io.github.gaming32.syntaxtweaker.tweaks.SyntaxTweak
import io.github.gaming32.syntaxtweaker.tweaks.registry.TweakParser
import io.github.gaming32.syntaxtweaker.util.stableToString
import java.io.File
import java.security.MessageDigest
import kotlin.script.experimental.api.ScriptAcceptedLocation
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.acceptedLocations
import kotlin.script.experimental.api.baseClass
import kotlin.script.experimental.api.defaultImports
import kotlin.script.experimental.api.hostConfiguration
import kotlin.script.experimental.api.ide
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.compilationCache
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.jvmTarget
import kotlin.script.experimental.jvmhost.CompiledScriptJarsCache

object TweakerCompilationConfiguration : ScriptCompilationConfiguration({
    defaultImports(TweakTarget::class, SyntaxTweak::class, TweakParser::class)
    defaultImports(
        "io.github.gaming32.syntaxtweaker.data.*",
        "io.github.gaming32.syntaxtweaker.data.MemberReference.Companion.toMemberReference",
        "io.github.gaming32.syntaxtweaker.util.*",
        "org.jetbrains.kotlin.com.intellij.psi.*",
        "org.jetbrains.kotlin.utils.addToStdlib.*"
    )

    baseClass(TweakerScript::class)

    jvm {
        dependenciesFromCurrentContext(wholeClasspath = true)
        jvmTarget(System.getProperty("java.specification.version"))
    }

    ide {
        acceptedLocations(ScriptAcceptedLocation.Everywhere)
    }

    hostConfiguration(ScriptingHostConfiguration(defaultJvmScriptingHostConfiguration) {
        jvm {
            compilationCache(CompiledScriptJarsCache { source, config ->
                CACHE_ROOT.resolve("${hashScript(source, config)}.jar").also { it.parentFile.mkdirs() }
            })
        }
    })
}) {
    private fun readResolve(): Any = this
}

private val CACHE_ROOT = File(System.getProperty("user.home")).resolve(".syntax-tweaker/cache/scripts")

@OptIn(ExperimentalStdlibApi::class)
private fun hashScript(source: SourceCode, config: ScriptCompilationConfiguration): String {
    val digest = MessageDigest.getInstance("SHA-1")
    digest.update(source.text.encodeToByteArray())
    config.notTransientData.entries
        .sortedBy { it.key.name }
        .forEach { (key, value) ->
            digest.update(key.name.encodeToByteArray())
            digest.update(value.stableToString().encodeToByteArray())
        }
    return digest.digest().toHexString()
}
