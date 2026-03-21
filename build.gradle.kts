import nl.littlerobots.vcu.plugin.resolver.VersionSelectors
import nl.littlerobots.vcu.plugin.versionCatalogUpdate

plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.android.lint) apply false
    alias(libs.plugins.version.catalog.update) // Check for dependency updates
    alias(libs.plugins.detekt)
}

versionCatalogUpdate {
    sortByKey.set(true)
    versionSelector(VersionSelectors.PREFER_STABLE)
    keep {
        versions.add("android-compileSdk")
        versions.add("android-minSdk")
    }
    pin {
        versions.add("material3") // Would like to switch to stable version
    }
}

subprojects {
    pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
        apply(plugin = "dev.detekt")
        dependencies {
            detektPlugins(libs.detektComposeRules)
        }
        detekt {
            buildUponDefaultConfig = true
            allRules = false
            baseline = file("${projectDir}/detekt-baseline.xml")
            source = files(projectDir.resolve("src"))
        }
    }
}
