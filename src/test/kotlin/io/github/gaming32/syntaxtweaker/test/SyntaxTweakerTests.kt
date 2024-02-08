package io.github.gaming32.syntaxtweaker.test

import io.github.gaming32.syntaxtweaker.SyntaxTweaker
import io.github.gaming32.syntaxtweaker.tweaks.parser.InvalidTweaksException
import io.github.gaming32.syntaxtweaker.tweaks.parser.TweaksParser
import io.github.gaming32.syntaxtweaker.tweaks.registry.TweakRegistry
import io.github.gaming32.syntaxtweaker.tweaks.registry.loader.TweakRegistrar.Companion.register
import io.github.gaming32.syntaxtweaker.tweaks.registry.loader.script.ScriptTweakLoader
import io.github.gaming32.syntaxtweaker.tweaks.writer.TweaksWriter
import org.junit.jupiter.api.assertThrows
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class SyntaxTweakerTests {
    @Test
    fun testReadWrite() {
        val tweaksData = loadResource("/standard.tweaks")
        val tweaks = TweaksParser.parse(tweaksData)
        assertEquals(tweaksData, TweaksWriter.writeToString(tweaks))
    }

    @Test
    fun testMinify() {
        val minTweaksData = loadResource("/standard.min.tweaks")
        val tweaks = TweaksParser.parse(loadResource("/standard.tweaks"))
        assertEquals(minTweaksData, TweaksWriter.writeToString(tweaks, indent = null))
    }

    @Test
    fun testApply() {
        val tweaks = TweaksParser.parse(loadResource("/standard.tweaks"))
        val originalSource = loadResource("TestClass.java")
        val expectedSource = loadResource("TestClass.processed.java")
        val originalSourceFile = File.createTempFile("TestClass.", ".java")
        originalSourceFile.writeText(originalSource)
        originalSourceFile.deleteOnExit()
        SyntaxTweaker(tweaks).tweak(listOf(originalSourceFile)) { _, _, newBody ->
            assertEquals(expectedSource, newBody)
        }
    }

    @Test
    fun testCustomTweaker() {
        val registry = TweakRegistry.DEFAULT.copy()
        ScriptTweakLoader.load(loadResource("/test.tweaker.kts")).register(registry)
        val exception = assertThrows<InvalidTweaksException> {
            TweaksParser.parse(loadResource("/test-tweaker.tweaks"), registry)
        }
        assertEquals(
            "Invalid tweak script-test at 2:5: Reference type 'class' not supported by script-test. The following reference types are supported: ",
            exception.message
        )
    }

    private fun loadResource(path: String) =
        javaClass.getResource(path)!!
            .readText()
            .replace(System.lineSeparator(), "\n")
}
