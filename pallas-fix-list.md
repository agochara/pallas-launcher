# Pallas Launcher: agent task list

Package: `com.pallas.launcher`. Stack: Kotlin, Compose, material3 1.4.0 (seen in build output; confirm in `libs.versions.toml`).

## Product rules (read first)

- Home, search and the app list are minimal and Olauncher-like: plain text, sparse, no icons, no badges, no cards.
- Settings and other secondary UI (long-press sheet, dialogs, app picker) use real Material 3 Expressive.
- Home look and Expressive look are separate systems. Do not mix them.
- Prefer deleting code to adding abstraction. Do not leave the old architecture under the new UI.
- Do not add features that were not asked for. Do not "improve" the home screen beyond the items below.

## Target interaction model

| Gesture | Result |
|---|---|
| Swipe up | Search opens, keyboard focused, empty list |
| Touch and drag along the edge strip | Niagara-style scrub through the alphabetical app list |
| Long swipe left/right | Switch home screens |
| Short swipe left/right | User-configurable action (any app, camera, dialer, none) |
| Swipe down | Notifications or quick settings |
| Long-press app | Options sheet (shortcuts, rename, alias, hide, add to screen, uninstall) |
| Long-press background | Settings |

Screens (multiple home screens, each with its own app list) are a core feature. Keep them.

## P0: Reachability, search, scrub list

1. **Wire up the app list.** `AlphabeticalAppDrawer` is unreachable: nothing calls `setAppDrawerOpen(true)`. Replace it with the scrub list in task 4.
2. **Search is apps-only.**
   - Remove `ShortcutSearchProvider`, `SystemActionSearchProvider`, `CommandSearchProvider` (including the always-appended web fallback), `ContactSearchProvider` and `ScreenSearchProvider` from the engine.
   - Delete the matching `SearchCategory` values and `SearchAction` variants.
   - Keep user aliases only as extra index entries. An alias result looks like the app itself, with no "Alias" badge or subtitle.
   - Empty query shows nothing. No recents.
   - Switching screens is not a search feature. Use the long swipe (default; ask the owner before changing).
3. **Build an in-memory index once, when the app list loads.**
   - Per app: normalized label, word list, initials.
   - Normalize the query once per keystroke.
   - Filter synchronously. No `LauncherApps`, contacts, content-provider or disk access while typing.
   - Remove the `delay(50)` debounce and the per-keystroke provider fan-out.
   - Cache launch counts in memory (`AppUsageTracker` currently hits `SharedPreferences` per result).
   - Replace `SearchResult` with `AppSearchResult(app, score, matchedRanges)`.
4. **Edge scrub list.**
   - Invisible vertical strip on one edge. Configurable Left / Right / Off. Default Right.
   - Touching and dragging along it opens the list. Nothing is drawn at rest. While a finger is down, show faint letters along the strip and a bubble with the current letter.
   - Rewrite the rail input. The current code uses a raw `awaitPointerEvent` loop that never consumes changes. Use `awaitEachGesture` and consume.
   - Snap to the nearest letter that has apps. Today the mapping spans all 27 slots, so empty letters show a bubble and go nowhere.
   - Haptic tick on each letter change.
   - Build the list from the same index as search. Strict alphabetical order, plain text rows, no icons, no letter chips, no "N opens" pills.
   - Hidden apps excluded. Decide grouping for non-Latin names (currently all land in "#").
   - After release: default is that the list stays open and back or a swipe dismisses it. Alternative: launch on release. Expose as a setting.
   - Long-press a row opens the same options sheet as home.
5. **Gesture conflicts.**
   - The strip must win pointer events in its zone. Place it above `GestureContainer` and consume there.
   - Android's back gesture starts at the same edge. Test on-device with gesture navigation. Consider `systemGestureExclusion` (capped at about 200dp of vertical extent per edge). If that is not enough, inset the strip. 3-button navigation has no conflict.
   - Keep the strip inside the home list's 32dp side padding so it does not steal label taps.
6. **Rank in tiers, with recent use deciding within a tier.**
   - Order: exact, label prefix, word-prefix or initials (one combined tier), substring, fuzzy.
   - Fuzzy needs 3+ characters, or 2 characters when the first letter matches the label's first letter. This makes "ff" and "tg" work.
   - Within a tier: frecency, then alphabetical. Frecency must decide "wa", otherwise Waze outranks WhatsApp.
   - User aliases outrank everything.
   - Remove category weights and the 15000/10000-style constants.
   - Remove the hard-coded default aliases (wa, calc, cam, yt) in `SettingsRepository`. They re-seed whenever the alias list is empty. Seed once as normal editable data, or drop.
   - Cap search results at about 8. The scrub list is uncapped.
7. **Search correctness.**
   - Hidden apps still appear in search, aliases, recents and `AppPickerSheet`. Filter once in `AppRepository`.
   - De-duplication keys on package, so apps with several launcher activities lose one. Key on `uniqueKey`.
   - `AliasSearchProvider` resolves with `label.contains(target)` and takes the first hit. Resolve by exact package or activity.
8. **Search UI.**
   - Text field and plain rows only. No badges, no subtitles, no per-letter gold highlight, no One Dark category colors.
   - Replace the "✕" glyph with an icon.
   - Replace the `delay(60)` focus hack.
   - Replace the dismiss gesture that fires on any single downward event over 30px. Use total drag distance or back.
   - Enter launches the top result.
9. **Tests, behavior-based.**
   - Cases: "fire" and "firefox" return Firefox first; "wa" returns WhatsApp; "yt", "ff", "calc"; hidden app absent; empty query empty; no-match empty.
   - Assert only apps are returned.
   - Latency measurement with a few hundred apps.
   - Delete tests that encode category-first ranking or test local data classes (`testAppCategoryAlwaysRanksHighest`, `testAlphabeticalFrequencySorting`, `testScreenSearch`).

## P1: Screens

Keep screens and fix what is weak.

10. **Removal and rename by key.** `removeAppFromScreen`, `updateAppLabel` and `updateAppLabelAcrossScreens` match on `uniqueKey || packageName`, so removing one app removes every clone or profile instance. Match `uniqueKey` only.
11. **Add-screen path stores a blank tag.** `addScreen(name, newContextTag.trim())` stores `""`. Normalize to null, as `renameScreen` does.
12. **Switching feedback.** No position indicators, just a smooth subtle beautiful transition. 
13. **Short-vs-long swipe by distance (75dp vs 150dp) is error-prone.** A slightly long short-swipe switches screens and a slightly short long-swipe launches the camera. Options: raise the gap, or make the short swipe edge-originated. Ask the owner; do not change silently.
14. **Screens manager UI.** Implement Material 3 Expressive. 
    - Move Left and Move Right text buttons become drag-to-reorder.
    - App up/down arrows become drag handles.
    - "+ Add Screen" and "+ Add App" become icon actions.
    - Hidden-apps rows show labels, not package names.
15. **Context tags.** Keep them as optional metadata (shown in the name pill). The default first-screen tag "General" is noise. Make it empty.
16. **Empty-screen prompt.** "+ Add apps to this screen" opens full settings. Open the app picker directly.
17. **Screens and profiles.** `ScreenApp.userHandle` can be null after a profile is removed, and then launches the primary profile. Hide or flag such entries.

## P2: Settings as Material 3 Expressive

18. **Confirm the Material 3 version and which Expressive APIs it has.** Bump the dependency only if needed. Do not upgrade everything blindly.
19. **Theme.**
    - Use `MaterialExpressiveTheme` with `MotionScheme.expressive()`, a real `Shapes` set and a full `Typography` set, for settings and secondary UI only.
    - `minSdk` is 33, so the `SDK_INT >= S` check always passes and dynamic color is always on. `DarkColorScheme` and `LightColorScheme` are dead. Decide dynamic color on or off explicitly and delete the dead schemes.
    - Remove the parallel `LauncherColors` system, or scope it clearly to home.
    - Delete the leftover `purple_*` and `teal_*` resources.
    - `AppPickerSheet` uses static `CardBackground` and `TextWhite`. Move it to the M3 scheme.
20. **Layout.**
    - Use `Scaffold` with `LargeFlexibleTopAppBar`.
    - Headed sections of list rows. Cards only where they earn it. Today every setting sits in a titled card that repeats its own label ("Status Bar" > "Hide Status Bar").
    - Terse page titles. Drop the feature-list subtitles.
21. **Controls.**
    - Connected button group or segmented buttons for 2–4 exclusive choices.
    - Switches for binary settings.
    - Slider persists on `onValueChangeFinished`, not on every `onValueChange`.
    - These replace `PillOption`, which has no ripple and no selection semantics.
22. **Details.**
    - Icons instead of glyphs ("◀ ▶", "+", "✕", "⚠️", "•").
    - Delete the About page and the "Navigation Guide" card.
    - Keep the subpage structure. Swap the 180ms tweens for the motion scheme and make back navigation predictive.
23. **Other secondary UI.** Apply the same system to the long-press sheet, dialogs and picker. Rename `ExpressiveActionTile` and the misleading "Material 3 Expressive" comments.
24. **Structure.** Split the 1,780-line `SettingsScreen.kt` into one file per page. Dedupe the rename-label dialog, which exists in `HomeScreen.kt` (pasted twice there) and in `SettingsScreen.kt`.

## P3: Functional bugs

25. **Clock and date do not refresh when the 24-hour or show-battery settings change.** `MainViewModel` updates only on time ticks and battery events. Collect the settings flow.
26. **Gesture targets.** Add an app picker for the short swipe left/right. `GestureAction.LaunchApp` exists but nothing in the UI sets it.
27. **Work profile.** Do not fall back to `Process.myUserHandle()` when a launch fails with `SecurityException`. If icons stay anywhere, use `LauncherActivityInfo.getBadgedIcon` and key the icon cache by `uniqueKey`, not package.
28. **Permissions.** Verify and probably remove CALL_PHONE, CAMERA, SET_WALLPAPER and QUERY_ALL_PACKAGES. Check whether `expandNotificationsPanel` (hidden API via reflection) still works at `targetSdk` 37.
29. **Release build.** Enable R8 (currently `optimization.enable = false`).
30. **Architecture.**
    - Replace the seven overlay booleans in `MainViewModel` with one sealed state.
    - Stop composables calling `viewModel.screenRepository` and `settingsRepository` directly.
    - Remove the duplicate back handling (`MainActivity` callback plus per-screen `BackHandler`s).

## P4: Cleanup and verification

31. **Delete dead code.**
    - `alignment` and `updateAlignment`
    - `KEY_PINNED_CONTACTS`
    - the unused `*Str` locals in `loadSettings`
    - `isScreenEditorOpen`
    - `timeUpdateJob`
    - the no-op `initDefaultScreenWithApps`
    - the second `enableEdgeToEdge` call
    - `ExampleUnitTest` and `ExampleInstrumentedTest`
    - contacts-related settings and storage, if contact search is gone
32. **Leave the home screen alone** apart from the clock fix, the hidden-app filter and the edge strip.
33. **Verify.** `./gradlew test assembleDebug`

## Questions and Answers for Alignment
Here is a little Questions and Answers session for the sake of alignment of motives:
"""What should Pallas optimize for? Speed, calmness, beauty, reduced distraction, discoverability, or some combination?
Simplicity. In Information Theory, the things you need to do very often (high probability) are assigned a command with lower number of bits. Similarly, the launcher should fade into the background and remove distractions, and make using the phone faster, simpler, easier. Aesthetics and elegance are very important. 

What should the Home screen represent?
The Apps the user actually wants to use, right there within a few clicks and swipes, without having to look through a pile of apps and icons or typing trying to find it. 

What exactly are Screens?Categories like finance / reading / work, or personal contexts like morning / work / evening?
Whatever the user wants to let it be. 

What should Search feel like?
“Search for an app” or “command line for my phone”? How far should commands like timer 20 go?
Mainly search for an app, with "command line for phone" style features limited to very few specific features like alarm. 
How much should Pallas learn automatically?
Should frequent apps/results rise to the top, or should the launcher remain almost entirely user-controlled?
The home screens should remain user controlled, while the search+app list should be a bit smart.
Describe one perfect 30-second interaction with Pallas.
Unlock → launch something → switch context → search for something obscure → execute an action. Tell Gemini exactly what you expect to happen.
Let's say you want to open your finance app. You unlock phone, long right left (to go right), click on your finance app. Boom. You want to set alarm -- you unlock, click the clock, boom, Clock app opens. You want to open camera? Unlock, small swipe left, boom. You want to open an app you don't usually open? Unlock, swipe up for search, type, let's say, "yt" and book, youtube opens. """


