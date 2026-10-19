# Wakeroo wins by fixing what Alarmy broke

Wakeroo should be **the mission alarm people can trust**: it always rings, it can't be slept through, every core mission is free, no ad ever appears between the ring and the dismissal, and Pro costs **$4.99 once**. Lead with **heavy sleepers and students**, who are the largest search pool and the largest audience visible in reviews. The demo-able hook is the **"Mission Spot"**: the off-switch lives in another room, as a QR code on the toothbrush or a photo of the sink. Add **Fajr Mode** as v1.1 for Pakistan and the Gulf, live about two weeks before Ramadan 2027 (expected to start around 8 February). ADHD works best as listing keywords plus routine features, and camera-counted exercise belongs in v2. The evidence runs both ways. In **14,157 recent Play reviews** of 17 alarm apps, the three biggest complaints are paywalls (**14.9%** of negative reviews), ads (**12.9%**) and alarms that never rang (**9.8%**), and a solo developer can fix all three by design. Demand is real: Alarmy's maker earned about **₩46B (~US$33M) in 2025**, and "alarmy" brand searches rose roughly **90% from 2024 to 2026**. Supply is flooded, though. The top "mission alarm" results are 2026 clones with 12 to 9,674 installs, and only apps with one sharp hook broke out. The plan opens a closed test on **1 December 2026**, launches publicly around **28 December** to catch the New Year and India's January peak, and ships Fajr Mode by **24 January 2027**. Expect revenue in the tens to low hundreds of dollars a month for the first six months. Real upside depends on one viral mission video or a strong Ramadan, not on ads.

## Wakeroo leads with heavy sleepers, adds Fajr, and sells trust

### What is already decided

Several decisions are locked and verified. **The name is Wakeroo.** On 2026-10-20, official RDAP registry lookups returned 404 (unregistered) for wakeroo.com, wakeroo.app, wakeroo.dev and wakeroo.io, while control domains returned 200. The same day, Play searches in the US, Pakistan and India found no app named Wakeroo, and web results showed no product, only Australian place names such as Watheroo. A USPTO search returned just one nearby mark, WAKEROOT, a Class 3 shampoo mark that is unrelated. Naming research scored Wakeroo **18/20**, the highest of 105 candidates. Most "Wake-" and "Alarm-" coinages were already taken by alarm apps, among them Wakey, Wakely, Waku, WakeQuest, WakeWave, Alarmo and Alarmly ([Play: Alarmly](https://play.google.com/store/apps/details?id=com.b2spirit.alarmly); [Play: Wakey](https://play.google.com/store/apps/details?id=com.sofaking.moonworshipper)). Coined names disappear fast: new "Wake-" apps appeared weekly in September and October 2026 ([Play: Wakeuply](https://play.google.com/store/apps/details?id=com.mwapps.wakeuply)). So this week, **register wakeroo.com and wakeroo.app** and **run a WIPO Global Brand Database search**. Only the USPTO was checked, so Pakistan, India, Indonesia and Brazil remain unchecked. **Drop the tagline "The Alarm That Never Fails."** It is word for word the short description of Alarmly, a 100K+ install competitor ([Play: Alarmly](https://play.google.com/store/apps/details?id=com.b2spirit.alarmly)). Candidate replacements are **"Bounce out of bed."** (plays on the kangaroo root), **"The alarm you can't sleep through."**, **"No mission, no silence."** and **"Up for real."** None of these has been checked against existing listings yet, so do a quick Play and web search before using one.

The engineering base is also fixed. The applicationId is **com.wakeroo.app**, and the project at `C:/Users/rahee/Desktop/AppDev/Wakeroo` uses Kotlin and Jetpack Compose with AGP 9.4.1, Gradle 9.4.1, Kotlin 2.4.21 (Compose compiler plugin), Compose BOM 2026.09.00, minSdk 26 and target/compile SDK 36. The code lives in a GitHub repo. Targeting API 36 is mandatory for new apps since 31 August 2026 ([Target API requirements](https://developer.android.com/google/play/requirements/target-sdk)). The visual direction is **Expressive Dawn**: Material 3 Expressive, a sky that changes with the time of day, an abstract sun shape, and no cartoon clutter. A subtle kangaroo-inspired brand mark is optional. The palette is Night `#0E1120`, Surface `#1A1E33`, Sunrise Coral `#FF7A59`, Dawn Lavender `#B9A7FF`, Morning Gold `#FFC857`, Text `#F5F3EE` and Error `#FF5470`.

### Which audience to lead with

Five audiences were weighed. Heavy sleepers win on search volume. Fajr wins on how concentrated and loyal the demand is, and it has a deadline. ADHD has the fastest-growing search but carries claims risk. Exercise is the most viral, but it is also the most fragile to build.

| Hook | Demand evidence | Competition | Fit for a zero-budget beginner | Decision |
|---|---|---|---|---|
| **Heavy sleepers / students** | "Alarm clock for heavy sleepers" runs at about **20% of "alarmy" search volume** and doubled in 2026 ([Google Trends](https://trends.google.com/trends/explore?date=today%205-y&q=fajr%20alarm,alarmy,alarm%20clock%20for%20heavy%20sleepers,adhd%20alarm)). "Alarm for heavy sleepers" is the top Play autocomplete for "alarm for". Students are the largest audience visible in reviews (117 negative and 200 positive mentions), and they are price-sensitive and often have no card ([Play: Wakey review](https://play.google.com/store/apps/details?id=com.sofaking.moonworshipper)) | Crowded exact-match titles. Four apps use near-identical "Alarm Clock for Heavy Sleepers" titles ([Play: nbow](https://play.google.com/store/apps/details?id=com.nbow.alarmclock)) | Largest pool. Works in every English market. The Mission Spot is easy to film | **Primary** |
| **Fajr / prayer wake-up** | Pakistan scores **100** on worldwide interest for "fajr alarm", followed by the UAE (43), Qatar (37) and Bangladesh (34). Pakistani autocomplete suggests "alarm for namaz time" and "alarm for prayer". The leading app, Fajr, has **951K installs and 4.92★** ([Play: Fajr](https://play.google.com/store/apps/details?id=com.blink22.fajr)). Tazakka ("Fajr alarm with up to 5 wake-up tasks") reached about 110K installs in roughly 17 months ([Play scrape](https://play.google.com/store/search?q=mission%20alarm&c=apps)) | One strong leader that is not built around missions, plus tiny 2026 entrants | Home-market advantage, a predictable Ramadan spike, and very satisfied users. Prayer-time maths must be accurate | **Secondary: Fajr Mode in v1.1, before Ramadan** |
| **ADHD** | Search for "adhd alarm" grew about **10x**, from 1.2 (2024) to 12.1 (2026 to date), and now equals "heavy sleepers" ([Google Trends](https://trends.google.com/trends/explore?date=today%205-y&q=fajr%20alarm,alarmy,alarm%20clock%20for%20heavy%20sleepers,adhd%20alarm)). No dedicated ADHD wake-up app has traction | A supply gap | Only 19 explicit ADHD reviewers. Medical claims are risky | **Keywords plus features** (QR at the meds shelf, big labels, wake-up check). Not the brand |
| **Exercise-to-dismiss** | Pushy reached 127K installs since Dec 2025 and RiseRep 54K since May 2026 ([Play: Pushy](https://play.google.com/store/apps/details?id=com.iuri.pushups_alarm)) | Three or more 2025–26 apps | Steps and squats have the most mission bug reports (28) and trap users ([Play: Alarmy reviews](https://play.google.com/store/apps/details?id=droom.sleepIfUCan)) | **v2**, as a viral content feature |
| **Social / wake-with-friends** | No search demand. Top result has 55 installs | Empty except Galarm | No evidence anyone is looking for it | Later, as a sharing feature |

So the **primary positioning** is: *"Wakeroo: the loud mission alarm for heavy sleepers. It always rings, you can't sleep through it, missions are free, and there are no ads at wake-up."* The **demo hook** is the Mission Spot. Reviews and community posts agree that physical, location-bound missions are what actually get people out of bed: "I have to go downstairs and take a picture of our house thermostat… a horrible first fortnight, but after that it's honestly been the best thing I've done" ([HN](https://news.ycombinator.com/item?id=29352637)); "the barcode feature alone is worth the paid version" ([Play: Xtreme](https://play.google.com/store/apps/details?id=com.alarmclock.xtreme.free)).

### Seven differentiators against Alarmy and the 2026 clone wave

| Dimension | Alarmy | 2026 mission-alarm clones | Wakeroo |
|---|---|---|---|
| Reliability | 11.4% of its negative reviews say "didn't ring" ([Play](https://play.google.com/store/apps/details?id=droom.sleepIfUCan)) | Unproven | Visible proof that alarms fire: a ring log, a health checklist, a "test in 1 minute" button and missed-alarm detection |
| Ads | Ads during the alarm, and ads that let users escape the alarm screen | Usually ad-funded | **Zero ads on the alarm path.** One banner on Stats and Settings for free users only |
| Price | Subscriptions, with IAP up to **$159.99** | Often subscription-first | Every core mission free; **$4.99 one-time Pro** |
| Escape design | Emergency escape needs 3–4 lines of perfect typing | Basic | A humane but costly exit: a 15-second hold plus a short phrase, logged in the ring log |
| Post-alarm screen | Weather, quotes and characters that can't be turned off | Varies | A five-second "Good morning" that can be switched off |
| Offline | Some users report "won't save without internet" | Varies | Fully offline. No account, no location permission |
| Niche | Generic | Generic | Fajr Mode (v1.1), with prayer-anchored alarms and missions |

The "missions + honest price + no ads at wake-up + it actually rings" pitch beats the clones because they compete on mission count, and none of them can show a track record. Wakeroo turns trust into a feature the user can see: a home-screen health card that turns green, and a ring log that proves each alarm fired.

### How a $25 budget still produces growth

Discovery in this category happens through **Play search and short-form video**, not web SEO. Niche web search volume is close to zero, while Alarmy's mission clips drew **26.7M views on one X post** in September 2026 ([Dexerto](https://www.dexerto.com/entertainment/viral-alarm-app-forces-users-to-complete-mini-games-before-it-will-shut-off-3412598/)). Exact-match titles also work for new entrants. Coolie Hat's "Alarm Clock for Heavy Sleepers" reached **215K installs in about 4.5 months** ([Play: Coolie Hat](https://play.google.com/store/apps/details?id=com.digitalclock.wakeupalarm)). The growth loop has four parts, all free:

1. A **keyword title** ("Wakeroo: Loud Alarm Clock").
2. A **first burst of reviews** from the closed-test cohort.
3. **Mission Spot videos** posted by the founder and early users.
4. **Pakistan and Gulf Fajr communities**, reached before Ramadan.

Retention matters more than acquisition here. Alarmy has **~110M cumulative downloads but only ~8M monthly users** ([VentureSquare](https://www.venturesquare.net/?p=1081658)), so most installs churn. Play now weights retention and Android vitals more heavily than listing text ([AppRadar](https://appradar.com/academy/aso-basics/app-store-ranking-factors)). An alarm that never fails is the growth strategy itself.

## Alarmy's 90% brand growth proves demand; the clone flood proves you need a hook

The category is large, profitable, ad-heavy and growing. Delight Room's revenue rose from **₩23B (2023) to ₩33.7B (2024) to ₩46B (2025)**, with about ₩20B operating profit on roughly 50 staff ([Unicorn Factory](https://www.unicornfactory.co.kr/article/2026011309500633471); [MTN](https://news.mtn.co.kr/news-detail/2026012916134611205)). In 2024, Alarmy's revenue split was about **72% in-app ads and 28% subscriptions**, and **85% of its users were outside Korea** ([Daum interview](https://v.daum.net/v/XyDKg12XHQ)). In May 2026 the company reported **110M cumulative downloads, 4.3M DAU and 8M MAU** ([VentureSquare](https://www.venturesquare.net/?p=1081658)). That works out to roughly $4 per monthly user per year. On Play, Alarmy shows **51.9M installs and a 4.65★ rating from 2.18M ratings** ([Play](https://play.google.com/store/apps/details?id=droom.sleepIfUCan)).

Search trends confirm the growth but need careful reading. Worldwide, the yearly average for "alarmy" went from **5.7 (2024) to 7.5 (2025) to 10.9 (2026 to date)** ([Google Trends](https://trends.google.com/trends/explore?date=today%205-y&q=alarmy,alarm%20app,loud%20alarm,alarm%20clock%20app,wake%20up%20app)). Generic "alarm app" jumped from about 17 to 60 over the same period, but the jump happened abruptly in August 2025 across every country and generic term at once. That pattern suggests partly a measurement change, so the smoother brand series is the more reliable signal. Two niche terms are rising fast:

- **"adhd alarm"** went from 1.2 (2024) to 5.2 (2025) to **12.1 (2026)**.
- **"fajr alarm"** went from 0.4 to 1.5 to **2.5**, on a scale where "alarmy" is 56 ([Google Trends](https://trends.google.com/trends/explore?date=today%205-y&q=fajr%20alarm,alarmy,alarm%20clock%20for%20heavy%20sleepers,adhd%20alarm)).

The only rising related query for "alarmy" is **"alarmy mod apk" (+76,700%)** ([Google Trends](https://trends.google.com/trends/explore?date=today%205-y&q=alarmy)). People want premium features without paying, which supports Wakeroo's generous free tier.

Seasonality is mild but usable:

| Window | What happens | Evidence |
|---|---|---|
| **August–September** | The worldwide "alarmy" monthly average peaks in **September (8.8 vs a 6.0–7.5 baseline)**. "Loud alarm" peaks in Aug–Sep (back to school) | [Google Trends](https://trends.google.com/trends/explore?date=today%205-y&q=alarmy) |
| **January** | India's "alarmy" interest peaks in January (17.3), and the UK's "alarm app" also peaks in January (43.2) | [Google Trends IN](https://trends.google.com/trends/explore?date=today%205-y&geo=IN&q=alarmy,alarm%20app,alarm%20clock%20app); [Google Trends GB](https://trends.google.com/trends/explore?date=today%205-y&geo=GB&q=alarmy,alarm%20app,alarm%20clock%20app) |
| **Ramadan** | Prayer apps surge. Dubai's prayer app reported **54,000+ new downloads and +185% hourly active users on day 1** of Ramadan 2026, with peaks around Fajr | [Dubai Media Office](https://www.mediaoffice.ae/en/news/2026/march/12-03/dulook-dxb-nears-one-million-downloads) |

Ramadan 2027 is expected to run from about **8 February to 9 March 2027**. These dates are approximate and depend on moon sighting.

The competitive map shows big but slow incumbents and a flood of small clones:

- **Incumbents.** Alarm Clock Xtreme has 83.8M installs. "I Can't Wake Up!" has 5.9M installs, has not been updated since **July 2023**, and still ranks #2 for "wake up alarm" in the US. j labs' Loud Alarm Clock (1.65M installs, 4.79★) has not been updated since September 2025 ([Play scrape](https://play.google.com/store/search?q=alarm%20clock%20for%20heavy%20sleepers&c=apps)).
- **Breakouts.** Each 2024–26 breakout had one sharp hook. Wayk reached **364K installs in about six months** with lifestyle and faith missions on a subscription. Pushy uses camera-counted push-ups. Coolie Hat and Nbow used the exact-match "Heavy Sleepers" title. Alarmly sells a "guaranteed" promise ([Play: Wayk](https://play.google.com/store/apps/details?id=mg.WaykUp)).
- **Clones.** The **top 11 US and India results for "mission alarm" were all released between March and October 2026, with 12 to 9,674 installs each**.
- **Weak spots.** Some sub-niches are named but poorly served. Apps for "qr code alarm" have under 1K installs, apps for "step alarm walk to turn off" are tiny, and Math Alarm Clock sits at 3.0★ with 197K installs ([Play search](https://play.google.com/store/search?q=mission%20alarm&c=apps)).

The lesson: launching "another Alarmy clone" fails. A focused promise plus exact-match ASO can win.

Regional data sets the market order:

- **English-speaking markets** (US, UK, Ireland, Australia, New Zealand, Canada) lead "alarm app" interest, and they pay the most.
- **India** has the strongest Alarmy brand share, at about 25% of "alarm app" searches versus about 10% in the US.
- **Pakistan** ranks #8 worldwide for "loud alarm" and #1 for "fajr alarm".
- **Korea, Egypt, Turkey, Mexico and Israel** are Alarmy's strongest Play usage markets ([Similarweb](https://www.similarweb.com/app/google/droom.sleepIfUCan/india/)).

Pakistan and Indonesia barely search the English brand terms, which suggests local-language queries and weak Alarmy awareness. A localized, Fajr-led listing therefore faces less head-to-head competition there.

## 14,157 reviews say: ring every time, charge fairly, keep ads away

The user-needs evidence comes from **14,157 English Play reviews dated January 2025 to October 2026 across 17 apps**: 5,939 negative (1–3★) and 8,218 positive. Themes were tagged with keyword regexes, so read the percentages as approximate. Reddit could not be reached during the research. The pattern is consistent: users don't want more features. They want an alarm that doesn't betray them.

| Theme (share of negative reviews) | All apps | Alarmy | What it means for Wakeroo |
|---|---|---|---|
| Paywall / price | **14.9%** | 13.3% | Missions free; one cheap one-time unlock |
| Ads (any) | **12.9%** | 11.0% | No ads on the alarm path; minimal elsewhere |
| Alarm didn't ring / overslept | **9.8%** | 11.4% | Reliability is engineering priority #1 |
| Volume / loudness | 4.1% | 3.6% | Alarm stream, respected volume cap, ramp control |
| Snooze | 3.7% | 2.9% | Interval, limit, shrinking snooze, big button |
| Ads during the alarm or mission | 3.3% | 3.2% | Hard rule: never |
| Can't turn off / stuck ringing | 3.2% | 3.0% | Mission fallback plus a humane emergency exit |
| Mission recognition bug | 2.1% | 1.8% | Bundled ML models, torch, sensitivity slider, fallback |

Source: review dataset; e.g., [Alarmy on Play](https://play.google.com/store/apps/details?id=droom.sleepIfUCan).

The most-upvoted quotes show what is at stake. The **most-upvoted review in the whole dataset (213 👍)** asks for an end to auto-dismissing: "The app dismisses the alarm too soon. An option to turn this off… would be a huge improvement" ([Play: Challenges](https://play.google.com/store/apps/details?id=com.garageapp.alarmchallenges)). The highest-voted request overall (**270 👍**) is a one-time quick alarm: "I have to do the math and figure out HOW LONG until that time" ([Play: Xtreme](https://play.google.com/store/apps/details?id=com.alarmclock.xtreme.free)). Ads during the alarm drive real anger. Alarmy users report "30 SEC UNSKIPPABLE AD WALLS" and AliExpress ads opening when they tap dismiss. Ads also break anti-cheat: "clicking the ad during alarm effectively gets you out of the alarm screen." Alarmy's own developer confirms that prevent-power-off "works only while Alarmy stays on top" ([Play: Alarmy](https://play.google.com/store/apps/details?id=droom.sleepIfUCan)).

On pricing, users strongly favour small one-time payments. Positive examples: "$5 one time purchase for no ads is a great deal" ([Play: AMdroid](https://play.google.com/store/apps/details?id=com.amdroidalarmclock.amdroid)) and "It's like $5 forever, no subscription" ([Play: Xtreme](https://play.google.com/store/apps/details?id=com.alarmclock.xtreme.free)). Negative examples: "$100 for premium?????? You are an alarm" (Alarmy). A hard paywall before first use drove **48.7% of Wayk's negative reviews** ([Play: Wayk](https://play.google.com/store/apps/details?id=mg.WaykUp)). Lost purchases are a recurring trust breaker across AMdroid, Alarmy and Xtreme.

On missions, **math is the most-mentioned mission (267 reviews) but users learn it**: "I literally solve them in my sleep" ([Play: Sleep as Android](https://play.google.com/store/apps/details?id=com.urbandroid.sleep)). Physical missions in another room are the ones users love. Steps, photo and QR missions carry most of the bug reports, and a broken sensor traps users: "the step counting doesn't work, so I… [had] to uninstall the app to get my alarm to shut off" ([Play: Xtreme](https://play.google.com/store/apps/details?id=com.alarmclock.xtreme.free)). Typing missions freeze on the system keyboard (Alarmy, 118 👍; Xtreme, 76 👍).

The cheats users describe are system-level, not mission-level:

- Powering off or restarting the phone: **166 reviews**.
- Uninstalling or force-stopping the app: 186.
- Pressing home or recents.
- Tapping an ad.
- Spamming snooze from the notification.

Users also want the **wake-up check to make noise**: "I do wish that the follow-up alarm… made some noise. As it is, it only vibrates" (35 👍, [Play: Challenges](https://play.google.com/store/apps/details?id=com.garageapp.alarmchallenges)).

| Must-haves (v1.0) | Delighters (cheap, high-vote) | Avoid list (each is a top complaint about incumbents) |
|---|---|---|
| Rings every time, including after reboot, OS update and time-zone change; fallback tone | Big label on the ring screen showing why you set the alarm (65–79 👍) | Ads on the ring, mission or dismiss screens |
| **Never auto-dismiss by default**; configurable timeout | Quick alarm "in X minutes" or at a time (270 👍) | Subscriptions for core missions |
| Core missions free: math, typing on an in-app keypad, shake, QR/barcode, photo | Switch off the post-dismiss screen (59 👍) | Hard paywall before first use |
| Sensor-mission fallback plus a humane emergency exit | Preview / test-alarm button; default settings template; copy a QR code between alarms; confirm before delete | Weather, quotes or cartoon screens after dismissal that can't be turned off |
| Audible wake-up check, free | Ring history log (17 👍) | Internet required to save or ring |
| Snooze control: interval, limit, shrinking, disable | Screen-flash and flashlight option for deaf and hard-of-hearing users | Rating prompts at bedtime (35 👍 complaint) |
| Gradual volume from 0 to 10+ minutes, a volume cap that is respected, local MP3 support | Optional "harder mode" | Location or other permissions without need |
| One-off date, repeat days, skip next | Light streaks (wanted mostly by Fajr users) | Purchases that disappear after an update |

## The roadmap ships trust in v1.0 and reach in v1.x

The rule for scoping is simple. **v1.0 must make every alarm reliable before it adds variety.** Every feature below is tied to evidence. The free-or-Pro split is covered in the monetization section.

### v1.0 (closed test on 1 Dec 2026, public around 28 Dec 2026)

| Feature | Why (evidence) |
|---|---|
| `setAlarmClock` scheduling, `USE_EXACT_ALARM`, an idempotent "reschedule all" routine, Direct Boot ringing, a fallback sound chain, never auto-dismiss | Fixes the 9.8% "didn't ring" complaints and the 213 👍 auto-dismiss complaint ([Schedule alarms](https://developer.android.com/develop/background-work/services/alarms/schedule)) |
| Reliability toolkit: health checklist, OEM guides, "test alarm in 1 minute", ring log, missed-alarm notification, pre-alarm heartbeat | Samsung, Xiaomi, OnePlus and Huawei each score 5/5 for killing apps, and "no known solution on dev end" for Samsung sleeping apps ([dontkillmyapp](https://dontkillmyapp.com/samsung)) |
| Missions: **Math** (adaptive difficulty), **Typing** (in-app keypad), **Shake**, **QR/Barcode**, **Photo of a spot**; up to 2 missions chained per alarm for free | Most-mentioned and most-loved missions; physical missions in another room work best; the system keyboard freezes |
| Anti-escape basics: locked ring screen, no snooze from the notification, volume re-raise, a capped mute while solving, re-ring after reboot | Matches the restart, home-button and snooze-spam cheats users describe |
| Sensor fallback plus a humane emergency exit; audible wake-up check | Steps and QR traps; complaints about Alarmy's emergency escape; 35–37 👍 wake-check reviews |
| Snooze control; gradual volume; volume cap; ringtone or local MP3 with preview; vibration patterns | Gradual volume is mentioned 100 times; "volume of media, not alarm" bug (106 👍) ([Play: SaA](https://play.google.com/store/apps/details?id=com.urbandroid.sleep)) |
| Scheduling: repeat days, one-off specific date, skip next, quick "in X minutes" alarm | 270 👍 and 48 👍 requests |
| Big label on the ring and mission screens; minimal "Good morning" screen that can be switched off | 65–79 👍 and 59 👍 requests |
| Onboarding with the Mission Spot wizard and a "first two weeks are hard" message | HN users describe a hard first fortnight before the habit sticks ([HN](https://news.ycombinator.com/item?id=29352637)) |
| Stats basics: streak, last 7 days, success rate | Streaks are wanted mostly by Fajr users; keep them light |
| Pro one-time unlock with restore on every launch; banner on Stats and Settings only | Pricing evidence above |
| Next-alarm Glance widget | Apps with active widgets are exempt from the Restricted standby bucket ([App Standby Buckets](https://developer.android.com/topic/performance/appstandby)) |

### v1.x (January to June 2027)

| Release | Feature | Why |
|---|---|---|
| **v1.1, by 24 Jan 2027** | **Fajr Mode.** Pick a city manually (no location permission) or optionally grant approximate location once. Choose a calculation method and Asr madhab. The Fajr alarm moves daily with an offset. Multiple pre-alarms, a QR mission, an adhan or gentle tone, a suhoor alarm preset, a Fajr streak, and **zero ads on Fajr screens**. Urdu, Arabic and Indonesian listings | Pakistan scores 100 for "fajr alarm"; users ask for "more than one alarm for fajr" and "QR scan" ([Play: Fajr](https://play.google.com/store/apps/details?id=com.blink22.fajr)); prayer-app users hate ads ([Play: Muslim Pro](https://play.google.com/store/apps/details?id=com.bitsmedia.android.muslimpro&hl=en)) |
| v1.2 (Feb–Mar) | **Steps** mission (`TYPE_STEP_DETECTOR`, with an accelerometer fallback); **NFC tag** mission; memory/pattern game; "type your to-do or label" mission | NFC (64 👍 "converted from Alarmy because… NFC"); typing your to-do (31–41 👍) |
| v1.3 (Apr–May) | Harder mode (task required to disable or delete an alarm, emergency exit removed, snooze requires a mini-mission); random or rotating sounds against habituation; headphone-only output | 41 👍 "requiring a task to delete an alarm"; requests to rotate sounds |
| v1.4 (Jun) | Alarm groups and profiles; backup and restore to a file; Quick Settings tile | Requests from shift workers and for backup |

### v2 (ready for August–September 2027, back to school)

**Camera-counted exercise missions** (squats or push-ups with ML Kit Pose Detection, always with a fallback) give the most viral content mechanic ([Play: Pushy](https://play.google.com/store/apps/details?id=com.iuri.pushups_alarm)). **Wear OS** adds dismiss, snooze and a wrist-vibration wake-up check. Google's "Best for Watches 2025" went to a sleep and alarm app, which makes this a route to editorial featuring ([Google blog](https://blog.google/products/google-play/best-apps-games-2025/)). Wear OS apps must target API 35 or higher. v2 also adds **calendar and shift sync**, **sharing a streak with a friend**, and optional Spotify sounds. Sleep tracking stays out: it appears in only 1.5% of negative reviews, Alarmy users call it bloat, and Sleep as Android owns that niche.

### Missions: anti-cheat and fallback design

| Mission | How it works | Anti-cheat | Fallback / trap prevention |
|---|---|---|---|
| **Math** | 3 difficulty levels plus "adaptive", which adds one step after 5 easy wins in a row. Large keypad buttons (64dp or more). The "no calculator" note appears at setup | A fresh random problem each time; the problem changes if the user is idle for 20 seconds | No fallback needed. This mission is itself the universal fallback |
| **Typing** | Type a phrase, the alarm label, or a to-do on an **in-app keypad** (never the system keyboard) | Phrases are random; paste is disabled | Short phrases only (4–8 words), with forgiving case and spacing |
| **Shake** | `TYPE_LINEAR_ACCELERATION` magnitude, counting a shake above about 12–15 m/s² with a 250 ms debounce. Default target is 40; adjustable from 20 to 100. A morphing shape fills as the count rises | The count decays after 5 seconds of inactivity | Thresholds are heuristics, so tune them on real devices. Adjustable sensitivity |
| **QR / Barcode** | At setup, register a code at a spot (toothbrush, coffee jar, meds shelf). At ring time, scan with CameraX plus **bundled** ML Kit barcode (works offline on the first ring). Torch toggle | Only the registered code value is accepted; live camera only | After 60 seconds or 3 failed scans, offer "Use backup mission" (hard math, 5 problems). A "re-register code" path exists in settings, not on the ring screen |
| **Photo of a spot** | At setup, capture a reference photo. At ring time, compare a live capture using ML Kit image-label overlap plus a perceptual hash. Sensitivity slider | Live capture only, no gallery. A liveness hint checks for frame-to-frame change | Same 60-second / 3-attempt fallback. Before first unlock, while the device is still locked after a reboot, the reference photo is unreadable, so Wakeroo automatically uses math |
| **Steps** (v1.2) | `TYPE_STEP_DETECTOR` (latency under 2 seconds). The `ACTIVITY_RECOGNITION` permission is requested at setup, never at ring time | Steps must be continuous | If no step event arrives within 20 seconds, switch to an accelerometer heuristic, then to the fallback |

The **anti-escape layer** relies only on policy-safe tools:

- **Locked ring screen.** The ringing Activity uses `showWhenLocked`/`turnScreenOn`, `excludeFromRecents` and `resizeableActivity="false"` (which blocks the multi-window force-stop trick on phones), and `BackHandler` blocks Back.
- **The service fights back.** If the Activity loses focus, the ringing service re-posts the full-screen intent within seconds. The notification has **no dismiss or snooze action** when missions are set, only "Open mission". Volume keys are consumed, and the alarm-stream volume is restored if lowered.
- **Capped mute while solving.** Sound drops to 20% while the user is making progress and returns to full after 15 seconds without progress. This closes the "hold a finger on the screen" loophole users found in Challenges.
- **Re-ring after reboot.** If the phone restarts mid-ring, the boot receiver does not ring directly, because a boot-started media FGS is blocked on Android 15+ and suppressed on 17. Instead it **schedules a fresh `setAlarmClock` for about 60 seconds later**. That alarm fires through the exact-alarm exemption and rings normally. This reconciles the "restart cheat" fix with Android's boot restrictions ([FGS types](https://developer.android.com/develop/background-work/services/fgs/service-types); [Android 17 audio hardening](https://developer.android.com/about/versions/17/changes/bg-audio)).
- **Out of scope by policy.** Accessibility-based "prevent power-off/uninstall" and Device Admin are excluded. Play bans using Accessibility to stop users disabling or uninstalling apps, and Android 17's Advanced Protection Mode revokes it ([Play Accessibility policy](https://support.google.com/googleplay/android-developer/answer/10964491); [The Hacker News](https://thehackernews.com/2026/03/android-17-blocks-non-accessibility.html)). Force-stop and uninstall cannot be prevented, and the FAQ says so honestly.

The **emergency exit** is humane but costly. A small "Emergency" text button opens a sheet. The user **holds a button for 15 seconds** while a ring drains (the alarm keeps sounding), then types "I am awake" on the in-app keypad. The escape is logged and breaks the streak. Harder mode (v1.3) can remove it. For planned exceptions, "Skip next" and "Pause until…" sit on the home screen. In harder mode they require completing the alarm's mission first. This answers both complaint camps: people trapped while driving or in an emergency call, and power users who want no escape.

The **wake-up check** runs after dismissal. Wakeroo schedules a second `setAlarmClock` at +3 minutes (adjustable from 1 to 10). It posts a **sound-ramping** full-screen "Still up?" with a 60-second countdown. If the user doesn't tap, the full alarm restarts with its missions. It is free and on by default for new alarms.

### Reliability features (all v1.0)

The reliability set includes:

- **Scheduling.** `setAlarmClock` for every alarm, the wake-up check and the post-reboot re-arm.
- **Rescheduling.** A single idempotent `rescheduleAll()` triggered by `LOCKED_BOOT_COMPLETED`, `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_SET`, `TIMEZONE_CHANGED` and `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`.
- **Time handling.** Wall-clock times stored with `ZoneId` and recomputed after time changes, because DST bugs are a common cause of "didn't ring".
- **Direct Boot.** A device-protected mirror of the next triggers and ring settings, so alarms ring before first unlock.
- **Pre-alarm heartbeat.** An exact alarm 30 minutes before each alarm checks that the main `PendingIntent` still exists and re-arms it if it doesn't.
- **Ring log and missed-alarm detection.** Every occurrence is logged, and on app start, boot and unlock the app compares expected against fired. A missed alarm produces a "Your 7:00 alarm didn't ring — fix it" notification that deep-links to the OEM guide.
- **Test alarm.** A "Test alarm in 1 minute" button in onboarding and settings, re-prompted after an OS update (detected via a build-fingerprint change).
- **Health checklist** (described in the UX section).
- **Fallback sound chain.** Custom URI, then the system default alarm, then a bundled raw tone.
- **Offline.** No network is needed anywhere on the alarm path.

## Expressive Dawn: a sky that moves with your morning

### Design system

Material 3 Expressive gives the most "trendy and animated" result for a beginner's effort. Google built it on **46 studies with 18,000+ participants**, and testers spotted key elements up to **4x faster** ([Android Authority](https://www.androidauthority.com/google-material-3-expressive-details-3554486/)). Google's open-source Androidify sample shows every pattern needed: `MaterialExpressiveTheme` with `MotionScheme.expressive()`, `MaterialShapes` clips, a shape-morph reveal and press-scale feedback ([Android Developers Blog](https://android-developers.googleblog.com/2025/05/androidify-building-delightful-ui-with-compose.html)). **One dependency change is required.** Expressive APIs were removed from material3 1.4.0 and live in **1.5.0, currently 1.5.0-beta01 (2026-10-07)**. Add an explicit `androidx.compose.material3:material3:1.5.0-beta01` over the BOM, and move to 1.5.0 stable when it ships. Most Expressive APIs are now stable, but **`MaterialShapes` and `LoadingIndicator` still need `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`** ([material3 release notes](https://developer.android.com/jetpack/androidx/releases/compose-material3)).

| Token | Value | Use |
|---|---|---|
| Night / background | `#0E1120` | Default dark background; ring screen |
| Surface | `#1A1E33` | Cards, sheets |
| Primary: Sunrise Coral | `#FF7A59` | Primary buttons, FAB, active switches, "Start mission" |
| Secondary: Dawn Lavender | `#B9A7FF` | Chips, secondary actions, night sky gradients |
| Tertiary: Morning Gold | `#FFC857` | Streaks, success, sun mark highlights |
| Text | `#F5F3EE` | Off-white instead of pure white, to avoid glare at 6 AM |
| Error | `#FF5470` | Wrong answer, failed scan |
| Light mode | Background `#FFF8F2`, primary `#E85D3F` | Daytime use; ring screen always stays dark |
| Sky states | Night `#0E1120`→`#1A1E33`; Dawn `#B9A7FF`→`#FF7A59`; Morning `#FF7A59`→`#FFC857`; Day `#FFF8F2` | Background `Brush.verticalGradient` keyed to the next alarm's hour (home) or the selected hour (editor) |

**Dark theme is the default.** Dynamic colour (Material You) is offered as an option on ordinary screens only. The ring and mission screens always use the fixed high-contrast palette. Run every pair through a WCAG checker before shipping; the palettes have not been contrast-tested.

**Typography uses free Google Fonts.** [Outfit](https://fonts.google.com/specimen/Outfit) handles display text and the clock, and [Inter](https://fonts.google.com/specimen/Inter) handles body and UI text. Bundle both as downloadable or embedded font resources. Outfit's tabular-figure support was not verified, so render clock digits in **fixed-width boxes** (one `Box` per digit) and the time never jitters as it changes. The type scale is:

| Text | Font and size |
|---|---|
| Ring-screen time | Outfit, 96–120sp, auto-sized with `TextAutoSize.StepBased` |
| Home hero time | Outfit Medium, 64sp |
| Card time | Outfit Medium, 40sp |
| Headlines | Outfit SemiBold, 24–28sp |
| Body | Inter, 16sp |
| Labels | Inter Medium, 14sp |

Use `sp` everywhere and test at 200% font scale.

**Shapes.** Cards use 28dp corners. Buttons and chips are full pills. The **sun brand mark** is a `MaterialShapes` polygon (Sunny / Cookie9Sided via `.toShape()`) in gold over a lavender horizon arc. The optional kangaroo nod is a dotted **bounce-arc trajectory** above the sun, an abstract mark rather than a character. The FAB is a rounded square that morphs into the editor container. The app icon is a coral sun rising over a lavender arc on Night, with an adaptive icon and a monochrome themed variant.

**Motion principles.** There are four rules:

1. **Springs, not durations.** Use `MaterialTheme.motionScheme.defaultSpatialSpec()` for position, size and scale, `fastSpatialSpec()` for small elements, and `defaultEffectsSpec()` (no bounce) for colour and alpha.
2. **Calm at night, energetic in the morning.** Bedtime screens use low-bounce springs. Success and morning screens use the expressive bounce.
3. **One hero motion per screen.** Avoid stacked effects.
4. **Never animate information away.** Countdowns and labels stay readable throughout.

**Haptics.** Use Compose's semantic `HapticFeedbackType` values ([reference](https://developer.android.com/reference/kotlin/androidx/compose/ui/hapticfeedback/HapticFeedbackType)):

| Moment | Haptic |
|---|---|
| Each time-picker minute | `SegmentTick` |
| Alarm switch on / off | `ToggleOn` / `ToggleOff` |
| Correct mission step, success | `Confirm` |
| Wrong answer, failed scan | `Reject` |

Ringing vibration is a separate system: `VibratorManager` waveforms with a slow ramp.

**Reduce motion.** Compose has no built-in flag. Add a Settings → Motion option (System / Full / Reduced), and in "System" mode read `Settings.Global.ANIMATOR_DURATION_SCALE`, treating 0 as reduced. Verify this API behaviour on devices. Expose it through a `LocalReducedMotion` CompositionLocal. When reduced, springs become `snap()`, the breathing glow and confetti are removed, and shared-element transforms become cross-fades.

**Performance.** Follow four practices:

- Read animated state in lambdas (`graphicsLayer {}`, `drawBehind {}`, `Modifier.offset {}`).
- Use `derivedStateOf` so the countdown recomposes once a minute.
- Give every `LazyColumn` stable keys.
- Add a Baseline Profile and test release builds.

**Gradients and illustration.** Use animated `Brush` gradients everywhere. AGSL `RuntimeShader` via `ShaderBrush` is an optional API 33+ enhancement for the ring-screen glow ([AGSL guide](https://developer.android.com/develop/ui/views/graphics/agsl/using-agsl)). Use at most one or two Lottie files (onboarding, success) ([Callstack](https://callstack.com/blog/lottie-vs-rive-optimizing-mobile-app-animation)).

### Screen-by-screen spec

| Screen | Layout | Animation and haptics | Compose APIs |
|---|---|---|---|
| **Onboarding 1: Value** | Full-screen night-to-dawn sky. Sun mark rises. "Wake up for real." One coral "Get started" pill in the bottom third | Sun rises with a spatial spring; sky colour animates from Night to Dawn over 1.2 s | `Animatable` offset; `animateColorAsState` on gradient stops; `MaterialShapes.Sunny.toShape()` |
| **Onboarding 2: Mission Spot wizard** | "Where should your off-switch live?" Three big tiles (Bathroom sink, Kitchen/coffee, Meds shelf) plus "Just math for now". Tip: "The first two weeks are hard. Then it sticks." | Tiles scale-in with a stagger; the selected tile morphs into a cookie shape | `LazyVerticalGrid`; `animateFloatAsState` scale; `Morph` clip |
| **Onboarding 3–5: Permissions** (one ask per screen) | (3) Notifications: system `POST_NOTIFICATIONS` dialog. (4) Full-screen alarm: shown **only if** `canUseFullScreenIntent()` is false, with a deep link to `ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT`. (5) Phone brand guide: detects `Build.MANUFACTURER` and shows 2–3 illustrated steps (e.g., Samsung: battery "Unrestricted" plus "Never sleeping apps"), opening `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` (no special permission). Exact-alarm permission is never asked: `USE_EXACT_ALARM` is granted at install | A check icon draws on grant (`Confirm` haptic); "Skip for now" is allowed. Skipped items show as amber on the home health card | `rememberLauncherForActivityResult`; `AnimatedContent` between steps; `HorizontalPager` with page indicator |
| **Onboarding 6: Test alarm** | "Let's prove it works." Button: "Ring in 1 minute — lock your phone." Result screen logs success | Countdown ring with `CircularWavyProgressIndicator`; confetti burst on success (skipped when reduce motion is on) | `CircularWavyProgressIndicator`; Lottie (optional) |
| **Home / alarm list** | Sky background keyed to the next alarm's hour. **Hero card**: "Next alarm in 7 h 20 m", big time, label, mission icons, **health card** (green, amber or red). Alarm cards: big time, day chips, mission icons, switch. Swipe to skip or delete (confirm before delete). Extended FAB "+ Alarm" bottom-right; "Quick alarm" chip row (+10 min, +30 min, In 8 h) | Hero countdown ticks once a minute; switches use `ToggleOn`/`ToggleOff` haptics and the card dims with an effects spring; swipe springs back; **FAB morphs into the editor** via a container transform | `LazyColumn` with stable keys; `SwipeToDismissBox`; `SharedTransitionLayout` + `Modifier.sharedBounds`; `derivedStateOf` countdown |
| **Create / edit alarm** | Top: **time wheel** (hours and minutes, one-thumb) with a live "Rings in 8 h 05 m" line beneath. Chips: +5, +15, +30, "8 h sleep". Sections as rounded cards: Repeat (day chips or "Pick a date"), Label ("Why are you getting up?"), Missions (opens picker), Sound and volume (ringtone/MP3, cap slider, ramp 0–15 min, vibration), Snooze (interval, max, shrink, off), Wake-up check (on, +3 min), "Never stop until mission" (default on). Sticky "Save" pill at the bottom | Sky gradient animates to the selected hour (navy at 2 AM, peach at 6, gold at 9); `SegmentTick` per minute; a Save success snackbar reads "Alarm set for 7 h 20 m from now" | `LazyColumn` + `rememberSnapFlingBehavior` wheel (fallback: M3 `TimePicker`/`TimeInput`); `animateColorAsState`; `ButtonGroup` for snooze presets; `SnackbarHost` |
| **Mission picker** | Bottom sheet with a grid of mission tiles (icon, name, "works in another room" badge on QR/Photo). Selecting opens a per-mission config: difficulty preview ("Try it now"), count, register code or photo. "Add second mission" (free up to 2). Reorder by drag | Tiles press-scale; the selected tile expands into the config with shared bounds; a live mini-preview runs inside the card | `ModalBottomSheet` (no shared elements inside; use `AnimatedContent` instead); `FlowRow`; `updateTransition` |
| **Ringing** | Fixed dark palette. Slow-breathing radial glow behind an **enormous time**. Label in 24sp+ ("Exam at 9 — don't snooze"). One giant **"Start mission"** pill (88dp tall, bottom third). Smaller "Snooze 9 min (2 left)" if allowed. Tiny "Emergency" text link top-right | Glow breathes on a 4 s infinite cycle (static when reduced); the button gently pulses its scale; **no other motion** | `rememberInfiniteTransition`; `drawBehind` radial `Brush`; `BackHandler`; `TextAutoSize.StepBased`; `Modifier.semantics` labels |
| **Math mission** | `LinearWavyProgressIndicator` across the top (problem 2 of 3). The problem in 48sp. A 4×3 keypad of 72dp keys. Label shown small above | Problems slide in and out with `AnimatedContent`; wrong answer gives a horizontal shake plus `Reject`; right answer gives `Confirm` and the wave advances | `AnimatedContent` with `slideInHorizontally`; `Animatable` shake offset; `LinearWavyProgressIndicator` |
| **Typing mission** | Target phrase in a large card; the typed text underneath with per-letter colouring; custom A–Z keypad | Correct letters tint gold, mistakes flash error red | Custom keypad composables; `AnnotatedString` |
| **Shake mission** | A big sun shape fills from bottom to top with a count ("27 / 40") | The shape morphs from Cookie to Sunny as the count rises; each shake gives a scale pop and a light tick | `Morph` + `RoundedPolygon`; `graphicsLayer` scale; `SensorManager` flow collected in a `LaunchedEffect` |
| **QR / Photo mission** | Full-bleed camera preview with a rounded viewfinder; torch button; hint chip "Scan the code on your toothbrush". Countdown to "Use backup mission" appears after 60 s | Viewfinder corners breathe while searching and snap green on a match (`Confirm`); a failed photo match shakes the frame (`Reject`) | CameraX `PreviewView` via `AndroidView`; `ImageAnalysis` (`STRATEGY_KEEP_ONLY_LATEST`); ML Kit analyzers |
| **Emergency exit sheet** | "Only for real emergencies." A large hold-to-escape button with a 15 s draining ring, then an "I am awake" keypad | The ring drains linearly; releasing early resets it with a spring | `pointerInput` + `detectTapGestures(onPress)`; `Animatable` progress |
| **Wake-up check** | Full-screen "Still up?" with a 60 s wavy countdown and one huge "I'm up" button; volume ramps if ignored | Countdown wave; `Confirm` on tap | `CircularWavyProgressIndicator`; FSI-launched Activity |
| **Good morning (success)** | Coral **colour splash expands from the button**; the sun morphs to Sunny; "Good morning, 6:34 — 5-day streak" and the label's "why". Auto-closes in 5 s. Can be turned off in Settings | Splash radius animates with an expressive spring; the streak number counts up; one `Confirm` haptic | `animateFloatAsState` circular reveal via `drawBehind`; `animateIntAsState`; `MaterialShapes` morph |
| **Stats / streaks** | Bento grid: streak tile (big gold number), 7-day bar row (full history with Pro), average wake time, mission success rate, ring log list ("Rang 6:30 · mission 6:34 · wake-check passed"). Free users see a banner at the bottom | Numbers count up on first view; tiles stagger in | `LazyVerticalGrid` bento; `animateIntAsState`; `Canvas` bars |
| **Settings + Reliability checklist** | Top card: **"Alarm health"** rows with a green/amber/red dot each: Exact alarms (`canScheduleExactAlarms`), Full-screen (`canUseFullScreenIntent`), Notifications and channel importance, Battery (`isIgnoringBatteryOptimizations`), Standby bucket (`getAppStandbyBucket`), Alarm volume > 0, Do Not Disturb, Phone-brand steps, Last test result with a "Test now" button. Then: Defaults for new alarms, Motion, Theme, Post-alarm screen on/off, Pro, Restore purchases, Privacy options (UMP), Help, Export ring log | Dots animate colour; tapping a red row expands inline instructions | `AnimatedVisibility`; `ListItem`; intents to system settings with `try/catch` fallback to app details |
| **Paywall (Pro)** | Opened only from a Pro feature, from Settings, or once after the 7th successful wake. Sky hero with the sun mark; three benefit rows; **"$4.99 once — yours forever"**; "Every mission stays free. Always." line; Restore purchases; close X always visible. Never shown in onboarding | The sun gently morphs; the price card scales in; a `Confirm` haptic on purchase | `BillingClient` (9.1.0) `queryProductDetailsAsync` / `launchBillingFlow`; `AnimatedContent` for the post-purchase thank-you |
| **Fajr Mode (v1.1)** | City and method picker; a timeline card showing today's Fajr time; the alarm rule "20 min before Fajr"; pre-alarms; missions; adhan picker; Fajr streak. No ads | A crescent-to-sun morph on the timeline at Fajr; the sky gradient follows the prayer time | Same components; prayer-time library (see below) |

## One idempotent scheduler sits under everything

### Package structure

For a beginner, use a single `:app` module organized by packages, plus a `:baselineprofile` module later. Use manual dependency injection (an `AppContainer` owned by the `Application`), because receivers and services need it too, and it avoids annotation-processing setup on bleeding-edge AGP 9.4 and Kotlin 2.4. Hilt is a fine later switch.

```
com.wakeroo.app
  WakerooApp.kt                 // Application + AppContainer
  core/designsystem/            // Theme.kt (MaterialExpressiveTheme), Color, Type, Shapes, Motion, LocalReducedMotion, components/
  core/data/db/                 // Room: AlarmEntity, MissionConfigEntity, RingEventEntity, RegisteredCodeEntity, DAOs
  core/data/prefs/              // DataStore (credential storage): settings, defaults template, Pro cache
  core/data/dps/                // Device-protected mirror: next triggers, ring config, "ringing now" state (JSON or DataStore)
  core/alarm/                   // NextTriggerCalculator (java.time), AlarmScheduler, receivers/, ring/RingService,
                                //   ring/AlarmPlayer, ring/VolumeGuard, heartbeat/, health/HealthChecker, oem/OemGuides
  core/missions/                // Mission engine + math/, typing/, shake/, scan/ (QR), photo/, steps/ (v1.2)
  core/billing/  core/ads/      // Play Billing wrapper; AdMob + UMP (lazy, never on the ring path)
  feature/onboarding, home, editor, missionpicker, ringing, wakecheck, success, stats, settings, paywall, fajr/
  widget/                       // Glance next-alarm widget
```

### Data layer

**Room** (credential-encrypted storage) holds four tables:

| Entity | Contents |
|---|---|
| `AlarmEntity` | id, hour, minute, zoneId, daysMask, oneOffDate, label, enabled, soundUri, volumeCap, rampSec, vibratePattern, snoozeMin, snoozeMax, snoozeShrink, wakeCheckMin, autoStopMin (null = never), skipNextDate, harderMode, emergencyExitEnabled |
| `MissionConfigEntity` | alarmId, order, type, difficulty, count, paramsJson (code value, photo reference path, label hash) |
| `RingEventEntity` | occurrenceId, alarmId, scheduledAt, firedAt, missionStartedAt, dismissedAt, outcome (dismissed / escaped / missed / snoozed), snoozeCount, wakeCheckResult |
| `RegisteredCodeEntity` | id, label, format, rawValue. Lets one code be reused across alarms, a requested feature |

**DataStore** holds settings: theme, motion, default alarm template, onboarding progress, a Pro entitlement cache, the last health-check result and the OS build fingerprint.

The **device-protected mirror** is written on every schedule change via `createDeviceProtectedStorageContext()`. It contains everything the ring path needs before first unlock: next trigger per alarm, sound fallback, and mission configs except photo references. It excludes personal data ([Direct Boot](https://developer.android.com/privacy-and-security/direct-boot)).

**Backup.** Enable Auto Backup with `dataExtractionRules` that exclude the device-protected mirror and the ring log. After a restore, force a health check, because exact-alarm and full-screen permission states don't transfer ([Auto Backup](https://developer.android.com/identity/data/autobackup)).

### The scheduling and ringing pipeline

| Step | Component | Key API | If it fails |
|---|---|---|---|
| 1. Compute | `NextTriggerCalculator` | `java.time` with `ZonedDateTime` from local time + `ZoneId`; repeat days, one-off date, skip-next | Unit-tested for DST, time zones and leap days |
| 2. Schedule | `AlarmScheduler` | `AlarmManagerCompat.setAlarmClock(am, triggerMillis, showIntent→editor, operation)`, where `operation = PendingIntent.getForegroundService(..., FLAG_IMMUTABLE)` targeting `RingService`. Schedule only the next occurrence, plus heartbeat and wake-check entries | If `canScheduleExactAlarms()` is false (rare OEM cases), fall back to `setExactAndAllowWhileIdle` and show a red health row |
| 3. Survive | `RescheduleReceiver` (`directBootAware`) | `LOCKED_BOOT_COMPLETED`, `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_SET`, `TIMEZONE_CHANGED`, `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`, all calling `rescheduleAll()`. **Never rings from boot.** If "ringing now" was persisted, re-arm `setAlarmClock` for +60 s; if an alarm passed while the phone was off, post a "missed" notification | Logged to the ring log |
| 4. Start | `RingService` | `ServiceCompat.startForeground(..., FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)` within seconds. Exact alarms are exempt from background FGS-start limits ([FGS restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)) | Crash handler posts a fallback high-priority notification |
| 5. Alert | Notification | Channel at `IMPORTANCE_HIGH`; `NotificationCompat` with `CATEGORY_ALARM`, `setOngoing(true)`, `setFullScreenIntent(ringActivityPI, true)`, a single "Open mission" action | If full-screen intent is revoked, the heads-up notification still rings, and the health card turns red |
| 6. Show | `RingActivity` | Manifest `showWhenLocked`, `turnScreenOn`, `showForAllUsers`, `directBootAware`, `excludeFromRecents`, `launchMode="singleTask"`, separate `taskAffinity`, `resizeableActivity="false"`; `BackHandler` | Service re-posts the full-screen intent if focus is lost |
| 7. Sound | `AlarmPlayer` | `MediaPlayer` with `AudioAttributes.USAGE_ALARM` + `CONTENT_TYPE_SONIFICATION`, looping; ramp the alarm-stream volume toward the cap; save and restore the user's original stream volume. **Always `USAGE_ALARM`**, the path Android 17's audio hardening exempts for exact-alarm holders ([Android 17 bg audio](https://developer.android.com/about/versions/17/changes/bg-audio)) | Custom URI, then `RingtoneManager.getDefaultUri(TYPE_ALARM)`, then a bundled raw tone |
| 8. Keep awake | `RingService` | `PARTIAL_WAKE_LOCK` with a 60-minute timeout; `VibratorManager` waveform | Timeout releases the lock; the ring log records it |
| 9. Finish | Mission engine → service | Mission done → stop sound → log → schedule wake-check `setAlarmClock` (+3 min) → recompute next occurrence → `stopForeground` | — |

Recommended manifest permissions:

```xml
<uses-permission android:name="android.permission.USE_EXACT_ALARM"/>
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" android:maxSdkVersion="32"/>
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
<uses-permission android:name="android.permission.WAKE_LOCK"/>
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
<uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT"/>
<uses-permission android:name="android.permission.VIBRATE"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK"/>
<uses-permission android:name="android.permission.CAMERA"/>
<uses-permission android:name="android.permission.INTERNET"/> <!-- billing + ads only -->
<!-- v1.2: ACTIVITY_RECOGNITION, NFC. Never: location, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, SYSTEM_ALERT_WINDOW, Accessibility, Device Admin -->
```

**Why these permission choices:**

- **`USE_EXACT_ALARM`** is auto-granted, cannot be revoked, and is explicitly allowed for "an alarm or timer app" ([Play exact alarm policy](https://support.google.com/googleplay/android-developer/answer/13161072)). It also exempts the app from the Restricted standby bucket, and it makes `systemExempted` FGS eligible.
- **`mediaPlayback` as the FGS type.** It is proven on Play by the open-source QRAlarm ([QRAlarm AlarmService](https://github.com/sweakpl/qralarm-android/blob/master/app/src/main/java/com/sweak/qralarm/alarm/service/AlarmService.kt)), and its review path is clear. The boot re-arm design avoids its one weakness, that it can't start from `BOOT_COMPLETED` on Android 15+. Try `systemExempted` on an internal track later; it is semantically closer and is listed for exact-alarm holders ([FGS types](https://developer.android.com/develop/background-work/services/fgs/service-types)).
- **Camera** is used only inside the visible `RingActivity`. A camera FGS cannot be created from the background ([FGS restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)).
- **No `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.** Alarm clocks are not on Play's list of acceptable uses, so the app uses the settings-list intent instead ([Doze](https://developer.android.com/training/monitoring-device-state/doze-standby)).

### OEM battery handling

No code can defeat Samsung's sleeping-apps lists or Xiaomi's autostart block. The answer is **guided user action plus detection**:

- **Health checklist** (above).
- **Per-brand guides** for Samsung, Xiaomi/Redmi/POCO, Oppo/realme/OnePlus, Vivo, Motorola, Huawei and Pixel. Each is two or three screenshots in the app, with the same text on wakeroo.com/help. The Samsung guide covers "Unrestricted" battery, "Never sleeping apps", and turning off "Put unused apps to sleep" ([dontkillmyapp Samsung](https://dontkillmyapp.com/samsung)). The Xiaomi guide covers Autostart and "No restrictions" ([dontkillmyapp Xiaomi](https://dontkillmyapp.com/xiaomi)).
- **Deep links by `Build.MANUFACTURER`**, wrapped in `try/catch` with a fallback to app-details settings. The AutoStarter library's component list has been stale since 2021, so keep a small list of your own ([AutoStarter fork](https://github.com/minuhariharan/AutoStarter)).
- **Missed-alarm detection** that deep-links to the right guide.
- **Process-kill logging** via `ActivityManager.getHistoricalProcessExitReasons()`.
- **Widget promotion.** Active widgets exempt the app from the Restricted bucket.

### Mission implementation notes

| Mission | Implementation |
|---|---|
| Math and typing | Pure Kotlin. No permissions needed |
| Shake | `SensorManager` `TYPE_LINEAR_ACCELERATION` exposed as a `callbackFlow`. Sampling is rate-limited when targeting API 31+, which is fine here ([Motion sensors](https://developer.android.com/develop/sensors-and-location/sensors/sensors_motion)) |
| QR / barcode | CameraX `ImageAnalysis`, plus **bundled** `com.google.mlkit:barcode-scanning:17.3.0` (~2.4 MB) with `setBarcodeFormats` restricted to registered formats. The unbundled model produces no results until it downloads, which could make an alarm impossible to dismiss ([ML Kit barcode](https://developers.google.com/ml-kit/vision/barcode-scanning/android)) |
| Photo | **Bundled** `com.google.mlkit:image-labeling:17.0.9` (~5.7 MB; 400+ labels; confidence threshold 0.7) ([ML Kit labeling](https://developers.google.com/ml-kit/vision/image-labeling/android)), plus a dHash computed in Kotlin. A match requires label overlap ≥ N and Hamming distance ≤ a threshold set by the sensitivity slider |

All processing happens on-device, which keeps the Data safety form simple.

### Libraries

| Purpose | Artifact | Version | Status |
|---|---|---|---|
| UI | Compose BOM | 2026.09.00 | In project |
| Expressive components | `androidx.compose.material3:material3` | **1.5.0-beta01** (explicit override) | Verified in [release notes](https://developer.android.com/jetpack/androidx/releases/compose-material3) |
| Core | core-ktx 1.19.1, lifecycle 2.11.0, activity-compose 1.13.0 | as listed | In project |
| Billing | `com.android.billingclient:billing-ktx` | **9.1.0** (v8+ required since 31 Aug 2026) | Verified ([Billing release notes](https://developer.android.com/google/play/billing/release-notes)) |
| Consent | `com.google.android.ump:user-messaging-platform` | **4.0.0** | Verified ([UMP](https://developers.google.com/admob/android/privacy)) |
| Barcode | `com.google.mlkit:barcode-scanning` | **17.3.0** (bundled) | Verified |
| Labeling | `com.google.mlkit:image-labeling` | **17.0.9** (bundled) | Verified |
| Persistence | Room (+ KSP), DataStore Preferences | Latest stable at setup | Not verified in research; confirm KSP supports Kotlin 2.4.21 |
| Navigation | Navigation Compose (type-safe routes, kotlinx-serialization) | Latest stable | Not verified |
| Camera | CameraX `camera-camera2`, `camera-lifecycle`, `camera-view` | Latest stable | Not verified |
| Widget | Glance AppWidget | Latest stable | Not verified |
| Ads | Google Mobile Ads SDK | Latest stable | Not verified |
| Reviews | Play In-App Review (`review-ktx`) | Latest stable | Not verified |
| Animation | Lottie Compose | Latest stable | Optional |
| Prayer times (v1.1) | batoulapps **Adhan** | Check the licence and Kotlin/JVM artifact | Named in prior research |

**Do not copy GPL code.** QRAlarm and Fossify Clock are GPL-3.0, so you may read them to learn patterns but not copy their code. yuriykulikov/AlarmClock is Apache-2.0 according to its README, so its permission-fallback logic in `AlarmSetter.kt` can be reused with attribution after confirming the licence ([AlarmClock](https://github.com/yuriykulikov/AlarmClock)). The best study order is the QRAlarm manifest, then `QRAlarmManager.kt`, then `AlarmService.kt`, then `AlarmReschedulingReceiver.kt` ([QRAlarm](https://github.com/sweakpl/qralarm-android)).

### Testing strategy

| Layer | What to test | How |
|---|---|---|
| Unit (JVM) | `NextTriggerCalculator`: every repeat pattern, skip-next, one-off dates, DST forward/back in 3 zones, time-zone travel; mission validators; dHash distance; snooze shrink maths | JUnit with a fixed `Clock`; aim for 100% coverage on the calculator |
| Data | Room migrations; device-protected mirror written on every change | Instrumented DAO tests, `MigrationTestHelper` |
| UI | Editor saves correct fields; ring screen blocks Back; mission flows complete | Compose UI tests (`createAndroidComposeRule`) |
| **Reliability (emulator)** | See the matrix below; run before every release | Scripted `adb` commands; record pass or fail in a sheet |
| **Real devices** | At minimum one Samsung (One UI), one Xiaomi/Redmi (HyperOS, common in PK/IN) and one Pixel on Android 17; then testers' phones | DontKillMyApp benchmark app ([dontkillmyapp](https://dontkillmyapp.com/)); 7 consecutive real mornings per device |
| Release health | Crash and ANR rates | Play Console Android vitals (free, no SDK) plus an in-app ring-log export |

| Reliability scenario (emulators on API 26, 31, 33, 34, 35, 36, 37) | How to simulate | Pass criteria |
|---|---|---|
| Doze | `adb shell dumpsys deviceidle force-idle`, then wait for the alarm | Rings on time |
| Standby buckets | `adb shell am set-standby-bucket com.wakeroo.app restricted` (and `rare`); `am set-inactive com.wakeroo.app true` | Rings on time |
| Reboot before unlock | Set an alarm 3 min ahead, `adb reboot`, don't unlock | Rings over the lock screen (Direct Boot path) |
| Restart mid-ring (cheat) | `adb reboot` while ringing | Re-rings about 60 s after boot |
| App update | `adb install -r` with a pending alarm | Still scheduled (`MY_PACKAGE_REPLACED`) |
| Time and zone change | Change the time zone in settings; set the clock forward | Alarm recomputed to the correct wall-clock time |
| Android 17 audio hardening | `adb shell cmd audio set-enable-hardening throw`; check logcat for `AudioHardening` | Sound plays |
| Full-screen intent revoked | Toggle it off in Special app access | Heads-up rings, the health row turns red |
| Silent, DND, alarm volume at 0 | Set each; ring | Rings via the alarm stream when DND allows alarms; a warning appears when volume is 0 |
| Custom sound deleted | Delete the MP3, then ring | Fallback tone plays |
| Camera denied or broken | Revoke `CAMERA`; ring with a QR mission | Backup mission offered after 60 s |

### Play Console declarations

| Declaration | What to submit |
|---|---|
| Exact alarm (`USE_EXACT_ALARM`) | Core function is an alarm clock ([Play Help](https://support.google.com/googleplay/android-developer/answer/13161072)) |
| Full-screen intent | Core function "setting an alarm" ([Play Help](https://support.google.com/googleplay/android-developer/answer/13392821)) |
| Foreground service type `mediaPlayback` | Description, user impact if deferred, and a **video link**: the alarm rings on a locked phone and the user completes a mission ([FGS/FSI requirements](https://support.google.com/googleplay/android-developer/answer/16965181)) |
| Data safety | Camera images processed on-device and not collected; purchase history via Play; AdMob collects device and advertising IDs (declare as shared for advertising); no location; no account |
| Ads | "Contains ads" = yes (free tier banner) |
| Content rating, target audience | Complete the questionnaire; choose 13+ and do not opt into the Families program (see open questions) |
| Health apps and financial features declarations | Mandatory forms for all apps; answer "none" ([Play Help](https://support.google.com/googleplay/android-developer/answer/14738291)) |
| Privacy policy URL | wakeroo.com/privacy |
| App access | "No login required" |
| Later | `ACTIVITY_RECOGNITION` justification (steps, v1.2); never Accessibility |

## Pay once, $4.99, and never monetize the wake-up

**Decision: a one-time Pro unlock at US $4.99, with no subscription**, using Play's regional price conversion. Charge noticeably less in Pakistan, India, Indonesia and Brazil, roughly $1.50–$2.50 equivalent. That range is a judgment call; check it in Play's price templates. The evidence is consistent:

- Reviewers praise **$5 one-time unlocks** in AMdroid, Challenges and Xtreme.
- Subscription anger is high: Wayk's paywall drives 48.7% of its negative reviews, and "$100 for premium… You are an alarm" (Alarmy).
- One-time purchases avoid Android's involuntary-churn problem. **About 31% of Play subscription cancellations are billing failures**, against 14% on iOS ([RevenueCat SOSA 2026](https://www.revenuecat.com/state-of-subscription-apps)).
- An alarm has no ongoing server costs to justify a subscription.

Alarmy's subscriptions make money because of its brand and scale. Wakeroo's edge is being the fair alternative. A Play Billing 8+ library is mandatory, and the plan uses 9.1.0. Call `queryPurchasesAsync` on **every launch** and cache the entitlement, so a paid user never loses Pro, a recurring complaint about the incumbents.

| Free, forever | Pro ($4.99 once) |
|---|---|
| All five v1.0 missions and every later standard mission (steps, NFC, memory) | Up to 5 chained missions per alarm (free: 2) and random mission rotation |
| All reliability features, health check, test alarm, ring log | Harder mode: no emergency exit, task required to disable or delete alarms, snooze requires a mission |
| Wake-up check, emergency exit, snooze controls, gradual volume, custom MP3 | Extra sky themes, alternative app icons, a premium sound pack plus sound rotation |
| Labels, quick alarm, skip next, one-off dates, 1 widget | Full stats history, CSV export, extra widget styles |
| **Fajr Mode in full, with no ads anywhere in it** | **No banner** on Stats and Settings |

**AdMob placement rules.** Free users see exactly **one adaptive banner, at the bottom of the Stats and Settings screens.** No interstitials, app-open ads or native ads anywhere, ever. Rewarded ads come later, at most as an opt-in "try a Pro theme for 7 days".

The **alarm path** means every screen and moment from scheduling to wake-up: the editor, ringing, missions, the emergency exit, the wake-up check, "Good morning", onboarding and all Fajr screens. **The alarm path never shows or preloads an ad**, and the Mobile Ads SDK is never initialized from the ring process, because of Android 17 memory limits and startup latency.

UMP 4.0.0 runs `requestConsentInfoUpdate()` at app launch, outside the ring path, before any ad request. A "Privacy options" row appears when required ([UMP](https://developers.google.com/admob/android/privacy)). Play's Better Ads rules forbid unexpected full-screen interstitials ([Play Help](https://support.google.com/googleplay/android-developer/answer/9857753)). In this category, ads near the alarm are also the top reason users leave incumbents.

**Expected economics, as honest ranges.** These are illustrative arithmetic using the earlier research's benchmarks, not forecasts. Assumptions:

- About **$4 net per Pro sale**. Play takes 15%, or possibly 20% + 5% on some one-time purchases in the US, EEA and UK; verify on [Play Console Help](https://support.google.com/googleplay/android-developer/answer/112622).
- **1–2% of installs buy Pro.** No sourced one-time conversion benchmark exists. Android subscription download-to-paid is 0.9% ([RevenueCat](https://www.revenuecat.com/state-of-subscription-apps)).
- Banner global eCPM of **$0.2–0.8** ([Playio](https://blog.playio.co/mobile-game-ecpm-benchmarks-2026)).
- About **1.5 banner impressions per DAU per day**, because Stats and Settings are low-traffic screens.

| Scenario at month 6 | Cumulative installs | DAU | Pro revenue (cumulative) | Banner revenue / month | Run rate / month |
|---|---|---|---|---|---|
| Conservative (ASO only) | 3,000 | ~300 | 30 sales ≈ $120 | ≈ $5 | **≈ $15–30** |
| Base (ASO + closed-test reviews + Fajr Ramadan bump) | 15,000 | ~1,500 | 225 sales ≈ $900 | ≈ $30–40 | **≈ $150–200** |
| Upside (one viral Mission Spot video or a strong Ramadan) | 80,000 | ~10,000 | 1,600 sales ≈ $6,400 | ≈ $200–300 | **≈ $1,000–1,500** in peak months |

For scale:

- Alarmy earns roughly $4 per MAU per year, mostly from ads at huge volume.
- RevenueCat's **median subscription app earns $72/month one year after launch** ([RevenueCat](https://www.revenuecat.com/state-of-subscription-apps)).
- Banners at 1,000 DAU on a global mix earn tens of dollars a month.

**Money will come from Pro sales during traffic spikes, not from ads.** The banner mostly gives a visible reason to upgrade. If Fajr Mode becomes the main traffic source, consider dropping ads for everyone and adding a "support the developer" tier at $9.99. Muslim users strongly prefer ad-free apps.

## Exact-match titles and mission videos replace an ad budget

### Store listing

Google Play limits the **title to 30 characters** and bans emojis, ALL CAPS, "#1"/"best" and price or promo text in it ([Metadata policy](https://support.google.com/googleplay/android-developer/answer/9898842)). The title carries the strongest keyword weight. The short description is 80 characters, and the first 250–300 characters of the long description weigh most ([AppRadar](https://appradar.com/academy/aso-basics/app-store-ranking-factors)).

| Field | Copy | Length |
|---|---|---|
| **Title (launch)** | **Wakeroo: Loud Alarm Clock** | 25 |
| Title experiment A | Wakeroo: Mission Alarm Clock | 28 |
| Title experiment B | Wakeroo: Heavy Sleeper Alarm | 28 |
| **Short description** | **Loud alarm for heavy sleepers: solve math, scan QR or snap a photo to stop it.** | 78 |
| Short description experiment | Heavy sleeper? Solve math, scan a QR or snap your sink to stop this loud alarm. | 79 |
| Fajr-locale title (Urdu/Arabic/Indonesian, translated) | Wakeroo: Fajr Alarm & Missions | 30 in English |
| Fajr-locale short description (translated) | Fajr alarm you can't sleep through: scan a QR or solve math to stop it. | 71 |
| Developer name | Wakeroo (no keyword stuffing, to avoid metadata-policy risk) | — |

"Loud alarm clock" gets broader autocomplete coverage across the US, India and Pakistan than "mission alarm". "Mission alarm" is the least defended head term, so test it as variant A with Play's free store-listing experiments. Give each test 4–8 weeks. Avoid "No ads" anywhere in the listing, because free users see a banner. Say "**no ads while it rings**" in the long description instead. The **long description's first 300 characters** should read roughly: "Wakeroo is a loud alarm clock for heavy sleepers that won't stop until you complete a mission: solve math, type a phrase, shake, scan a QR code or take a photo of your sink. Every mission is free, there are no ads while it rings, and it's built to ring every time, even after a reboot." Follow with sections for missions, reliability, Fajr Mode, students and ADHD routines ("scan the code on your meds shelf"; no medical claims), and pay-once Pro.

| Keyword cluster | Phrases (from Play autocomplete, US/IN/PK) | Where to use |
|---|---|---|
| Head | alarm clock, loud alarm clock, loud alarm, alarm app | Title, short description |
| Heavy sleeper | alarm clock for heavy sleepers, alarm for heavy sleepers, loud alarm clock for heavy sleepers, super loud alarm clock | Short and long description |
| Mission | mission alarm, mission alarm clock, alarm with task, alarm with challenges, wake up alarm task | Title variant A, long description |
| Mission types | math alarm clock, alarm with math problems, qr alarm, qr code alarm clock, barcode alarm clock, shake alarm, photo mission alarm, step alarm (v1.2) | Long description, screenshots |
| Intent long-tail | alarm that forces you to wake up, alarm that won't turn off, alarm that doesn't stop, alarm that makes you take a picture | Long description |
| Audience | alarm for students, alarm for study, adhd alarm, alarm for namaz time, alarm for prayer, fajr alarm | Long description; Fajr locales |

Source: Play autocomplete collected 2026-10-20 (naming research); [AppTweak](https://www.apptweak.com/en/aso-blog/app-name-guidelines-and-best-practices).

**Screenshots** (8 portrait frames, a caption of 5 words or fewer on each, Expressive Dawn backgrounds):

1. "Can't sleep through this" (ring screen)
2. "Scan your toothbrush to stop it" (QR mission in a bathroom)
3. "Math, photo, shake, typing — all free"
4. "Rings even after a reboot" (health card)
5. "No ads while it rings"
6. "Still up? Wake-up check"
7. "Your mornings, tracked" (streak bento)
8. "Pay once. Keep it forever." (Pro)

Add a feature graphic with the sun mark and the chosen tagline, plus a 30-second promo video, which can reuse the Mission Spot short.

**Localization priorities.** Localized listings lift installs measurably: Noom's international Android revenue rose **80%** ([Think with Google](https://marketfinder.thinkwithgoogle.com/intl/en_us/case-study/noom)), and SayHi saw **120% more installs** in new languages ([Play Console](https://play.google.com/console/about/sayhi-casestudy/)). Research keywords per market rather than translating literally.

| Order | Locale | Why | When |
|---|---|---|---|
| 1 | English (US default, plus en-GB and en-IN listings) | Highest-paying markets; India has the strongest Alarmy brand share | v1.0 |
| 2 | Urdu | Pakistan is #1 for "fajr alarm" and #8 for "loud alarm" | v1.1 (Fajr) |
| 3 | Arabic | UAE, Qatar, Saudi Arabia, Egypt (Alarmy usage rank #57) | v1.1 |
| 4 | Indonesian | Fajr apps rank there; low Alarmy awareness | v1.1 |
| 5 | Turkish, Spanish (Mexico) | Among Alarmy's top usage markets ([Similarweb](https://www.similarweb.com/app/google/droom.sleepIfUCan/india/)) | v1.2 |
| 6 | Portuguese (Brazil), Hindi | Brazil's school year starts in February; strong organic consumer-app markets | v1.3 |

Have a native speaker check that "Wakeroo" has no negative meaning in Urdu, Hindi, Indonesian, Portuguese and Spanish. This was not verified.

### Closed test: 20 testers, 14 days, two updates

New personal accounts need **at least 12 testers opted in continuously for 14 days** before applying for production. Google usually reviews the application within 7 days and can demand more testing if engagement is thin ([Play Console Help](https://support.google.com/googleplay/android-developer/answer/14151465)). Third-party guides say the clock resets if testers drop below 12 ([ExtendsClass](https://extendsclass.com/blog/google-plays-closed-testing-requirement-what-developers-need-to-know-in-2026)). Because the test runs into December holidays, **recruit 20–25 testers**:

- About 8 friends and family.
- About 8 university classmates, the core persona.
- About 5 from a local mosque or Muslim student WhatsApp group, as early Fajr Mode testers.
- The rest from a tester-swap community.

Recruit them during weeks 4–5 so they are opted in from the first day of the test.

**Engagement script.** Each tester sets Wakeroo as their real morning alarm, tries the QR mission once, and taps "Test alarm in 1 minute". Feedback goes through a 5-question Google Form that records phone brand, model and Android version. Your real-device matrix grows with no hardware cost.

**Ship two updates during the test.** On about day 5, add Pro, the paywall and the banner. On about day 11, ship tester fixes and the widget. That gives the production questionnaire genuine "changes made" to report. Draft answers to the questionnaire as you go: how you recruited, engagement, feedback, changes and readiness.

### Zero-budget growth tactics, ranked by expected payoff

1. **Mission Spot short videos.** Post 3 a week on TikTok, YouTube Shorts, Instagram Reels and X. Formats: "POV: my alarm's off-switch is taped to the bathroom mirror", "Can you solve this at 6 AM?", "I rebooted my phone to cheat my alarm — it rang again", plus Fajr wake-up challenges before Ramadan. Mission clips are inherently watchable, as Alarmy's 26.7M-view moment showed ([Dexerto](https://www.dexerto.com/entertainment/viral-alarm-app-forces-users-to-complete-mini-games-before-it-will-shut-off-3412598/)).
2. **Turn the closed-test cohort into launch-week reviews.** Ask personally. Review velocity outweighs average rating in ranking ([AppRadar](https://appradar.com/academy/aso-basics/app-store-ranking-factors)).
3. **In-app review prompt** using the Play In-App Review API. Show it after the **3rd successful wake-up**, when the user next opens Home or Stats during the day. **Never at bedtime and never on the alarm path.**
4. **Fajr communities before Ramadan**: Pakistani university Islamic societies, mosque WhatsApp groups and Gulf student groups, with an Urdu/Arabic Fajr landing page.
5. **Answer existing questions honestly** on Quora threads such as "best alarm app for heavy sleepers" ([Quora](https://www.quora.com/What-is-the-best-alarm-app-for-heavy-sleepers)) and in relevant forums, following each community's self-promotion rules.
6. **Displace stale incumbents.** Target "wake up alarm" and "step alarm", where an app untouched since 2023 still ranks, by mentioning those phrases naturally.
7. **Update every 2–4 weeks with real changes.** Update cadence and vitals affect visibility ([AppRadar](https://appradar.com/academy/aso-basics/app-store-ranking-factors)).
8. **Wear OS in v2** as a path to editorial featuring.

Low-payoff channels exist but deserve little time. SuperAlarm's "Show HN" launch got 3 points ([HN](https://news.ycombinator.com/item?id=40144494)), so treat Hacker News and Product Hunt as optional.

### wakeroo.com: five pages, free hosting

The site exists for the privacy policy, support and trust, not for SEO, since niche web search volume is close to zero. Build a static site, either plain HTML or Astro/Hugo with the Expressive Dawn colours, and host it free on **GitHub Pages or Cloudflare Pages** with HTTPS. HTTPS is mandatory for `.app` domains. Pages:

1. **Home.** Hero, tagline, three promises (rings every time, missions free, no ads while it rings), Play badge, embedded promo short.
2. **/privacy.** Required by Play and AdMob. Covers on-device camera processing, AdMob identifiers and no location.
3. **/help.** FAQ plus **"My alarm didn't ring" guides per brand**, which the in-app health screen links to.
4. **/fajr.** English, Urdu and Arabic Fajr Mode landing page for Ramadan sharing.
5. **/press.** Icon, screenshots, one-paragraph story.

Add AdMob's `app-ads.txt` at the root and free email forwarding for support@wakeroo.com. The only cost beyond the $25 Play fee is domain registration, typically about $10–20 per year per domain (check your registrar). If even that must wait, launch on a free `*.pages.dev` or `*.github.io` subdomain and move later.

## Sixteen weeks from an empty project to Ramadan-ready

Weeks run Tuesday to Monday from today, **Tuesday 20 October 2026**. The schedule is aggressive for a beginner but workable with AI-assisted coding. Its built-in safety valve is a **cut line**: if week 6 slips, the Photo mission and stats move to the first closed-test update, but reliability and the QR mission never get cut.

| Wk | Dates | Build focus | Demoable on the emulator | Launch / ops tasks |
|---|---|---|---|---|
| 1 | Oct 20–26 | material3 1.5.0-beta01, Expressive Dawn theme, fonts, `LocalReducedMotion`, navigation skeleton, Room schema, DataStore, `AppContainer` | Themed empty Home with animated sky and sun mark | Register wakeroo.com/.app; WIPO search; pay $25 and start Play Console identity verification; GitHub branch rules |
| 2 | Oct 27–Nov 2 | `NextTriggerCalculator` (+ unit tests), `AlarmScheduler` (`setAlarmClock`), `RingService` (FGS, notification, full-screen intent), basic `RingActivity`, `AlarmPlayer` (`USAGE_ALARM` + fallback chain) | **Alarm rings on a locked emulator 1 minute after creation** | Draft privacy policy |
| 3 | Nov 3–9 | Home list, editor with time wheel and chips, repeat/date/skip, label, sound/volume/ramp, snooze; reschedule receivers; Direct Boot mirror; ring log | Create and edit alarms; `adb reboot` before unlock → still rings | Start the tester recruitment list |
| 4 | Nov 10–16 | Mission engine; Math, Typing (keypad), Shake; anti-escape (lock, focus re-post, volume guard, capped mute, re-ring after reboot) | Ring → math → dismiss; reboot mid-ring → re-rings | Recruit testers (target 20–25) |
| 5 | Nov 17–23 | CameraX + ML Kit QR mission (register, scan, torch, fallback); Photo mission; emergency exit; wake-up check | QR mission with an on-screen code; wake-up check re-rings if ignored | Store listing text drafted; screenshot mock-ups |
| 6 | Nov 24–30 | Onboarding (Mission Spot, permissions, OEM guide, test alarm); health checklist; Good-morning screen; release signing; full reliability matrix | **Full first-run flow on a clean emulator, with all matrix rows passing** | Upload to internal testing, then **closed test live Mon Nov 30 / Tue Dec 1**; complete declarations, video and Data safety |
| 7 | Dec 1–7 | Billing (Pro, restore), paywall, AdMob banner + UMP (Stats and Settings only), stats bento | Purchase with a test card; banner absent on alarm-path screens | **Update #1 (~day 5)**; first tester feedback |
| 8 | Dec 8–14 | Glance widget, motion polish, haptics, accessibility (200% font, TalkBack), Baseline Profile; fixes from testers | Widget shows the next alarm; reduce-motion mode | **Update #2 (~day 11)**; day 14 = Dec 14 → **apply for production** |
| 9 | Dec 15–21 | Bug fixes; start **Fajr Mode** (prayer-time library, city picker, rule engine) | Fajr time shown for Karachi/Lahore/Dubai | Final screenshots, promo short, website live |
| 10 | Dec 22–28 | Fajr alarm scheduling with daily recompute | A Fajr alarm auto-moves day to day (emulator date change) | Production approval expected; **staged rollout around Dec 28** (20% → 100%) |
| 11 | Dec 29–Jan 4 | Launch fixes from vitals | — | **New Year launch push**: videos, personal review asks, India January peak |
| 12 | Jan 5–11 | Fajr missions, pre-alarms, adhan tones, suhoor preset, Fajr streak | Full Fajr flow | Urdu/Arabic/Indonesian listing copy with native review |
| 13 | Jan 12–18 | Fajr polish, no-ads enforcement on Fajr screens, localization of in-app strings | Fajr Mode in Urdu UI | /fajr page; community outreach list |
| 14 | Jan 19–25 | **Release v1.1 (target Jan 24)** | — | Localized listings live, so they are indexed about 2 weeks before Ramadan |
| 15 | Jan 26–Feb 1 | Steps mission (v1.2 start) | Steps with fallback | Fajr outreach in mosque and university groups |
| 16 | Feb 2–8 | v1.2 hardening | — | **Ramadan begins around Feb 8**: daily Fajr/suhoor content; PK/IN exam season (Feb–Mar) |

After week 16, the plan continues as follows:

- **March–June:** v1.2 (NFC, memory, typing your to-do), v1.3 (harder mode, sound rotation) and v1.4 (groups, backup, Quick Settings tile), on a 2–4 week cadence.
- **By early August 2027:** v2 with exercise missions and Wear OS, ready for the September back-to-school peak, the strongest month in the "alarmy" trend data.

Watch one external date: the **target-API extension deadline of 1 November 2026** does not affect Wakeroo, which already targets 36. But next year's target-API bump (likely API 37) will need a few days of work around August 2027 ([Target API](https://developer.android.com/google/play/requirements/target-sdk)).

## Risks worth planning for, and questions only the founder can answer

| Risk | Likelihood / impact | Mitigation |
|---|---|---|
| Alarms fail on aggressive OEM ROMs, destroying trust | Medium / severe | `setAlarmClock` + `USE_EXACT_ALARM`; Direct Boot; heartbeat; missed-alarm detection; per-brand guides; real-device mornings with testers' Samsung and Xiaomi phones before launch |
| Play rejects a declaration (exact alarm, full-screen intent, FGS video) | Low–medium / delays launch 1–2 weeks | Wakeroo is a textbook "alarm app"; record a clear FGS video in week 6; avoid `specialUse`, Accessibility and battery-exemption requests |
| Closed-test testers drop below 12 over the holidays | Medium / adds 2+ weeks | Recruit 20–25; weekly check-ins; ship updates that give testers something to try |
| Lost in the 2026 clone flood | High / slow growth | Exact-match title + review burst + Mission Spot videos + Fajr niche + localized listings; experiment with titles every 4–8 weeks |
| Beginner scope creep pushes launch past the New Year | Medium / misses seasonality | Fixed cut line; v1.0 limited to 5 missions; Fajr strictly v1.1 |
| material3 1.5.0 beta API churn; AGP 9.4 / Kotlin 2.4 library incompatibility (KSP, Room) | Medium / build breaks | Pin exact versions; upgrade deliberately; keep `MaterialShapes` usage behind one wrapper file |
| A sensor mission traps a user, producing 1★ reviews | Medium / severe | Bundled ML models; 60-second / 3-attempt fallback; emergency exit; never request permissions at ring time |
| Fajr times wrong for a user's madhab or method | Medium / severe in that community | Selectable method and Asr madhab; a manual minute offset; testers in Karachi, Lahore and Dubai; "report a time error" link |
| Ads damage the "trust" brand or serve inappropriate content to Fajr users | Medium / reputational | Banner only on Stats/Settings; none on Fajr screens; block sensitive ad categories in AdMob; consider going fully ad-free if Fajr dominates |
| Revenue far below expectations | High / motivational | Treat v1.0 as the trust-and-learning release; the upside relies on spikes (video, Ramadan, back to school); keep costs at about $0 |
| Trademark conflict outside the US | Low / costly rename | WIPO search this week; check IPO Pakistan if it is the home market |
| Confusion or a complaint from Delight Room | Low | Wakeroo is a distinct coined name; never use "Alarmy" in the title (titles cannot begin with another app's name ([AppTweak](https://www.apptweak.com/en/aso-blog/app-name-guidelines-and-best-practices))) |

| Open question for the founder | Why it matters | Default if unanswered |
|---|---|---|
| Is Pakistan your home market, and do you speak Urdu or Arabic? | Decides how hard to push Fajr Mode, and whether you can write native copy | Assume yes for Pakistan; get a native reviewer for Arabic |
| Fajr Mode in v1.0 instead of v1.1? | v1.0 gets more testers from mosque groups but adds 2–3 weeks | Keep it in v1.1 (Jan 24) |
| Ads at all, or fully ad-free with Pro at $4.99? | "No ads" is a stronger listing claim; banners earn tens of dollars a month | Banner on Stats/Settings only; revisit at month 3 |
| Pro price: $3.99 or $4.99? | Students are price-sensitive; $5 is the praised anchor | $4.99 US, with regional pricing below $2.50 in PK/IN |
| Which real phones can you test on? | Samsung and Xiaomi behaviour can't be emulated | Borrow from testers; buy nothing |
| Can you pay for domains now (about $10–20 each per year)? | Name availability is perishable | Register .com and .app this week if possible |
| Target audience age: 13+ or 18+? | Under-13 users trigger Families policy and ad restrictions; students include teens | 13+, Families program not opted in; confirm in Play's target-audience form |
| Will you appear on camera in videos? | Face-led shorts usually outperform screen recordings | Screen-recorded POV videos with captions |
| Kangaroo brand mark: yes or no? | Personality vs the "no cartoon" brief | Abstract bounce-arc mark only; no mascot in the app |
| Crash reporting SDK (e.g., Crashlytics) or Play vitals only? | Adds a Data safety disclosure vs less detail | Play vitals plus ring-log export for v1.0 |

## Conclusion

The research reframes what wins this category. Mission variety, the thing clones compete on, is already a commodity, and the 2026 flood shows it. What incumbents actually sell badly is **certainty**: certainty the alarm fires, that a broken sensor won't trap you, that an ad won't hijack the dismiss button, and that you won't pay twice. For Wakeroo, certainty works as a product feature, with a health card, a ring log and a one-minute test, and as a marketing claim the incumbents can't copy without giving up their ad and subscription revenue. The most important engineering insight is that the reliability features users beg for (re-ring after reboot, Direct Boot ringing, never auto-dismiss) all fit cleanly inside Android 15–17's tightened rules if every ring goes through `setAlarmClock`. That includes the post-reboot re-arm.

The strategic insight is timing. Wakeroo can use three seasonal waves in its first year: the January resolution and Indian exam peak, Ramadan 2027 for Fajr Mode, and the September back-to-school peak for the v2 exercise missions. Each needs only a release date and free content, not money. Choosing a Fajr secondary hook turns the founder's likely home market from a low-eCPM liability into the one place where Wakeroo can be the obvious first choice rather than the fifty-first Alarmy alternative.
