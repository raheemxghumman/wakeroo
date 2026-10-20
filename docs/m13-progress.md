# M13 progress (paused 2026-10-20, 06:55)

Everything in M1–M12 is done and committed. M13 (regression) is in progress. This note is
the restart point.

## Done in M13 so far

Reliability matrix rows run on the emulator (`emulator-5554`, Android 16 image):

| Row | Result |
|---|---|
| Test alarm with screen off, battery unplugged, Doze forced | **Pass.** Rang on the minute. Deep Doze wouldn't engage: Android refuses deep idle with an AlarmClock due within ~30 min, and our alarm was the device's "next wake from idle". |
| Standby bucket set to restricted | **Pass.** Android keeps the app in WORKING_SET (exact-alarm apps are exempt from lower buckets). |
| App update mid-ring | **Pass.** The package-replaced handler resumes the ring about 5 s after install. |
| App update with alarms scheduled | **Pass.** The 7:00 alarm was re-registered after each install. |
| Process killed mid-ring (`kill -9`) | **Was failing, now passes.** Added a dead-man's switch in `RingService` (backup ring 30 s ahead, refreshed every 10 s; cancelled on dismiss, snooze or escape). Re-rang 26 s after the kill; the backup is gone after dismissal. |
| Ring while the phone is in use | Android blocks background activity starts from alarms (`BAL_BLOCK`). Without the overlay permission you get a heads-up with "Open mission" (same as Google Clock). Added an optional "Open over other apps" health row (`SYSTEM_ALERT_WINDOW`). With it granted, the full ring screen opened over the launcher. |
| Reopening Wakeroo during a ring | **Fixed.** `MainActivity.onResume` now jumps to the ring screen; before, Home could cover a ringing alarm. |
| Settings "Ring a test" button after a test | **Fixed.** It stayed disabled with stale text; it now re-arms after 75 s. |

## Still to do

1. Remaining matrix rows:
   - Do Not Disturb set to priority: `adb shell cmd notification set_dnd priority`, then `set_dnd off`.
   - Alarm volume 0: `adb shell cmd media_session volume --stream 4 --set 0`, then check that it's raised while ringing.
   - Time-zone change: the wall-clock alarm should re-register (`dumpsys alarm`).
   - Reboot before unlock: set a PIN with `adb shell locksettings set-pin 1234`, take a +1 min alarm, run `adb reboot`, and check that it rings on the lock screen. Clear the PIN afterwards with `locksettings clear --old 1234`.
   - Sleep sounds stop before an alarm.
   - Camera denied on an Item Hunt alarm, using `pm revoke com.wakeroo.app android.permission.CAMERA`. The backup mission should show.
   - Harder Mode emergency exit.
2. Update `docs/feature-plan.md` with the deviations:
   - The `tr()` catalogue in `assets/i18n` replaced `strings.xml`. The tools are `tools/i18n_build.py` (with `--sync`) and `tools/quotes_build.py`.
   - `keys.json` is append-only.
   - The dead-man's switch and the overlay row are new.
   - Data safety: Open-Meteo city lookup; NFC tag ID stored locally.
   - Play declarations: the `ACTIVITY_RECOGNITION` justification, and `SYSTEM_ALERT_WINDOW` as optional for alarms.
3. Real-device pass (Samsung or Xiaomi):
   - camera missions (Photo, QR, Item Hunt)
   - NFC
   - Bluetooth or earbuds routing
   - OEM battery killers
4. Final report to the user.

## Test helpers (scratchpad, not in the repo)

- `ui.py`: `state`, `tap TEXT`, `has TEXT`, `wait_ring SECS`, `solve`.
- `sched_test.sh`: schedules the Settings test alarm and verifies it in `dumpsys alarm`.
- `shot.sh NAME WIDTH`: takes a screenshot.

If the scratchpad is gone, rebuild them. They are thin wrappers around `adb shell uiautomator dump`, `dumpsys activity activities` (resumed activity) and taps on the math keypad:

| Key | Coordinates |
|---|---|
| 1 | 209,1537 |
| 2 | 540,1537 |
| 3 | 868,1537 |
| 4 | 209,1760 |
| 5 | 540,1760 |
| 6 | 868,1760 |
| 7 | 209,1984 |
| 8 | 540,1984 |
| 9 | 868,1984 |
| 0 | 540,2203 |
| ✓ | 868,2203 |

Emulator state: the overlay permission is granted (`appops set com.wakeroo.app SYSTEM_ALERT_WINDOW allow`) and the app locale is English. A real 7:00 AM daily alarm exists on the emulator.
