package io.github.gaming32.syntaxtweaker.tweaks.registry.loader

fun interface TweakLoader<P> {
    fun load(from: P): List<TweakRegistrar>

    fun load(from: List<P>): List<TweakRegistrar> = from.asSequence().map(::load).flatten().toList()
}
