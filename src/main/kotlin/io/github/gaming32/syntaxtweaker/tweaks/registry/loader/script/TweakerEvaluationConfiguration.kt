package io.github.gaming32.syntaxtweaker.tweaks.registry.loader.script

import io.github.gaming32.syntaxtweaker.tweaks.registry.TweakRegistry
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.compilationConfiguration
import kotlin.script.experimental.api.constructorArgs

class TweakerEvaluationConfiguration(registry: TweakRegistry) : ScriptEvaluationConfiguration({
    compilationConfiguration(TweakerCompilationConfiguration)
    constructorArgs(registry)
})
