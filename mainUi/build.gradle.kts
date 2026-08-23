import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import java.util.Properties
import kotlin.apply

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
        namespace = "de.drick.flightloglib"
        minSdk = libs.versions.android.minSdk.get().toInt()
        compileSdk = libs.versions.android.compileSdk.get().toInt()
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
    
    sourceSets {
        commonMain {
            kotlin.srcDir(generateBuildConfig)
            dependencies {
                implementation(project(":libs:log"))
                implementation(project(":libs:wtf_osd"))
                implementation(project(":libs:file_handling"))
                implementation(libs.compose.tilemap)

                implementation(libs.kdroidfilter.compose.mediaplayer)
                implementation(libs.kdroidfilter.platformtools.darkmodedetector)

                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.collections.immutable)
                implementation(libs.kotlinx.datetime)

                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.nav3)
                implementation(libs.compose.material3.adaptive)
                implementation(libs.compose.material3.adaptiveNavigation3)
                implementation(libs.compose.ui)
                implementation(libs.compose.components.resources)
                implementation(libs.compose.uiToolingPreview)

                implementation(libs.compose.edgeToEdge.preview)

                implementation(libs.androidx.lifecycle.viewmodelCompose)
                implementation(libs.androidx.lifecycle.runtimeCompose)

                implementation(libs.filekit.core)
                implementation(libs.filekit.dialogs)
                implementation(libs.filekit.dialogs.compose)
                implementation(libs.filekit.coil)

                implementation(libs.multiplatform.settings)
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidMain.dependencies {
            implementation(libs.compose.uiTooling)
        }
    }
}
