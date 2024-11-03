package org.eu.aschwz.kconst

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration

@OptIn(ExperimentalCompilerApi::class)
class KConstCompilerPluginRegistrar(
    private val defaultLoopLimit: Int = 64,
) : CompilerPluginRegistrar() {
    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(
        configuration: CompilerConfiguration
    ) {
        val messageCollector = configuration.get(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
        val loopLimit = configuration.get<Int>(KConstCommandLineProcessor.ARG_LOOP_LIMIT, defaultLoopLimit)

        IrGenerationExtension.registerExtension(KConstIrGenerationExpression(messageCollector, loopLimit))
    }
}