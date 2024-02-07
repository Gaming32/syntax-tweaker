package io.github.gaming32.syntaxtweaker.test

import io.github.gaming32.syntaxtweaker.SyntaxTweaker
import io.github.gaming32.syntaxtweaker.data.MemberReference
import io.github.gaming32.syntaxtweaker.tweaks.ClassTweaks
import io.github.gaming32.syntaxtweaker.tweaks.TweakSet
import io.github.gaming32.syntaxtweaker.tweaks.builtin.NumberBaseTweak
import java.io.File

fun main() {
    val testClass = "io.github.gaming32.syntaxtweaker.test.TestClass"
    val shouldBeHex = MemberReference("shouldBeHex", "I")
    val shouldBeOctal = MemberReference("shouldBeOctal", "(I)V")
    val tweaks = TweakSet(mapOf(
        testClass to ClassTweaks(
            testClass,
            listOf(),
            mapOf(
                shouldBeHex to listOf(
                    NumberBaseTweak(NumberBaseTweak.NumberBase.HEX, shouldBeHex)
                ),
                shouldBeOctal to listOf(
                    NumberBaseTweak(NumberBaseTweak.NumberBase.OCT, shouldBeOctal, 0)
                )
            )
        )
    ))

    SyntaxTweaker(tweaks).tweak(listOf(
        File("src/test/java/${testClass.replace('.', '/')}.java")
    )) { _, _, newBody ->
        println(newBody)
    }
}
