# Handoff: Lullaby — Sleep Timer for Fire TV

## Overview
Lullaby is a sleep-timer app for Amazon Fire TV Stick. The user picks a duration (or a custom length), keeps watching their show while the timer runs invisibly in the background, and during the final 60 seconds sees a small top-right corner overlay that lets them add 10 minutes or keep watching. Audio and brightness fade gradually over the last minute, then the TV turns off.

The full flow is 4 screens:
1. **Home** — preset chips (15 / 30 / 45 / 60 / 90 min) + option to enter Custom
2. **Custom length picker** — hours × minutes reel
3. **Confirmation** — briefly shown after selection, then dismisses (timer now runs in background)
4. **Last-60s corner overlay** — "+10 minutes" / "Keep watching" / ignore to sleep

## About the Design Files
The files in this bundle are **design references created in HTML / inline JSX** — prototypes showing intended look and behavior, not production code to copy directly. The task is to **recreate these designs in the target codebase's environment** using its established patterns and libraries.

For a Fire TV Stick app specifically, the target environment will likely be **Android TV / Fire OS (Kotlin + Jetpack Compose, or Java + Leanback)**, or a React Native for TV / Lightning app. Pick whatever matches the existing codebase. If greenfield, Jetpack Compose for TV is the recommended modern stack for Fire TV.

The HTML mocks use web-style pixel values at a native canvas of 1920×1080; translate those directly into your target framework's density-independent unit (dp on Android, pt on others). Every size, color, and spacing value below is literal and pixel-perfect.

## Fidelity
**High-fidelity (hifi)** — pixel-perfect mockups with final colors, typography, spacing, and interactions. Recreate pixel-perfectly using the target platform's idioms.

## Platform Context — Fire TV
- **Input**: D-pad remote only — no touch, no keyboard. All focus movement is ▲▼◀▶ with ● (center/select) to confirm.
- **Canvas**: 1920×1080 (native 16:9), TV-safe margins ≥ 60 px.
- **Typography minimums**: body text ≥ 24 px; numeric hero ≥ 88 px.
- **Focus states**: every focusable element MUST have an unmistakable focus state (accent border + background + shadow) — users cannot rely on a cursor.
- **Timer runs in the background** — after the confirmation flashes, the app hands control back to the OS / current content. The overlay only reappears in the final 60 s.

## Design Tokens

### Colors
```
--bg-canvas       #0a0907   (near-black, warm-tinted)
--bg-overlay      rgba(12,10,8,0.9) + backdrop-blur(24px)
--surface-soft    rgba(232,224,212,0.06)   /* focused rows, reel focus bg */
--divider         rgba(232,224,212,0.08)–(0.1)

--text-primary    #f2ead8   (warm off-white)
--text-body       #e8e0d4
--text-muted      rgba(232,224,212,0.55)
--text-dim        rgba(232,224,212,0.4)
--text-faint      rgba(232,224,212,0.25)

--accent          oklch(0.78 0.08 65)   /* warm amber — focus + glyph */
--accent-on       #0a0907               /* text on accent fill */
--accent-vignette radial-gradient(ellipse at 80% 10%, rgba(210,150,80,0.06), transparent 50%)

--overlay-scrim   linear-gradient(135deg, #1a1510, #0a0807 50%, #150f08)
                  + rgba(0,0,0,0.55) dimming layer
```

### Typography
- **Family**: Inter (weights 200 / 300 / 400 / 500 / 600)
- **Mono (hints)**: ui-monospace / SF Mono / Menlo
- Use `font-feature-settings: "tnum"` on all numeric displays so digits don't jitter.

| Role | Size | Weight | Letter-spacing |
|---|---|---|---|
| Hero headline ("Drift off.") | 76 | 200 | -2 |
| Hero numeric (confirmation "45") | 260 | 200 | -10 |
| Reel digit (custom picker) | 160 | 200 | -6 |
| Preset number (home ladder) | 88 | 200 | -3 |
| Overlay time (0:45) | 44 | 300 | -1 |
| Total minutes (custom) | 64 | 200 | -2 |
| Section title | 34 | 400 | 0 |
| Body copy | 20–24 | 300–400 | 0 |
| Eyebrow / label (UPPERCASE) | 16–22 | 500 | 3–5 |
| Remote hint (mono) | 20 | 400 | 0 |

### Spacing scale
8 · 12 · 16 · 20 · 24 · 32 · 40 · 48 · 72 · 80 · 96 · 120 (px @ 1080p)

### Radii
- Small chip / button: `12`
- Reel / surface card: `16–20`
- Pill: `999`

### Focus signature
Focused element gets: `2 px accent border` OR `3 px accent left-border` for ladder rows, plus `rgba(232,224,212,0.06)` surface background. For the two-button overlay actions, focused button = accent fill + `box-shadow: 0 0 0 2px rgba(232,224,212,0.2)`.

### Motion / fade-to-sleep
Over the final 60 s:
- Media audio fades linearly to 0.
- Screen brightness dims (system-level call on Fire TV if available, or a scrim layer that interpolates toward full black).
- Moon crescent in the overlay "eats itself" — `inset X -6 0 accent` where `X = 18 + (60 - secondsRemaining) * 0.4`.
- After 0 s, system "sleep" / power-off intent.

## Screens

### 1. Home (`VariantD_Home`)
Two-column layout on a warm near-black background with a soft amber vignette at `80% 10%`.

**Left column** (width `640`, padding `120 80 80 120`, `border-right` divider):
- `◐ Lullaby` eyebrow — 20 px, UPPERCASE, letter-spacing 5, color = accent, `margin-bottom: 80`.
- Hero headline "Drift off. / We'll take / care of the / TV." — 76 px, weight 200, line-height 1.05, color `#f2ead8`.
- Sub-copy "Audio and brightness fade over the final minute. No jarring shutoff." — 24 px, weight 300, line-height 1.5, max-width 400, color `rgba(232,224,212,0.55)`, `margin-top: 48`.
- Footer hint (mono) "▲ ▼ Choose · ● Begin" — 20 px, color `rgba(232,224,212,0.35)`, pinned to bottom.

**Right column** (flex: 1, padding `120 120 80 80`, vertical center, `gap: 4`):
Ladder of 5 preset rows. Each row is a 3-column grid `180px 1fr auto`, gap 40, padding `24 32`, radius 16.

Row data:
| mins | label | sub |
|---|---|---|
| 15 | Quick doze | one ad break |
| 30 | Short nap | half an hour |
| 45 | One episode | usual length |
| 60 | One hour | a whole movie act |
| 90 | Long movie | full film length |

- **Unfocused row**: no background, 3 px transparent left border. Number color `rgba(232,224,212,0.4)`, label `rgba(232,224,212,0.6)`.
- **Focused row**: background `rgba(232,224,212,0.06)`, 3 px accent left border. Number color `#f2ead8`, label `#f2ead8`, and a right-aligned `● BEGIN` in accent/mono appears.
- Transition: `all 0.15s`.

Below the ladder, a plain "+ Custom length" row (22 px, muted). Selecting it navigates to the Custom picker.

**Focus model**: ▲▼ moves between the 5 presets + Custom row. ● starts the timer (or enters Custom).

### 2. Custom length picker (`VariantD_Custom`)
Header row: `◀ Back` · dot · `Custom length` (eyebrow in accent). Then the main row:

**Two reels + summary**, centered, with a 160-px `:` separator between the reels.

Each reel: padding `20 32`, min-width 260, radius 20. Focused reel = 2 px accent border + faint surface bg + ▲/▼ carets in accent at top-right and bottom-right corners. Unfocused = transparent border + no bg.

Reel content (vertical stack):
- Above value (dimmed) — 36 px, `rgba(232,224,212,0.25)`, 48-px fixed line height
- Current value — 160 px, weight 200, letter-spacing -6, color `#f2ead8`
- Below value (dimmed) — same style as above
- Unit label — 18 px, UPPERCASE, letter-spacing 4, color `rgba(232,224,212,0.45)`

Hours reel: `min 0, max 8, step 1, pad 1`. Minutes reel: `min 0, max 55, step 5, pad 2`.

**Summary pane** (right of reels, left-divider, padding-left 80, min-width 360):
- `TOTAL` eyebrow → e.g. `80 min` at 64 px / weight 200.
- `LIGHTS OUT` eyebrow → computed clock time (e.g. `12:07 AM`), 40 px / weight 300.
- `● Begin` button — accent fill, `#0a0907` text, padding `18 28`, radius 12, 24 px / weight 500, centered, with `0 0 0 3px rgba(232,224,212,0.12)` shadow.

**Footer** (centered mono): `◀ ▶ Switch column · ▲ ▼ Adjust value · ● Begin`.

**Focus model**: ◀▶ toggles between hours and minutes columns. ▲▼ adjusts the focused reel by its step. ● starts.

### 3. Confirmation (`VariantD_Started`)
Full-screen centered stack, shown briefly (~3 s) after a duration is chosen, then dismissed back to the user's content.

From top:
- Moon crescent glyph — 120×120, circular, `box-shadow: inset 30px -10px 0 0 accent`, rotate(-15deg), `margin-bottom: 48`.
- `◐ Lullaby set` eyebrow — 22 px, accent, letter-spacing 5, UPPERCASE.
- Hero numeric — e.g. `45 min`, 260 px + 72 px for "min", weight 200, tnum.
- Closing line "Good night. TV off at **11:47 PM**." — 28 px / weight 300, muted body color with the clock time in `#f2ead8`.

### 4. Last-60s corner overlay (`VariantD_Overlay`)
The app is not visible before this. During the final 60 s:
- A subtle scrim dims the screen: `linear-gradient(135deg, #1a1510, #0a0807 50%, #150f08)` + `rgba(0,0,0,0.55)` overlay on top of the current content (real apps: render on top of the video surface; or if that's not possible, drop the video brightness via system API and render our overlay as the only content).
- Top-right card (pin: `top: 72, right: 72`, padding `28 32`, radius 20, background `rgba(12,10,8,0.9)` with 24 px backdrop blur, 1 px `accent33` border, min-width 520).

Card contents:
- **Top row**: moon crescent (72×72, shrinks as time runs out via `inset ${18 + (60-s)*0.4}px -6px 0 0 accent`) + block with `FADING TO SLEEP` eyebrow (accent) and `0:45` countdown (44 px / weight 300 / tnum).
- **Two actions** side-by-side, each flex: 1, padding `16 18`, radius 12:
  - **+10 minutes** — focused: accent fill + `#0a0907` text, weight 600, `0 0 0 2px rgba(232,224,212,0.2)` shadow.
  - **Keep watching** — focused: same accent treatment. Unfocused: `rgba(232,224,212,0.06)` bg, 1 px `rgba(232,224,212,0.1)` border, 400 weight.
- **Hint line** (mono, centered, muted): `◀ ▶ choose · ● confirm · ignore to sleep`.

**Behavior**: default focus is on `+10 minutes`. ◀▶ toggles focus. ● confirms. If the user does nothing, the timer completes and the TV turns off.

## State Management

State the app owns:
```
mode:           'home' | 'custom' | 'confirming' | 'running' | 'warning'
selectedMinutes:  number          // total minutes
presetFocusIndex: 0..5            // 0-4 presets, 5 = Custom row
customHours:      0..8
customMinutes:    0..55 step 5
customFocusCol:   'hours' | 'minutes'
overlayFocus:     'snooze' | 'cancel'
secondsRemaining: number          // drives countdown + moon + fades
```

Transitions:
- Home + ● on preset → `confirming(mins)` → after ~3 s → `running`
- Home + ● on "+ Custom length" → `custom`
- Custom + ● on Begin → `confirming(hours*60 + minutes)` → `running`
- `running`: when `secondsRemaining ≤ 60`, show overlay = `warning`
- Warning + ● on "+10 minutes" → `secondsRemaining += 600`, back to `running` (overlay hidden until next warning)
- Warning + ● on "Keep watching" → cancel timer entirely, dismiss overlay, back to normal watching
- `secondsRemaining === 0` → system sleep intent

Persistence: remember the last-used preset so Home can default focus there on next launch.

## Assets
- No raster assets — the moon crescent is drawn with a CSS `box-shadow: inset …` on a circular div. On Fire TV Compose, draw an equivalent crescent with a `Canvas` composable (circle minus circle) or a vector drawable.
- Fonts: Inter (Google Fonts). Bundle the weights 200/300/400/500/600.

## Files in this bundle
- `Sleep Timer.html` — runnable prototype (open in a browser). Canvas of all 4 screens + a Tweaks panel for focus/value states.
- `design-canvas.jsx` — the canvas host (not part of the product, only used to lay out the mocks).
- `components/TVFrame.jsx` — the 1920×1080 TV bezel wrapper.
- `components/VariantD.jsx` — the four screens as React components. This is the canonical source of truth for layout, tokens, and pixel values.

## Implementation notes for Claude Code
- Start by reading `components/VariantD.jsx` — every screen is there with exact values inline.
- Ignore the `window.VariantD_* = …` assignments at the end (they're for the canvas host).
- When translating to Jetpack Compose: focused states on rows/buttons should be driven by `Modifier.onFocusChanged` or a `FocusRequester`; map the ladder to a `Column` of `Row`s that are focusable; use `TvLazyColumn` if a focus scroller is needed. For the reels, a `Modifier.onKeyEvent` handler on a focusable container reading `DpadUp`/`DpadDown` is cleanest — don't use a web-style scroll wheel.
- The fade-to-sleep should be a `ValueAnimator` / `animate*AsState` over 60 s driving both the volume (via `AudioManager`) and an alpha-scrim view. For actually turning the TV off on Fire TV, send `Intent.ACTION_SHUTDOWN` / the platform's power-off helper (behavior varies; may require device admin or a HDMI-CEC standby call).
- Keep all text strings in a single `strings.xml` / equivalent — they are intentionally few and tight.
