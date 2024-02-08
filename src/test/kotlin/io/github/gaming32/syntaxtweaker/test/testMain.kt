package io.github.gaming32.syntaxtweaker.test

import io.github.gaming32.syntaxtweaker.SyntaxTweaker
import io.github.gaming32.syntaxtweaker.data.MemberReference
import io.github.gaming32.syntaxtweaker.tweaks.ClassTweaks
import io.github.gaming32.syntaxtweaker.tweaks.TweakSet
import io.github.gaming32.syntaxtweaker.tweaks.builtin.NumberBaseTweak
import io.github.gaming32.syntaxtweaker.tweaks.parser.TweaksParser
import io.github.gaming32.syntaxtweaker.tweaks.writer.TweaksWriter
import java.io.File
import kotlin.test.assertEquals

const val TWEAK_DATA = """
flag;
metadata = value;

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

fun main() {
    TweaksWriter.writeTo(TweaksParser.parse(TWEAK_DATA), System.out)
}

fun mainNormal() {
    val tweaks = TweaksParser.parse(TWEAK_DATA)
    assertEquals(TWEAK_DATA.trimStart(), TweaksWriter.writeToString(tweaks))

    val testClass = tweaks.classes.keys.first()
    SyntaxTweaker(tweaks).tweak(listOf(
        File("src/test/java/${testClass.replace('.', '/')}.java")
    )) { _, _, newBody ->
        println(newBody)
    }
}
