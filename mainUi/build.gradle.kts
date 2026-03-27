import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
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
        commonMain.dependencies {

            implementation(project(":libs:log"))
            implementation(project(":libs:wtf_osd"))
            implementation(project(":libs:tile_map"))
            implementation(project(":libs:file_handling"))
            implementation(project(":libs:concurrency"))

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

            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs)
            implementation(libs.filekit.dialogs.compose)
            implementation(libs.filekit.coil)

            implementation(libs.multiplatform.settings)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidMain.dependencies {
            implementation(libs.compose.uiTooling)
        }
    }
}
