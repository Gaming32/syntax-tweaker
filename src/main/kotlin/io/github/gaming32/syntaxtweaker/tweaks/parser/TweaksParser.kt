package io.github.gaming32.syntaxtweaker.tweaks.parser

import io.github.gaming32.syntaxtweaker.data.MemberReference
import io.github.gaming32.syntaxtweaker.data.Type
import io.github.gaming32.syntaxtweaker.data.Type.Companion.toSyntaxTweakerType
import io.github.gaming32.syntaxtweaker.tweaks.ClassTweaks
import io.github.gaming32.syntaxtweaker.tweaks.SyntaxTweak
import io.github.gaming32.syntaxtweaker.tweaks.TweakList
import io.github.gaming32.syntaxtweaker.tweaks.TweakSet
import io.github.gaming32.syntaxtweaker.tweaks.registry.TweakParser
import io.github.gaming32.syntaxtweaker.tweaks.registry.TweakRegistry
import io.github.gaming32.syntaxtweaker.util.plus
import java.io.Reader
import java.io.StringReader
import java.nio.file.Path
import kotlin.io.path.bufferedReader

object TweaksParser {
    fun parse(path: Path, registry: TweakRegistry = TweakRegistry.DEFAULT) =
        path.bufferedReader().use { parse(it, registry) }

    fun parse(tweaks: String, registry: TweakRegistry = TweakRegistry.DEFAULT) =
        parse(StringReader(tweaks), registry)

    fun parse(reader: Reader, registry: TweakRegistry = TweakRegistry.DEFAULT): TweakSet {
        val packages = mutableMapOf<String, TweakList>()
        val classes = mutableMapOf<String, ClassTweaks>()
        val metadata = mutableMapOf<String, String>()
        val tokens = Tokenizer(reader).tokenize().iterator()
        while (tokens.hasNext()) {
            var token = tokens.next()
            if (token !is StringToken) {
                throw InvalidTweaksException("Expected string/literal, found ${token.withPosition()}", token)
            }
            if (token.value == "package") {
                val packageName = tokens.expectString("package name").value
                tokens.expect(SimpleTokenType.LCURLY, "package name")
                packages[packageName] += parseTweakList(
                    tokens, registry, TweakParser.ParseContext(
                        metadata, SyntaxTweak.ReferenceType.PACKAGE, packageName, null
                    )
                )
                continue
            }
            if (token.value == "class") {
                val clazz = parseClass(tokens, registry, metadata)
                classes[clazz.className] += clazz
                continue
            }
            val metadataKey = token.value
            token = tokens.expect()
            if (token !is SimpleToken) {
                throw InvalidTweaksException("Expected = or ; after metadata key, found ${token.withPosition()}", token)
            }
            when (token.type) {
                SimpleTokenType.SEMI -> metadata[metadataKey] = ""
                SimpleTokenType.EQUALS -> {
                    metadata[metadataKey] = tokens.expectString("metadata value").value
                    tokens.expect(SimpleTokenType.SEMI, "metadata value")
                }
                else -> throw InvalidTweaksException("Expected = or ; after metadata key, found ${token.withPosition()}", token)
            }
        }
        return TweakSet(packages, classes, metadata)
    }

    private fun parseClass(
        tokens: Iterator<Token>, registry: TweakRegistry, metadata: Map<String, String>
    ): ClassTweaks {
        val className = tokens.expectString("class name").value
        val context = TweakParser.ParseContext(metadata, SyntaxTweak.ReferenceType.CLASS, className, null)
        val classTweaks = mutableListOf<SyntaxTweak>()
        val memberTweaks = mutableMapOf<MemberReference, TweakList>()
        tokens.expect(SimpleTokenType.LCURLY, "class name")
        while (true) {
            val startToken = tokens.expect()
            if (startToken is SimpleToken) {
                if (startToken.type != SimpleTokenType.RCURLY) {
                    throw InvalidTweaksException("Expected } to end class declaration, found ${startToken.withPosition()}", startToken)
                }
                break
            }
            startToken as StringToken
            if (startToken.value == "member") {
                val (member, tweaks) = parseMember(tokens, registry, context)
                memberTweaks[member] += tweaks
                continue
            }
            parseTweak(tokens, startToken, registry, context)?.let(classTweaks::add)
        }
        return ClassTweaks(className, classTweaks, memberTweaks)
    }

    private fun parseMember(
        tokens: Iterator<Token>, registry: TweakRegistry, context: TweakParser.ParseContext
    ): Pair<MemberReference, TweakList> {
        val returnOrName = tokens.expectString("member type or name")
        var nextToken = tokens.expect()
        val (returnType, name) = if (nextToken is StringToken) {
            val name = nextToken.value
            nextToken = tokens.expect()
            Pair(parseType(returnOrName), name)
        } else {
            Pair(null, returnOrName.value)
        }
        if (nextToken !is SimpleToken) {
            throw InvalidTweaksException("Expected ( or { after member name, found ${nextToken.withPosition()}", nextToken)
        }
        val memberType = if (nextToken.type == SimpleTokenType.LPAREN) {
            if (returnType == null && name != context.owner.substringAfterLast('.')) {
                throw InvalidTweaksException("Constructor name must match class name, found ${returnOrName.withPosition()}", returnOrName)
            }
            val params = mutableListOf<Type>()
            while (true) {
                nextToken = tokens.expect()
                if (nextToken is SimpleToken) {
                    if (nextToken.type != SimpleTokenType.RPAREN) {
                        throw InvalidTweaksException("Expected ) after method parameters, found ${nextToken.withPosition()}", nextToken)
                    }
                    break
                }
                try {
                    params.add((nextToken as StringToken).value.toSyntaxTweakerType())
                } catch (e: IllegalArgumentException) {
                    throw InvalidTweaksException("Invalid parameter type ${nextToken.withPosition()}: ${e.localizedMessage}", nextToken, e)
                }
            }
            nextToken = tokens.expect()
            try {
                Type.MethodType(returnType, params)
            } catch (e: IllegalArgumentException) {
                throw InvalidTweaksException("Invalid method type ${returnOrName.withPosition()}: ${e.localizedMessage}", returnOrName, e)
            }
        } else {
            returnType ?: throw InvalidTweaksException("Missing field type at ${returnOrName.positionString}", returnOrName)
        }
        if (nextToken !is SimpleToken || nextToken.type != SimpleTokenType.LCURLY) {
            throw InvalidTweaksException("Expected { after member header, found ${nextToken.withPosition()}", nextToken)
        }
        val member = MemberReference(name, memberType)
        return Pair(member, parseTweakList(tokens, registry, context.copy(referenceType = member.referenceType, member = member)))
    }

    private fun parseTweakList(
        tokens: Iterator<Token>, registry: TweakRegistry, context: TweakParser.ParseContext
    ): TweakList {
        val result = mutableListOf<SyntaxTweak>()
        while (true) {
            val startToken = tokens.expect()
            if (startToken is SimpleToken) {
                if (startToken.type != SimpleTokenType.RCURLY) {
                    throw InvalidTweaksException("Expected } to end tweak list, found ${startToken.withPosition()}", startToken)
                }
                break
            }
            parseTweak(tokens, startToken as StringToken, registry, context)?.let(result::add)
        }
        return result
    }

    private fun parseTweak(
        tokens: Iterator<Token>, startToken: StringToken, registry: TweakRegistry, context: TweakParser.ParseContext
    ): SyntaxTweak? {
        val args = mutableListOf(startToken.value)
        val namedArgs = mutableMapOf<String, String>()
        var hasPutInNamed = true // Prevent startToken from becoming a named arg
        var lastKey = startToken
        while (true) {
            val token = tokens.expect()
            if (token is SimpleToken) {
                if (token.type == SimpleTokenType.EQUALS && !hasPutInNamed) {
                    val value = tokens.expectString("named arg value")
                    val key = args.removeLast()
                    val oldValue = namedArgs.putIfAbsent(key, value.value)
                    if (oldValue != null) {
                        throw InvalidTweaksException("Duplicate named arg ${lastKey.withPosition()}", lastKey)
                    }
                    hasPutInNamed = true
                    continue
                }
                if (token.type != SimpleTokenType.SEMI) {
                    throw InvalidTweaksException("Expected ; to end tweak args, found ${token.withPosition()}", token)
                }
                break
            }
            lastKey = token as StringToken
            args.add(token.value)
            hasPutInNamed = false
        }
        val parsed = try {
            registry.parse(context, args, namedArgs)
        } catch (e: IllegalArgumentException) {
            throw InvalidTweaksException("Invalid tweak ${args[0]} at ${startToken.positionString}: ${e.localizedMessage}", startToken, e)
        }
        if (parsed == null && "skip-unknown" !in context.metadata) {
            throw InvalidTweaksException(
                "Unknown tweak ${startToken.withPosition()}. Use skip-unknown; at the top of the file to skip it.",
                startToken
            )
        }
        return parsed
    }

    private fun parseType(token: StringToken): Type {
        try {
            return token.value.toSyntaxTweakerType()
        } catch (e: IllegalArgumentException) {
            throw InvalidTweaksException("Invalid type ${token.withPosition()}: ${e.localizedMessage}", token, e)
        }
    }

    private fun Iterator<Token>.expectString(what: String = "string/literal"): StringToken {
        val token = expect()
        return token as? StringToken
            ?: throw InvalidTweaksException("Expected $what, found ${token.withPosition()}", token)
    }

    private fun Iterator<Token>.expect(type: SimpleTokenType, after: String): SimpleToken {
        val token = expect()
        if ((token as? SimpleToken)?.type != type) {
            throw InvalidTweaksException("Expected $type after $after, found ${token.withPosition()}", token)
        }
        return token
    }

    private fun Iterator<Token>.expect() =
        if (hasNext()) next() else throw InvalidTweaksException("Unexpected EOF in tweaks file")

    private fun Token.withPosition() = "$this at $positionString"

    private val Token.positionString get() = "$line:$column"
}
