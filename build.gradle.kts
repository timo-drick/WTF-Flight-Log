import nl.littlerobots.vcu.plugin.resolver.VersionSelectors
import nl.littlerobots.vcu.plugin.versionCatalogUpdate

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
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
        versions.add("compose-adaptive") // Unfortunately version "1.3.0-alpha05" does not work with wasm
        versions.add("compose-nav3") // Not working with next higher version
        versions.add("material3") // Not working with next higher version
    }
}
