// Adapted from https://github.com/bnorm/kotlin-ir-plugin-template
plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("com.github.gmazzo.buildconfig")
}

// TODO: check versions on some of these
dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable")

//    kapt("com.google.auto.service:auto-service:1.1.1")
//    compileOnly("com.google.auto.service:auto-service-annotations:1.1.1")

    testImplementation(kotlin("test-junit"))
    testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable")
    testImplementation("dev.zacsweers.kctfork:core:0.5.1")
}

buildConfig {
    packageName(group.toString())
    buildConfigField("String", "KOTLIN_PLUGIN_ID", "\"${rootProject.extra["kotlin_plugin_id"]}\"")
}