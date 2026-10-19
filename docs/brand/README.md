# Wakeroo brand and design system

## Logo

The mark is a sun resting on the horizon, with a tapered leap arc that lands on a gold point: getting up and out of bed. The leap is a quiet nod to the "roo" in the name. There is no mascot.

| File | Use |
|---|---|
| `mark.svg`, `mark-on-night.svg`, `mark-on-day.svg` | The symbol on transparent, dark and light backgrounds |
| `mark-mono-dark.svg`, `mark-mono-light.svg` | One-colour versions (stamps, themed icons) |
| `wordmark*.svg` | "wakeroo" in Outfit SemiBold, tracking -1.5%. The final "o" is coral: it is the sun |
| `lockup*.svg` | Symbol and wordmark side by side |
| `play-icon-512.svg`, `png/play-icon-512.png` | Google Play icon (Play applies the mask) |
| `feature-graphic.svg`, `png/feature-graphic-1024x500.png` | Google Play feature graphic |

All assets, including the app's adaptive and themed icons, are generated from one geometry definition:

```
python docs/brand/build_brand.py docs/brand app/src/main/res
```

Rules: keep clear space of one sun radius around the mark; never recolour the sun anything but coral (or the single mono colour); never add gradients, glows or outlines to the mark.

## Colour

| Token | Hex | Role |
|---|---|---|
| Night | `#0E1120` | Default background, ring screen |
| Surface | `#1A1E33` | Cards and sheets |
| Raised | `#252A44` | Pressed and elevated surfaces |
| Sunrise Coral | `#FF7A59` | Primary actions, the sun |
| Dawn Lavender | `#B9A7FF` | Secondary accents, the leap |
| Morning Gold | `#FFC857` | Success, streaks, the landing point |
| Paper | `#F5F3EE` | Text on dark (never pure white) |
| Paper Muted | `#A9ABB8` | Secondary text |
| Alert | `#FF5470` | Errors, wrong answers |
| Calm | `#5BD6A8` | Healthy status |
| Day | `#FFF8F2` | Light mode background |

Skies (`Sky` in `Color.kt`) are vertical gradients keyed to the hour an alarm rings: Night, Dawn, Sunrise, Morning, Day.

## Type

- **Outfit** (Medium, SemiBold, Bold): display text and every clock digit.
- **Inter** (Regular, Medium, SemiBold): everything you read.
- Both are SIL Open Font License fonts from Google Fonts, bundled in `res/font` (Inter subset to Latin).
- Clock sizes: ring 112sp, hero 64sp, card 40sp (`ClockType`).

## Shape and space

- 4dp spacing grid (`Space`). Touch targets on ring and mission screens are at least 72dp.
- Corners: 8, 12, 20, 28 (cards), 36dp. Buttons and chips are full pills.

## Motion

1. Springs, not fixed durations (`WakerooMotion.calm`, `bouncy`, `snappy`).
2. Calm at night, bouncy in the morning.
3. One hero motion per screen.
4. Never animate information out of view.
5. Respect reduced motion (`LocalReducedMotion`): springs snap, loops stop.

## Haptics

Minute tick on the time wheel: `SegmentTick`. Switches: `ToggleOn` / `ToggleOff`. Correct step: `Confirm`. Wrong answer: `Reject`.
