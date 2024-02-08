package io.github.gaming32.syntaxtweaker.tweaks.registry.loader

import io.github.gaming32.syntaxtweaker.tweaks.registry.TweakRegistry

fun interface TweakRegistrar {
    companion object {
        fun Collection<TweakRegistrar>.register(registry: TweakRegistry) = forEach { it.register(registry) }
    }

    fun register(registry: TweakRegistry)
}
