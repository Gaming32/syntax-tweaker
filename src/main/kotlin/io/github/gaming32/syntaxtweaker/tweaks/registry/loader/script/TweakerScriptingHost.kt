package io.github.gaming32.syntaxtweaker.tweaks.registry.loader.script

import io.github.gaming32.syntaxtweaker.tweaks.registry.loader.TweakRegistrar
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.valueOrThrow
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

object TweakerScriptingHost : BasicJvmScriptingHost() {
    fun createRegistrar(code: SourceCode): TweakRegistrar {
        val compilation = runInCoroutineContext {
            compiler(code, TweakerCompilationConfiguration)
        }.valueOrThrow()
        return TweakRegistrar { registry ->
            runInCoroutineContext { evaluator(compilation, TweakerEvaluationConfiguration(registry)) }
                .valueOrThrow()
        }
    }
}
