# Archie Browser (Android)

> [!NOTE]
> **Project Status: Experimental / In Development**
> Archie is an experimental, open-source native Android browser. It focuses on a clean, minimal mobile browsing experience with bottom-anchored navigation, fast search, native ad/tracker shielding, and integrated URL tracking sanitization.
>
> Archie uses native Android components with **Jetpack Compose** and **Android WebView** (`android.webkit.WebView`).

---

## Architecture & Tech Stack

Archie Android is engineered following Modern Android Architecture and Clean Architecture principles:

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
    │   ├── MainActivity.kt        # Edge-to-edge Compose host, engine & back handler
    │   ├── data/
    │   │   ├── local/
    │   │   │   └── TabDatabaseHelper.kt   # SQLite persistence for tabs, pins, and history
    │   │   ├── model/
    │   │   │   ├── HistoryEntity.kt       # Visited site record
    │   │   │   └── TabEntity.kt           # Tab record entity
    │   │   └── repository/
    │   │       ├── SettingsRepository.kt  # User preferences store
    │   │       └── TabRepository.kt       # Single source of truth for tab persistence
    │   ├── domain/
    │   │   └── shield/
    │   │       ├── BlockedCategory.kt     # Ads, tracker & telemetry classification
    │   │       └── ShieldEngine.kt        # Native domain matching engine
    │   ├── engine/
    │   │   ├── ArchieWebChromeClient.kt   # Progress, title, favicon, fullscreen video
    │   │   ├── ArchieWebViewClient.kt     # Request interception, navigation listener
    │   │   └── WebEngineManager.kt        # Safe WebView pooling, lifecycle & snapshots
    │   ├── ui/
    │   │   ├── components/
    │   │   │   ├── browser/
    │   │   │   │   └── BrowserWebView.kt  # AndroidView WebView host
    │   │   │   ├── omnibox/
    │   │   │   │   ├── BottomOmnibox.kt   # Floating omnibox pill with progress & shield badge
    │   │   │   │   └── OmniboxMenu.kt     # Contextual browser actions
    │   │   │   ├── search/
    │   │   │   │   └── SearchModal.kt     # Search overlay with history & clipboard chips
    │   │   │   ├── settings/
    │   │   │   │   └── SiteSettingsSheet.kt # Shield, desktop site, and force dark controls
    │   │   │   ├── switcher/
    │   │   │   │   ├── PinnedTabRow.kt    # Pinned favorites row
    │   │   │   │   ├── TabCard.kt         # Card with drag-to-dismiss & snapshot preview
    │   │   │   │   └── TabSwitcher.kt     # Horizontal card carousel tab switcher
    │   │   │   └── TabStackIcon.kt        # Dual-tilted card tab counter icon
    │   │   ├── state/
    │   │   │   ├── BrowserAction.kt       # Exhaustive MVI user intents
    │   │   │   └── BrowserUiState.kt      # Immutable UI state data class
    │   │   ├── theme/
    │   │   │   ├── Color.kt               # Dark palette & semantic colors
    │   │   │   └── Theme.kt               # Material 3 theme wrapper
    │   │   └── viewmodel/
    │   │       └── BrowserViewModel.kt    # Lean state holder; zero View/Context leaks
    │   └── util/
    │       └── UrlSanitizer.kt            # Tracking parameter stripper & domain resolver
    └── res/                               # App strings, themes, and resources
```

---

## Features & Capabilities

Archie delivers a fast, ergonomic mobile browsing experience:

- **Native Shield (Ad & Tracker Blocker)**: Zero-dependency in-memory domain classification engine (`ShieldEngine`) intercepting advertising and analytics requests before bandwidth is consumed.
- **Site Settings Bottom Sheet**: Per-tab site controls displaying live blocked item counts, desktop mode toggle, and force dark theme.
- **Multi-Tab Management & Snapshot Previews**: Multi-tab browsing with safe lifecycle pooling (`WebEngineManager`), downsampled bitmap page snapshot previews in `TabCard`, and background tab freezing.
- **Horizontal Card Tab Switcher**: Snapping card carousel displaying open tabs with titles, domains, favicons, swipe-up to dismiss, pinned favorites row, and an "Archive All" action.
- **Bottom Omnibox & Navigation**: Floating pill showing connection security lock, clean display domain, real-time loading progress, and shield counter badge.
- **Instant Search Overlay (`SearchModal`)**: Bottom-anchored search input above the software keyboard with clipboard paste chip, history autocomplete, and "Browse for me" smart query resolution.
- **Clean Links (URL Sanitization)**: Automatic stripping of marketing and analytics tracking parameters (`utm_*`, `fbclid`, `gclid`, `mc_eid`, etc.).
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
