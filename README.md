# Archie Browser (Android)

> [!NOTE]
> **Project Status: Experimental / In Development**
> Archie is an experimental, open-source native Android browser. It focuses on a clean, minimal mobile browsing experience with bottom-anchored navigation, fast search, and integrated URL tracking sanitization.
>
> Archie uses native Android components with **Jetpack Compose** and **Android WebView** (`android.webkit.WebView`).

---

## Architecture & Tech Stack

Archie Android is engineered to run cleanly on modern Android devices without unnecessary abstraction layers:

- **Package Name**: `in.caffeinelabs.archie` (debug builds: `in.caffeinelabs.archie.debug`)
- **Language & Runtime**: Kotlin 2.0.21 on Java 17
- **UI Framework**: Jetpack Compose (Compose BOM `2024.10.01`, Material 3, Edge-to-Edge)
- **Web Engine**: Android System `WebView` (`android.webkit.WebView` / `androidx.webkit:webkit:1.12.1`)
- **Target Platform**: Android SDK 35 (Android 15), minimum SDK 26 (Android 8.0)
- **Build System**: Gradle 8.13 with Android Gradle Plugin `8.7.3`

---

## Project Structure

```
app/
├── build.gradle.kts          # Android application build configuration
└── src/main/
    ├── AndroidManifest.xml   # Permissions, singleTask MainActivity, intent filters
    ├── java/in/caffeinelabs/archie/
    │   ├── ArchieApplication.kt   # Application entry point, WebView debugging
    │   ├── MainActivity.kt        # Edge-to-edge Compose host & back handler
    │   ├── ui/
    │   │   ├── components/
    │   │   │   ├── BottomOmnibox.kt   # Floating bottom omnibox pill with progress & controls
    │   │   │   ├── BrowserWebView.kt  # Compose AndroidView wrapper for WebView
    │   │   │   ├── SearchModal.kt     # Full-screen search overlay with paste chip
    │   │   │   ├── TabStackIcon.kt    # Dual-tilted card tab counter icon
    │   │   │   └── TabSwitcher.kt     # Horizontal card carousel tab switcher
    │   │   ├── state/
    │   │   │   ├── BrowserState.kt     # Immutable UI state data class
    │   │   │   └── BrowserViewModel.kt # StateFlow & navigation command dispatcher
    │   │   └── theme/
    │   │       ├── Color.kt           # Dark palette & semantic colors
    │   │       └── Theme.kt           # Material 3 theme wrapper
    │   └── util/
    │       └── UrlSanitizer.kt        # Tracking stripper & domain resolver
    └── res/                           # App strings, themes, and resources
```

---

## Features & Capabilities

Archie delivers a fast, ergonomic mobile browsing experience:

- **Multi-Tab Management**: Multi-tab browsing with an in-memory WebView pool preserving DOM state, scroll positions, and back-forward navigation across tab switches.
- **Horizontal Card Tab Switcher**: Horizontal snapping card carousel displaying open tabs with titles, domains, favicons, swipe-up to dismiss, and an "Archive All" action.
- **Bottom Omnibox & Browser Navigation**:
  - Dual-tilted tab stack icon displaying live open tab count and opening the Tab Switcher.
  - Floating pill showing connection security lock, clean display domain, and real-time loading progress.
  - Context menu: New Tab, Reload, Share Link, and Desktop Site toggle.
- **Search Overlay (`SearchModal`)**: Instant search overlay anchored directly above the software keyboard with clipboard URL paste chip, query resolution, and search engine integration.
- **Clean Links (URL Sanitization)**: 23 marketing and analytics tracking parameters (`utm_*`, `fbclid`, `gclid`, `mc_eid`, `vero_id`, etc.) stripped automatically without modifying legitimate query parameters.
- **Hardware-Accelerated WebView**: Modern standards support (DOM storage, zoom controls, isolated file/content access).
- **Fullscreen Video Handling**: Edge-to-edge HTML5 video playback with transient immersive system bars.

---

## Building Locally

### Prerequisites
- **JDK**: Java Development Kit 17 (e.g. Eclipse Temurin 17)
- **Android SDK**: Android SDK Platform 35 and Build-Tools 35.0.0
- **Android Device or Emulator**: Android 8.0 (API 26) or newer

### Setup & Compilation

Clone the repository and build the debug APK using the included Gradle wrapper:

```bash
git clone https://github.com/samyyy2311/archie.git
cd archie

# Build debug APK (applicationId: in.caffeinelabs.archie.debug)
./gradlew assembleDebug
```

The resulting package will be output to:
`app/build/outputs/apk/debug/app-debug.apk`

### Install on Device / Emulator

With an active adb connection:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## License

Apache License 2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE) for details.
