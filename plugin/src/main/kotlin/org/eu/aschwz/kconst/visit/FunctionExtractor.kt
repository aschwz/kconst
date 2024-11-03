package org.eu.aschwz.kconst.visit

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

// Since I can't find it in IrModuleFragment, this visitor pulls out all the functions
class FunctionExtractor : IrElementVisitor<Unit, Unit> {
    private val functions : MutableMap<IrFunctionSymbol, IrFunction> = mutableMapOf()
    override fun visitElement(element: IrElement, data: Unit) {
        element.acceptChildren(this, data)
    }

    override fun visitFunction(declaration: IrFunction, data: Unit) {
        functions.set(declaration.symbol, declaration)
        return
    }

    fun finish() : MutableMap<IrFunctionSymbol, IrFunction> = functions
}