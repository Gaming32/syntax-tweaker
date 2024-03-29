package io.github.gaming32.syntaxtweaker.tweaks.registry

import io.github.gaming32.syntaxtweaker.tweaks.SyntaxTweak
import io.github.gaming32.syntaxtweaker.tweaks.builtin.NumberBaseTweak

class TweakRegistry {
    companion object {
        val RESERVED_KEYS = setOf("member")
        val DEFAULT = TweakRegistry().apply { registerDefaults() }

        fun TweakRegistry.registerDefaults() {
            register(NumberBaseTweak.ID, NumberBaseTweak)
        }
    }

    private val registry = mutableMapOf<String, TweakParser<*>>()
    var defaultReplace = false

    fun register(key: String, parser: TweakParser<*>, replace: Boolean = defaultReplace) {
        if (key in RESERVED_KEYS) {
            throw IllegalArgumentException("Reserved tweak key: $key")
        }
        if (replace) {
            registry[key] = parser
        } else if (registry.putIfAbsent(key, parser) != null) {
            throw IllegalArgumentException("Attempted re-register of \"$key\"")
        }
    }

    operator fun get(key: String) = registry[key]

    val keys: Set<String> get() = registry.keys

    fun parse(context: TweakParser.ParseContext, args: List<String>, namedArgs: Map<String, String>): SyntaxTweak? {
        if (args.isEmpty()) {
            throw IllegalArgumentException("Tweak line cannot be empty")
        }
        val parsed = with(registry[args[0]] ?: return null) {
            context.copy(args = args.subList(1, args.size), namedArgs = namedArgs).parse()
        }
        if (context.referenceType !in parsed.supportedReferenceTypes) {
            throw IllegalArgumentException(
                "Reference type '${context.referenceType}' not supported by ${args[0]}. " +
                    "The following reference types are supported: ${parsed.supportedReferenceTypes.joinToString()}"
            )
        }
        return parsed
    }

    fun copy() = TweakRegistry().also {
        it.registry.putAll(registry)
        it.defaultReplace = defaultReplace
    }
}
