package io.github.gaming32.syntaxtweaker.test

import io.github.gaming32.syntaxtweaker.SyntaxTweaker
import io.github.gaming32.syntaxtweaker.data.MemberReference
import io.github.gaming32.syntaxtweaker.tweaks.ClassTweaks
import io.github.gaming32.syntaxtweaker.tweaks.TweakSet
import io.github.gaming32.syntaxtweaker.tweaks.builtin.NumberBaseTweak
import io.github.gaming32.syntaxtweaker.tweaks.parser.TweaksParser
import io.github.gaming32.syntaxtweaker.tweaks.registry.TweakRegistry
import io.github.gaming32.syntaxtweaker.tweaks.registry.loader.TweakRegistrar.Companion.register
import io.github.gaming32.syntaxtweaker.tweaks.registry.loader.script.ScriptTweakLoader
import io.github.gaming32.syntaxtweaker.tweaks.writer.TweaksWriter
import java.io.File
import kotlin.test.assertEquals

const val TWEAK_DATA = """
class io.github.gaming32.syntaxtweaker.test.TestClass {
    member int shouldBeHex {
        number-base hex true;
    }

    member void shouldBeOctal(int) {
        number-base oct true 0;
    }

    member TestClass() {
    }
}
"""

fun mainWriter() {
    val testClass = "io.github.gaming32.syntaxtweaker.test.TestClass"
    val shouldBeHex = MemberReference("shouldBeHex", "int")
    val shouldBeOctal = MemberReference("shouldBeOctal", "void(int)")
    val tweaks = TweakSet(
        mapOf(),
        mapOf(
            testClass to ClassTweaks(
                testClass,
                listOf(),
                mapOf(
                    shouldBeHex to listOf(
                        NumberBaseTweak(shouldBeHex, NumberBaseTweak.NumberBase.HEX, true)
                    ),
                    shouldBeOctal to listOf(
                        NumberBaseTweak(shouldBeOctal, NumberBaseTweak.NumberBase.OCT, true, 0)
                    )
                )
            )
        )
    )
    TweaksWriter.writeTo(tweaks, System.out, indent = null)
    println()
}

fun mainReader() {
    TweaksWriter.writeTo(TweaksParser.parse(TWEAK_DATA), System.out)
}

fun mainOriginal() {
    val tweaks = TweaksParser.parse(TWEAK_DATA)
    assertEquals(TWEAK_DATA.trimStart(), TweaksWriter.writeToString(tweaks))

    val testClass = tweaks.classes.keys.first()
    SyntaxTweaker(tweaks).tweak(listOf(
        File("src/test/java/${testClass.replace('.', '/')}.java")
    )) { _, _, newBody ->
        println(newBody)
    }
}

fun main() {
    val registry = TweakRegistry.DEFAULT.copy()
    ScriptTweakLoader.load("""
        object TestTweak : SyntaxTweak, TweakParser<TestTweak> {
            const val ID = "script-test"
            
            override val id get() = ID
            
            override val supportedReferenceTypes = emptyEnumSet<SyntaxTweak.ReferenceType>()
            
            override fun TweakParser.ParseContext.parse() = this@TestTweak
        }
        
        register(TestTweak.ID, TestTweak)
    """.trimIndent()).register(registry)
    TweaksParser.parse("""
        class io.github.gaming32.syntaxtweaker.test.TestClass {
            script-test;
        }
    """.trimIndent(), registry)
}
