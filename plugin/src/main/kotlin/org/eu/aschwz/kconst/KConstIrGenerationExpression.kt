package org.eu.aschwz.kconst

import org.eu.aschwz.kconst.eval.EvalEnvironment
import org.eu.aschwz.kconst.trans.Transformer
import org.eu.aschwz.kconst.visit.FunctionExtractor
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.dump

class KConstIrGenerationExpression(
    private val messageCollector: MessageCollector,
    private val loopLimit: Int,
) : IrGenerationExtension {
    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext
    ) {
        println(moduleFragment.dump())

        // first, visit to pull out all the functions
        val visitor = FunctionExtractor()
        moduleFragment.accept(visitor, Unit)
        val fns = visitor.finish()
        val new = moduleFragment.transform(Transformer(pluginContext, fns, messageCollector, EvalEnvironment(), loopLimit), null)

        println("\n-------------\n")
        println(new.dump())
    }
}