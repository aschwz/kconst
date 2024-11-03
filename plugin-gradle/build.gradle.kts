// Adapted from https://github.com/bnorm/kotlin-ir-plugin-template
plugins {
    id("java-gradle-plugin")
    kotlin("jvm")
    id("com.github.gmazzo.buildconfig")
}

dependencies {
    implementation(kotlin("gradle-plugin-api"))
}

buildConfig {
    val project = project(":plugin-gradle")
    packageName(project.group.toString())
    buildConfigField("String", "KOTLIN_PLUGIN_ID", "\"${rootProject.extra["kotlin_plugin_id"]}\"")
    buildConfigField("String", "KOTLIN_PLUGIN_GROUP", "\"${project.group}\"")
    buildConfigField("String", "KOTLIN_PLUGIN_NAME", "\"${project.name}\"")
    buildConfigField("String", "KOTLIN_PLUGIN_VERSION", "\"${project.version}\"")
}

gradlePlugin {
    plugins {
        create("kotlinIrPlugin") {
            id = rootProject.extra["kotlin_plugin_id"] as String
            displayName = "Kotlin Ir Plugin Template"
            description = "Kotlin Ir Plugin Template"
            implementationClass = "org.eu.aschwz.kconst.GradlePlugin"
        }
    }
}