# Pallas Launcher

**v2.0.0** · **Android 13+ (`minSdk 33`, `targetSdk 37`)** · **Kotlin & Jetpack Compose** · **MIT License**

Pallas Launcher is a minimalist, text-first Android home screen that pairs an editorial **Garamond** and **Inter** home experience with **Material 3 Expressive** settings, a one-handed edge-scrub app drawer, and a fast command bar backed by an offline computational, plotting, LaTeX, and conversion engine.

## Overview

- **Minimalist Multi-Screen Home**: Plain-text home screens with independent app lists, horizontal swipe switching, granular padding/alignment/font scaling, visual-width app sorting, wallpaper-aware contrast, cross-window blur, and work-profile support.
- **One-Handed Edge-Scrub Drawer**: Invisible thumb-zone edge rail (right, left, or off) with fisheye wave scrubbing, a floating section bubble, haptic ticks, and usage-tiered sorting.
- **Instant Search & Smart Command Bar**: Zero-I/O in-memory app search with aliases and frecency ranking, alongside customizable commands for web search (`q`), alarms (`al`), timers (`timer`), contact calls (`@` / `call`), weather (`w`), sunrise/sunset (`sun`), market quotes with sparklines (`st`), and Wolfram|Alpha (`wa`).
- **Offline Math, Statistics & Plotting**: Wolfram-Language-inspired engine covering numerical calculus and solvers, linear algebra, probability and heavy-tailed distributions, functional operators, and interactive 2D plots/charts with PNG export.
- **LaTeX Renderer & Built-in Guide**: Inline `tex` rendering powered by KaTeX fonts with Light/Dark/Clear PNG export, plus a searchable in-app **Math & Computational Engine Guide** with live command docs and LaTeX-typeset cheatsheets (Calculus, Linear Algebra, Probability & Stats, LaTeX).
- **Unit, Base & Currency Converter**: Offline-first physical units, number bases/Roman numerals, and 160+ world currencies with silent background FX sync (`convert` or inline expressions).
- **Gestures & JSON Backup**: Configurable swipe/tap gestures and human-readable JSON export/import for settings, screens, aliases, and contact nicknames (local usage history and API keys stay on-device).

## Command Bar Examples

```text
= Plot[{Sin[x], Cos[x]}, {x, -Pi, Pi}]
= NIntegrate[Exp[-x^2], {x, 0, 2}]
tex \int_0^\infty e^{-x^2}\,dx = \frac{\sqrt{\pi}}{2}
100 usd to inr
5 ft 11 in to cm
st nvda 1mo
w tokyo
al 7:30 am
@mom
```

## Building & Testing

**Prerequisites:** JDK 11+, Android SDK (API 37).

`app/build.gradle.kts` reads four Gradle properties for the `release` signing configuration (`LAUNCHER_STORE_FILE`, `LAUNCHER_STORE_PASSWORD`, `LAUNCHER_KEY_ALIAS`, `LAUNCHER_KEY_PASSWORD`). Set them in `~/.gradle/gradle.properties` or pass them with `-P` when building:

```bash
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew testDebugUnitTest
```

## License

Licensed under the **MIT License**.
