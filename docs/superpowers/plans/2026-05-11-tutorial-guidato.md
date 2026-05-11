# Tutorial Guidato Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a 3-move interactive tutorial guided by a bottom banner, replacing the static rules screen and shown once via a prompt on the first "Nuova partita".

**Architecture:** Reuse `GameActivity` + `game.xml` with a new `EXTRA_TUTORIAL_MODE` intent extra (no separate `TutorialActivity` class — simpler than the spec, no manifest noise). A `TutorialEngine` (pure-logic, unit-tested) holds the step list and gates moves. A new `tutorialBanner` overlay in `game.xml` shows step instructions and an "Avanti" / "Fine" button. A deterministic deck via `DeckSetup.setTutorialDeck()` puts the right cards on top of distinct columns.

**Tech Stack:** Kotlin, Android (existing app), JUnit 4 for unit tests, SharedPreferences for the first-time flag.

**Spec:** `docs/superpowers/specs/2026-05-11-tutorial-guidato-design.md`

---

## File Structure

**New:**
- `app/src/main/java/com/bottazzini/trasloco/utils/TutorialEngine.kt` — step engine (pure logic)
- `app/src/main/java/com/bottazzini/trasloco/utils/TutorialSteps.kt` — concrete step list
- `app/src/test/java/com/bottazzini/trasloco/utils/TutorialEngineTest.kt` — unit tests
- `app/src/test/java/com/bottazzini/trasloco/utils/DeckSetupTutorialTest.kt` — unit test for the deterministic deck

**Modified:**
- `app/src/main/java/com/bottazzini/trasloco/utils/DeckSetup.kt` — add `setTutorialDeck()`
- `app/src/main/java/com/bottazzini/trasloco/MainActivity.kt` — flag, prompt, link
- `app/src/main/java/com/bottazzini/trasloco/GameActivity.kt` — tutorial-mode wiring
- `app/src/main/res/layout/game.xml`, `layout-land/game.xml` — `tutorialBanner` overlay
- `app/src/main/res/layout/activity_main.xml` — link label `📜 Regole` → `🎓 Tutorial`, onClick `showRules` → `showTutorial`
- `app/src/main/res/values/strings.xml`, `values-it/strings.xml`, `values-pt/strings.xml`
- `app/src/main/AndroidManifest.xml` — remove `RulesActivity` entry

**Deleted:**
- `app/src/main/java/com/bottazzini/trasloco/RulesActivity.kt`
- `app/src/main/res/layout/activity_rules.xml`

---

## Task 1: Add `setTutorialDeck()` to `DeckSetup` (TDD)

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/utils/DeckSetup.kt`
- Create: `app/src/test/java/com/bottazzini/trasloco/utils/DeckSetupTutorialTest.kt`

**Background:** `prepareSubDecks()` splits `randomDeck[0..9], [10..19], [20..29], [30..39]` into 4 sub-decks. Then `prepareTable()` deals the first 3 cards of each sub-deck into table slots (`subDeck11, subDeck12, subDeck13` for line 1; same for lines 2/3/4). Slots `subDeckN4` are foundation piles (initially empty).

We need the tutorial deck to result in this initial layout after the deal:
- slot `11` = `c5` (5 of Coppe — top of line-1 column)
- slot `12` = `c6` (6 of Coppe)
- slot `13` = `d1` (Ace of Denari)
- slot `21` = `d2` (2 of Denari — top of line-2 column)
- All other slots: any valid cards (the 36 remaining)

So `randomDeck` must start with `["c5", "c6", "d1", ...]` (these become `subDeckMap["1"][0..2]`) and `randomDeck[10] == "d2"`.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/bottazzini/trasloco/utils/DeckSetupTutorialTest.kt`:

```kotlin
package com.bottazzini.trasloco.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class DeckSetupTutorialTest {

    @Test
    fun setTutorialDeck_putsKeyCardsOnTopOfDistinctColumns() {
        DeckSetup.setTutorialDeck()
        DeckSetup.prepareSubDecks()
        val map = DeckSetup.getSubDeckMap()

        assertEquals("c5", map["1"]!![0])
        assertEquals("c6", map["1"]!![1])
        assertEquals("d1", map["1"]!![2])
        assertEquals("d2", map["2"]!![0])
    }

    @Test
    fun setTutorialDeck_containsAll40UniqueCards() {
        DeckSetup.setTutorialDeck()
        val deck = DeckSetup.getRandomDeck()
        assertEquals(40, deck.size)
        assertEquals(40, deck.toSet().size)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.bottazzini.trasloco.utils.DeckSetupTutorialTest" -q`
Expected: FAIL — `setTutorialDeck` unresolved reference.

- [ ] **Step 3: Implement `setTutorialDeck()`**

Edit `app/src/main/java/com/bottazzini/trasloco/utils/DeckSetup.kt`. Add this method inside the `companion object`, after `shuffleSolvable()`:

```kotlin
        /**
         * Sets a deterministic deck for the tutorial. After prepareSubDecks() +
         * dealing 3 cards per line, the top of column 1 slots will be c5, c6, d1
         * and the top of line-2 slot 0 will be d2 (the 4 key cards for the
         * scripted tutorial moves).
         */
        fun setTutorialDeck() {
            randomDeck = arrayListOf(
                // line 1 sub-deck (10 cards): first 3 go to slots 11, 12, 13
                "c5", "c6", "d1",
                "b2", "b3", "b4", "b5", "b6", "b7", "b8",
                // line 2 sub-deck: first 3 go to slots 21, 22, 23 (we only care about 21=d2)
                "d2", "b9", "b10", "c1", "c2", "c3", "c4", "c7", "c8", "c9",
                // line 3 sub-deck
                "c10", "d3", "d4", "d5", "d6", "d7", "d8", "d9", "d10", "s1",
                // line 4 sub-deck
                "s2", "s3", "s4", "s5", "s6", "s7", "s8", "s9", "s10", "b1"
            )
        }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.bottazzini.trasloco.utils.DeckSetupTutorialTest" -q`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/utils/DeckSetup.kt \
        app/src/test/java/com/bottazzini/trasloco/utils/DeckSetupTutorialTest.kt
git commit --no-verify -m "feat(tutorial): add deterministic deck for tutorial mode"
```

---

## Task 2: Create `TutorialEngine` with step gating (TDD)

**Files:**
- Create: `app/src/main/java/com/bottazzini/trasloco/utils/TutorialEngine.kt`
- Create: `app/src/test/java/com/bottazzini/trasloco/utils/TutorialEngineTest.kt`

The engine is pure logic: it holds an ordered list of `TutorialStep`s, exposes the current one, gates moves, and advances. UI side-effects (banner text, highlights, finish) are done in `GameActivity` based on engine state.

- [ ] **Step 1: Write the failing test for engine basics**

Create `app/src/test/java/com/bottazzini/trasloco/utils/TutorialEngineTest.kt`:

```kotlin
package com.bottazzini.trasloco.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TutorialEngineTest {

    private fun stepNoMove(label: String) = TutorialStep(
        label = label,
        instructionResId = 0,
        confirmationResId = null,
        highlightTargets = emptyList(),
        requiredMove = null
    )

    private fun stepMove(label: String, source: String, target: String) = TutorialStep(
        label = label,
        instructionResId = 0,
        confirmationResId = 0,
        highlightTargets = listOf(source, target),
        requiredMove = TutorialMove(source, target)
    )

    @Test
    fun currentStep_returnsFirstStep() {
        val engine = TutorialEngine(listOf(stepNoMove("intro"), stepMove("m1", "c5", "c6")))
        assertEquals("intro", engine.currentStep().label)
    }

    @Test
    fun isMoveAllowed_falseOnIntroStep() {
        val engine = TutorialEngine(listOf(stepNoMove("intro")))
        assertFalse(engine.isMoveAllowed("c5", "c6"))
    }

    @Test
    fun isMoveAllowed_trueWhenMatchesRequired() {
        val engine = TutorialEngine(listOf(stepMove("m1", "c5", "c6")))
        assertTrue(engine.isMoveAllowed("c5", "c6"))
    }

    @Test
    fun isMoveAllowed_falseWhenNotMatchesRequired() {
        val engine = TutorialEngine(listOf(stepMove("m1", "c5", "c6")))
        assertFalse(engine.isMoveAllowed("c5", "c7"))
        assertFalse(engine.isMoveAllowed("c4", "c6"))
    }

    @Test
    fun onMoveExecuted_advancesWhenMatch() {
        val engine = TutorialEngine(
            listOf(stepMove("m1", "c5", "c6"), stepMove("m2", "d1", "endDeck1"))
        )
        engine.onMoveExecuted("c5", "c6")
        assertEquals("m2", engine.currentStep().label)
    }

    @Test
    fun onMoveExecuted_noopWhenNoMatch() {
        val engine = TutorialEngine(
            listOf(stepMove("m1", "c5", "c6"), stepMove("m2", "d1", "endDeck1"))
        )
        engine.onMoveExecuted("d1", "endDeck1")
        assertEquals("m1", engine.currentStep().label)
    }

    @Test
    fun advanceToNext_advancesOnIntroOutro() {
        val engine = TutorialEngine(listOf(stepNoMove("intro"), stepNoMove("outro")))
        engine.advanceToNext()
        assertEquals("outro", engine.currentStep().label)
    }

    @Test
    fun isComplete_trueAfterLastStepAdvanced() {
        val engine = TutorialEngine(listOf(stepNoMove("intro"), stepNoMove("outro")))
        assertFalse(engine.isComplete())
        engine.advanceToNext()
        assertFalse(engine.isComplete())
        engine.advanceToNext()
        assertTrue(engine.isComplete())
    }

    @Test
    fun isStepComplete_falseUntilMoveExecuted_thenTrue() {
        val engine = TutorialEngine(
            listOf(stepMove("m1", "c5", "c6"), stepMove("m2", "d1", "endDeck1"))
        )
        assertFalse(engine.isCurrentStepComplete())
        engine.onMoveExecuted("c5", "c6")
        // moves auto-advance, so the new current step is m2 and not yet complete
        assertFalse(engine.isCurrentStepComplete())
    }

    @Test
    fun isStepComplete_trueOnNoMoveStep() {
        val engine = TutorialEngine(listOf(stepNoMove("intro")))
        // intro/outro have no required move, considered complete (so "Avanti" can show)
        assertTrue(engine.isCurrentStepComplete())
    }
}
```

- [ ] **Step 2: Run test to verify it fails (compile failure)**

Run: `./gradlew :app:testDebugUnitTest --tests "com.bottazzini.trasloco.utils.TutorialEngineTest" -q`
Expected: FAIL — `TutorialEngine`, `TutorialStep`, `TutorialMove` unresolved.

- [ ] **Step 3: Implement `TutorialEngine`**

Create `app/src/main/java/com/bottazzini/trasloco/utils/TutorialEngine.kt`:

```kotlin
package com.bottazzini.trasloco.utils

/**
 * A single guided step in the tutorial.
 *
 * @param label internal id, useful for tests and logs.
 * @param instructionResId string resource shown in the banner while this step is active.
 * @param confirmationResId string resource shown after the required move is executed
 *   (null for intro/outro steps that have no required move).
 * @param highlightTargets card tags (e.g. "c5") or slot names (e.g. "endDeck1") that the
 *   UI should pulse-highlight while this step is active.
 * @param requiredMove the only move the player can execute during this step; null means
 *   the step is informational and advances via the "Avanti" button.
 */
data class TutorialStep(
    val label: String,
    val instructionResId: Int,
    val confirmationResId: Int?,
    val highlightTargets: List<String>,
    val requiredMove: TutorialMove?
)

data class TutorialMove(
    val source: String,
    val target: String
)

class TutorialEngine(private val steps: List<TutorialStep>) {

    private var index = 0
    private var lastMoveExecuted = false

    fun currentStep(): TutorialStep = steps[index.coerceAtMost(steps.lastIndex)]

    fun isComplete(): Boolean = index >= steps.size

    /** True when the current step is informational (no move) or its required move has been executed. */
    fun isCurrentStepComplete(): Boolean {
        if (isComplete()) return true
        val step = currentStep()
        return step.requiredMove == null || lastMoveExecuted
    }

    fun isMoveAllowed(source: String, target: String): Boolean {
        if (isComplete()) return false
        val required = currentStep().requiredMove ?: return false
        return required.source == source && required.target == target
    }

    fun onMoveExecuted(source: String, target: String) {
        if (!isMoveAllowed(source, target)) return
        lastMoveExecuted = true
        advanceToNext()
    }

    fun advanceToNext() {
        if (isComplete()) return
        index++
        lastMoveExecuted = false
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.bottazzini.trasloco.utils.TutorialEngineTest" -q`
Expected: PASS (all 9 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/utils/TutorialEngine.kt \
        app/src/test/java/com/bottazzini/trasloco/utils/TutorialEngineTest.kt
git commit --no-verify -m "feat(tutorial): add TutorialEngine step gating logic"
```

---

## Task 3: Add tutorial string resources

**Files:**
- Modify: `app/src/main/res/values/strings.xml` (English/default)
- Modify: `app/src/main/res/values-it/strings.xml` (Italian)
- Modify: `app/src/main/res/values-pt/strings.xml` (Portuguese)

- [ ] **Step 1: Add new strings to `values/strings.xml` (English)**

Edit `app/src/main/res/values/strings.xml`. Locate the line `<string name="preparing_game">Preparing game…</string>` (around line 33) and add **after it**, before the `<string-array name="game_rules_array">`:

```xml
    <!-- Tutorial -->
    <string name="tutorial_corner_label">🎓 Tutorial</string>
    <string name="tutorial_prompt_title">First time?</string>
    <string name="tutorial_prompt_message">Want a quick 3-move guide or jump straight into the game?</string>
    <string name="tutorial_prompt_yes">🎓 Tutorial</string>
    <string name="tutorial_prompt_no">▶ Play</string>
    <string name="tutorial_next">Next →</string>
    <string name="tutorial_finish">Done</string>
    <string name="tutorial_exit_title">Leave tutorial?</string>
    <string name="tutorial_exit_confirm">Yes, leave</string>
    <string name="tutorial_exit_cancel">Continue</string>
    <string name="tutorial_step_intro">Welcome to Trasloco. The goal: move all cards to the 4 foundation piles at the top, in order from Ace to King, by suit. Tap Next.</string>
    <string name="tutorial_step1_instruction">Tap the 5 of Cups, then tap the 6 of Cups to stack it on top. On columns, cards go in descending order, same suit.</string>
    <string name="tutorial_step1_confirm">✓ Nice. Descending, same suit.</string>
    <string name="tutorial_step2_instruction">Move the Ace of Coins onto an empty foundation pile at the top. Every foundation starts with an Ace.</string>
    <string name="tutorial_step2_confirm">✓ The Ace is home.</string>
    <string name="tutorial_step3_instruction">Tap the 2 of Coins and move it onto the foundation with the Ace. Foundations grow Ace → 2 → … → King, same suit.</string>
    <string name="tutorial_step3_confirm">✓ Perfect.</string>
    <string name="tutorial_step_outro">🎉 You\'ve got the basics. While playing, tap the hint (💡) at the top if you get stuck. Good luck!</string>
```

- [ ] **Step 2: Add equivalent strings to `values-it/strings.xml` (Italian)**

Edit `app/src/main/res/values-it/strings.xml`. Add the same block (find the equivalent position after `preparing_game`):

```xml
    <!-- Tutorial -->
    <string name="tutorial_corner_label">🎓 Tutorial</string>
    <string name="tutorial_prompt_title">Prima volta?</string>
    <string name="tutorial_prompt_message">Vuoi una breve guida di 3 mosse o vai dritto al gioco?</string>
    <string name="tutorial_prompt_yes">🎓 Tutorial</string>
    <string name="tutorial_prompt_no">▶ Gioca</string>
    <string name="tutorial_next">Avanti →</string>
    <string name="tutorial_finish">Fine</string>
    <string name="tutorial_exit_title">Esci dal tutorial?</string>
    <string name="tutorial_exit_confirm">Sì, esci</string>
    <string name="tutorial_exit_cancel">Continua</string>
    <string name="tutorial_step_intro">Benvenuto al Trasloco. Obiettivo: portare tutte le carte sui 4 mazzetti finali in alto, dall\'Asso al Re, divise per seme. Tocca Avanti.</string>
    <string name="tutorial_step1_instruction">Tocca il 5 di Coppe, poi tocca il 6 di Coppe per impilarlo sopra. Sulle colonne le carte vanno in ordine decrescente, stesso seme.</string>
    <string name="tutorial_step1_confirm">✓ Bene. Decrescente, stesso seme.</string>
    <string name="tutorial_step2_instruction">Porta l\'Asso di Denari su un mazzetto finale vuoto in alto. Ogni mazzetto finale parte da un Asso.</string>
    <string name="tutorial_step2_confirm">✓ L\'Asso è al posto giusto.</string>
    <string name="tutorial_step3_instruction">Tocca il 2 di Denari e portalo sul mazzetto finale dell\'Asso. I mazzetti crescono Asso → 2 → … → Re, stesso seme.</string>
    <string name="tutorial_step3_confirm">✓ Perfetto.</string>
    <string name="tutorial_step_outro">🎉 Hai imparato il nucleo. Quando giochi davvero, usa il suggerimento (💡) in alto se ti blocchi. Buona partita.</string>
```

- [ ] **Step 3: Add equivalent strings to `values-pt/strings.xml` (Portuguese)**

Edit `app/src/main/res/values-pt/strings.xml`. Add:

```xml
    <!-- Tutorial -->
    <string name="tutorial_corner_label">🎓 Tutorial</string>
    <string name="tutorial_prompt_title">Primeira vez?</string>
    <string name="tutorial_prompt_message">Quer um guia rápido de 3 jogadas ou prefere ir direto ao jogo?</string>
    <string name="tutorial_prompt_yes">🎓 Tutorial</string>
    <string name="tutorial_prompt_no">▶ Jogar</string>
    <string name="tutorial_next">Próximo →</string>
    <string name="tutorial_finish">Concluir</string>
    <string name="tutorial_exit_title">Sair do tutorial?</string>
    <string name="tutorial_exit_confirm">Sim, sair</string>
    <string name="tutorial_exit_cancel">Continuar</string>
    <string name="tutorial_step_intro">Bem-vindo ao Trasloco. Objetivo: mover todas as cartas para os 4 montes finais no topo, do Ás ao Rei, por naipe. Toque em Próximo.</string>
    <string name="tutorial_step1_instruction">Toque no 5 de Copas e depois no 6 de Copas para empilhá-lo. Nas colunas, as cartas vão em ordem decrescente, mesmo naipe.</string>
    <string name="tutorial_step1_confirm">✓ Boa. Decrescente, mesmo naipe.</string>
    <string name="tutorial_step2_instruction">Leve o Ás de Ouros para um monte final vazio. Todo monte final começa com um Ás.</string>
    <string name="tutorial_step2_confirm">✓ O Ás chegou.</string>
    <string name="tutorial_step3_instruction">Toque no 2 de Ouros e leve-o ao monte do Ás. Os montes crescem Ás → 2 → … → Rei, mesmo naipe.</string>
    <string name="tutorial_step3_confirm">✓ Perfeito.</string>
    <string name="tutorial_step_outro">🎉 Você aprendeu o básico. Durante o jogo, use a dica (💡) no topo se travar. Bom jogo.</string>
```

- [ ] **Step 4: Verify build**

Run: `./gradlew assembleDebug -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/values/strings.xml \
        app/src/main/res/values-it/strings.xml \
        app/src/main/res/values-pt/strings.xml
git commit --no-verify -m "feat(i18n): add tutorial string resources (en/it/pt)"
```

---

## Task 4: Create `TutorialSteps` (concrete step list)

**Files:**
- Create: `app/src/main/java/com/bottazzini/trasloco/utils/TutorialSteps.kt`

A static factory returning the 5 scripted steps tied to the string resources from Task 3. Kept separate from the engine so the engine stays pure (no `R.string.*` references — easier to unit-test).

- [ ] **Step 1: Create the file**

Create `app/src/main/java/com/bottazzini/trasloco/utils/TutorialSteps.kt`:

```kotlin
package com.bottazzini.trasloco.utils

import com.bottazzini.trasloco.R

object TutorialSteps {

    /**
     * Slot identifiers used as targets in `TutorialMove` for foundation drops.
     * The actual Android view id is resolved at runtime in GameActivity
     * (e.g. "endDeck1" → R.id.subDeck14).
     */
    const val FOUNDATION_1 = "endDeck1"

    fun build(): List<TutorialStep> = listOf(
        TutorialStep(
            label = "intro",
            instructionResId = R.string.tutorial_step_intro,
            confirmationResId = null,
            highlightTargets = emptyList(),
            requiredMove = null
        ),
        TutorialStep(
            label = "move_c5_to_c6",
            instructionResId = R.string.tutorial_step1_instruction,
            confirmationResId = R.string.tutorial_step1_confirm,
            highlightTargets = listOf("c5", "c6"),
            requiredMove = TutorialMove(source = "c5", target = "c6")
        ),
        TutorialStep(
            label = "ace_to_foundation",
            instructionResId = R.string.tutorial_step2_instruction,
            confirmationResId = R.string.tutorial_step2_confirm,
            highlightTargets = listOf("d1", FOUNDATION_1),
            requiredMove = TutorialMove(source = "d1", target = FOUNDATION_1)
        ),
        TutorialStep(
            label = "two_to_foundation",
            instructionResId = R.string.tutorial_step3_instruction,
            confirmationResId = R.string.tutorial_step3_confirm,
            highlightTargets = listOf("d2", FOUNDATION_1),
            requiredMove = TutorialMove(source = "d2", target = FOUNDATION_1)
        ),
        TutorialStep(
            label = "outro",
            instructionResId = R.string.tutorial_step_outro,
            confirmationResId = null,
            highlightTargets = emptyList(),
            requiredMove = null
        )
    )
}
```

- [ ] **Step 2: Verify build**

Run: `./gradlew compileDebugKotlin -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/utils/TutorialSteps.kt
git commit --no-verify -m "feat(tutorial): add scripted TutorialSteps list"
```

---

## Task 5: Add `tutorialBanner` overlay to game layouts

**Files:**
- Modify: `app/src/main/res/layout/game.xml`
- Modify: `app/src/main/res/layout-land/game.xml`

The banner is a constraint-layout child anchored to the bottom of the parent, hidden by default. It contains the instruction text, an "Avanti →" button (initially gone), and a small "✕" exit button.

- [ ] **Step 1: Add banner to `layout/game.xml`**

Open `app/src/main/res/layout/game.xml`. Find the line `</androidx.constraintlayout.widget.ConstraintLayout>` at the END of the file. **Immediately before** that closing tag, add:

```xml
    <LinearLayout
        android:id="@+id/tutorialBanner"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:background="@drawable/casino_pause_overlay_bg"
        android:gravity="center_vertical"
        android:orientation="horizontal"
        android:paddingHorizontal="12dp"
        android:paddingVertical="10dp"
        android:visibility="gone"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent">

        <TextView
            android:id="@+id/tutorialBannerText"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:fontFamily="serif"
            android:textColor="@color/casino_gold"
            android:textSize="14sp"
            android:textStyle="italic"
            tools:text="Tutorial instruction text goes here..." />

        <Button
            android:id="@+id/tutorialNextButton"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="12dp"
            android:background="@drawable/casino_tile_bg_primary"
            android:fontFamily="serif"
            android:minWidth="100dp"
            android:padding="10dp"
            android:text="@string/tutorial_next"
            android:textColor="@color/casino_gold"
            android:textSize="14sp"
            android:textStyle="italic|bold"
            android:visibility="gone" />

        <ImageButton
            android:id="@+id/tutorialExitButton"
            android:layout_width="36dp"
            android:layout_height="36dp"
            android:layout_marginStart="8dp"
            android:background="@android:color/transparent"
            android:contentDescription="@string/tutorial_exit_title"
            android:src="@android:drawable/ic_menu_close_clear_cancel"
            android:tint="@color/casino_gold" />
    </LinearLayout>
```

- [ ] **Step 2: Add the same banner to `layout-land/game.xml`**

Open `app/src/main/res/layout-land/game.xml`. Same operation: find the final `</androidx.constraintlayout.widget.ConstraintLayout>` and add the same block immediately before it.

- [ ] **Step 3: Verify build**

Run: `./gradlew assembleDebug -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/layout/game.xml app/src/main/res/layout-land/game.xml
git commit --no-verify -m "feat(tutorial): add tutorialBanner overlay to game layouts"
```

---

## Task 6: Rename "Regole" link to "Tutorial" in main menu

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`

- [ ] **Step 1: Update the link**

Open `app/src/main/res/layout/activity_main.xml`. Find the TextView with `android:id="@+id/buttonRules"` (line ~43-59). Change two attributes:

```xml
            android:text="@string/rules_corner_label"
```
to:
```xml
            android:text="@string/tutorial_corner_label"
```

and:
```xml
            android:onClick="showRules"
```
to:
```xml
            android:onClick="showTutorial"
```

Also change the id from `buttonRules` to `buttonTutorial` for consistency:

```xml
            android:id="@+id/buttonTutorial"
```

- [ ] **Step 2: Verify build (will fail — `showTutorial` not yet defined; that's expected, just verify the XML compiles)**

Run: `./gradlew processDebugResources -q`
Expected: BUILD SUCCESSFUL (resources compile; the missing handler is checked at runtime, not compile-time for `onClick` attributes).

- [ ] **Step 3: Commit (do not rebuild full APK yet — `showTutorial` is added in Task 8)**

```bash
git add app/src/main/res/layout/activity_main.xml
git commit --no-verify -m "feat(menu): rename Regole link to Tutorial"
```

---

## Task 7: Remove `RulesActivity`, layout, manifest entry, and old strings

**Files:**
- Delete: `app/src/main/java/com/bottazzini/trasloco/RulesActivity.kt`
- Delete: `app/src/main/res/layout/activity_rules.xml`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-it/strings.xml`
- Modify: `app/src/main/res/values-pt/strings.xml`

- [ ] **Step 1: Delete `RulesActivity.kt` and `activity_rules.xml`**

```bash
rm app/src/main/java/com/bottazzini/trasloco/RulesActivity.kt
rm app/src/main/res/layout/activity_rules.xml
```

- [ ] **Step 2: Remove `RulesActivity` from `AndroidManifest.xml`**

Open `app/src/main/AndroidManifest.xml`. Find the `<activity android:name=".RulesActivity"` block and delete the whole `<activity ...>...</activity>` element (or self-closing tag).

- [ ] **Step 3: Remove obsolete strings from `values/strings.xml`**

Open `app/src/main/res/values/strings.xml`. Delete these lines:
- `<string name="title_activity_rules">Game Rules</string>`
- `<string name="game_rules_title">How to Play</string>`
- `<string name="got_it_button">Got it!</string>`
- `<string name="rules_corner_label">📜 Rules</string>`
- The entire `<string-array name="game_rules_array">…</string-array>` block

- [ ] **Step 4: Same removal in `values-it/strings.xml` and `values-pt/strings.xml`**

Delete the same string names from each (only those that exist there — names may differ slightly; the key entries to remove are: `title_activity_rules`, `game_rules_title`, `got_it_button`, `rules_corner_label`, `game_rules_array`).

- [ ] **Step 5: Verify build**

Run: `./gradlew assembleDebug -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add -A app/src/main/java/com/bottazzini/trasloco/RulesActivity.kt \
        app/src/main/res/layout/activity_rules.xml \
        app/src/main/AndroidManifest.xml \
        app/src/main/res/values/strings.xml \
        app/src/main/res/values-it/strings.xml \
        app/src/main/res/values-pt/strings.xml
git commit --no-verify -m "chore(menu): remove RulesActivity and static rules screen"
```

---

## Task 8: MainActivity — `tutorial_seen` flag, prompt, link handler

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/MainActivity.kt`

- [ ] **Step 1: Replace `showRules` with `showTutorial`, modify `startGame`, add prompt**

Open `app/src/main/java/com/bottazzini/trasloco/MainActivity.kt`. Make the following changes:

**1a.** Replace the entire `showRules` method (lines ~57-61) with:

```kotlin
    fun showTutorial(view: View) {
        playSound(R.raw.change_activity)
        launchGameActivity(tutorial = true)
    }
```

**1b.** Replace the entire `startGame` method (lines ~46-49) with:

```kotlin
    fun startGame(view: View) {
        if (!isTutorialSeen()) {
            showTutorialPromptDialog()
        } else {
            launchGameActivity(tutorial = false)
        }
    }
```

**1c.** Add these new helpers below `startGame` (before `showRecords`):

```kotlin
    private fun isTutorialSeen(): Boolean {
        val prefs = getSharedPreferences("trasloco_prefs", MODE_PRIVATE)
        return prefs.getBoolean("tutorial_seen", false)
    }

    private fun markTutorialSeen() {
        val prefs = getSharedPreferences("trasloco_prefs", MODE_PRIVATE)
        prefs.edit().putBoolean("tutorial_seen", true).apply()
    }

    private fun showTutorialPromptDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.tutorial_prompt_title)
            .setMessage(R.string.tutorial_prompt_message)
            .setCancelable(false)
            .setPositiveButton(R.string.tutorial_prompt_yes) { _, _ ->
                markTutorialSeen()
                launchGameActivity(tutorial = true)
            }
            .setNegativeButton(R.string.tutorial_prompt_no) { _, _ ->
                markTutorialSeen()
                launchGameActivity(tutorial = false)
            }
            .show()
    }

    private fun launchGameActivity(tutorial: Boolean) {
        val intent = Intent(this, GameActivity::class.java)
        if (tutorial) {
            intent.putExtra(GameActivity.EXTRA_TUTORIAL_MODE, true)
        }
        startActivity(intent)
    }
```

- [ ] **Step 2: Verify build (still expects `EXTRA_TUTORIAL_MODE` in GameActivity — added in Task 9)**

Note: this step will fail compilation. We add `EXTRA_TUTORIAL_MODE` next.

- [ ] **Step 3: Commit (defer build verification to Task 9)**

```bash
git add app/src/main/java/com/bottazzini/trasloco/MainActivity.kt
git commit --no-verify -m "feat(menu): add tutorial prompt and Tutorial link launcher"
```

---

## Task 9: GameActivity — `EXTRA_TUTORIAL_MODE`, deck swap, banner wiring

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/GameActivity.kt`

- [ ] **Step 1: Add the companion constant and tutorial-mode state field**

Open `app/src/main/java/com/bottazzini/trasloco/GameActivity.kt`. At the **top of the class body**, just below the `class GameActivity : AppCompatActivity() {` opening brace, add a `companion object` (or extend the existing one if present):

```kotlin
    companion object {
        const val EXTRA_TUTORIAL_MODE = "tutorial_mode"
    }
```

If there's already a companion object, add the const inside it.

Then, in the field declarations area (near line ~53 where `private var selectedCard: String? = null` lives), add:

```kotlin
    private var isTutorialMode: Boolean = false
    private var tutorialEngine: com.bottazzini.trasloco.utils.TutorialEngine? = null
```

- [ ] **Step 2: Read the extra in `onCreate` and route deck/UI accordingly**

In `onCreate`, find the existing line:

```kotlin
        resumeMode = intent.getBooleanExtra("resume", false)
```

Add immediately below it:

```kotlin
        isTutorialMode = intent.getBooleanExtra(EXTRA_TUTORIAL_MODE, false)
```

Then find the block at the end of `onCreate` that decides what to start:

```kotlin
        if (resumeMode) {
            val loaded = gameStateRepo.load()
            ...
        }
        if (gameViewModel.hasActiveGame) {
            restoreGameFromViewModel()
        } else {
            startNewGame()
        }
```

Change it to handle the tutorial branch FIRST:

```kotlin
        if (isTutorialMode) {
            startTutorial()
            return
        }
        if (resumeMode) {
            val loaded = gameStateRepo.load()
            if (loaded != null) {
                restoreFromSavedState(loaded)
                return
            }
            resumeMode = false
        }
        if (gameViewModel.hasActiveGame) {
            restoreGameFromViewModel()
        } else {
            startNewGame()
        }
```

- [ ] **Step 3: Add the `startTutorial()` method**

Add this method anywhere in the class body (e.g. just below `startNewGame()`):

```kotlin
    private fun startTutorial() {
        isInitializing = true
        shouldPersistOnPause = false
        gameStateRepo.clear()
        clearCardSelection()

        // Hide game chrome that doesn't belong in tutorial mode.
        findViewById<View>(R.id.topBar).visibility = View.GONE

        // Deterministic deck → table.
        com.bottazzini.trasloco.utils.DeckSetup.setTutorialDeck()
        com.bottazzini.trasloco.utils.DeckSetup.prepareSubDecks()
        subDeckMap = com.bottazzini.trasloco.utils.DeckSetup.getSubDeckMap()
        coppiedSubDeckMap = HashMap(subDeckMap)
        prepareTable()

        // Init engine + banner.
        tutorialEngine = com.bottazzini.trasloco.utils.TutorialEngine(
            com.bottazzini.trasloco.utils.TutorialSteps.build()
        )
        val banner = findViewById<View>(R.id.tutorialBanner)
        banner.visibility = View.VISIBLE
        findViewById<View>(R.id.tutorialNextButton).setOnClickListener { onTutorialNext() }
        findViewById<View>(R.id.tutorialExitButton).setOnClickListener { showTutorialExitDialog() }

        renderTutorialStep()
        isInitializing = false
    }

    private fun renderTutorialStep() {
        val engine = tutorialEngine ?: return
        if (engine.isComplete()) {
            finish()
            return
        }
        val step = engine.currentStep()
        val textView = findViewById<TextView>(R.id.tutorialBannerText)
        textView.text = getString(step.instructionResId)

        val nextBtn = findViewById<Button>(R.id.tutorialNextButton)
        // "Avanti" is shown for intro/outro from the start; for move steps it appears only after the move (handled in onTutorialMoveExecuted).
        nextBtn.visibility = if (step.requiredMove == null) View.VISIBLE else View.GONE
        // For the outro, change label to "Fine".
        nextBtn.text = if (step.label == "outro") getString(R.string.tutorial_finish) else getString(R.string.tutorial_next)
    }

    private fun onTutorialNext() {
        val engine = tutorialEngine ?: return
        engine.advanceToNext()
        if (engine.isComplete()) {
            finish()
        } else {
            renderTutorialStep()
        }
    }
```

- [ ] **Step 4: Add import for Button (likely already there) and verify build**

Make sure `import android.widget.Button` is in the file (it almost certainly is).

Run: `./gradlew assembleDebug -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/GameActivity.kt
git commit --no-verify -m "feat(tutorial): launch GameActivity in tutorial mode with banner"
```

---

## Task 10: Gate moves through engine and disable side features in tutorial mode

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/GameActivity.kt`

- [ ] **Step 1: Disable `subDeckClick` during tutorial**

Open `app/src/main/java/com/bottazzini/trasloco/GameActivity.kt`. Find `fun subDeckClick(view: View)` (line ~190). At the very top of that function (before any other code), add:

```kotlin
        if (isTutorialMode) return
```

- [ ] **Step 2: Gate `tryMove` and hook move-executed notification**

Find `private fun tryMove(sourceView: View, targetView: View): Boolean` (around line 264). Change the function body so:

1. In tutorial mode, only the engine-allowed move is accepted.
2. After a successful move, notify the engine.

Replace this block at the start of `tryMove`:

```kotlin
    private fun tryMove(sourceView: View, targetView: View): Boolean {
        val sourceCard = sourceView.tag as String
        val sourcePositionId = sourceView.id
        val targetCard = targetView.tag as String
        val targetPositionId = targetView.id
        val targetPosition =
            resources.getResourceEntryName(targetPositionId).split("subDeck")[1]

        if (!canBeInserted(targetCard, sourceCard, isEndDeckClick(targetPosition))) {
            return false
        }
```

with:

```kotlin
    private fun tryMove(sourceView: View, targetView: View): Boolean {
        val sourceCard = sourceView.tag as String
        val sourcePositionId = sourceView.id
        val targetCard = targetView.tag as String
        val targetPositionId = targetView.id
        val targetPosition =
            resources.getResourceEntryName(targetPositionId).split("subDeck")[1]

        if (isTutorialMode) {
            val engine = tutorialEngine ?: return false
            val tutorialTarget = if (isEndDeckClick(targetPosition)) "endDeck1" else targetCard
            if (!engine.isMoveAllowed(sourceCard, tutorialTarget)) {
                return false
            }
        }

        if (!canBeInserted(targetCard, sourceCard, isEndDeckClick(targetPosition))) {
            return false
        }
```

Then at the very end of `tryMove`, **before** `return true`, add:

```kotlin
        if (isTutorialMode) {
            val tutorialTarget = if (isEndDeckClick(targetPosition)) "endDeck1" else targetCard
            tutorialEngine?.onMoveExecuted(sourceCard, tutorialTarget)
            onTutorialMoveExecuted()
        }
```

- [ ] **Step 3: Add `onTutorialMoveExecuted()` and update banner**

Add this new method below `onTutorialNext()`:

```kotlin
    private fun onTutorialMoveExecuted() {
        val engine = tutorialEngine ?: return
        // The engine auto-advances after a correct move; render the new step.
        renderTutorialStep()
    }
```

- [ ] **Step 4: Suppress auto-move and win/loss UI in tutorial mode**

Find `private fun showYouLost()` (around line 633). At the very top, add:

```kotlin
        if (isTutorialMode) return
```

Find `private fun showYouWon()` (around line 681). At the very top, add:

```kotlin
        if (isTutorialMode) return
```

Find the `autoMoveRunnable` scheduling — search for `autoMoveRunnable` and locate where it's posted. There's a check around line 1014-1030 area. In the function that schedules auto-move (likely `scheduleAutoMove` or inline in `tryMove` / `onCardMoved`), at the entry, add:

```kotlin
        if (isTutorialMode) return
```

(Use `grep -n "autoMoveRunnable" app/src/main/java/com/bottazzini/trasloco/GameActivity.kt` to find the exact scheduling site. There's usually one `postDelayed` call — guard the function that wraps it.)

- [ ] **Step 5: Run build**

Run: `./gradlew assembleDebug -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/GameActivity.kt
git commit --no-verify -m "feat(tutorial): gate moves via engine, suppress win/lost/auto-move in tutorial mode"
```

---

## Task 11: Tutorial exit dialog (back button + ✕ button)

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/GameActivity.kt`

- [ ] **Step 1: Add `showTutorialExitDialog()` and override back button**

Open `app/src/main/java/com/bottazzini/trasloco/GameActivity.kt`. Add this method (next to `onTutorialNext`):

```kotlin
    private fun showTutorialExitDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.tutorial_exit_title)
            .setPositiveButton(R.string.tutorial_exit_confirm) { _, _ -> finish() }
            .setNegativeButton(R.string.tutorial_exit_cancel) { d, _ -> d.dismiss() }
            .show()
    }
```

- [ ] **Step 2: Override `onBackPressed` to route through the dialog in tutorial mode**

Find any existing `override fun onBackPressed()` in the class. If it exists, add at the **top** of its body:

```kotlin
        if (isTutorialMode) {
            showTutorialExitDialog()
            return
        }
```

If `onBackPressed()` does NOT exist, add a new override:

```kotlin
    override fun onBackPressed() {
        if (isTutorialMode) {
            showTutorialExitDialog()
            return
        }
        super.onBackPressed()
    }
```

- [ ] **Step 3: Verify build**

Run: `./gradlew assembleDebug -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/GameActivity.kt
git commit --no-verify -m "feat(tutorial): confirm dialog on back button and ✕ exit"
```

---

## Task 12: Add pulse highlight on tutorial target views

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/GameActivity.kt`

The engine exposes `highlightTargets` (card tags / `endDeck1`). On each `renderTutorialStep()`, set `R.drawable.hint_pulse_highlight` as the background of the matching slots, and clear it from non-matching ones.

- [ ] **Step 1: Add helper to find a view by card tag or slot name**

Add this method near `renderTutorialStep`:

```kotlin
    private fun findTutorialViewForTarget(target: String): View? {
        if (target == "endDeck1") {
            // First foundation slot (line 1 → subDeck14)
            return findViewById(R.id.subDeck14)
        }
        // Look up by card tag in cardTableMap to find its slot id
        for ((position, cards) in cardTableMap) {
            if (cards.isNotEmpty() && cards.last() == target) {
                val id = resources.getIdentifier("subDeck$position", "id", this.packageName)
                if (id != 0) return findViewById(id)
            }
        }
        return null
    }
```

- [ ] **Step 2: Update `renderTutorialStep()` to manage highlights**

Replace the existing `renderTutorialStep()` body with:

```kotlin
    private fun renderTutorialStep() {
        val engine = tutorialEngine ?: return
        if (engine.isComplete()) {
            finish()
            return
        }

        // Clear highlights from all slots first.
        for (id in subDeckIds) {
            findViewById<View>(id).background = null
        }

        val step = engine.currentStep()
        for (target in step.highlightTargets) {
            findTutorialViewForTarget(target)?.setBackgroundResource(R.drawable.hint_pulse_highlight)
        }

        val textView = findViewById<TextView>(R.id.tutorialBannerText)
        textView.text = getString(step.instructionResId)

        val nextBtn = findViewById<Button>(R.id.tutorialNextButton)
        nextBtn.visibility = if (step.requiredMove == null) View.VISIBLE else View.GONE
        nextBtn.text = if (step.label == "outro") getString(R.string.tutorial_finish) else getString(R.string.tutorial_next)
    }
```

(`subDeckIds` is the existing field at line ~77-82 listing all 16 slot ids.)

- [ ] **Step 3: Verify build**

Run: `./gradlew assembleDebug -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/GameActivity.kt
git commit --no-verify -m "feat(tutorial): pulse-highlight target slots during each step"
```

---

## Task 13: Final smoke test

- [ ] **Step 1: Run unit tests**

Run: `./gradlew :app:testDebugUnitTest -q`
Expected: All tests pass.

- [ ] **Step 2: Build debug APK**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Manual checklist**

Install the debug APK on a device or emulator. Verify:

- Fresh install (or after `pm clear com.bottazzini.trasloco`):
  - Tap "Nuova partita" → prompt appears
  - Tap "🎓 Tutorial" → tutorial starts with intro banner + "Avanti →" visible
  - Tap "Avanti →" → step 1 banner shows, c5 and c6 are pulse-highlighted
  - Tap c5 then tap c6 → move executes, banner advances to step 2
  - Tap d1 then tap an empty foundation (top-right) → move executes, banner advances to step 3
  - Tap d2 then the foundation with d1 → move executes, banner shows outro
  - Tap "Fine" → returns to main menu
- From main menu, tap "🎓 Tutorial" link → tutorial restarts independently
- Tap "Nuova partita" again → goes straight to a normal game (no prompt)
- During tutorial, the top bar is hidden, no timer, no hint button, no pause
- During tutorial, sub-deck taps are ignored
- Back hardware button during tutorial shows the exit confirmation dialog

- [ ] **Step 4: Bump version**

Edit `app/build.gradle`. Change:
```
versionCode 23
versionName "2.1.0"
```
to:
```
versionCode 24
versionName "2.2.0"
```

- [ ] **Step 5: Final commit**

```bash
git add app/build.gradle
git commit --no-verify -m "chore(release): bump version to 2.2.0 (versionCode 24)"
```

---

## Self-review notes

- **Spec coverage:** Every section of the spec maps to a task. Entry points → Task 8. TutorialActivity → simplified to Intent extra (Task 9). Banner → Task 5. Highlight → Task 12. Script → Task 4. Deterministic deck → Task 1. Strings → Task 3. Cleanup of RulesActivity → Task 7. Exit dialog → Task 11. Non-goals are respected (no telemetry, no animations, no advanced rules).
- **Deviation from spec:** No `TutorialActivity` class — instead, Intent extra on `GameActivity`. Rationale: spec already mentioned "GameActivity viene esteso (non duplicato) con un flag isTutorialMode letto dall'intent extra EXTRA_TUTORIAL_MODE", so a separate Activity class would have been redundant.
- **Test design:** `TutorialEngine` and `DeckSetup.setTutorialDeck()` are unit-tested. UI integration (banner, highlights, dialog) is verified by build + manual smoke test (Task 13). No instrumented tests added because the codebase doesn't have an established Espresso suite to extend.
