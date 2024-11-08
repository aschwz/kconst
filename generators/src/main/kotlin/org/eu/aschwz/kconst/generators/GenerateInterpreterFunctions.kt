package org.eu.aschwz.kconst.generators

import java.io.File
import java.nio.file.Paths
import kotlin.collections.drop
import kotlin.collections.getOrPut
import kotlin.collections.joinToString
import kotlin.collections.mapIndexed
import kotlin.io.writeText

//fun main() {}

const val output: String = "../plugin/src/main/kotlin/org/eu/aschwz/kconst/generated/interpret.kt"

data class GeneratableFunction(val name: String, val argTypes: List<String>, val retTySource: String?,
                               val isInfix: Boolean = true, val isSpecialInfix: Boolean = false,
                               val mapsTo: String = name,
                               val origins: List<String>? = null
) {
    constructor(name: String, argTypes: List<String>, retTySource: String?,
                isInfix: Boolean = true, isSpecialInfix: Boolean = false,
                mapsTo: String = name,
                origin: String? = null
    ) : this(name, argTypes, retTySource, isInfix, isSpecialInfix, mapsTo, origin?.let {listOf(it)})
}

fun generateCall(fn: GeneratableFunction): String {
    val firstArg = "args[0].value as ${fn.argTypes[0]}"
    val postArgs = fn.argTypes.drop(1).mapIndexed { idx, ty -> "args[${idx+1}].value as $ty" }.joinToString(", ")
    if (fn.isInfix) {
        if (fn.isSpecialInfix) {
            return "($firstArg) ${fn.mapsTo} ($postArgs)"
        } else {
            return "($firstArg).${fn.mapsTo}($postArgs)"
        }
    } else {
        return "${fn.mapsTo}($firstArg, $postArgs)"
    }
}

fun recursiveFunctionArgTypeGrouping(s: Scribe, fns: List<GeneratableFunction>, currOffset: Int) {
    // if we only have one function, just do that
    if (fns.size == 1) {
        val fn = fns[0]
        if (fn.origins != null) {
            val condition = fn.origins.map{"origin == \"$it\""}.joinToString("||")
            s.println("\"${fn.argTypes[currOffset]}\" -> return if ($condition) (${generateCall(fn)}).toIrConst(${
                fn.retTySource ?: "args[0].type"
            }) else null")
        } else {
            s.println(
                "\"${fn.argTypes[currOffset]}\" -> return (${generateCall(fn)}).toIrConst(${
                    fn.retTySource ?: "args[0].type"
                })"
            )
        }
        return
    }
    // bucket by n-th type
    val fnsByArgType = kotlin.collections.mutableMapOf<String, MutableList<GeneratableFunction>>()
    fns.forEach {fnsByArgType.getOrPut(it.argTypes[currOffset], { kotlin.collections.mutableListOf() }).add(it)}
    // for every bucket, recurse
    for ((ty, fns) in fnsByArgType) {
        s.println("\"$ty\" -> when (args[${currOffset+1}].type.classFqName.toString()) {")
        s.pushIndent()
        recursiveFunctionArgTypeGrouping(s, fns, currOffset+1)
        s.popIndent()
        s.println("}")
    }
}

fun generateFunctions(s: Scribe, fns: List<GeneratableFunction>) {
    // first, bucket by name
    val fnsByName = kotlin.collections.mutableMapOf<String, MutableList<GeneratableFunction>>()
    fns.forEach {
        fnsByName.getOrPut(it.name, { kotlin.collections.mutableListOf<GeneratableFunction>() }).add(it)
    }
    s.println("package org.eu.aschwz.kconst.generated\n")
    s.println("// DO NOT EDIT")
    s.println("// This file generated by GenerateInterpreterFunctions.kt\n")
    s.println("import org.jetbrains.kotlin.ir.util.toIrConst")
    s.println("import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext")
    s.println("import org.jetbrains.kotlin.ir.types.classFqName")
    s.println("import org.jetbrains.kotlin.ir.expressions.IrConst\n")
    s.println("internal fun interpretBuiltinFunction(ctx: IrPluginContext, name: String, origin: String, args: List<IrConst<*>>) : IrConst<*>? {")
    s.pushIndent()
    s.println("when (name) {")
    s.pushIndent()
    for ((name, fns) in fnsByName) {
        // just sort of tree-search by arguments
        s.println("\"$name\" -> when (args[0].type.classFqName.toString()) {")
        s.pushIndent()
        recursiveFunctionArgTypeGrouping(s, fns, 0)
        s.popIndent()
        s.println("}")
    }
    s.popIndent()
    s.println("}")
    s.println("return null")
    s.popIndent()
    s.println("}")
}

fun main() {
    val s: Scribe = Scribe()
    generateFunctions(s, functionsToGenerate)
    File(output).writeText(s.finish())
}