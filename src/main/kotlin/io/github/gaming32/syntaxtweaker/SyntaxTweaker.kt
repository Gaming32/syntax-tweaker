package io.github.gaming32.syntaxtweaker

import io.github.gaming32.syntaxtweaker.tweaks.TweakSet
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.compiler.setupIdeaStandaloneExecution
import org.jetbrains.kotlin.cli.jvm.config.addJavaSourceRoots
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.addJvmSdkRoots
import org.jetbrains.kotlin.cli.jvm.modules.CoreJrtFileSystem
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.com.intellij.openapi.vfs.StandardFileSystems
import org.jetbrains.kotlin.com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.kotlin.com.intellij.openapi.vfs.local.CoreLocalFileSystem
import org.jetbrains.kotlin.com.intellij.psi.JavaRecursiveElementVisitor
import org.jetbrains.kotlin.com.intellij.psi.PsiField
import org.jetbrains.kotlin.com.intellij.psi.PsiFile
import org.jetbrains.kotlin.com.intellij.psi.PsiJavaCodeReferenceElement
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import org.jetbrains.kotlin.com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.util.*

open class SyntaxTweaker(
    val tweaks: TweakSet,
    val classpath: List<File> = defaultClasspath
) {
    companion object {
        val defaultClasspath by lazy {
            System.getProperty("java.class.path")
                .splitToSequence(File.pathSeparatorChar)
                .map(::File)
                .toList()
        }
    }

    fun tweak(sources: List<File>, result: (source: File, sourcePsi: PsiFile, newBody: String?) -> Unit) {
        val disposable = Disposer.newDisposable()
        try {
            val config = createCompilerConfiguration(sources)

            setupIdeaStandaloneExecution()
            val environment = KotlinCoreEnvironment.createForProduction(disposable, config, EnvironmentConfigFiles.JVM_CONFIG_FILES)
            val project = environment.project
            val psiManager = PsiManager.getInstance(project)
            val vfs = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL) as CoreLocalFileSystem

            TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                project, listOf(), NoScopeRecordCliBindingTrace(), config, { environment.createPackagePartProvider(it) }
            )

            for (file in sources) {
                val psiFile = psiManager.findFile(vfs.findFileByIoFile(file) ?: continue) ?: continue
                val tweaker = FileTweaker(psiFile)
                tweaker.tweak()
                if (tweaker.changes.isNotEmpty()) {
                    result(file, psiFile, tweaker.getResult())
                } else {
                    result(file, psiFile, null)
                }
            }
        } finally {
            disposable.dispose()
        }
    }

    private fun createCompilerConfiguration(sources: List<File>): CompilerConfiguration {
        val config = CompilerConfiguration()
        config.put(CommonConfigurationKeys.MODULE_NAME, "main")

        val jdkHome = File(System.getProperty("java.home"))
        config.put(JVMConfigurationKeys.JDK_HOME, jdkHome)
        if (!CoreJrtFileSystem.isModularJdk(jdkHome)) {
            config.addJvmSdkRoots(PathUtil.getJdkClassesRoots(jdkHome))
        }

        config.addJavaSourceRoots(sources)
        config.addJvmClasspathRoots(classpath)

        setupCompilerConfiguration(config)

        config.put(JVMConfigurationKeys.USE_PSI_CLASS_FILES_READING, true)

        return config
    }

    open fun setupCompilerConfiguration(config: CompilerConfiguration) {
        config.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
    }

    private inner class FileTweaker(val file: PsiFile) : TweakTarget {
        val changes = TreeMap<TextRange, () -> String>(
            compareBy<TextRange> { it.startOffset }.thenBy { it.endOffset }
        )

        override fun replace(range: TextRange, with: () -> String) {
            changes.merge(range, with) { old, new -> { old() + new() } }
        }

        override fun canReplace(range: TextRange): Boolean {
            val before = changes.floorKey(range) ?: TextRange.EMPTY_RANGE
            val after = changes.ceilingKey(range) ?: TextRange.EMPTY_RANGE
            return !before.intersectsStrict(range) && !after.intersectsStrict(range)
        }

        fun tweak() {
            file.accept(object : JavaRecursiveElementVisitor() {
                override fun visitReferenceElement(reference: PsiJavaCodeReferenceElement) {
                    when (val resolved = reference.resolve()) {
                        is PsiField -> tweaks.applyFieldReference(reference, resolved, this@FileTweaker)
                        is PsiMethod -> tweaks.applyMethodReference(reference, resolved, this@FileTweaker)
                    }
                }
            })
        }

        fun getResult(): String {
            var result = file.text
            changes.descendingMap().forEach { (range, replacement) ->
                result = range.replace(result, replacement())
            }
            return result
        }
    }
}
