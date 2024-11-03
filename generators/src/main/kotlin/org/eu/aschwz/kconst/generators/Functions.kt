package org.eu.aschwz.kconst.generators

import kotlin.collections.map

fun repeatForNTypes(fn: GeneratableFunction, types: List<String>, repeats: Int) : List<GeneratableFunction> =
    types.map { ty: String ->
        GeneratableFunction(fn.name, List<String>(repeats) { ty }, fn.retTySource, fn.isInfix, fn.isSpecialInfix,
            fn.mapsTo, fn.origins)
    }

val constantTypes = listOf("kotlin.Boolean", "kotlin.String", "kotlin.Int")


val boolT = "ctx.irBuiltIns.booleanType"
val intT = "ctx.irBuiltIns.intType"
val intT2 = listOf("kotlin.Int", "kotlin.Int")

fun String.withEq() : List<String> = listOf(this, "${this}EQ")

val functionsToGenerate : List<GeneratableFunction> = repeatForNTypes(
    GeneratableFunction("EQEQ", listOf(), boolT, true, true, "==", "EQEQ"),
    constantTypes,
    2
) + repeatForNTypes(GeneratableFunction("EQEQEQ", listOf(), boolT, true, true, "===", "EQEQEQ"), constantTypes, 2
) + listOf(
    GeneratableFunction("plus", intT2, intT, true, true, "+", "PLUS".withEq()),
    GeneratableFunction("plus", listOf("kotlin.String", "kotlin.String"), "ctx.irBuiltIns.stringType", true, true, "+", "PLUS".withEq()),
    GeneratableFunction("minus", intT2, intT, true, true, "-", "MINUS".withEq()),
    GeneratableFunction("rem", intT2, intT, true, true, "%", "PERC".withEq()),
    GeneratableFunction("div", intT2, intT, true, true, "/", "DIV".withEq()),
    GeneratableFunction("times", intT2, intT, true, true, "*", "MUL".withEq()),
    GeneratableFunction("less", intT2, boolT, true, true, "<", "LT"),
    GeneratableFunction("greater", intT2, boolT, true, true, ">", "GT"),
    GeneratableFunction("lessOrEqual", intT2, boolT, true, true, "<=", "LTEQ"),
    GeneratableFunction("greaterOrEqual", intT2, boolT, true, true, ">=", "GTEQ"),
)
