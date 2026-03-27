import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
    android {
        namespace = "de.drick.concurrency"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = 26
    }

    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.filekit.core)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.kotlinx.serialization.json)
            }
        }
        val nonWebMain by creating {
            dependsOn(commonMain.get())
        }
        jvmMain {
            dependsOn(nonWebMain)
        }
        androidMain {
            dependsOn(nonWebMain)
        }
        val wasmJsMain by getting {
            dependencies {
                implementation(libs.kotlinx.browser)
            }
        }
    }
}
