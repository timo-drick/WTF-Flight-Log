# Agent Instructions

## Project Structure
- **Kotlin Multiplatform (KMP)**: Targets Android, Web (Wasm), and Desktop (JVM).
- **Shared Code**: Located in `mainUi/src`.
- **Platform Entry Points**: Found in `startpoints/`:
  - `androidApp`: Android application.
  - `desktopApp`: Desktop (JVM) application.
  - `webApp`: Web (Wasm) application.
- **Libraries**: Located in `libs/` (`log`, `wtf_osd`, `tile_map`, `file_handling`).

## Development Commands

### Running Applications
- **Android**: `./gradlew :startpoints:androidApp:assembleDebug`
- **Desktop**: `./gradlew :startpoints:desktopApp:run`
- **Web (Wasm)**: `./gradlew :startpoints:webApp:wasmJsBrowserDevelopmentRun`

### Testing
- **All Tests**: `./gradlew allTests`
- **Specific Targets**:
  - **JVM**: `...:jvmTest` (e.g., `./gradlew :mainUi:jvmTest`)
  - **Wasm/JS**: `...:wasmJsBrowserTest` (e.g., `./gradlew :startpoints:webApp:wasmJsBrowserTest`)

### Linting & Verification
- **Linting**: `detekt` is applied to all subprojects. Run `./gradlew detekt` to check for code smells.
- **Dependency Management**: Uses Gradle Version Catalog (`gradle/libs.versions.toml`).

## Key Workflow Notes
- Always use the `gradlew` wrapper.
- When adding new dependencies, update `gradle/libs.versions.toml`.
- Respect the module boundaries; shared logic belongs in `mainUi`.
