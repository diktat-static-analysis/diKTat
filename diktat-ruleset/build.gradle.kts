import com.github.jengelman.gradle.plugins.shadow.ShadowExtension
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("org.cqfn.diktat.buildutils.kotlin-jvm-configuration")
    id("org.cqfn.diktat.buildutils.code-quality-convention")
    id("org.cqfn.diktat.buildutils.publishing-configuration")
    id("com.github.johnrengelman.shadow") version "7.1.2"
    `maven-publish`
}

project.description = "This module builds jar that can be used to run diktat using ktlint -R via command line"

dependencies {
    implementation(projects.diktatRules)
    implementation(libs.log4j2.core)
    implementation(libs.kotlinx.cli)
    implementation(libs.ktlint.reporter.baseline)
    implementation(libs.ktlint.reporter.checkstyle)
    implementation(libs.ktlint.reporter.html)
    implementation(libs.ktlint.reporter.json)
    implementation(libs.ktlint.reporter.plain)
    implementation(libs.ktlint.reporter.sarif)
    testImplementation(projects.diktatTestFramework)
    testImplementation(libs.kotlin.stdlib.common)
    testImplementation(libs.kotlin.stdlib.jdk7)
    testImplementation(libs.kotlin.stdlib.jdk8)
    testImplementation(libs.kotlin.stdlib)
    testImplementation(libs.kotlin.compiler.embeddable)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.platform.suite)
    testImplementation(libs.assertj.core)
    testImplementation(libs.mockito)
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("diktat")
    archiveClassifier.set("")
}

// disable default jar
tasks.named("jar") {
    enabled = false
}

// it triggers shadowJar with default build
tasks {
    build {
        dependsOn(shadowJar)
    }
}

// it creates a publication for shadowJar
publishing {
    publications {
        create<MavenPublication>("shadow") {
            // https://github.com/johnrengelman/shadow/issues/417#issuecomment-830668442
            project.extensions.configure<ShadowExtension> {
                component(this@create)
            }
        }
    }
}
