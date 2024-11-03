package org.eu.aschwz.kconst.eval

import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.utils.addToStdlib.popLast

data class ConstHolder(var value: IrConst<*>?, val cleanSlateCount: Number)

class Frame {
    // TODO: matching strings instead of IrValueSymbols seems _hacky_ but it makes it work
    internal var knownObjects = mutableMapOf<String, ConstHolder>()
    fun get(name: IrValueSymbol) : ConstHolder? = knownObjects.get(name.toString())
    fun set(name: IrValueSymbol, value: ConstHolder) {
        val name = name.toString()
        // if it's here, but null, that means it's marked as Polluted and should not be unmarked
        if (knownObjects.containsKey(name) && knownObjects.get(name) == null) return
        knownObjects.set(name, value)
    }
}


class EvalEnvironment {
    var cleanSlateCount = 0
    internal var frames = mutableListOf<Frame>()
    var watchlists: MutableList<MutableList<IrValueSymbol>> = mutableListOf()


    fun get(name: IrValueSymbol) : IrConst<*>? {
        for (frame in frames.asReversed()) {
            val value = frame.get(name)
            if (value != null) {
                if (value.cleanSlateCount != cleanSlateCount) return null
                return value.value
            }
        }
        return null
    }
    fun set(name: IrValueSymbol, value: IrConst<*>?) {
        watchlists.forEach {it.push(name)}
        frames.lastOrNull()?.set(name, ConstHolder(value, cleanSlateCount))
    }
    fun invalidate(name: IrValueSymbol) = set(name, null)
    fun pushFrame() {
        frames.push(Frame())
    }
    fun popFrame() {
        frames.popLast()
    }

    // if, for instance, we enter a while loop, then it becomes hard to reason about how variables outside
    // the loop will affect execution within the loop since we might have set them in previous iterations
    // hence we can pushCleanSlate, which will (until popCleanSlate is called) refuse to get for variables
    // outside
    // Should be used in conjunction with push/popUncertain
    fun pushCleanSlate() {
        cleanSlateCount++
    }
    fun popCleanSlate() {
        cleanSlateCount--
    }

    fun pushWatchlist() {watchlists.push(mutableListOf())}
    fun popWatchlist() : MutableList<IrValueSymbol> = watchlists.popLast()
}