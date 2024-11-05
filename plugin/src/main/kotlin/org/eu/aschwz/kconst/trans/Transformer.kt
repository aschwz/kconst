package org.eu.aschwz.kconst.trans

import org.eu.aschwz.kconst.eval.EvalEnvironment
import org.eu.aschwz.kconst.generated.interpretBuiltinFunction
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.wasm.ir2wasm.getSourceLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrBranch
import org.jetbrains.kotlin.ir.expressions.IrBreak
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrContinue
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.IrWhileLoop
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.deepCopyWithoutPatchingParents
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.utils.addToStdlib.popLast
import kotlin.math.max

fun IrStatement.const() : IrConst<*>? = this as? IrConst<*>
fun IrExpression.isEmptyBlock() : Boolean = (this as? IrBlockImpl)?.statements?.isEmpty() == true

// enormous URGENT: check purity of statements before assuming correctness of set/getValues
// specifically, when walking through a function, if any previous part of it is not pure then we can't
// reason about how side effects will affect state
// bonus: if we can reason how side effects _will_ affect state, we can "pollute/mark unsafe" only certain
// variables and continue, but this could be dodgy with things like reflection
// UPDATE: has been done, but reflection will still very probably be _very_ dodgy
// this code also assumes that all arguments are passed by-value, not by-ref, so functions should not
// modify their arguments in any way
// really, anything by-ref would break this


// TO fix the "cannot-use-copy" problem, simply:
// - if write back, then write back
// - if no write back, then return a copy
// this could be problematic for non-constant loops, and so _really_ I should make use of the builders, but
// they're not documented!! yay!!!
class Transformer(
    private val ctx: IrPluginContext,
    private val funcs: Map<IrFunctionSymbol, IrFunction>,
    private val msg: MessageCollector,
    private var env: EvalEnvironment,
    private val loopLimit: Int
) : IrElementTransformerVoidWithContext() {
    // a stack of functions we've already visited, to prevent recursing infinitely
    var visitedFuncs : MutableList<IrFunctionSymbol> = mutableListOf()
    // an Uncertainty State is used when the wider context is not deterministic, but a smaller, independent, inner
    // context could be, so we transform the inner context in a "bubble" (likely using cleanSlates),
    // which allows us to optimize it locally, and then once we exit this "bubble" we remember that the overall nature
    // of the block is nondeterministic, so we invalidate any side effects it has so we can't use them as "certain"s
    var uncertainStates : MutableList<MutableList<IrValueSymbol>> = mutableListOf()
    fun pushUncertain() = uncertainStates.push(mutableListOf())
    fun popUncertain() {
        // mark everything we touched as uncertain
        uncertainStates.popLast().forEach {env.invalidate(it)}
    }
    fun <R>withUncertain(l: () -> R) : R {
        pushUncertain()
        val res = l()
        popUncertain()
        return res
    }
    fun isUncertain() = uncertainStates.isNotEmpty()
    // used when we don't want to end up modifying the input
    // we can't just always copy it, since for some reason copying means it won't compile
    // so noWriteBack is like a "testing the waters" phase
    var noWriteBacks : Int = 0
    fun pushNoWriteBack() = noWriteBacks++
    fun popNoWriteBack() = max(noWriteBacks-1, 0)
    fun canWriteBack() = noWriteBacks == 0

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        currFuncSym = declaration.symbol
        return super.visitSimpleFunction(declaration)
    }
    var currFuncSym : IrFunctionSymbol? = null
    fun builder() = DeclarationIrBuilder(ctx, currFuncSym!!)

    // when evalFunction-ing, these track the return value + if it was reached in a deterministic manner
    var isCertain : Boolean = true
    var retVal : IrConst<*>? = null
    // used for visitCall
    fun evalFunction(fn: IrFunction, call: IrCall) : IrConst<*>? {
        if (visitedFuncs.contains(fn.symbol)) {return null}
        visitedFuncs.push(fn.symbol)
        val oldCertain = isCertain
        isCertain = true
        val oldRet = retVal
        retVal = null
        val oldEnv = env
        env = EvalEnvironment()
        val oldFuncSym = currFuncSym
        currFuncSym = call.symbol
        pushNoWriteBack()
        if (call.valueArguments.size != fn.valueParameters.size) {
            msg.report(CompilerMessageSeverity.WARNING, "Mismatch in argument count")
            return null
        }
        // make a stack frame for the arguments
        env.pushFrame()
        // add the arguments
        for ((p, v) in fn.valueParameters.zip(call.valueArguments)) {
            val v = v as IrConst<*>
            val vs = p.symbol as IrValueSymbol
            env.set(vs, v)
        }
        // eval the body
        env.pushFrame()
        fn.body?.transform(this, null)
        env.popFrame()
        env.popFrame()
        var wasCertain = isCertain
        isCertain = oldCertain
        var wasRetVal = retVal
        retVal = oldRet
        env = oldEnv
        currFuncSym = oldFuncSym
        visitedFuncs.popLast()
        popNoWriteBack()
        return if (wasCertain) wasRetVal else null
    }

    override fun visitReturn(expression: IrReturn): IrExpression {
        if (expression.returnTargetSymbol != currFuncSym) {
            msg.report(CompilerMessageSeverity.ERROR, "KConst does not currently support labelled returns",
                expression.getSourceLocation(null) as CompilerMessageSourceLocation?
            )
            isCertain = false
        }
        var expression = if (canWriteBack()) {expression} else {expression.deepCopyWithoutPatchingParents()}
        expression.value = expression.value.transform(this, null)
        val const = expression.value.const()
        if (const != null && !isUncertain()) {
            if (retVal == null) retVal = const
        } else {
            // we might go on to visit other returns, so we need to ensure we don't erroneously return a bad value
            isCertain = false
        }
        return expression
    }

    override fun visitFunctionNew(declaration: IrFunction): IrStatement {
        env.pushFrame()
        visitedFuncs.push(declaration.symbol)
        val newFun = super.visitFunctionNew(declaration)
        env.popFrame()
        visitedFuncs.popLast()
        return newFun
    }

    override fun visitVariable(declaration_: IrVariable): IrStatement {
        var declaration = if (canWriteBack()) {declaration_} else {declaration_.deepCopyWithoutPatchingParents()}
        val body = declaration.initializer?.transform(this, null)
        declaration.initializer = body
        // if the body isn't const, then we could have side effects, and so we can't fold with this variable
        declaration.initializer?.const()?.let {env.set(declaration_.symbol, it)}
        return declaration
    }
    override fun visitGetValue(get: IrGetValue): IrExpression {
        val value = env.get(get.symbol)
        return value ?: get
    }
    override fun visitSetValue(set_: IrSetValue): IrExpression {
        // we _need_ to use the original symbol, else we run into issues
        var set = if (canWriteBack()) {set_} else {set_.deepCopyWithoutPatchingParents()}
        set.symbol = set_.symbol
        val body = set.value.transform(this, null)
        set.value = body
        set.value.const()?.let {env.set(set.symbol, it)}
        uncertainStates.forEach {it.push(set.symbol)}
        // TODO: figure out in which situations we don't have to emit the set
        // this is more like reaching-def or data-flow analysis
        return set
    }

    override fun visitBlockBody(body: IrBlockBody): IrBody {
        // un-nest nested blocks
        var newExprs = mutableListOf<IrStatement>()
        for (stmt in body.statements) {
            val newExp = stmt.transform(this, null) as IrStatement
            if (newExp is IrBlock) {
                for (stmt in newExp.statements) {
                    newExprs.push(stmt)
                }
            } else {
                newExprs.push(newExp)
            }
        }
        val newExprsFiltered = newExprs.filter {
            !(
                    it is IrConst<*>
                            || (it is IrSetValue
                            && env.get(it.symbol) != null
                            && !isUncertain()
                            ) || (it is IrVariable
                            && env.get(it.symbol) != null
                            && !isUncertain()
                            )
                    )
        }
        return if (canWriteBack()) {
            body.statements.clear()
            body.statements.addAll(newExprsFiltered)
            body
        } else {
            builder().irBlockBody(body, body = {
                newExprsFiltered.forEach { +it }
            })
        }
    }

    override fun visitBlock(block: IrBlock): IrExpression {
        // un-nest nested blocks
        var newExprs = mutableListOf<IrStatement>()
        for (stmt in block.statements) {
            if (hasBreak || hasContinue) break
            val newExp = stmt.transform(this, null) as IrStatement
            if (newExp is IrBlock) {
                for (stmt in newExp.statements) {
                    newExprs.push(stmt)
                }
            } else {
                newExprs.push(newExp)
            }
        }
        // if something in the block is either a const or a set of a variable that is
        // tracked, and we're not in an uncertain context, then we can like ignore it
        val newExprsFiltered = newExprs.filter {
            !(
                 it is IrConst<*>
                 || (it is IrSetValue
                     && env.get(it.symbol) != null
                     && !isUncertain()
                 ) || (it is IrVariable
                         && env.get(it.symbol) != null
                         && !isUncertain()
                 )
            )
        }
        return if (canWriteBack()) {
            block.statements.clear()
            block.statements.addAll(newExprsFiltered)
            block
        } else {
            builder().irBlock(body = {
                newExprsFiltered.forEach { +it }
            })
        }
    }

    override fun visitWhen(whenExpr: IrWhen): IrExpression {
        var whenExpr = if (canWriteBack()) {whenExpr} else {whenExpr.deepCopyWithoutPatchingParents()}
        // visit each branch in order
        var branchesToKeep = mutableListOf<IrBranch>()
        var canEarlyExit = true
        for (branch in whenExpr.branches) {
            // if we can const-ify the condition, then yay
            branch.condition = branch.condition.transform(this, null)
            val cond = branch.condition.const()
            if (cond != null) {
                if (cond.value as Boolean) {
                    if (canEarlyExit) {
                        // this branch will Always Hit
                        // we can just return the then
                        return branch.result.transform(this, null)
                    } else {
                        branchesToKeep.push(branch)
                    }
                }
                // this branch is an if (false) so we don't need to keep it
            } else {
                // we can't reason about if this branch will or will not run
                // so it's not pure
                canEarlyExit = false
                pushUncertain()
                // fold it anyway
                branch.result = branch.result.transform(this, null)
                // but remember that it may or may not happen
                popUncertain()
                branchesToKeep.push(branch)
            }
        }
        whenExpr.branches.clear()
        whenExpr.branches.addAll(branchesToKeep)
        return whenExpr
    }

    var hasBreak = false
    var hasContinue = false
    override fun visitBreak(jump: IrBreak): IrExpression {
        hasBreak = true
        return super.visitBreak(jump)
    }

    override fun visitContinue(jump: IrContinue): IrExpression {
        hasContinue = true
        return super.visitContinue(jump)
    }

    override fun visitWhileLoop(loop: IrWhileLoop): IrExpression {
        hasBreak = false
        hasContinue = false
        pushNoWriteBack()
        var condition = loop.condition.transform(this, null)
        var body = loop.body ?: builder().irBlock(body = {})
        var conditionBreaksFirst = false
        env.pushWatchlist()
        if (condition is IrConstImpl<*>) {
            if (condition.value as? Boolean == true) {
                // we have no way of conclusively proving that a loop will eventually stop
                // so we shall try up to a maximum of like, 64 times?
                var currBody: IrExpression
                var loopTerminates = false
                @Suppress("unused")
                for (i in 1..loopLimit) {
                    currBody = body.transform(this, null)
                    hasContinue = false
                    if (currBody.const() == null && !currBody.isEmptyBlock()) {
                        // no point in continuing, we cannot flatten the loop
                        break
                    }

                    if (!hasBreak) {
                        // don't re-eval the condition if we broke out of the loop
                        condition = loop.condition.transform(this, null)
                    }
                    // since if we broke, we haven't changed const, so a || !hasBreak is redundant here
                    if (condition !is IrConstImpl<*>) {
                        // something has gone a bit awry
                        // this means that the loop may or may not continue executing, IDK
                        conditionBreaksFirst = true
                        break
                    } else {
                        if (!(condition.value as Boolean) || hasBreak) {
                            // we term
                            loopTerminates = true
                            break
                        }
                    }
                }
                if (loopTerminates) {
                    // we have "played out" the loop entirely
                    // all the sets have been set etc.
                    // we just preserve them here
                    val watchlist = env.popWatchlist()
                    // so we don't need the loop at all!
                    // return the block with explicit sets
                    popNoWriteBack()
                    return builder().irBlock(body = {
                        watchlist.toSet().forEach {+irSet(it, env.get(it)!!)}
                    })
                }
            } else {
                // the loop is a while (false) o.e.
                // we can return a nothing
                popNoWriteBack()
                return builder().irBlock(body = {})
            }
        }
        // no idea _what_ the loop does
        isCertain = false

        // the reason that we can do this is a bit odd, but here it is
        // since pushCleanSlate forces us to make no assumptions about external state,
        // the transforms will hit everything that could ever be hit
        // so due to pushUncertain, anything the loop _could_ have messed with when we
        // ran it above is reset to null, so we are good
        // TODO: run this once, catch what it sets, cleanSlate _only that_ and then finish off
        // with it again
        env.pushCleanSlate()
        var cond: IrExpression? = null
        if (conditionBreaksFirst) {
            withUncertain { body = loop.body?.transform(this, null) ?: builder().irBlock(body = {}) }
            withUncertain { cond = condition.transform(this, null) }
        } else {
            withUncertain { cond = loop.condition.transform(this, null) }
            withUncertain { body = loop.body?.transform(this, null) ?: builder().irBlock(body = {}) }
        }
        env.popCleanSlate()
        popNoWriteBack()
        if (canWriteBack()) {
            loop.body = body
            loop.condition = cond!!
        }


        return loop
    }

    // opt-in since call.symbol.owner is unsafe sometimes
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitCall(call: IrCall): IrExpression {
        var call = if (canWriteBack()) {call} else {call.deepCopyWithoutPatchingParents()}
        call.dispatchReceiver = call.dispatchReceiver?.transform(this, null)
        val newValueArgs = call.valueArguments.map {it?.transform(this, null)}
        newValueArgs.forEachIndexed { idx, exp -> call.putValueArgument(idx, exp) }
        // if they're all constant, then yay!
        // we can go further
        if (call.valueArguments.all { it is IrConst<*> }
            && (call.dispatchReceiver is IrConst<*> || call.dispatchReceiver == null)) {
            var constArgs = newValueArgs.map {it as IrConst<*>}.toMutableList()
            call.dispatchReceiver?.let {constArgs.add(0, it as IrConst<*>)}
            // we can try and inline it, if we know the name
            val name: String = call.symbol.owner.name.asString()
            val orig = call.origin.toString()
            val evaluated = interpretBuiltinFunction(ctx, name, orig, constArgs)
                ?: funcs[call.symbol]?.let {evalFunction(it, call)}
            if (evaluated != null) {
                if (call.symbol.owner.name.toString().startsWith("eval")) {
                    println("Flattened this call:")
                    println(call.dump())
                    println("to this value (${evaluated.value}):")
                    println(evaluated.dump())
                }
                return evaluated
            } else {
                isCertain = false
                return call
            }
        }

        return call
    }
}