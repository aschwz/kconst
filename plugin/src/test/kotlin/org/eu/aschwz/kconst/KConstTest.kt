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
    if (false) {while (true) {}}
    var curr = 1
    var last = 1
    var idx = 0
    while (eqBools(idx < c, true)) {
        val tmp = last
        last = curr
        curr = tmp + curr
        idx += 1
    }
    return curr
}

fun whileExample() : Int {
    var q = 0
    while (q < 8) {
        q += 1
    }
    return q
}

fun foo() : Int {
    // this is reduced down to `return 8`
    return evalFib(4)
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