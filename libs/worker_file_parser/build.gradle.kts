import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "worker_file_parser"
        browser {
            binaries.executable()
        }
    }

    sourceSets {
        wasmJsMain {
            dependencies {
                implementation(project(":libs:concurrency"))
                implementation(project(":libs:wtf_osd"))
                implementation(project(":libs:tile_map"))

                implementation(libs.compose.ui)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.filekit.core)
            }
        }
    }
}
