package io.github.gaming32.syntaxtweaker.tweaks.writer

import io.github.gaming32.syntaxtweaker.data.Type
import io.github.gaming32.syntaxtweaker.tweaks.SyntaxTweak
import io.github.gaming32.syntaxtweaker.tweaks.TweakSet
import io.github.gaming32.syntaxtweaker.tweaks.parser.SimpleTokenType
import io.github.gaming32.syntaxtweaker.util.asWriter
import org.jetbrains.kotlin.js.parser.sourcemaps.JsonString
import java.nio.file.Path
import java.util.*
import kotlin.io.path.bufferedWriter

object TweaksWriter {
    private val needsQuoting = BitSet(127)

    init {
        SimpleTokenType.entries.forEach { needsQuoting.set(it.char.code) }
        for (char in "'\"/") {
            needsQuoting.set(char.code)
        }
    }

    fun writeToString(tweaks: TweakSet, indent: String? = "    ") =
        buildString { writeTo(tweaks, this, indent) }

    fun writeTo(tweaks: TweakSet, path: Path, indent: String? = "    ") =
        path.bufferedWriter().use { writeTo(tweaks, it, indent) }

    fun writeTo(tweaks: TweakSet, writer: Appendable, indent: String? = "    ") {
        require(indent?.isBlank() != false) { "Indent ${JsonString(indent!!)} is not blank" }
        var wroteSomething = false

        for ((key, value) in tweaks.metadata) {
            wroteSomething = true
            writer.writeString(key)
            if (value.isNotEmpty()) {
                if (indent != null) {
                    writer.append(" = ")
                } else {
                    writer.append('=')
                }
                writer.writeString(value)
            }
            if (indent != null) {
                writer.appendLine(';')
            } else {
                writer.append(';')
            }
        }

        for ((pkg, packageTweaks) in tweaks.packages) {
            if (wroteSomething && indent != null) {
                writer.appendLine()
            }
            wroteSomething = true
            writer.append("package ").writeString(pkg)
            if (indent != null) {
                writer.appendLine(" {")
            } else {
                writer.append('{')
            }
            for (tweak in packageTweaks) {
                writer.writeTweak(tweak, indent)
            }
            if (indent != null) {
                writer.appendLine('}')
            } else {
                writer.append('}')
            }
        }

        val deepIndent = indent?.repeat(2)
        for ((clazz, classTweaks) in tweaks.classes) {
            if (wroteSomething) {
                writer.appendLine()
            }
            wroteSomething = true
            writer.append("class ").writeString(clazz)
            if (indent != null) {
                writer.appendLine(" {")
            } else {
                writer.append('{')
            }
            var wroteSomethingInner = false
            for (tweak in classTweaks.classTweaks) {
                wroteSomethingInner = true
                writer.writeTweak(tweak, indent)
            }
            for ((member, memberTweaks) in classTweaks.memberTweaks) {
                if (wroteSomethingInner && indent != null) {
                    writer.appendLine()
                }
                wroteSomethingInner = true
                if (indent != null) {
                    writer.append(indent)
                }
                writer.append("member ")
                val type = member.type
                if (type is Type.MethodType) {
                    if (type.returnType != null) {
                        writer.writeString(type.returnType).append(' ')
                    }
                    writer.writeString(member.name).append('(')
                    type.parameters.forEachIndexed { index, param ->
                        if (index > 0) {
                            if (indent != null) {
                                writer.append(", ")
                            } else {
                                writer.append(',')
                            }
                        }
                        writer.writeString(param)
                    }
                    writer.append(')')
                } else {
                    writer.writeString(type).append(' ').writeString(member.name)
                }
                if (indent != null) {
                    writer.appendLine(" {")
                } else {
                    writer.append('{')
                }
                for (tweak in memberTweaks) {
                    writer.writeTweak(tweak, deepIndent)
                }
                if (indent != null) {
                    writer.append(indent).appendLine('}')
                } else {
                    writer.append('}')
                }
            }
            if (indent != null) {
                writer.appendLine('}')
            } else {
                writer.append('}')
            }
        }
    }

    private fun Appendable.writeTweak(tweak: SyntaxTweak, indent: String?): Appendable {
        val id = tweak.id ?: return this
        if (indent != null) {
            append(indent)
        }
        writeString(id)
        for (arg in tweak.serializeArgs()) {
            append(' ').writeString(arg)
        }
        for ((key, value) in tweak.serializeNamedArgs()) {
            append(' ').writeString(key).append('=').writeString(value)
        }
        return if (indent != null) {
            appendLine(';')
        } else {
            append(';')
        }
    }

    private fun Appendable.writeString(s: Any?) = writeString(s.toString())

    private fun Appendable.writeString(s: String) =
        if (needsQuoting(s)) {
            JsonString(s).write(asWriter())
            this
        } else {
            append(s)
        }

    private fun needsQuoting(s: String): Boolean {
        for (c in s) {
            if (c !in ' '..<127.toChar() || needsQuoting[c.code] || c.isWhitespace()) {
                return true
            }
        }
        return false
    }
}
