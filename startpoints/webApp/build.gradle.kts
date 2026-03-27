import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "composeApp"
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    static(
                        project(":libs:worker_file_parser")
                            .layout.buildDirectory
                            .dir("dist/wasmJs/developmentExecutable")
                            .get().asFile.absolutePath
                    )
                    static(
                        project(":libs:worker_file_parser")
                            .layout.projectDirectory.asFile.absolutePath
                    )
                }
            }
        }
        compilerOptions {
            freeCompilerArgs.add("-Xwasm-kclass-fqn")
            freeCompilerArgs.add("-Xwasm-enable-array-range-checks")
        }
        binaries.executable()
    }
    
    sourceSets {
        wasmJsMain.dependencies {
            implementation(project(":mainUi"))
            implementation(project(":libs:worker-example"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
        }
    }
}

val copyWorkerFiles = tasks.register<Copy>("copyWorkerFiles") {
    group = "kotlin browser"
    // Reference the worker project's distribution directory
    val workerProject = project(":libs:worker-example")
    from(workerProject.layout.buildDirectory.dir("dist/wasmJs/productionExecutable"))

    // Target the webApp's distribution directory
    into(layout.buildDirectory.dir("dist/wasmJs/productionExecutable"))

    // Ensure the worker is built before copying
    dependsOn(workerProject.tasks.named("wasmJsBrowserDistribution"))
}

// 2. Hook it into the webApp's distribution task
tasks.named("wasmJsBrowserDistribution") {
    dependsOn(copyWorkerFiles)
}

tasks.named("wasmJsBrowserDevelopmentExecutableDistribution") {
    dependsOn(":libs:worker-example:wasmJsBrowserDevelopmentExecutableDistribution")
}
