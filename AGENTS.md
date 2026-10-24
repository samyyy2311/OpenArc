# Archie Browser: Agent Guidelines

Instructions for AI coding agents and human contributors working on the Archie Browser codebase.

---

## 1. Project Overview & Philosophy

Archie Browser is an open-source native Android browser built with **Kotlin**, **Jetpack Compose**, and **Android WebView** (`android.webkit.WebView`).

### Core Engineering Principles
- **Least Possible Code (Ponytail Mindset)**: Solve problems with the minimum necessary code. Do not introduce abstractions, state wrappers, or libraries when standard Jetpack Compose APIs, standard Android APIs, or native WebView features can do it.
- **YAGNI**: Build for today's concrete requirement, not speculative future requirements.
- **Antislop Code Quality**:
  - No filler or narrative code comments (`// handle click`, `// state variables`, `// imports`). Comments are strictly reserved for non-obvious bug workarounds or hardware/OS constraints.
  - No generic AI design slop (no rainbow gradient soup, no full-page blur, no rounded pills on every element).
  - High contrast, keyboard accessible, and performance-focused.

---

## 2. Architecture & File Structure

```
app/
├── build.gradle.kts              # Application build config (in.caffeinelabs.archie)
└── src/main/
    ├── AndroidManifest.xml       # SingleTask MainActivity & intent filters
    ├── java/in/caffeinelabs/archie/
    │   ├── ArchieApplication.kt   # WebView debugging init
    │   ├── MainActivity.kt        # Edge-to-edge Compose host & back handler
    │   ├── ui/
    │   │   ├── components/
    │   │   │   ├── BottomOmnibox.kt   # Floating bottom omnibox pill with progress
    │   │   │   ├── BrowserWebView.kt  # AndroidView WebView wrapper
    │   │   │   ├── SearchModal.kt     # Fullscreen search overlay with paste chip
    │   │   │   ├── TabStackIcon.kt    # Dual-tilted card tab counter icon
    │   │   │   └── TabSwitcher.kt     # Horizontal card carousel tab switcher
    │   │   ├── state/
    │   │   │   ├── BrowserState.kt     # UI state data class
    │   │   │   └── BrowserViewModel.kt # StateFlow & navigation commands
    │   │   └── theme/
    │   │       ├── Color.kt           # Dark palette & semantic colors
    │   │       └── Theme.kt           # Material 3 theme wrapper
    │   └── util/
    │       └── UrlSanitizer.kt        # Tracking parameter stripper & domain resolver
    └── res/                           # App strings, themes, and drawables
```

---

## 3. Development Commands

```bash
# Compile debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run Android lint
./gradlew lintDebug
```

---

## 4. Code Standards

- **Kotlin**: Strict typing, idiomatic Kotlin, no raw `Any` casts.
- **Compose**: Functional composables with state hoisting and clear parameters.
- **Centralization**: Reusable theme colors, shapes, and utility functions in `ui/theme` and `util/`.
- **Identity**: Base package name is `in.caffeinelabs.archie`; debug builds use `in.caffeinelabs.archie.debug`.
- **Commits**: Plain English, active voice, and clear cause-and-effect without prefix noise or buzzwords. Include a brief `## Tested` note detailing real verification.

<!-- antislop:start -->
## antislop
For UI, copy, people, mobile layout, or code comments work, read `antislop.md` (core) and then the skill for the task:
- UI / visual: `skills/antislop-ui/SKILL.md`
- Copy & text: `skills/antislop-copywriting/SKILL.md`
- People: `skills/antislop-human/SKILL.md`
- Mobile / responsive: `skills/antislop-layoutmobile/SKILL.md`
- Code comments: `skills/antislop-code/SKILL.md`
Before starting, follow the core's "Two Usage Modes" section in strict order: explicit session instruction first, then global preference, then ask. A session instruction always wins. For a resolved mode, say `antislop active: <mode> (session override).` or `antislop active: <mode> (global preference).` once before presenting findings or making edits, using the actual mode and source. Acknowledging the user's request without naming the source does not replace this notice.
Only an explicit choice of antislop during or after selects a session mode. A request to review, audit, or avoid file edits does not select a mode; read the global preference in that case. Another skill's mode does not select antislop's mode.
If the mode is unresolved, ask during/after and end the response; wait for the answer before any UI review, planning, or concept. For read-only tasks, put the active-mode notice only at the start of the final answer, never in progress messages. For editing tasks, announce before the first edit and omit it from the final answer.
To update antislop later: download `antislop.md` again, or run `npx antislop-ai --update` if it was installed as skill folders.
<!-- antislop:end -->
