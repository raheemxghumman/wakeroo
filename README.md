<div align="center">

# Wakeroo

**The mission alarm that won't stop until you're actually up.**

An Android alarm clock for heavy sleepers and serial snoozers. To turn it off, you finish a wake-up mission: solve sums, scan a code in another room, walk, shake, find an object and more.

`Kotlin` · `Jetpack Compose` · `Material 3` · `Android 8.0+`

</div>

---

## Contents

- [Status](#status)
- [Features](#features)
- [Free and Pro](#free-and-pro)
- [Reliability](#reliability)
- [Privacy](#privacy)
- [Tech stack](#tech-stack)
- [Getting started](#getting-started)
- [Project structure](#project-structure)
- [Translations](#translations)
- [Tools](#tools)
- [Testing](#testing)
- [Documentation](#documentation)
- [Roadmap](#roadmap)

---

## Status

| | |
|---|---|
| **Version** | 1.0.0 (launch candidate, not yet on Google Play) |
| **Developer** | Qubiq Studios |
| **Package** | `com.wakeroo.app` |
| **SDK levels** | min 26 · target 36 · compile 37 |
| **Progress** | All milestones (M1–M13) are done and the release build is tested. What's left is account setup and Play's closed test; see [`docs/play-console.md`](docs/play-console.md). |
| **Deferred** | Real AdMob banner ID and a website (after launch) |

---

## Features

### The alarm

- **Setup:** a 3D drum-wheel time picker with a live "Rings in 7 h 20 min" read-out. Repeat on chosen weekdays, weekdays, every day, once, or on a specific date.
- **Wake-up reason:** "Why are you getting up?" shows in large type on the alarm screen.
- **Snooze:** a snooze limit per alarm, plus snoozes that cost a mission in Harder Mode.
- **Wake-up check:** a few minutes after you dismiss, Wakeroo asks "Still up?". If you don't answer, the full alarm comes back.
- **Mission time limit:** stop interacting during a mission for 10, 20, 30 or 60 s and full volume returns while the mission restarts. This closes the "lie there with the screen on" loophole.
- **Emergency exit:** a 15 s hold, or 60 s in Harder Mode, followed by typing a phrase. It's logged and breaks your streak.
- **Quick alarms:** one-tap chips (+10 / +20 / +30 min, +1 h, "In 8 h") and a **Power nap** Quick Settings tile.
- **Home screen widget:** shows the next alarm.

### Missions

There are 10 missions. Each one has a looping demo animation in the picker and a "Try it now" practice run. Free alarms take up to 2 missions; Pro alarms take up to 5.

| Mission | What you do | Gets you out of bed |
|---|---|:---:|
| Math | Solve quick sums (three difficulty levels) | |
| Typing | Type a short phrase in your language | |
| Shake | Shake the phone until the sun fills | |
| QR / Barcode | Scan a code you registered in another room | ✓ |
| Photo spot | Match a photo of a place, such as your sink | ✓ |
| Steps | Walk a number of steps; standing still drains the count | ✓ |
| Memory tiles | Repeat a growing pattern of lit tiles | |
| NFC tag | Tap a sticker you placed away from the bed | ✓ |
| Type your plan | Write at least three words about your day; the plan shows on the Morning tab | |
| Item hunt | Show the camera an everyday object; recognised on-device by ML Kit | ✓ |

Missions that need setup (QR, photo, NFC) can be set up later. Until then they fall back to Math. If a mission can't run, for example because a permission was denied or the phone has no sensor, a backup mission takes its place, so you're never stuck.

### Sound

- **Tones:** 12 original bundled tones in three groups (Gentle, Energetic, Extreme), synthesised in-house so there are no licensing issues. You can also use any phone ringtone or your own music.
- **Sound modes:**
  - **Same sound:** the same tone every time.
  - **Rotate:** a different tone each morning, so you never get used to one.
  - **Escalate:** with no response after a minute, the alarm crossfades to an Extreme tone at full volume.
- **Gentle start:** the volume ramps up from quiet.
- **Speaker routing:** "Always ring on the phone speaker" is on by default, so a dead Bluetooth speaker or forgotten earbuds never silence the alarm. You can also play on connected audio.

### Wake-up scenes

The alarm screen has animated backgrounds rendered in Compose, with no video files:

| Scene | Tier |
|---|---|
| Night Sky | Free |
| Into Space | Free |
| Minimal Black | Free |
| Aurora (AGSL shader) | Pro |
| Ocean Dawn | Pro |
| WAKE UP (kinetic type) | Pro |
| Your photo | Pro |

Each scene suggests a matching sound. You can preview any scene full-screen before choosing it.

### Tabs

| Tab | What's there |
|---|---|
| **Alarms** | Orbit clock hero (tap to flip it in 3D), streak strip, quick alarms, alarm cards, a Fix banner when something could block an alarm, and a Monday weekly recap |
| **Sleep** | 7 synthesised sleep sounds (mix two with Pro), a timer that always fades out a minute before your alarm, and a bedtime calculated from your next alarm and sleep goal |
| **Morning** | Weather with an hourly strip (Open-Meteo) and a one-tap "Change" city chip, quote of the day (150 quotes, themes, save, share as an image), mood log, a routine checklist of up to 5 habits, and today's plan |
| **You** | Streak, on-time rate, average time to get up, a wake-up calendar, 14 spinning 3D medals, and a shareable streak card |

### Good morning briefing

After you dismiss an alarm, a briefing shows:

- your plan for the day
- the weather
- your streak
- a mood check-in
- a quote

You can turn it off.

### Reminders

These are opt-in and use their own notification channel, so muting them never touches alarms:

- a wind-down reminder before bedtime
- a "No alarm for tomorrow" nudge in the evening
- a morning weather heads-up for rain, frost or heat

### Onboarding

A guided setup that takes about 40 seconds:

1. Pick a time.
2. Pick a wake-up scene.
3. Pick a sound.
4. Pick a mission.
5. Go through "Make sure it rings", a single permissions screen.
6. Finish with an optional one-minute test alarm.

There's no paywall, no tracking prompt and no account. On a new phone, Wakeroo spots restored alarms and opens with "Welcome back".

### Settings and about

- Alarm health check with a test alarm (see [Reliability](#reliability)).
- Send feedback by email (pre-filled with app version, phone and Android version), Rate Wakeroo, the privacy policy and open-source licenses, all inside the app.
- A Play in-app rating prompt after a 3-morning streak, at most three times and at least 60 days apart. Never on alarm screens.

### Appearance and languages

- **Theme:** Dark, Light, or System. Alarm and mission screens always stay dark.
- **Motion:** Full, Reduced, or follow the phone.
- **Clock:** 12- or 24-hour.
- **Languages:** 9, with a per-app language picker. Arabic and Urdu use full right-to-left layouts; see [Translations](#translations).

---

## Free and Pro

The free version is a complete alarm. **Every mission and every reliability feature is free.**

| | Free | Pro ($4.99 once, `wakeroo_pro`) |
|---|:---:|:---:|
| All 10 missions | ✓ | ✓ |
| Missions per alarm | 2 | 5 |
| Wake-up scenes | 3 | All 7, including your photo |
| Alarm tones | 6 | All 12 |
| Sleep sounds | 4, one at a time | All 7, mix two |
| Wake-up calendar | Current month | Every month since your first alarm |
| Ring log | Last 14 rings | Full history |
| Harder Mode | | ✓ |
| Ads | One banner (You tab and Settings) | None |

**Harder Mode** (per alarm) means turning off, deleting, skipping, snoozing, or moving the alarm 2 h or later all require a mission first. To keep anyone from locking themselves in by accident, it can't be switched on for an alarm less than 10 minutes away.

Pro is a one-time purchase through Google Play Billing: no subscription and no renewals.

---

## Reliability

Most "my alarm didn't ring" reviews come down to the phone getting in the way. Wakeroo checks for those problems openly and works around them.

- **Exact alarms:** every ring, including snoozes, wake-up checks and re-rings, goes through `setAlarmClock`. It fires on time in Doze and shows in the status bar.
- **Direct boot:** alarms are mirrored to device-protected storage, so they ring after a reboot even before you unlock.
- **Restarts and updates:** alarms are re-registered after a reboot, an app update, a time or time-zone change, and on every launch. A ring that's interrupted by a reboot or an update resumes on its own.
- **Dead-man's switch:** while ringing, a backup ring always sits 30 s ahead. If the process is killed mid-ring (an OEM task killer, low memory, a crash), the alarm starts again.
- **Alarm health check:**
  - checks: exact alarms, notifications, full-screen alarm, battery optimisation, app standby, alarm volume, headphones, Do Not Disturb, and the optional "Open over other apps" permission
  - shows step-by-step guides for each phone brand (Samsung, Xiaomi, Oppo/Realme/OnePlus, Vivo, Huawei/Honor, Motorola)
  - surfaces anything red or amber in a Fix banner on Home
- **Test alarm:** a one-minute test proves the phone rings Wakeroo alarms properly.
- **Ring log:** proof of every ring. Missed alarms are shown and notified.
- **Volume:** the alarm stream is raised to the alarm's level while ringing, even if it was set to zero.
- **Sleep sounds** always stop before an alarm so they can never mask it.

---

## Privacy

- Alarms, missions, photos, NFC tag IDs and stats stay on the phone. There's no account and no analytics on the alarm path.
- The camera is used only while a mission screen is open. Image recognition (ML Kit) runs on-device.
- The weather sends only the chosen city's coordinates to [Open-Meteo](https://open-meteo.com). Wakeroo never asks for location permission.
- Ads, only for free users, use Google's UMP consent flow. Privacy options are in Settings.

| Permission | Why |
|---|---|
| `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM` | Ring on the minute |
| `USE_FULL_SCREEN_INTENT`, `POST_NOTIFICATIONS` | Show the alarm over the lock screen |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `WAKE_LOCK` | Keep ringing until the mission is done |
| `RECEIVE_BOOT_COMPLETED` | Re-register alarms after a reboot |
| `VIBRATE` | Vibration with the alarm |
| `CAMERA` | QR, Photo spot and Item hunt missions |
| `ACTIVITY_RECOGNITION` | Steps mission |
| `NFC` | NFC tag mission |
| `INTERNET` | Weather, billing and ads |
| `SYSTEM_ALERT_WINDOW` (optional) | Open the alarm screen while you're using another app; without it you get a heads-up banner, as with Google Clock |

---

## Tech stack

| Area | Library |
|---|---|
| Language | Kotlin 2.4 (JDK 17) |
| Build | Android Gradle Plugin 9.4, version catalogue (`gradle/libs.versions.toml`), KSP |
| UI | Jetpack Compose (BOM 2026.09), Material 3, Navigation Compose with type-safe routes, AGSL shaders |
| Data | Room 2.8 (schema v2 with migration), DataStore Preferences, kotlinx.serialization |
| Async | Kotlin Coroutines and Flow |
| Camera and AI | CameraX, ML Kit barcode scanning and image labelling (on-device) |
| Widget | Jetpack Glance |
| Money | Play Billing 9, Google Mobile Ads, User Messaging Platform |
| Fonts | Outfit and Inter (SIL OFL), bundled |

---

## Getting started

### Requirements

- Android Studio (a recent stable release that supports AGP 9.4)
- JDK 17
- Android SDK Platform 37
- For `tools/`: Python 3 with `numpy`, and `ffmpeg` on `PATH`

### Build and run

```bash
# Debug build
./gradlew assembleDebug

# Install on a connected device or emulator
./gradlew installDebug

# Release APK for testing (minified and resource-shrunk)
./gradlew assembleRelease

# Play upload bundle (needs the support email and the upload key, see below)
./gradlew bundleRelease
```

Or open the project folder in Android Studio and run the `app` configuration.

> **Debug-only helpers:**
> - a **+1 min** quick alarm
> - **Pretend Pro** (Settings › About)
> - simulated NFC on devices without NFC
>
> None of these ship in release builds.

### Release signing

Release builds are signed with the Play **upload key**, which lives outside the repo:

- `keystore.properties` in the project root (gitignored) holds `storeFile`, `storePassword`, `keyAlias` and `keyPassword`.
- Without it, release builds fall back to debug signing so they can still be tested.
- `bundleRelease` refuses to build without the upload key and `wakeroo.supportEmail`.

### Emulator notes

- The emulator camera shows a black frame, so camera missions (QR, Photo spot, Item hunt) need a real phone.
- NFC is simulated in debug builds.
- To try a language, use the in-app picker or run:

  ```bash
  adb shell cmd locale set-app-locales com.wakeroo.app --locales ar
  ```

---

## Project structure

```text
app/src/main/java/com/wakeroo/app/
├── alarm/              Scheduling, receivers and notifications
│   ├── health/         Alarm health checks and per-brand battery guides
│   ├── ring/           Ringing service, ring screen, player, morning briefing
│   └── sound/          Bundled tone library
├── billing/            Play Billing (Pro) and the ad banner
├── camera/             CameraX helpers and ML Kit readers
├── data/               Repository, settings, direct-boot mirror
│   ├── db/             Room database, entities, DAOs, migrations
│   └── model/          Alarm, mission and sound models
├── feature/
│   ├── editor/         Alarm editor, mission picker, sound and spot setup
│   ├── home/           Alarms tab: orbit hero, streak strip, Fix banner
│   ├── main/           Bottom navigation scaffold
│   ├── morning/        Morning tab: weather, quotes, mood, routine
│   ├── onboarding/     Setup wizard and permission primer
│   ├── pro/            Pro screen
│   ├── settings/       Settings and alarm health
│   ├── sleep/          Sleep tab: sounds and bedtime
│   ├── stats/          Wake-up stats
│   └── you/            You tab: calendar, medals, recap, share card
├── missions/           All 10 missions, demos, mission flow, Harder Mode
├── reminders/          Bedtime, wind-down and no-alarm reminders
├── sleep/              Sleep sound synthesis and playback service
├── tile/               Power nap Quick Settings tile
├── ui/
│   ├── brand/          Logo and wordmark
│   ├── components/     Shared composables (drum wheel, headers, buttons)
│   ├── i18n/           tr() translations and per-app locale
│   ├── intro/          Launch intro sequence
│   ├── navigation/     Navigation graph
│   ├── scenes/         Wake-up scenes and scene picker
│   ├── theme/          Palette, typography, motion, light and dark themes
│   └── util/           Time formatting and layout helpers
├── weather/            Open-Meteo client and weather notification
└── widget/             Next-alarm home screen widget

app/src/main/assets/
├── i18n/               Translation catalogues (<lang>.json)
└── quotes/             Morning quotes per language (<lang>.json)

app/schemas/            Room schema history
docs/                   Plans, brand system and milestone notes
tools/                  Tone synthesis and translation build scripts
```

---

## Translations

Wakeroo ships in **English, Hindi, Urdu, Spanish, Portuguese (Brazil), Indonesian, Arabic, Turkish and French**.

### How it works

- UI text is written in English in code and wrapped in `tr("…")`, with numbered placeholders: `tr("Rings in {0}", until)`.
- Text stored once, such as enum labels, is marked with `n("…")` and translated when it's read.
- At runtime, `tr()` looks the English text up in `assets/i18n/<lang>.json`. Anything missing simply shows in English.
- Clock digits stay Western in every language. In right-to-left languages, the logo, keypads and hour:minute wheels stay left-to-right; everything else mirrors.
- Typing missions use phrases in the app's language. Accents are optional on the Latin keypad, and Hindi and Urdu use romanised phrases.

### Workflow

| Step | Command |
|---|---|
| Wrap new strings in a file | `python tools/i18n.py wrap <files>` |
| Add new code strings to the key list (append-only, so existing indexes never move) | `python tools/i18n_build.py --sync` |
| Translate | Add `index\|translation` lines to `tools/i18n_src/<lang>.txt` |
| Build and validate the catalogues (placeholders must match the English key) | `python tools/i18n_build.py` |
| Check for missing or unused keys | `python tools/i18n.py check` |
| Build the quote files from `tools/i18n_src/quotes_<lang>.txt` | `python tools/quotes_build.py` |

---

## Tools

| Script | Purpose |
|---|---|
| `tools/make_tones.py` | Synthesises the 12 bundled alarm tones and encodes them to OGG in `res/raw` |
| `tools/i18n.py` | Wraps UI strings in `tr()`, lists keys, checks catalogues |
| `tools/i18n_build.py` | Builds and validates `assets/i18n/<lang>.json`; `--sync` appends new keys |
| `tools/quotes_build.py` | Builds `assets/quotes/<lang>.json` from the translated quote lists |
| `docs/brand/build_brand.py` | Generates every logo, icon and Play graphic from one geometry definition |

```bash
python tools/make_tones.py app/src/main/res/raw
python docs/brand/build_brand.py docs/brand app/src/main/res
```

---

## Testing

```bash
./gradlew testDebugUnitTest
```

67 unit tests cover:

- next-trigger calculation
- math problems and typing phrases, including accent-insensitive matching
- the logic of the new missions
- Harder Mode rules
- bedtime calculation
- weather parsing
- stats, achievements and the weekly recap
- when to ask for a rating

The reliability matrix (Doze, standby buckets, reboot before unlock, process kill mid-ring, app update mid-ring, phone in use, time-zone change, Do Not Disturb, volume 0 and more) is run on an emulator and on real phones. Results and the remaining rows are in [`docs/m13-progress.md`](docs/m13-progress.md).

---

## Documentation

| Document | Contents |
|---|---|
| [`docs/build-plan.md`](docs/build-plan.md) | Original v1.0 build plan and reliability matrix |
| [`docs/feature-plan.md`](docs/feature-plan.md) | The 26 pre-launch features, milestones M1–M13, and the Alarmy comparison |
| [`docs/m13-progress.md`](docs/m13-progress.md) | Regression results (reliability matrix) |
| [`docs/play-console.md`](docs/play-console.md) | Step-by-step Play Console guide: declarations, data safety answers, store listing, Pro product, testing |
| [`docs/privacy-policy.md`](docs/privacy-policy.md) | The privacy policy, ready to publish (the app bundles the same text) |
| [`docs/brand/README.md`](docs/brand/README.md) | Logo, colour, type, shape, motion and haptics |

---

## Roadmap

| Milestone | Scope | Status |
|---|---|:---:|
| M1 | Data model v2, palette, bottom navigation | ✅ |
| M2 | Onboarding wizard, restore welcome | ✅ |
| M3 | Five new missions, demo previews, mission time limit | ✅ |
| M4 | Sound library, rotation, escalation, speaker routing | ✅ |
| M5 | Motion wake-up scenes | ✅ |
| M6 | Sleep tab, sleep sounds, reminders | ✅ |
| M7 | Morning tab, briefing, weather heads-up | ✅ |
| M8 | You tab: calendar, medals, recap, share card | ✅ |
| M9 | Harder Mode and Pro gating | ✅ |
| M10 | Home upgrade, Fix banner, light theme | ✅ |
| M11 | Power nap Quick Settings tile | ✅ |
| M12 | 8 more languages, right-to-left layouts | ✅ |
| M13 | Regression, release build, launch readiness | ✅ |

### Before launch

- [ ] Finish the reliability matrix on the emulator and a real Samsung or Xiaomi phone. Real devices are needed for camera missions, NFC, Bluetooth and OEM battery killers.
- [ ] Native-speaker review of the translations, at least Hindi, Urdu, Spanish and Portuguese.
- [x] Support email `qubiq.wakeroo@gmail.com` set as `wakeroo.supportEmail` in `gradle.properties`.
- [ ] Publish the privacy policy at a public URL (a Google Doc is fine).
- [ ] Play Console: developer account, declarations and data safety (answers in [`docs/play-console.md`](docs/play-console.md)), store listing, `wakeroo_pro` product.
- [ ] Closed test with 12 testers for 14 days.
- [ ] After launch: real AdMob banner ID, website.

### Later (v2)

- Camera-counted squats and push-ups
- Wear OS
- Calendar and shift-pattern sync

### Deliberately left out

- Blocking power-off or uninstall: it relies on banned Accessibility and Device Admin use, and the restart re-ring covers that cheat anyway.
- Sleep tracking and snore recording: reviewers call it bloat.
- Paywalls and tracking prompts in onboarding.
