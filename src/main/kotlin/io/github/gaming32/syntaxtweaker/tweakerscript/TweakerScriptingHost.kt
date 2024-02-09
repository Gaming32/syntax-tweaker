package io.github.gaming32.syntaxtweaker.tweakerscript

import io.github.gaming32.syntaxtweaker.tweaks.registry.loader.TweakRegistrar
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.compilationConfiguration
import kotlin.script.experimental.api.constructorArgs
import kotlin.script.experimental.api.isError
import kotlin.script.experimental.api.valueOr
import kotlin.script.experimental.api.valueOrThrow
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

object TweakerScriptingHost : BasicJvmScriptingHost() {
    fun createRegistrar(code: SourceCode): TweakRegistrar {
        val compilation = runInCoroutineContext {
            compiler(code, TweakerCompilationConfiguration)
        }.valueOr { failure ->
            throw IllegalArgumentException(
                failure.reports.asSequence()
                    .filter(ScriptDiagnostic::isError)
                    .joinToString("\n") { it.render().replace("\n", "\n    ") }
            )
        }
        return TweakRegistrar { registry ->
            val evaluationConfiguration = ScriptEvaluationConfiguration {
                compilationConfiguration(TweakerCompilationConfiguration)
                constructorArgs(registry)
            }
            runInCoroutineContext { evaluator(compilation, evaluationConfiguration) }
                .valueOrThrow()
        }
    }
}
