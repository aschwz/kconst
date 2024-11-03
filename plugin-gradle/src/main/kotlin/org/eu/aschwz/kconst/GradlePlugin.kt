package org.eu.aschwz.kconst

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class GradlePlugin : KotlinCompilerPluginSupportPlugin {
    override fun apply(target: Project) : Unit = with(target) {
        extensions.create("kconst", GradleExtension::class.java)
    }
    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val extension = project.extensions.getByType(GradleExtension::class.java)
        return project.provider {
            listOf(
                SubpluginOption(key = "string", value = extension.stringProperty.get()),
                SubpluginOption(key = "file", value = extension.fileProperty.get().asFile.path)
            )
        }
        TODO("Not yet implemented")
    }

    override fun getCompilerPluginId(): String = BuildConfig.KOTLIN_PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = BuildConfig.KOTLIN_PLUGIN_GROUP,
        artifactId = BuildConfig.KOTLIN_PLUGIN_NAME,
        version = BuildConfig.KOTLIN_PLUGIN_VERSION,
    )

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true
}