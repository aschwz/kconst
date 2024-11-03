package org.eu.aschwz.kconst.generated

// DO NOT EDIT
// This file generated by GenerateInterpreterFunctions.kt

import org.jetbrains.kotlin.ir.util.toIrConst
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.expressions.IrConst

internal fun interpretBuiltinFunction(ctx: IrPluginContext, name: String, origin: String, args: List<IrConst<*>>) : IrConst<*>? {
    when (name) {
        "EQEQ" -> when (args[0].type.classFqName.toString()) {
            "kotlin.Boolean" -> when (args[1].type.classFqName.toString()) {
                "kotlin.Boolean" -> return if (origin == "EQEQ") ((args[0].value as kotlin.Boolean) == (args[1].value as kotlin.Boolean)).toIrConst(ctx.irBuiltIns.booleanType) else null
            }
            "kotlin.String" -> when (args[1].type.classFqName.toString()) {
                "kotlin.String" -> return if (origin == "EQEQ") ((args[0].value as kotlin.String) == (args[1].value as kotlin.String)).toIrConst(ctx.irBuiltIns.booleanType) else null
            }
            "kotlin.Int" -> when (args[1].type.classFqName.toString()) {
                "kotlin.Int" -> return if (origin == "EQEQ") ((args[0].value as kotlin.Int) == (args[1].value as kotlin.Int)).toIrConst(ctx.irBuiltIns.booleanType) else null
            }
        }
        "EQEQEQ" -> when (args[0].type.classFqName.toString()) {
            "kotlin.Boolean" -> when (args[1].type.classFqName.toString()) {
                "kotlin.Boolean" -> return if (origin == "EQEQEQ") ((args[0].value as kotlin.Boolean) === (args[1].value as kotlin.Boolean)).toIrConst(ctx.irBuiltIns.booleanType) else null
            }
            "kotlin.String" -> when (args[1].type.classFqName.toString()) {
                "kotlin.String" -> return if (origin == "EQEQEQ") ((args[0].value as kotlin.String) === (args[1].value as kotlin.String)).toIrConst(ctx.irBuiltIns.booleanType) else null
            }
            "kotlin.Int" -> when (args[1].type.classFqName.toString()) {
                "kotlin.Int" -> return if (origin == "EQEQEQ") ((args[0].value as kotlin.Int) === (args[1].value as kotlin.Int)).toIrConst(ctx.irBuiltIns.booleanType) else null
            }
        }
        "plus" -> when (args[0].type.classFqName.toString()) {
            "kotlin.Int" -> when (args[1].type.classFqName.toString()) {
                "kotlin.Int" -> return if (origin == "PLUS"||origin == "PLUSEQ") ((args[0].value as kotlin.Int) + (args[1].value as kotlin.Int)).toIrConst(ctx.irBuiltIns.intType) else null
            }
            "kotlin.String" -> when (args[1].type.classFqName.toString()) {
                "kotlin.String" -> return if (origin == "PLUS"||origin == "PLUSEQ") ((args[0].value as kotlin.String) + (args[1].value as kotlin.String)).toIrConst(ctx.irBuiltIns.stringType) else null
            }
        }
        "minus" -> when (args[0].type.classFqName.toString()) {
            "kotlin.Int" -> return if (origin == "MINUS"||origin == "MINUSEQ") ((args[0].value as kotlin.Int) - (args[1].value as kotlin.Int)).toIrConst(ctx.irBuiltIns.intType) else null
        }
        "rem" -> when (args[0].type.classFqName.toString()) {
            "kotlin.Int" -> return if (origin == "PERC"||origin == "PERCEQ") ((args[0].value as kotlin.Int) % (args[1].value as kotlin.Int)).toIrConst(ctx.irBuiltIns.intType) else null
        }
        "div" -> when (args[0].type.classFqName.toString()) {
            "kotlin.Int" -> return if (origin == "DIV"||origin == "DIVEQ") ((args[0].value as kotlin.Int) / (args[1].value as kotlin.Int)).toIrConst(ctx.irBuiltIns.intType) else null
        }
        "times" -> when (args[0].type.classFqName.toString()) {
            "kotlin.Int" -> return if (origin == "MUL"||origin == "MULEQ") ((args[0].value as kotlin.Int) * (args[1].value as kotlin.Int)).toIrConst(ctx.irBuiltIns.intType) else null
        }
        "less" -> when (args[0].type.classFqName.toString()) {
            "kotlin.Int" -> return if (origin == "LT") ((args[0].value as kotlin.Int) < (args[1].value as kotlin.Int)).toIrConst(ctx.irBuiltIns.booleanType) else null
        }
        "greater" -> when (args[0].type.classFqName.toString()) {
            "kotlin.Int" -> return if (origin == "GT") ((args[0].value as kotlin.Int) > (args[1].value as kotlin.Int)).toIrConst(ctx.irBuiltIns.booleanType) else null
        }
        "lessOrEqual" -> when (args[0].type.classFqName.toString()) {
            "kotlin.Int" -> return if (origin == "LTEQ") ((args[0].value as kotlin.Int) <= (args[1].value as kotlin.Int)).toIrConst(ctx.irBuiltIns.booleanType) else null
        }
        "greaterOrEqual" -> when (args[0].type.classFqName.toString()) {
            "kotlin.Int" -> return if (origin == "GTEQ") ((args[0].value as kotlin.Int) >= (args[1].value as kotlin.Int)).toIrConst(ctx.irBuiltIns.booleanType) else null
        }
    }
    return null
}