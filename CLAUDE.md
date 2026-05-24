# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

Use `gradlew.bat` on Windows (or `./gradlew` on Unix).

```bash
# Android
gradlew :app:androidApp:assembleDebug

# Server (runs on localhost:8080)
gradlew :server:run

# iOS — open app/iosApp in Xcode
```

## Test Commands

```bash
# Android host tests (JVM-based, fast)
gradlew :app:shared:testAndroidHostTest

# iOS simulator tests (requires macOS)
gradlew :app:shared:iosSimulatorArm64Test

# Server tests
gradlew :server:test
```

## Architecture

This is a **Kotlin Multiplatform** project targeting Android, iOS, and JVM (server), using Compose Multiplatform for shared UI.

### Module layout

| Module | Targets | Purpose |
|--------|---------|---------|
| `core` | Android, iOS, JVM | Shared business logic with no UI |
| `app/shared` | Android, iOS, JVM | Compose Multiplatform UI |
| `app/androidApp` | Android | Native Android entry point (`MainActivity`) |
| `app/iosApp` | iOS | Xcode project; integrates via `MainViewController` |
| `server` | JVM | Ktor HTTP server (Netty, port 8080) |

### Platform abstraction pattern

Platform-specific behavior uses Kotlin `expect/actual`. Declare in `commonMain`:
```kotlin
expect fun getPlatform(): Platform
```
Then implement in `androidMain`, `iosMain`, and `jvmMain` source sets respectively.

### UI entry point

All platforms share the `App()` composable defined in `app/shared/commonMain`. Android boots via `MainActivity`, iOS via the `ComposeUIViewController` wrapper in `MainViewController.kt`.

### Key versions

- Kotlin: 2.3.21
- Compose Multiplatform: 1.11.0
- Ktor: 3.4.3
- Android compileSdk/targetSdk: 36, minSdk: 24

Version catalog is at `gradle/libs.versions.toml` — update versions there, not inline.
