package io.github.gaming32.syntaxtweaker.tweakerscript

import io.github.gaming32.syntaxtweaker.tweaks.registry.TweakParser
import io.github.gaming32.syntaxtweaker.tweaks.registry.TweakRegistry
import kotlin.script.experimental.annotations.KotlinScript

@KotlinScript(
    fileExtension = "tweaker.kts",
    compilationConfiguration = TweakerCompilationConfiguration::class
    // evaluationConfiguration is custom, see TweakerScriptingHost
)
abstract class TweakerScript(val registry: TweakRegistry) {
    fun register(key: String, parser: TweakParser<*>, replace: Boolean = false) =
        registry.register(key, parser, replace)
}
