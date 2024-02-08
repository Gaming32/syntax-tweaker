package io.github.gaming32.syntaxtweaker.tweaks.registry.loader.script

import io.github.gaming32.syntaxtweaker.tweaks.registry.loader.TweakLoader
import java.io.File
import java.net.URL
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.host.UrlScriptSource
import kotlin.script.experimental.host.toScriptSource

object ScriptTweakLoader : TweakLoader<SourceCode> {
    fun load(file: File) = load(file.toScriptSource())

    fun load(source: String) = load(source.toScriptSource())

    fun load(url: URL) = load(UrlScriptSource(url))

    override fun load(from: SourceCode) = listOf(TweakerScriptingHost.createRegistrar(from))
}
