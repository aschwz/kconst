package org.eu.aschwz.kconst

import com.tschuchort.compiletesting.JvmCompilationResult
import kotlin.test.Test
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.KotlinCompilation
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.assertEquals

class KconstTest {
    @OptIn(ExperimentalCompilerApi::class)
    @Test
    fun `Plugin Runnable`() {
        val result = compile(
            sourceFile = SourceFile.kotlin(
                "test.kt",
                """
fun eqBools(a: Boolean, b: Boolean) : Boolean {
    return a == b
}
fun evalFib(c: Int) : Int {
    var curr = 1
    var last = 1
    var idx = 0
    while (idx < c) {
        val tmp = last
        last = curr
        curr = tmp + curr
        idx += 1
    }
    return curr
}
fun main() {
    // this is reduced down to `val fa = 8`
    val fa = evalFib(4)
}
                """.trimIndent()
            )
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }
}

@OptIn(ExperimentalCompilerApi::class)
fun compile(
    sourceFiles: List<SourceFile>,
    plugin: CompilerPluginRegistrar = KConstCompilerPluginRegistrar(),
): JvmCompilationResult {
    return KotlinCompilation().apply {
        sources = sourceFiles
        compilerPluginRegistrars = listOf(plugin)
        inheritClassPath = true
    }.compile()
}

@OptIn(ExperimentalCompilerApi::class)
fun compile(
    sourceFile: SourceFile,
    plugin: CompilerPluginRegistrar = KConstCompilerPluginRegistrar(),
): JvmCompilationResult {
    return compile(listOf(sourceFile), plugin)
}