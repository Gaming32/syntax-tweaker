package io.github.gaming32.syntaxtweaker.tweaks

import io.github.gaming32.syntaxtweaker.tweaks.builtin.NumberBaseTweak

class TweakRegistry {
    companion object {
        val DEFAULT = TweakRegistry().apply { registerDefaults() }

        fun TweakRegistry.registerDefaults() {
            register(NumberBaseTweak.ID, NumberBaseTweak)
        }
    }

    private val registry = mutableMapOf<String, TweakParser<*>>()

    fun register(key: String, parser: TweakParser<*>, replace: Boolean = false) {
        if (replace) {
            registry[key] = parser
        } else if (registry.putIfAbsent(key, parser) != null) {
            throw IllegalArgumentException("Attempted re-register of \"$key\"")
        }
    }

    operator fun get(key: String) = registry[key]

    val keys: Set<String> get() = registry.keys

    fun parseLine(context: ParseContext, line: List<String>): SyntaxTweak? {
        if (line.isEmpty()) {
            throw IllegalArgumentException("Tweak line cannot be empty")
        }
        return with(registry[line[0]] ?: return null) {
            context.copy(args = line.subList(1, line.size)).parse()
        }
    }
}
