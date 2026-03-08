import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
}


// Generate BuildConfig with secrets
val generateBuildConfig by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/buildconfig")
    outputs.dir(outputDir)

    // Use input property for configuration cache compatibility
    val localPropertiesFile = rootProject.layout.projectDirectory.file("local.properties")
    inputs.files(localPropertiesFile).optional()

    doLast {
        // Read local.properties for secrets like mapboxToken
        val localProperties = Properties().apply {
            val propsFile = localPropertiesFile.asFile
            if (propsFile.exists()) {
                propsFile.inputStream().use { load(it) }
            }
        }

        val mapboxToken: String = System.getenv("MAPBOX_TOKEN")
                ?: localProperties.getProperty("mapbox.token", "")

        val dir = outputDir.get().asFile.resolve("de/drick/compose/tilemap")
        dir.mkdirs()
        dir.resolve("BuildConfig.kt").writeText(
            """
            |package de.drick.compose.tilemap
            |
            |object BuildConfig {
            |    const val MAPBOX_TOKEN = "$mapboxToken"
            |}
            """.trimMargin()
        )
    }
}

kotlin {

    android {
        namespace = "de.moaps.composemultiplatformtilemap"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = 26
        androidResources {
            enable = true
        }
    }

    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    // Source set declarations.
    // Declaring a target automatically creates a source set with the same name. By default, the
    // Kotlin Gradle Plugin creates additional source sets that depend on each other, since it is
    // common to share sources between related targets.
    // See: https://kotlinlang.org/docs/multiplatform-hierarchy.html
    sourceSets {
        commonMain {
            kotlin.srcDir(generateBuildConfig)
            dependencies {
                implementation(project(":libs:log"))

                implementation(libs.kotlin.stdlib)

                implementation(libs.compose.foundation)
                implementation(libs.compose.ui)
                implementation(libs.compose.components.resources)

                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.logging)
                implementation(libs.kotlinx.serialization.json)
            }
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
        
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}