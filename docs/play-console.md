# Google Play Console: launch guide

Everything needed to publish Wakeroo from the **Qubiq Studios** developer account, in the order Play Console asks for it. Answers are written to match exactly what the app does.

---

## 1. Before you start

| Item | Status | Notes |
|---|:---:|---|
| Developer account "Qubiq Studios" | ⏳ | $25 one-time, plus identity verification |
| Support email | ⏳ | Create it, then set `wakeroo.supportEmail=` in `gradle.properties`. The Play bundle won't build without it |
| Upload key | ✅ | `C:\Users\rahee\keystores\qubiq-wakeroo-upload.jks`, with its password in `qubiq-wakeroo-upload.properties` next to it. **Back up both** (cloud drive or USB). Play App Signing holds the real signing key |
| Privacy policy URL | ⏳ | Required. The text is in [`privacy-policy.md`](privacy-policy.md). No website is needed: publish it as a public Google Doc or Google Sites page and paste the link |
| AdMob app and banner ID | ⏳ Later | The app ships with Google's test banner until you replace it |
| Pro product | ⏳ | Create it after the first upload; see section 6 |

## 2. Build the upload file

```bash
./gradlew bundleRelease
# → app/build/outputs/bundle/release/app-release.aab
```

When you create the app in Play Console, choose **Use Play App Signing** (the default) and upload this `.aab`.

---

## 3. App setup

### Create app

| Field | Value |
|---|---|
| App name | Wakeroo |
| Default language | English (United States) |
| App or game | App |
| Free or paid | Free (Pro is an in-app purchase) |

### App content declarations

| Section | Answer |
|---|---|
| **Privacy policy** | Your public policy URL |
| **App access** | All functionality is available without special access (no login) |
| **Ads** | Yes, the app contains ads |
| **Content rating** | Category: Utility / Productivity. No violence, sexual content, gambling or user-generated content. No sharing of the user's location. Expected rating: Everyone / PEGI 3 |
| **Target audience** | 18 and over is the safest choice, or 13+. Not designed for children |
| **News app** | No |
| **COVID-19 tracing** | No |
| **Data safety** | See section 4 |
| **Government app** | No |
| **Financial features** | None |
| **Health** | No health features. A step count is used only as a mission, not stored |

### Permission declarations

Play asks about these. Paste the justification for each.

| Permission | Justification |
|---|---|
| `USE_EXACT_ALARM` | Wakeroo is an alarm clock. Its core function is ringing user-set alarms at the exact minute. |
| `USE_FULL_SCREEN_INTENT` | When an alarm rings, the alarm screen must appear over the lock screen so the user can stop it. Alarm clock is the app's core function. |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | The alarm sound plays from a foreground service until the user finishes the wake-up mission, and sleep sounds play at night. Video for the declaration: record the alarm ringing on a phone. |
| `ACTIVITY_RECOGNITION` | Used only by the optional "Steps" wake-up mission, which counts the user's steps while the mission is on screen. Nothing is stored or shared. |
| `CAMERA` | Used only by the optional QR / Barcode, Photo spot and Item hunt wake-up missions while they're on screen. Images are processed on the device. |
| `SYSTEM_ALERT_WINDOW` | Optional. Lets the alarm screen open while the user is in another app. Without it, the alarm shows as a heads-up notification. |

---

## 4. Data safety form

**Does your app collect or share any of the required user data types?** Yes, because of the AdMob banner in the free version. Wakeroo itself collects nothing.

| Data type | Collected | Shared | Why | Optional |
|---|:---:|:---:|---|:---:|
| Device or other IDs (advertising ID) | ✓ | ✓ | Advertising (Google AdMob) | No, but consent is asked where the law requires |
| App interactions (ad views and clicks) | ✓ | ✓ | Advertising (Google AdMob) | No |
| Approximate location (derived from IP by AdMob) | ✓ | ✓ | Advertising (Google AdMob) | No |
| Purchase history | ✓ | | App functionality (Google Play checks Pro ownership) | No |

| Question | Answer |
|---|---|
| Is all user data encrypted in transit? | Yes (HTTPS) |
| Can users request deletion? | Yes. All app data is on the device; uninstall or clear storage. Ad data follows Google's controls |
| Photos and videos | Not collected. Mission photos never leave the phone |
| Fitness / physical activity | Not collected. Steps are counted on screen only |
| Location | Not collected by Wakeroo. The weather city is chosen by name; only its coordinates go to Open-Meteo, a weather service, not linked to the user |

> If you remove ads later, or for Pro users, the ad rows still apply, because the free version contains the SDK.

---

## 5. Store listing

| Field | Text |
|---|---|
| **App name** (30) | Wakeroo: Mission Alarm Clock |
| **Short description** (80) | The alarm that won't stop until you're up. Solve a mission to turn it off. |

**Full description:**

```text
Wakeroo is an alarm clock for heavy sleepers and serial snoozers. It won't stop ringing until you complete a wake-up mission, so you're actually out of bed, not back asleep.

★ 10 WAKE-UP MISSIONS
Math, Typing, Shake, Memory tiles, Type your plan, and missions that get you out of bed: scan a QR or barcode in another room, match a photo of your sink, walk a number of steps, tap an NFC sticker, or show the camera an everyday object.

★ WAKE UP TO A SCENE, NOT A BEEP
Animated wake-up scenes: Night Sky, Into Space, Aurora, Ocean Dawn, WAKE UP and your own photo.

★ SOUNDS THAT WORK
12 original tones, gentle start, a different tone each morning, or escalation to full volume if you don't respond. Rings on the phone speaker even if your earbuds are connected.

★ BUILT TO RING
Exact alarms that ring through Doze and after a reboot, a one-minute test alarm, a health check with step-by-step fixes for your phone, and a "Still up?" check after you dismiss.

★ YOUR MORNING, SORTED
After the alarm: your plan, the weather, your streak, a mood check-in and a quote. Track your mornings with a wake-up calendar, streaks and medals.

★ SLEEP BETTER
Sleep sounds that always fade out before your alarm, a bedtime worked out from your next alarm, and gentle reminders.

★ PRIVATE BY DESIGN
No account. Your alarms, missions and photos stay on your phone.

Free with one small banner. Wakeroo Pro is a one-time $4.99, with no subscription: up to 5 missions per alarm, every scene and tone, Harder Mode, full history and no ads.

Available in English, हिन्दी, اردو, Español, Português, Bahasa Indonesia, العربية, Türkçe and Français.
```

| Asset | Source |
|---|---|
| App icon (512×512) | `docs/brand/png/play-icon-512.png` |
| Feature graphic (1024×500) | `docs/brand/png/feature-graphic-1024x500.png` |
| Phone screenshots (2–8, 9:16) | Capture from the emulator: Home, ring screen, mission, mission picker, scenes, Morning, You |
| Promo video (optional) | The brag video `brag-output/brag.mp4`, uploaded to YouTube |
| Category | Tools (or Productivity) |
| Contact email | Your support email |

Translate the listing for each language. Ask Claude to produce them from this text.

---

## 6. Wakeroo Pro (in-app product)

Monetize › Products › In-app products › Create product:

| Field | Value |
|---|---|
| Product ID | `wakeroo_pro` (must match exactly) |
| Name | Wakeroo Pro |
| Description | Up to 5 missions per alarm, all scenes and tones, Harder Mode, full history, no ads. |
| Price | $4.99 (Play converts it for other countries) |
| Type | One-time, non-consumable |

You need a payments profile first: Setup › Payments profile.

---

## 7. Testing and release

1. **Internal testing**: upload the AAB and add yourself. Install from the Play link and check that the Pro purchase works. Testers on the license-testing list can buy without being charged.
2. **Closed testing** (required for new personal accounts): at least **12 testers for 14 days in a row**. Share the opt-in link with friends and family; they must stay opted in.
3. **Apply for production** once the 14 days pass, then answer Google's short questionnaire about the test.
4. **Production release**: roll out to 100% or start at 20%.

## 8. After launch

- Replace the AdMob test banner ID with your real one, and add `app-ads.txt` once you have a website.
- Watch Android vitals in Play Console for crashes and ANRs.
- Reply to reviews. The app asks happy users (a 3-morning streak) for a rating at most three times.
