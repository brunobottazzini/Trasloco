# Design: Audio Mute Setting + Card Flight Animations

**Date:** 2026-05-22  
**Status:** Approved

---

## Overview

Two independent features added to Trasloco:

1. **Audio mute toggle** — a new switch in Settings to enable/disable all game sounds, persisted via the existing SQLite settings system.
2. **Card flight animations** — when cards move automatically (end-deck auto-complete or deck press deal), a ghost card flies visually from the source slot to the destination slot instead of updating instantly.

Additionally, the "Fast Deal" setting label is renamed to "Completamento automatico" across all locales (DB key unchanged).

---

## Feature 1 — Audio Mute Setting

### Configuration

- New enum value in `Configuration`: `SOUND_ENABLED("soundEnabled")`
- Default value: `"enabled"` (sound on for all users, including existing ones)
- No DB migration needed — `setDefaultSetting` only inserts if the key is absent

### SettingsHandler

`insertDefaultSettings()` gains:
```kotlin
setDefaultSetting(Configuration.SOUND_ENABLED.value, "enabled")
```

### SettingsActivity

- New method `changeSoundEnabled(view: View)` reads `switchSound.isChecked` and calls `updateSetting`
- `readConfigurations()` reads `SOUND_ENABLED` and sets `switchSound.isChecked`

### settings.xml

New `LinearLayout` row added **after `autoMoveRow`, before the credits `TextView`**. Identical structure to the other switch rows:
- `android:id="@+id/soundRow"`
- Label text: `@string/sound_with_icon` (e.g. `"🔊 Suono"`)
- Switch: `android:id="@+id/switchSound"`, `android:onClick="changeSoundEnabled"`, gold thumb/track drawables

### GameActivity

- New property: `private var soundEnabled: Boolean = true`
- Read in `processSettings()`:
  ```kotlin
  soundEnabled = settingsHandler.readValue(Configuration.SOUND_ENABLED.value) != "disabled"
  ```
- Guard in `playSound()`: return early if `!soundEnabled`
- Guard in `playSoundAtomic()`: return early if `!soundEnabled`

### String Resources

New key `sound_with_icon` added to **all locale files**:
- `values/strings.xml` (default/English): `"🔊 Sound"`
- `values-it/strings.xml`: `"🔊 Suono"`
- All other locales (`de`, `es`, `fr`, `hi`, `ja`, `ko`, `nl`, `pl`, `pt`, `pt-rBR`, `pt-rPT`, `ru`, `th`, `tr`, `zh-rCN`): translated appropriately or fallback to English

---

## Feature 2 — Card Flight Animations

### Architecture

A new `utils/CardAnimator.kt` singleton handles all ghost-based flight animations. `GameActivity` calls it in two places. This keeps animation logic out of the already-large `GameActivity`.

### CardAnimator API

```kotlin
object CardAnimator {
    /**
     * Animates a ghost card flying from [sourceView] to [targetView] inside [root].
     * Calls [onComplete] when the animation finishes.
     * If either view has zero size (not yet laid out), [onComplete] is called immediately.
     */
    fun animateCardFlight(
        root: ViewGroup,
        sourceView: View,
        targetView: View,
        drawable: Drawable?,
        durationMs: Long = 350L,
        onComplete: () -> Unit
    )
}
```

**Internals:**
1. Compute screen coordinates of `sourceView` and `targetView` via `getLocationOnScreen()`
2. Create ghost `ImageView`:
   - Size = source view size
   - `elevation = 16f`, `alpha = 0.85f`
   - Positioned at source screen coordinates relative to root
3. Add ghost to `root` (above all other views due to elevation)
4. Animate `translationX` and `translationY` to reach target position using `ValueAnimator` (linear interpolator, `durationMs`)
5. On animation end: remove ghost from root, invoke `onComplete`
6. Safety: if source or target width/height is 0, skip animation and call `onComplete` immediately

### Animation Point A — End-deck auto-complete

Triggered by `forceCardsEndDeck()` and `triggerAutoMoveCycle()`.

**Behaviour:**
- Source slot image is cleared to `"zero"` immediately (the pile is gone)
- Ghost flies from source slot to end deck slot (350ms)
- On complete: `setImage(targetId, cardName)` updates the end deck slot
- Cards go one at a time; the existing 350ms delay in `triggerAutoMoveCycle` already chains them correctly

**Change in `forceCardsEndDeck()`:**
```
Before: setImage(selectedPositionId, "zero") + setImage(desiredCardPositionId, lastCard)
After:  setImage(selectedPositionId, "zero")
        CardAnimator.animateCardFlight(root, sourceView, endDeckView, drawable, 350) {
            setImage(desiredCardPositionId, lastCard)
            endDeckList[line] = lastCard
        }
```

**Change in `tryMove()` for end-deck moves (non-`forceCardsEndDeck` path):**
The direct `moveCard()` call for end-deck moves also gains a flight animation:
```
setImage(sourceId, "zero")   // clear source immediately
CardAnimator.animateCardFlight(...) {
    setImage(targetId, card)
}
```

### Animation Point B — Deck press deal

Triggered by `dealCard(line)` when the user taps a deck stack.

**Behaviour:**
- Cards are dealt in sequence with a staggered delay: card 1 at 0ms, card 2 at 120ms, card 3 at 240ms
- Each animation is 350ms; the ghost flies from the deck ImageView (`subDeck{line}`) to the destination slot
- Each card's `setImage` + `cardTableMap` update happens in its animation's `onComplete` callback
- `subDeckMap[line]` is updated immediately (cards removed from the pending list before animations start) to keep state consistent

**Implementation note:** A helper `animateDealSequence(line, cards, slots)` will be introduced in `GameActivity` to manage the staggered `postDelayed` calls cleanly.

---

## Rename: "Fast Deal" → "Completamento automatico"

- **DB key unchanged:** `Configuration.FAST_DEAL.value = "fastDeal"` — no migration, no data loss
- **String key unchanged:** `fast_deal_with_icon` — only the **value** changes in each locale file
- All locale files updated: Italian `"🎯 Completamento automatico"`, English `"🎯 Auto-complete"`, other locales translated or English fallback

---

## Files Changed

| File | Change |
|------|--------|
| `settings/SettingsHandler.kt` | Add `SOUND_ENABLED` default |
| `settings/Configuration.kt` (enum in SettingsHandler.kt) | Add `SOUND_ENABLED` enum value |
| `SettingsActivity.kt` | Add `changeSoundEnabled()`, update `readConfigurations()` |
| `GameActivity.kt` | Add `soundEnabled` flag, guards in `playSound`/`playSoundAtomic`, animate `forceCardsEndDeck` + `dealCard` |
| `utils/CardAnimator.kt` | New file |
| `res/layout/settings.xml` | Add `soundRow` LinearLayout with Switch |
| `res/values/strings.xml` | Add `sound_with_icon`, update `fast_deal_with_icon` |
| `res/values-it/strings.xml` | Same |
| All other `values-*/strings.xml` | Same |

---

## Non-Goals

- No animation for manual card moves (drag & drop stays as-is)
- No animation for undo
- No music/background audio — this is purely a sound effects mute
- No per-sound granular control (one toggle for all sounds)
