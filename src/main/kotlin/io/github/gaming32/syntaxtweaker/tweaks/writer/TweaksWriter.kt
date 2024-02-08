package io.github.gaming32.syntaxtweaker.tweaks.writer

import io.github.gaming32.syntaxtweaker.tweaks.TweakSet
import org.jetbrains.kotlin.js.parser.sourcemaps.JsonString
import java.io.Writer
import java.nio.file.Path
import kotlin.io.path.writeLines

object TweaksWriter {
    fun writeToString(tweaks: TweakSet) = write(tweaks).joinToString("") { "$it\n" }

    fun writeTo(tweaks: TweakSet, path: Path) {
        path.writeLines(write(tweaks))
    }

    fun writeTo(tweaks: TweakSet, writer: Writer) = write(tweaks).forEach { writer.write("$it\n") }

    fun write(tweaks: TweakSet): Sequence<String> = sequence {
        for ((key, value) in tweaks.metadata) {
            line(key, value)
        }
        for ((pkg, packageTweaks) in tweaks.packages) {
            line("package", pkg)
            for (tweak in packageTweaks) {
                line("", tweak.id ?: continue, *tweak.serialize().toTypedArray())
            }
        }
        for ((clazz, classTweaks) in tweaks.classes) {
            line("class", clazz)
            for (tweak in classTweaks.classTweaks) {
                line("", tweak.id ?: continue, *tweak.serialize().toTypedArray())
            }
            for ((member, memberTweaks) in classTweaks.memberTweaks) {
                line("", "member", member.name, member.type.toString())
                for (tweak in memberTweaks) {
                    line("", "", tweak.id ?: continue, *tweak.serialize().toTypedArray())
                }
            }
        }
    }

    private suspend fun SequenceScope<String>.line(vararg args: String) {
        yield(args.joinToString("\t") { escape(it) })
    }

    private fun escape(s: String): String {
        val escaped = JsonString(s).toString()
        if ('\\' !in escaped) {
            return s
        }
        return escaped
    }
}