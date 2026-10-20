# M13 regression and launch readiness (completed 20 October 2026)

Test setup:

- **Device:** emulator `nascon_api35` (Android 16 image, 1080×2400).
- **Build:** rows from "App update mid-ring" onward used the **release build** (R8-minified, signed with the upload key), the same artefact that ships.

## Reliability matrix

| Row | Result | Notes |
|---|:---:|---|
| Screen off, battery unplugged, Doze forced | ✅ | Rang on the minute. Android won't enter deep idle with an AlarmClock due within ~30 min, and our alarm was the device's "next wake from idle" |
| App standby bucket forced to restricted | ✅ | Android keeps apps that hold the exact-alarm permission in WORKING_SET or higher |
| App update mid-ring | ✅ | The package-replaced handler resumes the ring about 5 s after install |
| App update with alarms scheduled | ✅ | All alarms re-registered after every install |
| Process killed mid-ring (`kill -9`) | ✅ after fix | Was silent forever. Added a dead-man's switch: a backup ring 30 s ahead, refreshed every 10 s. Re-rang 26 s after the kill |
| Ring while the phone is in use | ✅ after fix | Android blocks background activity starts from alarms (`BAL_BLOCK`), so you get a heads-up with "Open mission", the same as Google Clock. With the optional "Open over other apps" permission (onboarding and Health), the full ring screen opens over other apps |
| Opening Wakeroo during a ring | ✅ after fix | Used to show Home over a ringing alarm; now jumps to the ring screen |
| Reboot with a PIN, nobody unlocks | ✅ after fix | Alarm re-registered on LOCKED_BOOT_COMPLETED from the direct-boot mirror and rang on time over the lock screen; the mission ran on the lock screen. **Found a data-loss bug:** the ring screen read settings from credential-encrypted DataStore while locked, cached an empty file, and could later write it back over the real settings after unlock. Fixed: `SettingsStore` serves the direct-boot mirror and skips writes while locked; the briefing hides the mood picker before unlock; the language choice moved to device-protected storage (it would have crashed app start before unlock on Android 12 and older) |
| Do Not Disturb (priority) | ✅ | Alarm audio plays (`USAGE_ALARM` started) |
| Alarm volume at minimum | ✅ | Android no longer allows alarm volume 0 (minimum 1); Wakeroo raises the stream to the alarm's level when ringing |
| Time-zone change | ✅ | 7:00 alarm re-registered at 7:00 in the new zone (Karachi → London) |
| Sleep sounds playing when an alarm rings | ✅ | Media player and sleep service stop at ring start, so they can never mask the alarm |
| Camera denied for a camera mission | ✅ | The picker won't add a camera mission without permission. If it's revoked later, the ring swaps in the backup mission |
| Emergency exit | ✅ | Hold (15 s; 60 s in Harder Mode), then type the phrase; the ring stops and the morning is logged as escaped |
| "Still up?" wake-up check | ✅ | Unanswered, it escalated to the full alarm with snoozes removed |
| Settings › Ring a test (repeat) | ✅ after fix | The button stayed disabled after a test; now re-arms after 75 s |

## Release build checks

| Check | Result |
|---|:---:|
| R8 minify and resource shrink | ✅ after adding a `-dontwarn` rule for a Play services annotation used by review-ktx |
| Fresh install → onboarding → test alarm → mission → "Test passed" | ✅ |
| JSON under R8: quotes, weather city search and forecast | ✅ |
| Billing client start, AdMob test banner, UMP | ✅ |
| Debug-only items hidden (Pretend Pro, +1 min, NFC simulation) | ✅ |
| Privacy policy and open-source licenses screens | ✅ |
| Download size | ~25 MB per device from the AAB (the universal APK is 73 MB; ML Kit native libraries for 4 ABIs) |

## Needs a real phone

The emulator can't cover these:

- Camera missions with a real camera (QR, Photo spot, Item hunt)
- NFC tag
- Bluetooth and earbud routing
- OEM battery killers (Samsung, Xiaomi)

Run them during the closed test.
