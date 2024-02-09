package io.github.gaming32.syntaxtweaker.cli

import io.github.gaming32.syntaxtweaker.SyntaxTweaker
import io.github.gaming32.syntaxtweaker.tweaks.TweakSet
import io.github.gaming32.syntaxtweaker.tweaks.parser.InvalidTweaksException
import io.github.gaming32.syntaxtweaker.tweaks.parser.TweaksParser
import io.github.gaming32.syntaxtweaker.tweaks.registry.TweakRegistry
import io.github.gaming32.syntaxtweaker.tweaks.registry.loader.ClassTweakLoader
import io.github.gaming32.syntaxtweaker.tweaks.registry.loader.ScriptTweakLoader
import io.github.gaming32.syntaxtweaker.tweaks.registry.loader.TweakRegistrar
import io.github.gaming32.syntaxtweaker.tweaks.registry.loader.TweakRegistrar.Companion.register
import io.github.gaming32.syntaxtweaker.util.plus
import net.sourceforge.argparse4j.ArgumentParsers
import net.sourceforge.argparse4j.annotation.Arg
import net.sourceforge.argparse4j.ext.java7.PathArgumentType
import net.sourceforge.argparse4j.impl.Arguments
import net.sourceforge.argparse4j.impl.type.FileArgumentType
import net.sourceforge.argparse4j.inf.ArgumentParserException
import org.jetbrains.kotlin.incremental.deleteDirectoryContents
import org.jetbrains.kotlin.incremental.isJavaFile
import java.io.File
import java.net.URL
import java.nio.file.Path
import kotlin.system.exitProcess

val tweakerTypes: Map<String, (URL) -> List<TweakRegistrar>> = mapOf(
    ".jar" to ClassTweakLoader::load,
    ".tweaker.kts" to ScriptTweakLoader::load,
)

fun main(vararg args: String) {
    val parser = ArgumentParsers.newFor("syntax-tweaker")
        .fromFilePrefix("@")
        .build()
        .defaultHelp(true)
    parser.addArgument("-T", "--tweakers")
        .help("Additional tweakers (.jar or .tweaker.kts files)")
        .type(PathArgumentType().verifyExists())
        .action(Arguments.append())
        .setDefault(mutableListOf<File>())
    parser.addArgument("-t", "--tweaks")
        .help(".tweaks files to use")
        .type(PathArgumentType().verifyExists().verifyIsFile())
        .action(Arguments.append())
        .required(true)
    parser.addArgument("-m", "--multiple-tweakers")
        .help("Allow tweakers to override each other. Last specified tweaker will take priority.")
        .action(Arguments.storeTrue())
    parser.addArgument("-s", "--skip-unmodified")
        .help("Skip unmodified files")
        .action(Arguments.storeTrue())
    parser.addArgument("destination")
        .help("Destination directory for processed sources")
        .type(FileArgumentType())
    parser.addArgument("sources")
        .help("Source files or directories to tweak")
        .type(FileArgumentType().verifyExists().verifyIsDirectory())
        .nargs("+")

    val parsedArgs = object {
        @field:Arg
        var tweakers = listOf<Path>()

        @field:Arg
        lateinit var tweaks: List<Path>

        @field:Arg(dest = "multiple_tweakers")
        var multipleTweakers = false

        @field:Arg(dest = "skip_unmodified")
        var skipUnmodified = false

        @field:Arg
        lateinit var destination: File

        @field:Arg
        lateinit var sources: List<File>
    }

    try {
        parser.parseArgs(args, parsedArgs)
    } catch (e: ArgumentParserException) {
        parser.handleError(e)
        exitProcess(1)
    }

    val registry = TweakRegistry.DEFAULT.copy()
    registry.defaultReplace = parsedArgs.multipleTweakers
    for (tweakerPath in parsedArgs.tweakers) {
        val tweakers = try {
            loadTweakers(tweakerPath)
        } catch (e: IllegalArgumentException) {
            System.err.println("Failed to load tweaker $tweakerPath")
            System.err.println(e.localizedMessage)
            exitProcess(1)
        }
        if (tweakers.isEmpty()) {
            System.err.println("Couldn't load tweaker $tweakerPath")
            exitProcess(1)
        }
        tweakers.register(registry)
    }

    val tweaks = parsedArgs.tweaks.asSequence()
        .map {
            try {
                TweaksParser.parse(it, registry)
            } catch (e: InvalidTweaksException) {
                System.err.println("Error parsing $it")
                System.err.println(e.localizedMessage)
                exitProcess(1)
            }
        }
        .reduce(TweakSet::plus)

    val destination = parsedArgs.destination
    if (destination.exists()) {
        destination.deleteDirectoryContents()
    }
    destination.mkdirs()

    val originsMap = buildMap {
        for (source in parsedArgs.sources) {
            source.walk()
                .filter { it.isFile }
                .forEach { put(it, source) }
        }
    }
    val allFiles = originsMap.keys
    val sourceFiles = allFiles.asSequence().filter(File::isJavaFile).toSet()

    SyntaxTweaker(tweaks).tweak(sourceFiles) { source, _, newBody ->
        if (newBody == null && parsedArgs.skipUnmodified) return@tweak
        val thisDestination = getDestination(source, destination, originsMap)
        if (newBody == null) {
            source.copyTo(thisDestination, true)
        } else {
            thisDestination.writeText(newBody)
        }
    }

    if (!parsedArgs.skipUnmodified) {
        allFiles.asSequence().filter { it !in sourceFiles }.forEach { source ->
            source.copyTo(getDestination(source, destination, originsMap), true)
        }
    }
}

private fun loadTweakers(path: Path): List<TweakRegistrar> {
    val url = path.toUri().toURL()
    val filename = path.fileName.toString().lowercase()

    val exactMatch = tweakerTypes.entries.firstOrNull { filename.endsWith(it.key) }?.value
    if (exactMatch != null) {
        val tweakers = exactMatch(url)
        if (tweakers.isNotEmpty()) {
            return tweakers
        }
    }

    for (type in tweakerTypes.values) {
        val tweakers = type(url)
        if (tweakers.isNotEmpty()) {
            return tweakers
        }
    }

    return listOf()
}

fun getDestination(original: File, destination: File, originsMap: Map<File, File>): File {
    val absolute = destination.resolve(original.relativeTo(originsMap.getValue(original)))
    absolute.parentFile?.mkdirs()
    return absolute
}
