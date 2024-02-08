package io.github.gaming32.syntaxtweaker.tweaks.parser

import io.github.gaming32.syntaxtweaker.data.MemberReference
import io.github.gaming32.syntaxtweaker.data.Type
import io.github.gaming32.syntaxtweaker.data.Type.Companion.toSyntaxTweakerType
import io.github.gaming32.syntaxtweaker.tweaks.ClassTweaks
import io.github.gaming32.syntaxtweaker.tweaks.SyntaxTweak
import io.github.gaming32.syntaxtweaker.tweaks.TweakList
import io.github.gaming32.syntaxtweaker.tweaks.TweakSet
import io.github.gaming32.syntaxtweaker.util.plus
import org.jetbrains.kotlin.js.parser.sourcemaps.JsonString
import org.jetbrains.kotlin.js.parser.sourcemaps.JsonSyntaxException
import org.jetbrains.kotlin.js.parser.sourcemaps.parseJson
import java.nio.file.Path
import kotlin.io.path.useLines

private typealias LinesParser = Iterator<IndexedValue<Pair<String, String>>>

object TweaksParser {
    fun parse(path: Path, registry: TweakRegistry = TweakRegistry.DEFAULT) = path.useLines { parse(it, registry) }

    fun parse(tweaks: String, registry: TweakRegistry = TweakRegistry.DEFAULT) = parse(tweaks.lineSequence(), registry)

    fun parse(lines: Sequence<String>, registry: TweakRegistry = TweakRegistry.DEFAULT): TweakSet {
        val packages = mutableMapOf<String, TweakList>()
        val classes = mutableMapOf<String, ClassTweaks>()
        val metadata = mutableMapOf<String, String>()
        val iterator: LinesParser = (lines + "").zipWithNext().withIndex().iterator()
        while (iterator.hasNext()) {
            val (lineNo, linePair) = iterator.next()
            val (line, nextLine) = linePair
            if (line.startsWith("#") || line.isBlank()) continue
            val parts = parseParts(line.split("\t"), lineNo)
            when (parts[0]) {
                "package" -> {
                    val name = parts.getOrNull(1)
                        ?: throw InvalidTweaksException("Missing package name on line ${lineNo + 1}")
                    if (!nextLine.startsWith("\t")) continue
                    packages[name] += parseBasic(
                        iterator, "\t", registry, TweakParser.ParseContext(
                            metadata, SyntaxTweak.ReferenceType.PACKAGE, name, null
                        )
                    ).first
                }
                "class" -> {
                    val name = parts.getOrNull(1)
                        ?: throw InvalidTweaksException("Missing class name on line ${lineNo + 1}")
                    if (!nextLine.startsWith("\t")) continue
                    classes[name] += parseClass(
                        iterator, registry, TweakParser.ParseContext(
                            metadata, SyntaxTweak.ReferenceType.CLASS, name, null
                        )
                    )
                }
                else -> {
                    val key = parts.getOrNull(1)
                        ?: throw InvalidTweaksException("Missing metadata key on line ${lineNo + 1}")
                    val value = parts.getOrNull(2)
                        ?: throw InvalidTweaksException("Missing metadata value on line ${lineNo + 1}")
                    metadata[key] = value
                }
            }
        }
        return TweakSet(packages, classes, metadata)
    }

    private fun parseClass(
        iterator: LinesParser, registry: TweakRegistry, context: TweakParser.ParseContext
    ): ClassTweaks {
        val classTweaks = mutableListOf<SyntaxTweak>()
        val memberTweaks = mutableMapOf<MemberReference, TweakList>()
        while (iterator.hasNext()) {
            val (lineNo, linePair) = iterator.next()
            val (line, nextLine) = linePair
            if (!nextLine.startsWith("\t")) break
            val parts = parseParts(line.substring(1).split("\t"), lineNo)
            when (parts[0]) {
                "member" -> {
                    val name = parts.getOrNull(1)
                        ?: throw InvalidTweaksException("Missing member name on line ${lineNo + 1}")
                    val typeStr = parts.getOrNull(2)
                        ?: throw InvalidTweaksException("Missing member type on line ${lineNo + 1}")
                    val type = try {
                        typeStr.toSyntaxTweakerType()
                    } catch (e: IllegalArgumentException) {
                        throw InvalidTweaksException("Invalid member type on line ${lineNo + 1}", e)
                    }
                    val member = MemberReference(name, type)
                    if (!nextLine.startsWith("\t\t")) continue
                    val (membersTweaks, innerNextLine) = parseBasic(
                        iterator, "\t\t", registry, context.copy(
                            referenceType = if (member.type is Type.MethodType) {
                                SyntaxTweak.ReferenceType.METHOD
                            } else {
                                SyntaxTweak.ReferenceType.FIELD
                            },
                            member = member
                        )
                    )
                    memberTweaks[member] += membersTweaks
                    if (!innerNextLine.startsWith("\t")) break
                }
                else -> parseTweak(parts, lineNo, registry, context)?.let(classTweaks::add)
            }
        }
        return ClassTweaks(context.owner, classTweaks, memberTweaks)
    }

    private fun parseBasic(
        iterator: LinesParser, indent: String, registry: TweakRegistry, context: TweakParser.ParseContext
    ): Pair<TweakList, String> {
        val result = mutableListOf<SyntaxTweak>()
        var returnNextLine = ""
        while (iterator.hasNext()) {
            val (lineNo, linePair) = iterator.next()
            val (line, nextLine) = linePair
            returnNextLine = nextLine
            val parts = parseParts(line.substring(indent.length).split("\t"), lineNo)
            parseTweak(parts, lineNo, registry, context)?.let(result::add)
            if (!nextLine.startsWith(indent)) break
        }
        return Pair(result, returnNextLine)
    }

    private fun parseTweak(
        parts: List<String>, lineNo: Int, registry: TweakRegistry, context: TweakParser.ParseContext
    ): SyntaxTweak? {
        try {
            return registry.parseLine(context, parts)
        } catch (e: IllegalArgumentException) {
            throw InvalidTweaksException("Invalid tweak on line ${lineNo + 1}", e)
        }
    }

    private fun parseParts(parts: List<String>, lineNo: Int): List<String> {
        var result: MutableList<String>? = null
        for (i in parts.indices) {
            val part = parts[i]
            if (!part.startsWith('"')) continue
            if (result == null) {
                result = parts.toMutableList()
            }
            try {
                result[i] = (parseJson(part) as JsonString).value
            } catch (e: JsonSyntaxException) {
                throw InvalidTweaksException("Invalid part at index $i on line ${lineNo + 1}", e)
            }
        }
        return result ?: parts
    }
}
