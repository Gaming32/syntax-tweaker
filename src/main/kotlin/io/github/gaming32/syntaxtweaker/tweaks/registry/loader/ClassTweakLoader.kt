package io.github.gaming32.syntaxtweaker.tweaks.registry.loader

import java.net.URL
import java.net.URLClassLoader
import java.util.*

object ClassTweakLoader : TweakLoader<URL> {
    override fun load(from: URL) = load(listOf(from))

    override fun load(from: List<URL>): List<TweakRegistrar> {
        val cl = URLClassLoader(javaClass.simpleName, from.toTypedArray(), null)
        return ServiceLoader.load(TweakRegistrar::class.java, cl).toList()
    }
}
