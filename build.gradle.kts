// Adapted from https://github.com/bnorm/kotlin-ir-plugin-template
buildscript {
    extra["kotlin_plugin_id"] = "org.eu.aschwz.kconst.plugin"
}

plugins {
    kotlin("jvm") version "2.0.20" apply false
    id("org.jetbrains.dokka") version "1.9.20" apply false
    id("com.gradle.plugin-publish") version "1.3.0" apply false
    id("com.github.gmazzo.buildconfig") version "5.5.0" apply false
}

subprojects {
    repositories {
        mavenCentral()
    }
}

allprojects {
    group = "org.eu.aschwz.kconst"
    version = "0.0.0-SNAPSHOT"
}