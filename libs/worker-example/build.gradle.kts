import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "worker-example"
        browser {
            binaries.executable()
        }
    }

    sourceSets {
        wasmJsMain {
            dependencies {
                implementation(project(":libs:concurrency"))
                implementation(libs.kotlinx.serialization.json)
            }
        }
    }
}
