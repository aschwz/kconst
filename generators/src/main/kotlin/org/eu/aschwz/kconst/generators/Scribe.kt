package org.eu.aschwz.kconst.generators

import kotlin.text.repeat

class Scribe {
    private var currIndent = 0
    private var currContent = StringBuilder()
    private var indentString = "    "
    private fun indent() = currContent.append(indentString.repeat(currIndent))
    fun pushIndent() = currIndent++
    fun popIndent() = currIndent--
    fun print(s: String) = currContent.append(s)
    fun println(s: String) = indent().append(s).append('\n')

    fun finish() : String = currContent.toString()
}