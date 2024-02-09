package io.github.gaming32.syntaxtweaker.tweakerscript

import io.github.gaming32.syntaxtweaker.TweakTarget
import io.github.gaming32.syntaxtweaker.tweaks.SyntaxTweak
import io.github.gaming32.syntaxtweaker.tweaks.registry.TweakParser
import java.io.File
import java.security.MessageDigest
import kotlin.script.experimental.api.ScriptAcceptedLocation
import kotlin.script.experimental.api.ScriptCompilationConfiguration
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

@OptIn(ExperimentalStdlibApi::class)
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
            compilationCache(CompiledScriptJarsCache { source, _ ->
                val hash = MessageDigest.getInstance("SHA-1").digest(source.text.encodeToByteArray()).toHexString()
                TweakerCompilationConfiguration.CACHE_ROOT.resolve("$hash.jar").also { it.parentFile.mkdirs() }
            })
        }
    })
}) {
    private val CACHE_ROOT = File(System.getProperty("user.home")).resolve(".syntax-tweaker/cache/scripts")

    private fun readResolve(): Any = this
}
