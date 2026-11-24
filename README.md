# Pallas Launcher

**v3.0.0** · **Android 13+ (`minSdk 33`, `targetSdk 37`)** · **Kotlin & Jetpack Compose** · **MIT License**

Pallas Launcher is a minimalist, text-first Android home screen that pairs an editorial **Garamond** and **Inter** home experience with **Material 3 Expressive** settings, a one-handed edge-scrub app drawer, Focus Mode & Macros, and a fast command bar backed by an offline computational, plotting, LaTeX, Notebook, Document Studio, and World Atlas suite.

## Overview

- **Minimalist Multi-Screen Home**: Plain-text home screens with independent app lists, horizontal swipe switching, granular padding/alignment/clock-date spacing/font scaling, visual-width app sorting, wallpaper-aware contrast, clickable date calendar shortcut, cross-window blur, and work-profile support.
- **One-Handed Edge-Scrub Drawer**: Invisible thumb-zone edge rail (right, left, or off) that centers dynamically for short lists, with fisheye wave scrubbing, floating section bubble, haptic ticks, and usage-tiered sorting.
- **Focus Mode (`focus`, `give up`)**: Via negativa distraction hiding with named profiles (selected home screens + allowed apps), zero-indicator stealth, optional Do Not Disturb / ringer mute sync, automatic daily schedules, persistent timer state, per-profile disabled inline commands (including inline `=` math and `tex`), and on-enter macro hooks.
- **Macros & Routines**: Bind custom single-word search triggers, swipe gestures, or time-of-day schedules to multi-step action chains covering stream volumes, ringer mode, media playback, DND, flashlight, brightness, auto-brightness, auto-rotate, screen timeout, quick settings panels (Internet, Wi-Fi, Bluetooth, NFC, Volume), Pallas settings toggles, timers/alarms, Quick Capture notes, delays, app launches, and built-in tools.
- **Instant Search & Smart Command Bar**: Zero-I/O in-memory app search with aliases and frecency ranking, hardware toggles (`fl`, `dnd`), direct in-app search (`!yt`, `!az`, `!sp`, `!gm`, `!play`, `!rd`, `!gh`, `!x`), smart `pallas` navigation (`pallas engine`, `pallas cheatsheets`, `pallas stats`, `pallas jensen`, `pallas focus`, `pallas macros`, etc.), indexed local file search (`#`), offline calendar agenda (`cal` / `show cal`), weather (`w`), sunrise/sunset (`sun`), markets with sparklines (`st`), Wolfram|Alpha (`wa`), Wikipedia (`wiki`), dictionary (`def`), world time (`tz`), quick capture (`n`), offline QR (`qr`), EMI (`emi`), and web/contact shortcuts (`q`, `@` / `call`).
- **Offline Math, Statistics, Plotting & Computational Notebook**: Wolfram-Language-inspired engine covering numerical calculus and solvers, linear algebra, probability and heavy-tailed distributions, functional operators, 2D plots/charts with PNG export, plus a multi-cell **Computational Notebook** with shared kernel state and symbol bar.
- **LaTeX Renderer, Document Studio & Guide**: Inline `tex` rendering and full **LaTeX Document Studio** (sectioning, theorem environments, multi-column preview, `.tex`/`.pdf`/`.png` export) powered by KaTeX fonts, alongside a deep-searchable in-app **Guide** (`Engine`, `Cheatsheets`, `Commands`) across mathematics, probability/tail risk, linear algebra, physics, engineering, discrete math, and more.
- **Offline World Atlas**: Vector Physical & Political world map with smooth pan/zoom gestures, country polygons, capital/city search, and elevation/region cards.
- **Unit, Date, Color & Currency Tools**: Offline-first physical units, 160+ currencies with silent background FX sync (`convert`), natural date arithmetic, and color/radix inspection (`#RRGGBB`, binary/octal/hex).
- **Gestures & JSON Backup**: Customizable swipe/double-tap gestures (launch apps, Focus profiles, Macros, Notebook, LaTeX Studio, or World Atlas) and JSON backup/restore (API keys stay encrypted on-device).

## Command Bar Examples

```text
= Plot[{Sin[x], Cos[x]}, {x, -Pi, Pi}]
= NIntegrate[Exp[-x^2], {x, 0, 2}]
tex \int_0^\infty e^{-x^2}\,dx = \frac{\sqrt{\pi}}{2}
focus study 90m + dnd
give up
!yt fourier transform
pallas jensen
show cal
#report
fl
dnd
100 usd to inr
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
