# Wakeroo pre-launch feature plan

Scope: the 13 features plus the home-screen upgrade agreed on 20 October 2026, built on top of the working v1.0 (engine, 5 missions, onboarding, home, editor, stats, settings, Pro, widget, launch intro).

Principles that apply to every feature:

- **Reliability first.** Nothing new may touch the ring path unless it has a fallback. Sensor and NFC missions always offer the backup mission after 60 seconds or 3 failures.
- **No ads on the alarm path.** That includes sleep sounds, bedtime, the morning briefing and achievements.
- **Optional by default where reviews complained.** The morning briefing can be turned off in one tap; rotation and escalation are opt-in per alarm.
- **Policy-safe.** No location permission, no Accessibility, no Device Admin. New permissions: `ACTIVITY_RECOGNITION` (steps, asked only when a user picks that mission) and `NFC` (normal permission, no prompt).
- **Motion system.** Springs from `WakerooMotion`, one hero motion per screen, everything respects `LocalReducedMotion`.

## Build order

| # | Milestone | Contents | Estimate |
|---|---|---|---|
| M1 | Foundations | Data model v2 + Room migration, strings moved to `strings.xml`, settings additions | 1 day |
| M2 | New missions | Steps, Memory tiles, NFC tag, Type your plan | 3 days |
| M3 | Sound library | 12 original tones, categories, preview, rotation, escalation | 1.5 days |
| M4 | Sleep | Sleep sounds player, bedtime reminder, Bedtime card | 2 days |
| M5 | Morning briefing | Weather (Open-Meteo), quote, plan, streak; off switch | 1.5 days |
| M6 | Achievements | Badges, 3D medals, unlock celebration, weekly recap, share card | 2 days |
| M7 | Pro: Harder Mode + Themes | Mission-gated off/delete/snooze; ring-screen themes incl. custom photo | 2 days |
| M8 | Home upgrade | 3D orbit clock, Bedtime card slot, 7-day streak strip, sky-tinted cards | 2 days |
| M9 | Quick Settings tile | Power-nap tile | 0.5 day |
| M10 | Languages | 8 languages, RTL, per-app language picker | 2 days |
| M11 | Regression | Full emulator reliability matrix, real-device pass, screenshots | 1.5 days |

Total: about 19 working days. M1 comes first because every later feature adds strings and fields; doing translations last is only cheap if strings were extracted first.

---

## M1. Foundations

**Data model (Room v1 → v2, real `Migration`, no destructive fallback):**

| Where | New field | Default | Used by |
|---|---|---|---|
| `Alarm` | `hardMode: Boolean` | false | M7 |
| `Alarm` | `soundMode: Single / Rotate / Escalate` | Single | M3 |
| `Alarm` | `soundCategory: Gentle / Energetic / Extreme` | Energetic | M3 |
| `Alarm` | `theme: String?` (null = app default) | null | M7 |
| `MissionType` | `Steps`, `Memory`, `Nfc`, `Plan` | — | M2 |
| `MissionConfig` | `nfcTagId: String?`, `nfcLabel: String?`, `planText: String?` | null | M2 |
| `Settings` | `sleepGoalMinutes` (480), `bedtimeReminder` (on), `windDownMinutes` (30) | — | M4 |
| `Settings` | `briefing` (on), `weatherCity` (name, lat, lon, null = no weather) | — | M5 |
| `Settings` | `ringTheme` (`night`), `customThemePath` | — | M7 |
| `Settings` | `achievements: Map<id, unlockedAt>`, `lastRecapWeek` | empty | M6 |

Missions are stored as JSON, so new mission fields need no migration. New `Alarm` columns get `ALTER TABLE` statements in `MIGRATION_1_2`, covered by a `MigrationTestHelper` test.

**Strings:** move every user-facing string from Kotlin into `res/values/strings.xml` (plurals for counts such as "3 problems"). This is the prerequisite for M10.

---

## M2. New missions

All four plug into the existing `MissionFlow` (progress bar, 3D card flip between missions, backup mission, emergency exit) and into the mission picker with a "Try it now" practice run where possible.

### 1. Steps

- **What the user sees:** a 3D running track, a ring of footprints that fills as they walk, the count in the middle, and a "keep walking" nudge. Target 20–200 steps (default 40).
- **How it works:** `Sensor.TYPE_STEP_DETECTOR` (about 2 s latency). If no step event arrives within 20 s, it switches to an accelerometer step heuristic; if that also fails, the backup mission is offered. Steps must be continuous: 10 s without a step and the count drains slowly, like Shake.
- **Permission:** `ACTIVITY_RECOGNITION` is requested in the mission picker when Steps is chosen, never at ring time. Without it, the mission is swapped for the backup up front.
- **Review fix:** Alarmy's top steps complaint is "doesn't count, had to uninstall". The fallback chain and the always-available emergency exit prevent that trap.
- **Tests:** unit-test the heuristic step detector with recorded accelerometer traces; emulator test with the virtual sensors panel.

### 2. Memory tiles

- **What the user sees:** a 3×3 or 4×4 grid of tiles. A pattern lights up (tiles flip in 3D to show gold faces), then flips back; the user taps the same tiles. Each round adds a tile. Wrong tap: the grid shakes and the round replays.
- **Settings:** grid size (3×3 / 4×4), rounds (3–6). Difficulty presets Easy / Medium / Hard.
- **How it works:** pure Compose. `graphicsLayer { rotationY }` per tile with spring flips; pattern generated with a seeded `Random` so a practice run and the real one differ.
- **Tests:** unit-test pattern generation and answer checking.

### 3. NFC tag

- **What the user sees (setup):** "Hold your phone to the sticker." A pulsing NFC wave animation; on read, the tag "locks in" with a gold ripple, then a place chip like the QR mission (bathroom mirror, kettle, front door).
- **At ring time:** "Tap your phone on the sticker by the mirror." The same wave; success when the registered tag UID is read.
- **How it works:** `NfcAdapter.enableReaderMode` in `RingActivity` and the setup dialog, matching the tag UID (hex). `<uses-feature android:name="android.hardware.nfc" android:required="false"/>` so phones without NFC can still install; the mission tile is hidden there.
- **Edge cases:** NFC switched off: show "Turn on NFC" with a deep link to `Settings.ACTION_NFC_SETTINGS`, plus the backup mission after 60 s. Locked before first unlock: works (the UID is in the direct-boot mirror).
- **Marketing hook:** "Stick it by the mirror. Tap to stop." Cheap NTAG213 stickers cost cents.

### 4. Type your plan

- **What the user sees:** "What's the first thing you'll do today?" They type at least three words on the in-app keyboard (or retype the plan they saved the night before, if set in the editor). The plan then shows big on the morning briefing.
- **How it works:** reuses the custom `LetterPad`. Minimum 3 words / 12 letters; rejects keyboard mashing (a simple vowel-ratio and repeated-character check).
- **Why:** reviewers asked to "type something specific like a to-do"; it also feeds the briefing (M5).

---

## M3. Sound library, rotation and escalation

- **12 original tones**, synthesised by `tools/make_tone.py` (no licensing issues), encoded to OGG with ffmpeg to keep the APK small (about 40–80 KB each):
  - **Gentle:** Dawn Chimes, Soft Marimba, Birdsong Bells, Warm Pads
  - **Energetic:** Wakeroo Rise (current), Bright Pulse, Steel Drum Run, Fanfare
  - **Extreme:** Klaxon, Siren Sweep, Rooster Riot, Alarm Bells
- **Picker UI:** a sound sheet with category tabs; each tone is a card with an animated waveform; tap to preview at the alarm's volume with a 3-second fade. Plus "Phone tone" and "Your music" as today.
- **Rotation (`soundMode = Rotate`):** picks a random tone from the chosen category each ring, never the same twice in a row. Stops users getting used to one sound.
- **Escalation (`soundMode = Escalate`):** starts with the chosen gentle tone; if there's no screen interaction after 60 s, it crossfades to an Extreme tone at full volume. The ring screen shows a small "getting louder in 0:42" hint.
- **Engine changes:** `AlarmPlayer` gets crossfade (two `MediaPlayer`s with volume ramps) and the existing fallback chain stays: chosen tone → bundled default → system tone.

---

## M4. Sleep: sounds, bedtime reminder, Bedtime card

### 6. Sleep sounds

- **Sounds:** Rain, Ocean waves, White noise, Pink noise, Brown noise, Fan, Night crickets. Noises are generated live in code (`AudioTrack` streaming filtered noise), so they cost no APK space and never loop audibly; rain and waves are noise plus modulation; crickets are synthesised chirps.
- **Player:** a full-screen "Sleep" sheet with a slowly breathing moon, sound tiles (3D press), a mix of up to 2 sounds with individual volume, and a timer (15, 30, 45, 60, 90 min or "until alarm") that fades out over the last 2 minutes.
- **How it runs:** `SleepSoundService`, a `mediaPlayback` foreground service with a notification (pause, stop, time left). Audio uses `USAGE_MEDIA` so it obeys media volume, and it stops automatically 1 minute before any alarm so the alarm is never masked.
- **Free vs Pro:** 4 sounds free, all 7 plus mixing with Pro.

### 7. Bedtime reminder

- **Logic:** bedtime = next alarm − sleep goal (default 8 h). A notification fires at bedtime − wind-down (default 30 min): "Bed by 10:45 to get 8 hours before your 6:45 alarm." Actions: "Start sleep sounds", "Snooze 15 min".
- **Scheduling:** recomputed every time alarms change; uses inexact `setAndAllowWhileIdle` (a reminder doesn't need exact timing, and exact-alarm quota stays reserved for real alarms). No reminder if the next alarm is under 5 hours away.
- **Settings:** sleep goal (6–10 h), wind-down (0–60 min), on/off.

### Bedtime card (Home)

Shows from 6 PM until the alarm: "Bed by 10:45 · 8 h sleep", a moon that rises as bedtime approaches, and a "Sleep sounds" button.

---

## M5. Morning briefing (optional)

- **Where:** replaces the current "Good morning" screen when on; one switch in Settings and a "Don't show this again" link on the screen itself (the Alarmy complaint: "I didn't ask for weather and quotes").
- **Content, in order:** greeting and time, today's plan (from the Type-your-plan mission or the alarm label), weather (temperature, high/low, rain chance, an animated weather glyph), streak with the day's medal progress, and one short quote. Auto-closes after 12 s; tap to close.
- **Weather:** [Open-Meteo](https://open-meteo.com) forecast API, free and keyless. The user picks a city once (Open-Meteo geocoding search) instead of granting location permission. Cached for 3 hours; fetched the evening before and refreshed when the alarm is dismissed, so the briefing never waits on the network. Offline: the weather row is simply hidden.
- **Quotes:** about 120 bundled, short, non-religious, translated with the app.
- **Data safety:** only the chosen city's coordinates are sent to Open-Meteo; no identifiers.

---

## M6. Achievements and weekly recap

- **Badges (first set):** First Rise; 3-, 7-, 14-, 30- and 100-day streaks; Early Bird (5 wake-ups before 6 AM); No Snooze Week; Mission Master (all mission types used); Night Owl Reformed (average wake time 30+ min earlier than week one); Spot On (10 QR/NFC/photo missions).
- **3D medals:** drawn on `Canvas` as a coin with rim, embossed icon and a moving specular shine; spins on `rotationY` with a physics fling, and tilts with the phone (`rememberDeviceTilt`). Locked medals are dark silhouettes.
- **Unlock moment:** on the morning briefing or Good-morning screen, the medal flies in, spins and lands with a gold particle burst and a haptic.
- **Weekly recap:** every Monday, a card on Home: mornings won, average wake time, fastest mission, and the week's medal.
- **Share card:** a 1080×1350 image rendered from Compose (`rememberGraphicsLayer().toImageBitmap()`) with the streak, medal and the Wakeroo mark, shared through the Android share sheet. This is free word-of-mouth marketing.
- **Logic:** `AchievementEngine`, a pure function over the ring log (unit-tested), run after each ring; newly unlocked IDs are saved with their date in settings.

---

## M7. Pro: Harder Mode and Themes

### 10. Harder Mode (per alarm, Pro)

- Turning the alarm off, deleting it, skipping the next one or moving it more than 2 hours later requires completing one of its missions first (shown in a dialog using `MissionFlow`).
- Snooze requires a quick mission (one easy math problem).
- The emergency exit stays (people do have emergencies) but the hold becomes 60 seconds and it's logged.
- Safety valve: Harder Mode can't be switched on for an alarm less than 10 minutes away, so nobody locks themselves in by accident.

### 11. Themes (ring screen and home tint)

- **Built-in:** Night Sky (current), Aurora (an AGSL shader on Android 13+, animated gradient fallback below), Ocean Dawn, Desert Sunrise, Minimal Black (OLED, battery-friendly).
- **Custom photo:** chosen with the system Photo Picker (no storage permission), copied into app storage, shown with a dark gradient scrim so the clock and button keep 7:1 contrast.
- **Free:** Night Sky and Minimal Black. Pro: the rest and custom photo.
- Theme previews in Settings are live miniature ring screens.

---

## M8. Home upgrade

- **3D orbit clock (hero):** a 24-hour ring drawn in perspective (tilted about 60° and turning slowly with the phone's tilt). The sun marker sits at the current time; the alarm sits as a gold marker; the arc between them glows and shows "in 7 h 20 min". Tapping it flips the card to the next-alarm details.
- **Bedtime card** (from M4) between the hero and the quick chips in the evening.
- **7-day streak strip:** seven small suns that fill gold for won mornings, coral for missed ones; tapping opens Stats.
- **Alarm cards tinted with their own sky:** a subtle gradient matching the alarm's hour (navy for 4 AM, peach for 7 AM) with a tiny sun or moon glyph, so the list reads at a glance.
- **Weekly recap card** (from M6) on Mondays.
- All new elements join the existing staggered entrance after the launch intro.

---

## M9. Quick Settings tile

- `PowerNapTileService`: tap starts a 20-minute nap alarm (using the user's last mission); tap again while a nap is set cancels it; the tile label shows "Nap · 18 min left". Long-press opens Wakeroo.
- Uses the same `AlarmRepository.save` path, so it is scheduled exactly and mirrored for direct boot.

---

## M10. Languages

- **Languages:** Hindi, Urdu, Spanish, Portuguese (Brazil), Indonesian, Arabic, Turkish, French.
- **RTL:** Urdu and Arabic get mirrored layouts; check every screen, keep clocks and numbers left-to-right, use auto-mirrored icons.
- **Per-app language picker:** `android:localeConfig` plus a language row in Settings (Android 13+ uses the system picker).
- **Process:** translations drafted by me, then a native-speaker check for at least Hindi, Urdu, Spanish and Portuguese before launch (or the closed testers).
- **Fonts:** Outfit and the Inter subset cover Latin; Hindi, Urdu and Arabic fall back to the system Noto fonts. The Inter subset is extended for Turkish and Vietnamese-style diacritics already in Latin Extended.
- **Store listings:** translated title, short description and screenshots captions for each language (done at launch prep).

---

## M11. Regression and launch readiness

- Re-run the full reliability matrix from the build plan (Doze, standby buckets, reboot before unlock, restart mid-ring, app update mid-ring, time zone change, DND, volume 0, deleted custom sound, camera denied) plus new rows:
  - Escalation crossfade works while the screen is off.
  - Sleep sounds stop before an alarm and never mask it.
  - NFC off / no NFC hardware offers the backup.
  - Steps permission denied swaps to the backup.
  - Harder Mode can't trap: emergency exit works.
- Real-device pass on at least one Samsung or Xiaomi phone.
- Update the Data safety notes (Open-Meteo city lookup, NFC tag ID stored locally) and Play declarations (`ACTIVITY_RECOGNITION` justification).

## Deliberately not included

- Blocking power-off or uninstall (Accessibility/Device Admin): banned use under Play policy and an account risk; the restart re-ring covers the same cheat.
- Sleep tracking and snore recording: reviewers call it bloat; Sleep as Android owns it.
- Camera-counted squats/push-ups, Wear OS, calendar/shift sync: planned for v2.

---

# Addendum: changes after reviewing Alarmy's walkthrough (iOS, recorded 20 Oct 2026)

## What Alarmy shows, screen by screen

| Area | What it has | Our response |
|---|---|---|
| **Onboarding (4 steps, about 30 s)** | 1/4 set alarm time on a wheel; "Restore your previous alarms?" sheet; permission primer with a mock dialog and a finger tapping Allow. 2/4 choose an alarm wallpaper (video wallpapers: motivational text like "WAKE UP!", "YOU LAZY", space, cats) with a full-screen preview that "rings with video sound". 3/4 choose a sound (tabs: Loud, Classic, Nature, Trending) plus a volume sheet with "Gentle wake-up 30 s" and Preview. 4/4 choose a mission (Math, Typing, Find Color Tiles, Household Item Hunt (AI), Shake, Off) with a demo-video sheet. Then a mascot loader "Your alarm is almost ready: Picking your sound 28%… Finding the right mission 77%… All set!" | **Adopt the structure**: a fast setup that ends with a real alarm. Do it our way: 3D scenes, live previews, no tracking prompt |
| **Monetisation in onboarding** | App-tracking prompt (a primer, then the system dialog), a full paywall ("It is not charged right now. Free trial 7 days", yearly/monthly/lifetime), then "Sale 76%" confetti, "Only now 76% off", and "Your discount will be lost if you exit" | **Reject.** These are the dark patterns reviewers hate. No paywall or tracking prompt in onboarding |
| **Home** | Red "Background App Refresh is Off · Fix" banner, "Overslept AGAIN? Try our new mission" promo, "Ring in 3 h 23 min", alarm cards, red + button, 5-tab bottom bar, permanent ad strip | Adopt the **Fix banner** (driven by our health check) and **bottom navigation**. No promos, no ad strip |
| **Sleep tab** | Snore/sleep tracking with the mic, "place phone by pillow, keep charging", night clock, sleep report | Keep sleep *tracking* out (reviewers call it bloat); our Sleep tab hosts sleep sounds and bedtime |
| **Morning tab** | Weather (location prompt, hourly strip), My pet, Morning feeling (mood log: Annoyed, Tired, No feeling, Calm, Refreshed), Daily Tarot, points, daily quotes (themes, save, share, swipe for more), large ads | Adopt **weather, mood log, and quotes with themes, save and share**; add a **morning routine checklist**. Skip tarot and points; no ads |
| **Report tab** | Wake-up report (average wake-up time, average time to wake), calendar daily report, sleep report, "Habit alarm" | Fold into our **You** tab (stats, calendar, achievements) |
| **Settings** | Sign in (Apple), points, Pro, prevent uninstall, advanced alarm settings (**mission time limit 20 s**), theme (dark/light/system), **sound output (phone vs Bluetooth)**, notification settings (**weather notification, "no alarm for tomorrow" reminder**, promos), language, battery saving, "Alarm didn't ring? Alarm optimization" banner | Adopt the mission time limit, sound output, app theme, the two useful reminders and the optimisation banner. Skip sign-in, points, promos and prevent-uninstall |

## New features added to the plan

| # | Feature | Notes |
|---|---|---|
| 14 | **Onboarding setup wizard** | Replaces the current onboarding; spec below |
| 15 | **Bottom navigation** | Alarms · Sleep · Morning · You; Settings as a gear in the top bar |
| 16 | **Morning tab** | The briefing (#8) becomes a full tab: hourly weather strip, quote of the day (themes, save, share as an image, swipe for more), morning feeling log, routine checklist (3–5 habits you tick after waking), streak and today's medal |
| 17 | **Item Hunt mission (on-device AI)** | "Find a cup", "Show a toothbrush": a random everyday object from a curated list of about 40 that ML Kit image labeling recognises reliably. No setup needed; backup after 60 s. Our version of Alarmy's AI "Household Item Hunt" |
| 18 | **Motion scenes** | Replaces plain themes (#11): animated alarm backgrounds rendered in Compose, with no video files. Night Sky, Into Space (the intro's planet), Aurora, Ocean Dawn, Kinetic "WAKE UP" typography, Minimal Black. 3 free; the rest plus custom photo with Pro. Each scene suggests a matching sound |
| 19 | **Mission demo previews** | Every mission tile in the picker plays a short looping animation of how it works (like Alarmy's demo video), alongside the existing "Try it now" |
| 20 | **Mission time limit** | Settings › Advanced: if you stop interacting during a mission for 10/20/30/60 s, full volume returns and the mission resets. Closes the "lie there with the screen open" loophole |
| 21 | **Sound output** | "Always ring on the phone speaker" (default on), so a dead Bluetooth speaker or forgotten earbuds never silence the alarm; an option to also play on connected audio |
| 22 | **Evening reminders** | Opt-in "No alarm set for tomorrow" (8 PM) and a morning weather notification, on the same channel as the bedtime reminder (#7) |
| 23 | **Home Fix banner** | When the health check is red or amber, a prominent banner at the top of Home ("Battery saver may block alarms · Fix") becomes the main signal instead of the small health dot |
| 24 | **Wake-up calendar** | In the You tab: a month calendar where each day shows a sun (won), a cloud (missed) or nothing; tap a day for that morning's details |
| 25 | **App light theme** | Dark / Light / System for the app (ring screens stay dark) |
| 26 | **Restore welcome** | If Android restored alarms from backup on a new phone, onboarding opens with "Welcome back: we restored your 3 alarms" and skips to the permission step |

## Onboarding wizard spec (#14)

Each step is one screen with a "1 of 5" progress bar and Back. The launch intro plays first.

1. **"When do you want to wake up?"** The 3D drum wheel (default 7:00) with Weekdays / Every day / Once chips and a live "Rings in 7 h 20 min".
2. **"Pick your wake-up scene"** A gallery of Motion scene cards playing live. Tapping one opens a full-screen preview of the real ring screen with your time (and the sound playing quietly), then "Use this scene".
3. **"Pick your sound"** Tabs Gentle / Energetic / Extreme / Phone; tap to preview; a sheet for volume and "Gentle start" with a Preview button.
4. **"Choose your wake-up mission"** The mission list with looping demo previews; tapping one opens a sheet with the demo, a difficulty choice and "Try it now". QR, NFC and photo setup can be skipped ("I'll set it up tonight") and fall back to math until done.
5. **"Make sure it rings"** One screen with three rows that turn green as they're granted: Notifications (system dialog), Full-screen alarm (settings deep link, only if needed), Battery (per-brand guide). An animated mock shows exactly what to tap, like Alarmy's primer.
6. **"Building your alarm…"** The Wakeroo sun rises behind a progress ring: "Setting your time… Tuning your sound… Loading your mission… All set!" (about 2.5 s, covering the real work of saving and scheduling). Ends with a medal-style card: "Your first alarm is ready: 7:00 AM, weekdays".
7. **Optional test.** "Want to test it now? It rings in 1 minute." Then Home, which assembles with the staggered entrance and shows the new alarm card glowing.

No paywall, no tracking prompt, no account. About 40 seconds in total, ending with a working alarm.

## Revised build order

| # | Milestone | Features | Days |
|---|---|---|---|
| M1 | Foundations | Data model v2, strings extraction, **bottom navigation** (#15) | 1.5 |
| M2 | **Onboarding wizard** | #14, #26, permission primer | 2.5 |
| M3 | Missions | Steps, Memory tiles, NFC, Type your plan, **Item Hunt** (#17), **demo previews** (#19), **mission time limit** (#20) | 4 |
| M4 | Sound | Library, rotation, escalation, **sound output** (#21) | 2 |
| M5 | **Motion scenes** | #18 (absorbs Themes, #11) | 2 |
| M6 | Sleep tab | Sleep sounds, bedtime reminder, **evening reminders** (#22) | 2.5 |
| M7 | **Morning tab** | Briefing, weather, quotes, mood log, routine checklist (#16) | 2.5 |
| M8 | You tab | Stats, **calendar** (#24), achievements, recap, share card | 2.5 |
| M9 | Pro | Harder Mode, Pro gating for scenes and sounds | 1 |
| M10 | Home upgrade | Orbit clock, streak strip, tinted cards, **Fix banner** (#23), **light theme** (#25) | 2.5 |
| M11 | Quick Settings tile | Power nap | 0.5 |
| M12 | Languages | 8 languages, RTL | 2 |
| M13 | Regression | Reliability matrix, real device, screenshots | 1.5 |

Total: about 27 working days.

---

# Implementation details for features 14–26

Same format as features 1–13: what the user sees, how it's built, data changes, edge cases, tests.

## 14. Onboarding setup wizard

- **Structure:** `feature/onboarding/` is rewritten as a `HorizontalPager` driven by an `OnboardingViewModel` that holds a draft `Alarm` (time, days, scene, sound, mission) plus permission state. Steps share one `StepScaffold` (progress bar "n of 5", title, content, primary button, Back). Pages turn with the existing cube transition.
- **Step 1, time:** reuses `DrumWheel`; `Rings in …` uses `NextTriggerCalculator`.
- **Step 2, scene:** a `LazyRow`/grid of live `SceneThumbnail`s (the same composables as the ring screen at reduced size, 30 fps cap). Tap opens a full-screen `RingScreen` preview in "preview mode" (no service, sound preview at 40% via a short-lived `MediaPlayer` on `USAGE_MEDIA`).
- **Step 3, sound:** the M4 sound sheet embedded as a page.
- **Step 4, mission:** the M3 picker list with demo previews; QR/NFC/photo setup offers "Set it up tonight", which saves the mission with `needsSetup = true` (math runs at ring time until done, and Home shows a reminder chip).
- **Step 5, permissions:** a single screen with three `PermissionRow`s (Notifications → `POST_NOTIFICATIONS`; Full-screen → `ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT`, only shown if `canUseFullScreenIntent()` is false; Battery → OEM guide). Each row re-checks on resume and animates to green with a `Confirm` haptic. An animated mock (a fake dialog with a finger tapping "Allow") plays above the row being requested.
- **Step 6, building:** a 2.5 s sequence; the alarm is saved through `AlarmRepository.save` in parallel, and the sequence waits for it to finish (it never fakes progress past real work).
- **Step 7, test:** "Ring in 1 minute" calls `scheduleTest`; "Later" goes Home.
- **Data:** `Settings.onboardingVersion` (Int) instead of a boolean, so a future onboarding change can re-show only new steps. Existing users (onboarding v1 done) skip the wizard.
- **Edge cases:** process death mid-wizard restores the step and draft (`SavedStateHandle`); denying notifications keeps the row red with "Open settings"; Back from step 1 exits the app.
- **Tests:** ViewModel unit tests (draft building, step gating); a Compose UI test that walks the wizard and asserts one alarm exists and is scheduled.

## 15. Bottom navigation

- **What the user sees:** a floating, rounded bar with four tabs: Alarms (clock), Sleep (moon), Morning (sun), You (medal). The selected tab's icon morphs from outline to filled and a glowing pill slides under it with a spring. Settings is a gear on each tab's top bar.
- **How:** a root `MainScaffold` with a `NavHost` per tab (`saveState`/`restoreState` so each tab keeps its scroll position). Editor, Pro, Settings and mission setup stay full-screen destinations above the tab host, so they keep the shared-element morphs.
- **Hides** while scrolling down long lists and reappears on scroll up (`nestedScroll` connection), and is always hidden on ring/mission screens.
- **Tests:** navigation tests for tab switching, back-stack per tab, deep link to Settings › Health.

## 16. Morning tab

Sections, top to bottom, each a card that joins the staggered entrance:

1. **Greeting:** "Good morning, 6:34" (or "Tomorrow's alarm: 7:00" in the evening) over the current scene's sky.
2. **Weather card:** current temperature, condition glyph (animated sun/cloud/rain drawn in Canvas), high/low, rain chance, and a horizontally scrolling 24-hour strip (temperature and an icon per hour). City chosen once via search; a "Use another city" link. Data from Open-Meteo forecast (`hourly=temperature_2m,precipitation_probability,weather_code`, `daily=temperature_2m_max,temperature_2m_min`), cached in DataStore for 3 hours.
3. **Quote of the day:** a large typographic card; swipe left for another (a deck of 3D-tilted cards); bookmark to save; share renders the card as an image. Themes: Positivity, Productivity, Calm, Hard times, Health & fitness, Learning (no religion category, to stay neutral). About 150 bundled quotes per language.
4. **Morning feeling:** five faces (Exhausted, Tired, Okay, Calm, Refreshed) as springy 3D buttons, one tap per day; also offered on the Good-morning screen. History in the You tab correlates with wake time ("You feel refreshed more often on 8 h nights").
5. **Routine checklist:** 3–5 user-editable habits (defaults: Drink water, Open the curtains, Stretch for 2 minutes). Each tick plays a check animation; completing all gives a small celebration and counts toward an achievement.
6. **Streak and today's medal progress.**

- **Data:** Room v3 tables `mood_log(date, mood)`, `routine_item(id, title, order)`, `routine_log(date, itemId)`; `saved_quote(id)`. Quotes ship as a JSON asset per locale.
- **Network:** only Open-Meteo, only when the tab or the briefing needs fresh data; offline hides the weather card.
- **Tests:** Open-Meteo response parsing (fixture JSON), quote-of-the-day determinism per date, routine completion logic.

## 17. Item Hunt mission (on-device AI)

- **What the user sees:** "Find something like this: a cup ☕." A camera viewfinder with a pulsing target; when the object is recognised, the frame snaps gold and the mission advances. Count 1–3 objects per mission.
- **How:** bundled ML Kit image labeling (already in the app). A curated list of about 40 objects with their ML Kit label names and a confidence threshold each (e.g. Cup ≥ 0.75, Toothbrush ≥ 0.6, Shoe ≥ 0.7, Book ≥ 0.7, Plant, Bottle, Spoon, Keyboard…). Objects are only picked if they've been recognised reliably in testing.
- **Anti-cheat:** the target is random each morning; labels must persist across 3 consecutive analysed frames (stops a single lucky frame).
- **Fallback:** "Skip this object" once per mission (picks another), then the standard backup after 60 s.
- **Tests:** label-matching logic with recorded label sets; manual device test for each listed object.

## 18. Motion scenes

- **Scenes (all drawn in Compose, no video):**
  - **Night Sky** (current, free): stars, breathing sun glow.
  - **Into Space** (free): the intro's planet and sunrise, slowly rotating.
  - **Minimal Black** (free): OLED black, a thin animated ring.
  - **Aurora** (Pro): AGSL `RuntimeShader` flowing curtains on Android 13+, animated gradient fallback.
  - **Ocean Dawn** (Pro): layered parallax waves with tilt.
  - **Kinetic "WAKE UP"** (Pro): bold typography that slams in, scales and shakes with the beat of the alarm tone.
  - **Custom photo** (Pro): Photo Picker image with a slow Ken Burns zoom and a dark scrim.
- **API:** `interface Scene { @Composable fun Background(state: SceneState) }`, where `SceneState` carries time, tilt, ring intensity (escalation level) and reduced-motion. The ring screen, mission screens, onboarding previews and Settings previews all use the same composables.
- **Performance:** each scene is capped at 60 fps, uses `drawBehind`/`graphicsLayer` only (no recomposition per frame), and is profiled on a low-end emulator profile; battery cost on the ring screen is irrelevant (minutes), but previews pause when off screen.
- **Data:** `Alarm.theme` (scene id, null = app default) and `Settings.ringTheme`.

## 19. Mission demo previews

- A `MissionDemo(type)` composable per mission: a 3–4 s looping mini animation (a hand shaking a phone, tiles flipping, a phone tapping a sticker, a camera finding a cup, digits being typed). Built from simple vector shapes and the app's motion tokens; no video files.
- Shown in the mission picker tiles (on focus) and in the mission detail sheet; paused when not visible.

## 20. Mission time limit

- **Setting:** Settings › Advanced › "If I stop during a mission": 10 / 20 (default) / 30 / 60 s.
- **How:** `MissionFlow` tracks the last interaction (`onActivity`); after the limit, it calls a new `RingService.ACTION_IDLE` that cancels ducking and returns to full volume, and the current mission resets its progress with a shake animation and a "Keep going!" toast.
- **Edge cases:** camera missions count frame analysis as activity only when the target is partly matched (so pointing at a wall doesn't keep the alarm quiet).

## 21. Sound output

- **Setting:** "Always ring on the phone speaker" (default on) and "Also play on headphones/Bluetooth" (default off).
- **How:** `MediaPlayer.setPreferredDevice()` with the `AudioDeviceInfo.TYPE_BUILTIN_SPEAKER` from `AudioManager.getDevices(GET_DEVICES_OUTPUTS)`; on API 31+ the stronger `AudioManager.setCommunicationDevice` is not used (it's for calls). If both are on, play two players (speaker + connected device).
- **Health check row:** warns when a Bluetooth device is connected and "phone speaker" is off.
- **Tests:** emulator with a virtual Bluetooth headset is limited; real-device test with earbuds is part of M13.

## 22. Evening reminders

- **"No alarm set for tomorrow":** at 8 PM (adjustable), if no enabled alarm rings in the next 24 h, a notification: "No alarm for tomorrow. Set one?" with "Set 7:00" (copies the last alarm) and "Not tomorrow" actions.
- **Morning weather notification:** at the user's first alarm time on days with notable weather (rain chance ≥ 60%, or temperature below 2 °C / above 35 °C): "Rain likely at 5 PM, take an umbrella."
- **How:** both use inexact `setAndAllowWhileIdle` (they don't need exact timing), recomputed when alarms change and at boot; a separate "Reminders" notification channel so users can mute them without touching alarm alerts.
- **Off by default?** "No alarm" reminder on, weather off; both in Settings › Notifications.

## 23. Home Fix banner

- A full-width banner at the top of Home whenever `HealthChecker.overall` is Bad (red) or Warn (amber), e.g. "Battery saver may stop alarms · Fix". "Fix" runs the same intent as the health row; the banner slides away with a spring once fixed (re-checked on resume).
- Amber banners can be dismissed for 7 days; red ones can't be dismissed.
- Also shown at the top of the editor when saving an alarm while health is red.

## 24. Wake-up calendar

- **What the user sees:** in the You tab, a month grid; each day shows a small sun (won), a cloud (missed or escaped), a moon (no alarm) or nothing. Swipe between months with a 3D page turn. Tap a day: a sheet with that morning's ring log (rang at, mission time, snoozes, wake-up check, mood).
- **How:** derived from `RingEventEntity` grouped by local date (same logic as `WakeStatsCalculator`); history beyond 14 days is a Pro feature (free users see the current month).

## 25. App light theme

- Settings › Appearance: Dark (default) / Light / System. Light uses the existing `DawnLight` scheme plus light variants of cards and skies (`Sky.Day` base).
- Ring, mission, wake-up check and Good-morning screens always stay dark (6 AM eyes).
- Every screen gets a light-mode screenshot test (Compose preview + Paparazzi-style snapshot later).

## 26. Restore welcome

- **Detect:** on first launch, if Room already contains alarms but `Settings.onboardingVersion` is 0 (Auto Backup restored the database but the device is new), show "Welcome back: we restored your 3 alarms" with the list, then jump to step 5 (permissions don't transfer between phones) and a test alarm.
- **Why:** Android 14 resets exact-alarm and full-screen permissions after a restore; without this step restored alarms could silently fail.
- **Backup rules:** confirm `data_extraction_rules.xml` includes Room and DataStore, and excludes the device-protected mirror (rebuilt on first launch) and camera thumbnails (re-taken).
