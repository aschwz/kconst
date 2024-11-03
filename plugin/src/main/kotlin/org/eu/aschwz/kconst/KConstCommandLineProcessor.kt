package org.eu.aschwz.kconst

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

@OptIn(ExperimentalCompilerApi::class)
class KConstCommandLineProcessor : CommandLineProcessor {
    companion object {
        val ARG_LOOP_LIMIT = CompilerConfigurationKey<Int>("loopLimit")
    }
    override val pluginId: String = BuildConfig.KOTLIN_PLUGIN_ID
    override val pluginOptions: Collection<AbstractCliOption> = listOf(
        CliOption(
            optionName = "loopLimit",
            valueDescription = "positive-or-zero integer, default=64",
            description = "Maximum number of times to try and resolve a loop." +
                    " If a loop doesn't finish in this many iterations, we treat it as nondeterministic",
            required = false,
        )
    )

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        return when (option.optionName) {
            "loopLimit" -> {
                val int = value.toIntOrNull()
                if (int == null) throw IllegalArgumentException("loopLimit should be a base-10 integer")
                if (int < 0) throw IllegalArgumentException("loopLimit should be >=0")
                configuration.put(ARG_LOOP_LIMIT, int)
            }
            else     -> throw IllegalArgumentException("Unexpected config option ${option.optionName}")
        }
    }
}