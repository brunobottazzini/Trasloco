# Audio Mute Setting + Card Flight Animations — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a sound mute toggle to Settings (persisted) and animate card movements for end-deck auto-complete and deck-press deals.

**Architecture:** The mute toggle follows the existing `Configuration` enum + `SettingsHandler` + Switch pattern. Card animations use a new `CardAnimator` singleton that creates a ghost `ImageView`, flies it from source to target via `ValueAnimator`, then calls a completion callback to update the real slot image. `GameActivity` calls `CardAnimator` in `forceCardsEndDeck` and `dealCard`; game state is always updated synchronously — only the visual is deferred.

**Tech Stack:** Kotlin, Android SDK (`ValueAnimator`, `ImageView`, `ConstraintLayout`), SQLite via existing `DatabaseHandler`, existing `MediaPlayer` audio pipeline.

**Spec:** `docs/superpowers/specs/2026-05-22-audio-mute-card-animations-design.md`

---

## File Map

| File | Action | Responsibility |
|------|--------|----------------|
| `settings/SettingsHandler.kt` | Modify | Add `SOUND_ENABLED` enum value + default |
| `SettingsActivity.kt` | Modify | Add `changeSoundEnabled()`, update `readConfigurations()` |
| `GameActivity.kt` | Modify | Sound flag + guards; `gameRoot` lazy prop; animate `forceCardsEndDeck` + `dealCard` |
| `utils/CardAnimator.kt` | **Create** | Ghost flight animation singleton |
| `res/layout/settings.xml` | Modify | Add `soundRow` Switch after `autoMoveRow` |
| `res/values/strings.xml` | Modify | Add `sound_with_icon`; update `fast_deal_with_icon` |
| `res/values-it/strings.xml` | Modify | Same |
| `res/values-de/strings.xml` | Modify | Same |
| `res/values-es/strings.xml` | Modify | Same |
| `res/values-fr/strings.xml` | Modify | Same |
| `res/values-hi/strings.xml` | Modify | Same |
| `res/values-ja/strings.xml` | Modify | Same |
| `res/values-ko/strings.xml` | Modify | Same |
| `res/values-nl/strings.xml` | Modify | Same |
| `res/values-pl/strings.xml` | Modify | Same |
| `res/values-pt/strings.xml` | Modify | Same |
| `res/values-pt-rBR/strings.xml` | Modify | Same |
| `res/values-pt-rPT/strings.xml` | Modify | Same |
| `res/values-ru/strings.xml` | Modify | Same |
| `res/values-th/strings.xml` | Modify | Same |
| `res/values-tr/strings.xml` | Modify | Same |
| `res/values-zh-rCN/strings.xml` | Modify | Same |

---

## Task 1: Rename "Fast Deal" → "Auto-complete" in all locales

**Files:**
- Modify: `app/src/main/res/values/strings.xml:90`
- Modify: `app/src/main/res/values-it/strings.xml:91`
- Modify: all other `values-*/strings.xml` line 90

Note: only the **string value** changes. The XML key (`fast_deal_with_icon`) and the DB key (`fastDeal`) stay the same — no migration needed.

- [ ] **Step 1: Update default (English) strings.xml**

In `app/src/main/res/values/strings.xml`, change line 90:
```xml
<!-- Before -->
<string name="fast_deal_with_icon">⚡ Transfer entire deck</string>
<!-- After -->
<string name="fast_deal_with_icon">🎯 Auto-complete</string>
```

- [ ] **Step 2: Update all locale files**

Make the equivalent change in each locale file (line 90 in most, line 91 in `values-it` and `values-pt`):

`values-it/strings.xml`:
```xml
<string name="fast_deal_with_icon">🎯 Completamento automatico</string>
```
`values-de/strings.xml`:
```xml
<string name="fast_deal_with_icon">🎯 Automatische Vervollständigung</string>
```
`values-es/strings.xml`:
```xml
<string name="fast_deal_with_icon">🎯 Finalización automática</string>
```
`values-fr/strings.xml`:
```xml
<string name="fast_deal_with_icon">🎯 Complétion automatique</string>
```
`values-hi/strings.xml`:
```xml
<string name="fast_deal_with_icon">🎯 स्वचालित पूर्णता</string>
```
`values-ja/strings.xml`:
```xml
<string name="fast_deal_with_icon">🎯 自動補完</string>
```
`values-ko/strings.xml`:
```xml
<string name="fast_deal_with_icon">🎯 자동 완성</string>
```
`values-nl/strings.xml`:
```xml
<string name="fast_deal_with_icon">🎯 Automatisch aanvullen</string>
```
`values-pl/strings.xml`:
```xml
<string name="fast_deal_with_icon">🎯 Automatyczne uzupełnianie</string>
```
`values-pt/strings.xml`:
```xml
<string name="fast_deal_with_icon">🎯 Conclusão automática</string>
```
`values-pt-rBR/strings.xml`:
```xml
<string name="fast_deal_with_icon">🎯 Conclusão automática</string>
```
`values-pt-rPT/strings.xml`:
```xml
<string name="fast_deal_with_icon">🎯 Conclusão automática</string>
```
`values-ru/strings.xml`:
```xml
<string name="fast_deal_with_icon">🎯 Автодополнение</string>
```
`values-th/strings.xml`:
```xml
<string name="fast_deal_with_icon">🎯 เติมอัตโนมัติ</string>
```
`values-tr/strings.xml`:
```xml
<string name="fast_deal_with_icon">🎯 Otomatik tamamlama</string>
```
`values-zh-rCN/strings.xml`:
```xml
<string name="fast_deal_with_icon">🎯 自动完成</string>
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/values/strings.xml \
        app/src/main/res/values-*/strings.xml
git commit -m "chore: rename Fast Deal setting to Auto-complete in all locales"
```

---

## Task 2: Add SOUND_ENABLED configuration

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/settings/SettingsHandler.kt`

- [ ] **Step 1: Add enum value**

In `SettingsHandler.kt`, add `SOUND_ENABLED` to the `Configuration` enum after `AUTO_MOVE`:
```kotlin
enum class Configuration(val value: String) {
    FAST_DEAL("fastDeal"),
    CARD_BACK("cardBack"),
    BACKGROUND("background"),
    CARD_TYPE("cardType"),
    HINT_ENABLED("hintEnabled"),
    AUTO_MOVE("autoMove"),
    SOUND_ENABLED("soundEnabled")          // ← add this line
}
```

- [ ] **Step 2: Add default value**

In `insertDefaultSettings()`, add the new line after the `AUTO_MOVE` default:
```kotlin
fun insertDefaultSettings() {
    setDefaultSetting(Configuration.FAST_DEAL.value, "enabled")
    setDefaultSetting(Configuration.CARD_BACK.value, "bg2")
    setDefaultSetting(Configuration.BACKGROUND.value, "bordeaux")
    setDefaultSetting(Configuration.CARD_TYPE.value, "piacentine")
    setDefaultSetting(Configuration.HINT_ENABLED.value, "enabled")
    setDefaultSetting(Configuration.AUTO_MOVE.value, "disabled")
    setDefaultSetting(Configuration.SOUND_ENABLED.value, "enabled")   // ← add this line
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/settings/SettingsHandler.kt
git commit -m "feat: add SOUND_ENABLED configuration with default enabled"
```

---

## Task 3: Add sound_with_icon strings to all locales

**Files:**
- Modify: `app/src/main/res/values/strings.xml` (add after `auto_move_with_icon`)
- Modify: all `values-*/strings.xml`

- [ ] **Step 1: Add string to default (English) file**

In `app/src/main/res/values/strings.xml`, add after the `auto_move_with_icon` line (currently line 101):
```xml
<string name="auto_move_with_icon">🎯 Auto-move to final deck</string>
<string name="sound_with_icon">🔊 Sound</string>    <!-- ← add this -->
```

- [ ] **Step 2: Add string to all locale files**

Add `sound_with_icon` immediately after `auto_move_with_icon` in each locale:

`values-it/strings.xml`:
```xml
<string name="sound_with_icon">🔊 Suono</string>
```
`values-de/strings.xml`:
```xml
<string name="sound_with_icon">🔊 Ton</string>
```
`values-es/strings.xml`:
```xml
<string name="sound_with_icon">🔊 Sonido</string>
```
`values-fr/strings.xml`:
```xml
<string name="sound_with_icon">🔊 Son</string>
```
`values-hi/strings.xml`:
```xml
<string name="sound_with_icon">🔊 ध्वनि</string>
```
`values-ja/strings.xml`:
```xml
<string name="sound_with_icon">🔊 サウンド</string>
```
`values-ko/strings.xml`:
```xml
<string name="sound_with_icon">🔊 사운드</string>
```
`values-nl/strings.xml`:
```xml
<string name="sound_with_icon">🔊 Geluid</string>
```
`values-pl/strings.xml`:
```xml
<string name="sound_with_icon">🔊 Dźwięk</string>
```
`values-pt/strings.xml`:
```xml
<string name="sound_with_icon">🔊 Som</string>
```
`values-pt-rBR/strings.xml`:
```xml
<string name="sound_with_icon">🔊 Som</string>
```
`values-pt-rPT/strings.xml`:
```xml
<string name="sound_with_icon">🔊 Som</string>
```
`values-ru/strings.xml`:
```xml
<string name="sound_with_icon">🔊 Звук</string>
```
`values-th/strings.xml`:
```xml
<string name="sound_with_icon">🔊 เสียง</string>
```
`values-tr/strings.xml`:
```xml
<string name="sound_with_icon">🔊 Ses</string>
```
`values-zh-rCN/strings.xml`:
```xml
<string name="sound_with_icon">🔊 声音</string>
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/values/strings.xml \
        app/src/main/res/values-*/strings.xml
git commit -m "feat: add sound_with_icon string to all locales"
```

---

## Task 4: Add sound switch to settings.xml

**Files:**
- Modify: `app/src/main/res/layout/settings.xml`

- [ ] **Step 1: Add soundRow LinearLayout before credits**

In `app/src/main/res/layout/settings.xml`, replace the `<!-- Credits -->` block (starting at line 432) with the sound switch row followed by the credits:

```xml
        <!-- Sound switch -->
        <LinearLayout
            android:id="@+id/soundRow"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:padding="12dp"
            android:background="@drawable/casino_tile_bg"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toBottomOf="@id/autoMoveRow">

            <TextView
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="@string/sound_with_icon"
                style="@style/CasinoBodyText" />

            <Switch
                android:id="@+id/switchSound"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:thumb="@drawable/casino_gold_switch_thumb"
                android:track="@drawable/casino_gold_switch_track"
                android:onClick="changeSoundEnabled" />
        </LinearLayout>

        <!-- Credits -->
        <TextView
            android:id="@+id/textViewCredits"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginTop="32dp"
            android:gravity="center"
            android:text="@string/credits_text"
            android:textColor="@color/casino_gold_alpha_50"
            android:textSize="10sp"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toBottomOf="@id/soundRow" />
```

Key changes:
- New `soundRow` LinearLayout with `app:layout_constraintTop_toBottomOf="@id/autoMoveRow"`
- Credits `TextView` constraint changed from `toBottomOf="@id/autoMoveRow"` → `toBottomOf="@id/soundRow"`

- [ ] **Step 2: Commit**

```bash
git add app/src/main/res/layout/settings.xml
git commit -m "feat: add sound switch row to settings layout"
```

---

## Task 5: Wire sound switch in SettingsActivity

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/SettingsActivity.kt`

- [ ] **Step 1: Add changeSoundEnabled method**

In `SettingsActivity.kt`, add the following method after `changeAutoMove`:
```kotlin
fun changeSoundEnabled(view: View) {
    val switch = view as Switch
    val value = if (switch.isChecked) "enabled" else "disabled"
    settingsHandler.updateSetting(Configuration.SOUND_ENABLED.value, value)
}
```

- [ ] **Step 2: Read sound setting in readConfigurations**

In `readConfigurations()`, add after the `autoMove` block and before `applyScreenBackground`:
```kotlin
val sound = settingsHandler.readValue(Configuration.SOUND_ENABLED.value) ?: "enabled"
findViewById<Switch>(R.id.switchSound).isChecked = (sound == "enabled")
```

The complete updated tail of `readConfigurations()` looks like:
```kotlin
val autoMove = settingsHandler.readValue(Configuration.AUTO_MOVE.value) ?: "disabled"
findViewById<Switch>(R.id.switchAutoMove).isChecked = (autoMove == "enabled")

val sound = settingsHandler.readValue(Configuration.SOUND_ENABLED.value) ?: "enabled"
findViewById<Switch>(R.id.switchSound).isChecked = (sound == "enabled")

applyScreenBackground(background)
updateHeroPreview()
```

- [ ] **Step 3: Verify on device**

Build and run. Open Settings. Scroll down — a new "🔊 Sound" row with a gold switch should appear below "Auto-move". Toggle it. Kill and reopen the app. Reopen Settings — the switch should remember its last state.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/SettingsActivity.kt
git commit -m "feat: wire sound switch in SettingsActivity"
```

---

## Task 6: Enforce sound mute in GameActivity

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/GameActivity.kt`

- [ ] **Step 1: Add soundEnabled property**

In `GameActivity.kt`, add the property near the other boolean flags (around line 75, after `autoMoveEnabled`):
```kotlin
private var soundEnabled: Boolean = true
```

- [ ] **Step 2: Read the setting in processSettings()**

In `processSettings()`, add after `autoMoveEnabled = ...`:
```kotlin
soundEnabled = settingsHandler.readValue(Configuration.SOUND_ENABLED.value) != "disabled"
```

- [ ] **Step 3: Guard playSound()**

In `playSound(soundId: Int)`, add an early return at the very top of the method body:
```kotlin
private fun playSound(soundId: Int) {
    if (!soundEnabled) return          // ← add this line
    try {
        if (mediaPlayer?.isPlaying == true) {
        // ... rest unchanged
```

- [ ] **Step 4: Guard playSoundAtomic()**

In `playSoundAtomic(soundId: Int)`, same guard:
```kotlin
private fun playSoundAtomic(soundId: Int) {
    if (!soundEnabled) return          // ← add this line
    try {
        mediaPlayerAtomic = MediaPlayer.create(this, soundId)
        // ... rest unchanged
```

- [ ] **Step 5: Verify on device**

Build and run. Go to Settings → disable Sound → go back to game. Shuffle, move cards, reach end deck. No sounds should play. Re-enable Sound in Settings — sounds return.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/GameActivity.kt
git commit -m "feat: enforce sound mute in GameActivity via soundEnabled flag"
```

---

## Task 7: Create CardAnimator utility

**Files:**
- Create: `app/src/main/java/com/bottazzini/trasloco/utils/CardAnimator.kt`

- [ ] **Step 1: Create the file**

Create `app/src/main/java/com/bottazzini/trasloco/utils/CardAnimator.kt` with:

```kotlin
package com.bottazzini.trasloco.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout

/**
 * Animates a card image flying from one slot to another using a ghost ImageView.
 *
 * The caller is responsible for updating game state before calling this.
 * The [onComplete] callback should perform the visual-only update on the target slot.
 */
object CardAnimator {

    /**
     * Fly a ghost card from [sourceView]'s screen position to [targetView]'s screen position
     * inside [root]. Calls [onComplete] when the animation ends.
     *
     * Falls back to calling [onComplete] immediately if either view has zero dimensions
     * (e.g. not yet laid out).
     */
    fun animateCardFlight(
        root: ViewGroup,
        sourceView: View,
        targetView: View,
        drawable: Drawable?,
        durationMs: Long = 350L,
        onComplete: () -> Unit
    ) {
        if (sourceView.width == 0 || sourceView.height == 0 ||
            targetView.width == 0 || targetView.height == 0) {
            onComplete()
            return
        }

        val srcLoc  = IntArray(2)
        val dstLoc  = IntArray(2)
        val rootLoc = IntArray(2)
        sourceView.getLocationOnScreen(srcLoc)
        targetView.getLocationOnScreen(dstLoc)
        root.getLocationOnScreen(rootLoc)

        val startX = (srcLoc[0]  - rootLoc[0]).toFloat()
        val startY = (srcLoc[1]  - rootLoc[1]).toFloat()
        val endX   = (dstLoc[0]  - rootLoc[0]).toFloat()
        val endY   = (dstLoc[1]  - rootLoc[1]).toFloat()

        val ghost = ImageView(root.context).apply {
            setImageDrawable(drawable)
            scaleType = ImageView.ScaleType.FIT_CENTER
            alpha = 0.92f
            elevation = 20f
            translationX = startX
            translationY = startY
            layoutParams = ConstraintLayout.LayoutParams(sourceView.width, sourceView.height)
        }
        root.addView(ghost)

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            addUpdateListener { anim ->
                val f = anim.animatedFraction
                ghost.translationX = startX + (endX - startX) * f
                ghost.translationY = startY + (endY - startY) * f
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    root.removeView(ghost)
                    onComplete()
                }
            })
            start()
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/utils/CardAnimator.kt
git commit -m "feat: add CardAnimator utility for ghost card flight animations"
```

---

## Task 8: Animate forceCardsEndDeck in GameActivity

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/GameActivity.kt`

Context: `forceCardsEndDeck` is called from `tryMove` when `enabledFastEndDeckClick` is true and the source pile still has cards after moving one to the end deck. It currently calls `setImage` directly. We replace the target `setImage` with a ghost animation.

**Important:** `endDeckList[line] = lastCard` must remain synchronous (before any animation) so that `hasReachedWonConditions()` — which reads `endDeckList` — gets correct data immediately after `tryMove` returns.

- [ ] **Step 1: Add gameRoot lazy property**

In `GameActivity.kt`, add a lazy property near the other lateinit/lazy fields (around line 78):
```kotlin
private val gameRoot: ConstraintLayout by lazy {
    findViewById<ConstraintLayout>(R.id.gameConstraintLayout)
}
```

`ConstraintLayout` is already imported in `GameActivity`.

- [ ] **Step 2: Replace forceCardsEndDeck body**

Replace the entire `forceCardsEndDeck` method with:

```kotlin
private fun forceCardsEndDeck(
    selectedPositionId: Int,
    selectedPositionName: String,
    desiredCardPositionId: Int,
    line: String
) {
    val lastCard = cardTableMap[selectedPositionName]!!.first()

    // Capture source view + card drawable BEFORE clearing the slot
    val sourceView = findViewById<ImageView>(selectedPositionId)
    val cardResourceName = if (lastCard == "zero") lastCard else "${cardType}_${lastCard}"
    val drawableId = ResourceUtils.getDrawableByName(resources, packageName, cardResourceName)
    val cardDrawable = ContextCompat.getDrawable(this, drawableId)
    val targetView = findViewById<ImageView>(desiredCardPositionId)

    // Update game state synchronously so win-condition checks are correct immediately
    cardTableMap[selectedPositionName]!!.clear()
    setNumberOfCards(cardTableMap[selectedPositionName]!!, selectedPositionName)
    endDeckList[line] = lastCard

    // Clear source slot visually right away
    setImage(selectedPositionId, "zero")

    // Animate the card ghost flying to the end-deck slot; show the card there on completion
    CardAnimator.animateCardFlight(gameRoot, sourceView, targetView, cardDrawable, 350L) {
        if (!isFinishing) {
            setImage(desiredCardPositionId, lastCard)
        }
    }
}
```

Also add the `CardAnimator` import at the top of the file (with the other utils imports):
```kotlin
import com.bottazzini.trasloco.utils.CardAnimator
```

- [ ] **Step 3: Verify on device**

Build and run. Enable "Auto-complete" in Settings. In a game, move a card from a pile onto the end deck. The remaining card in the source pile should visually fly to the end deck slot. Verify that win detection still works (game ends correctly when all end decks reach "10").

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/GameActivity.kt
git commit -m "feat: animate card flight in forceCardsEndDeck using CardAnimator"
```

---

## Task 9: Animate dealCard in GameActivity

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/GameActivity.kt`

Context: `dealCard(line)` fills empty table slots from the sub-deck when the user taps the deck stack. Cards should now fly from the deck view position to each target slot, staggered by 120ms per card (0ms, 120ms, 240ms). During `isInitializing` (game setup), images are applied directly — no animation.

- [ ] **Step 1: Replace dealCard body**

Replace the entire `dealCard` method with:

```kotlin
private fun dealCard(line: String) {
    val subDeck = getSubDeckListConcurrentSafely(line)
    val iterator = subDeck.iterator()

    // Collect (position, imageViewId, cardName) for each slot that will receive a card.
    // Update cardTableMap and subDeck state synchronously so lost-condition checks
    // after dealCard returns see the correct state.
    data class DealEntry(val position: String, val imageViewId: Int, val cardName: String)
    val deals = mutableListOf<DealEntry>()

    while (iterator.hasNext()) {
        val cardName = iterator.next()
        for (pos in 1..3) {
            val position = "$line$pos"
            val imageViewId =
                resources.getIdentifier("subDeck$position", "id", this.packageName)
            if (getCardName(imageViewId) == "zero") {
                cardTableMap[position] = arrayListOf(cardName)
                iterator.remove()
                clearUndoButton()
                deals.add(DealEntry(position, imageViewId, cardName))
                break
            }
        }
    }
    subDeckMap[line] = subDeck

    if (isInitializing || deals.isEmpty()) {
        // During board setup: apply images directly, no animation
        deals.forEach { setImage(it.imageViewId, it.cardName) }
        return
    }

    // Animate each card flying from the deck view to its target slot,
    // staggered by 120ms so cards arrive in a visible cascade.
    val deckViewId = resources.getIdentifier("subDeck$line", "id", packageName)
    val deckView = findViewById<ImageView>(deckViewId)

    deals.forEachIndexed { index, entry ->
        timerHandler.postDelayed({
            if (isFinishing) return@postDelayed
            playSoundAtomic(R.raw.flipcard)
            val cardResourceName = "${cardType}_${entry.cardName}"
            val drawableId =
                ResourceUtils.getDrawableByName(resources, packageName, cardResourceName)
            val cardDrawable = ContextCompat.getDrawable(this, drawableId)
            val targetView = findViewById<ImageView>(entry.imageViewId)
            CardAnimator.animateCardFlight(gameRoot, deckView, targetView, cardDrawable, 350L) {
                if (!isFinishing) {
                    setImage(entry.imageViewId, entry.cardName)
                }
            }
        }, index * 120L)
    }
}
```

Note: the `playSoundAtomic(R.raw.flipcard)` call that was in the original `dealCard` is now inside the staggered `postDelayed` block, so it plays once per card as each animation starts (rather than once immediately).

- [ ] **Step 2: Verify on device**

Build and run. In a game, tap one of the deck stacks (left column). The cards should fly one by one from the deck position to the empty slots with a slight cascade delay. Check all 4 rows. Check that when the deck is exhausted after dealing, the deck icon disappears correctly (this is handled in `subDeckClick` — unchanged).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/GameActivity.kt
git commit -m "feat: animate deal-card with staggered card flight from deck to table slots"
```

---

## Self-Review Notes

**Spec coverage check:**
- ✅ Audio mute toggle with persistence (Tasks 2–6)
- ✅ Rename Fast Deal → Auto-complete all locales (Task 1)
- ✅ `sound_with_icon` string in all locales (Task 3)
- ✅ `forceCardsEndDeck` animated (Task 8)
- ✅ `dealCard` animated with stagger (Task 9)
- ✅ `CardAnimator` singleton (Task 7)
- ✅ State updated synchronously, visual deferred (enforced in Tasks 8–9)
- ✅ `isInitializing` guard prevents animation during board setup (Task 9)
- ✅ `isFinishing` guard prevents crashes on activity teardown (Tasks 8–9)

**Placeholder scan:** None found — all steps include exact code.

**Type consistency:**
- `CardAnimator.animateCardFlight` signature used identically in Tasks 8 and 9.
- `gameRoot: ConstraintLayout` added in Task 8 and used in both Tasks 8 and 9 (Task 9 uses `gameRoot` which is set up in Task 8 — execute in order).
- `ResourceUtils.getDrawableByName(resources, packageName, name)` — already used in existing `GameActivity` code.
- `ContextCompat.getDrawable(this, drawableId)` — already imported in `GameActivity`.
