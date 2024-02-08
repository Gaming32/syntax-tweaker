package io.github.gaming32.syntaxtweaker.test

import io.github.gaming32.syntaxtweaker.SyntaxTweaker
import io.github.gaming32.syntaxtweaker.tweaks.parser.TweaksParser
import io.github.gaming32.syntaxtweaker.tweaks.writer.TweaksWriter
import java.io.File
import kotlin.test.assertEquals

const val TWEAK_DATA = """
class	io.github.gaming32.syntaxtweaker.test.TestClass
	member	shouldBeHex	int
		number-base	hex	true
	member	shouldBeOctal	void(int)
		number-base	oct	true	0
"""

fun main() {
    val tweaks = TweaksParser.parse(TWEAK_DATA)
    val testClass = tweaks.classes.keys.first()

    assertEquals(TWEAK_DATA.trimStart(), TweaksWriter.writeToString(tweaks))

    SyntaxTweaker(tweaks).tweak(listOf(
        File("src/test/java/${testClass.replace('.', '/')}.java")
    )) { _, _, newBody ->
        println(newBody)
    }
}
