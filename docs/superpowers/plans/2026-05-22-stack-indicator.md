# Stack Indicator Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Mostrare due sottili linee bianche nella parte bassa di ogni carta che ha carte sotto — scoperte (colonne 1–3), coperte (mazzi sorgente) e end deck — più un badge numerico sui mazzi sorgente.

**Architecture:** Un custom `Drawable` (`CardStackIndicatorDrawable`) viene aggiunto come `ViewOverlay` su ogni ImageView carta. I badge dei mazzi sorgente sono 4 nuovi `TextView` in `game.xml`. Tre metodi helper in `GameActivity` gestiscono i singoli tipi di carta; un quarto li coordina tutti per refresh globale.

**Tech Stack:** Kotlin, Android SDK (ViewOverlay API 18+, minSdk=24 ✓), ConstraintLayout, AndroidX Core KTX (`doOnLayout`)

---

## File Map

| File | Azione |
|---|---|
| `app/src/main/java/com/bottazzini/trasloco/utils/CardStackIndicatorDrawable.kt` | **Crea** — Drawable custom con le due linee |
| `app/src/main/res/layout/game.xml` | **Modifica** — aggiungi 4 TextView badge sorgente |
| `app/src/main/java/com/bottazzini/trasloco/GameActivity.kt` | **Modifica** — 6 nuovi metodi, 6 hook in codice esistente |

---

### Task 1: `CardStackIndicatorDrawable`

**Files:**
- Create: `app/src/main/java/com/bottazzini/trasloco/utils/CardStackIndicatorDrawable.kt`

- [ ] **Step 1: Crea il file del Drawable**

```kotlin
package com.bottazzini.trasloco.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/**
 * Draws two thin horizontal white lines near the bottom of a card view,
 * indicating that cards are stacked below.
 *
 * Apply via ViewOverlay so it doesn't interfere with existing foreground/background.
 */
class CardStackIndicatorDrawable(context: Context) : Drawable() {

    private val density = context.resources.displayMetrics.density

    // line 1: 2dp thick, 7dp from bottom edge (~30% white)
    private val paint1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(76, 255, 255, 255)
    }
    // line 2: 1.5dp thick, 3dp from bottom edge (~18% white)
    private val paint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(46, 255, 255, 255)
    }

    private val margin      = 4f   * density   // left/right inset
    private val l1Height    = 2f   * density
    private val l1FromBot   = 7f   * density
    private val l2Height    = 1.5f * density
    private val l2FromBot   = 3f   * density

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.isEmpty) return
        val bottom = b.bottom.toFloat()
        val left   = b.left  + margin
        val right  = b.right - margin
        // upper line (more visible)
        canvas.drawRect(left, bottom - l1FromBot - l1Height, right, bottom - l1FromBot, paint1)
        // lower line (fainter)
        canvas.drawRect(left, bottom - l2FromBot - l2Height, right, bottom - l2FromBot, paint2)
    }

    override fun setAlpha(alpha: Int) { /* not used */ }
    override fun setColorFilter(cf: ColorFilter?) { /* not used */ }
    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
```

- [ ] **Step 2: Verifica che compili**

```bash
cd /path/to/Trasloco && ./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL, nessun errore.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/utils/CardStackIndicatorDrawable.kt
git commit -m "feat: add CardStackIndicatorDrawable for stack depth visual"
```

---

### Task 2: Badge TextViews per i mazzi sorgente in `game.xml`

**Files:**
- Modify: `app/src/main/res/layout/game.xml`

I mazzi sorgente sono `subDeck1`–`subDeck4`. Aggiungiamo 4 TextView badge con lo stesso stile di `textView11` ma vincolati al bottom di ciascun mazzo.

- [ ] **Step 1: Aggiungi i 4 TextView nel layout**

Apri `app/src/main/res/layout/game.xml`. Dopo il blocco `<!-- ========== CARD COUNT TEXT VIEWS ========== -->` e prima di `<!-- ========== GAME STATUS UI ========== -->`, aggiungi:

```xml
    <!-- ========== SOURCE DECK COUNT TEXT VIEWS ========== -->

    <TextView
        android:id="@+id/textViewDeck1"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textStyle="bold"
        android:textColor="@color/white"
        android:background="@drawable/counter_badge"
        android:paddingStart="6dp"
        android:paddingEnd="6dp"
        android:paddingTop="1dp"
        android:paddingBottom="1dp"
        android:visibility="invisible"
        app:layout_constraintStart_toStartOf="@id/subDeck1"
        app:layout_constraintEnd_toEndOf="@id/subDeck1"
        app:layout_constraintBottom_toBottomOf="@id/subDeck1"
        android:layout_marginBottom="2dp" />

    <TextView
        android:id="@+id/textViewDeck2"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textStyle="bold"
        android:textColor="@color/white"
        android:background="@drawable/counter_badge"
        android:paddingStart="6dp"
        android:paddingEnd="6dp"
        android:paddingTop="1dp"
        android:paddingBottom="1dp"
        android:visibility="invisible"
        app:layout_constraintStart_toStartOf="@id/subDeck2"
        app:layout_constraintEnd_toEndOf="@id/subDeck2"
        app:layout_constraintBottom_toBottomOf="@id/subDeck2"
        android:layout_marginBottom="2dp" />

    <TextView
        android:id="@+id/textViewDeck3"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textStyle="bold"
        android:textColor="@color/white"
        android:background="@drawable/counter_badge"
        android:paddingStart="6dp"
        android:paddingEnd="6dp"
        android:paddingTop="1dp"
        android:paddingBottom="1dp"
        android:visibility="invisible"
        app:layout_constraintStart_toStartOf="@id/subDeck3"
        app:layout_constraintEnd_toEndOf="@id/subDeck3"
        app:layout_constraintBottom_toBottomOf="@id/subDeck3"
        android:layout_marginBottom="2dp" />

    <TextView
        android:id="@+id/textViewDeck4"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textStyle="bold"
        android:textColor="@color/white"
        android:background="@drawable/counter_badge"
        android:paddingStart="6dp"
        android:paddingEnd="6dp"
        android:paddingTop="1dp"
        android:paddingBottom="1dp"
        android:visibility="invisible"
        app:layout_constraintStart_toStartOf="@id/subDeck4"
        app:layout_constraintEnd_toEndOf="@id/subDeck4"
        app:layout_constraintBottom_toBottomOf="@id/subDeck4"
        android:layout_marginBottom="2dp" />
```

- [ ] **Step 2: Verifica che compili**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/layout/game.xml
git commit -m "feat(layout): add source deck count badges textViewDeck1-4"
```

---

### Task 3: Helper `setStackIndicator()` in `GameActivity`

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/GameActivity.kt`

Questo metodo aggiunge o rimuove l'overlay dell'indicatore su una View. Usa `doOnLayout` (da `androidx.core.view`) per gestire le View non ancora misurate.

- [ ] **Step 1: Aggiungi l'import mancante e il metodo**

Aggiungi tra gli import esistenti (già ci sono quelli di `androidx.core.view`):
```kotlin
import androidx.core.view.doOnLayout
import com.bottazzini.trasloco.utils.CardStackIndicatorDrawable
```

Aggiungi il metodo privato subito dopo `setNumberOfCards()` (riga ~668):

```kotlin
    /**
     * Adds or removes the stack-depth indicator overlay on [view].
     * Uses ViewOverlay so it does not conflict with existing foreground/background.
     */
    private fun setStackIndicator(view: View, show: Boolean) {
        view.overlay.clear()
        if (!show) return
        fun addOverlay() {
            if (view.width > 0 && view.height > 0) {
                val d = CardStackIndicatorDrawable(this)
                d.setBounds(0, 0, view.width, view.height)
                view.overlay.add(d)
            }
        }
        if (view.isLaidOut && view.width > 0) {
            addOverlay()
        } else {
            view.doOnLayout { addOverlay() }
        }
    }
```

- [ ] **Step 2: Verifica che compili**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/GameActivity.kt
git commit -m "feat(GameActivity): add setStackIndicator() helper via ViewOverlay"
```

---

### Task 4: Indicatore scoperte — hook in `setNumberOfCards()`

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/GameActivity.kt` riga ~659

`setNumberOfCards()` è già il punto centrale per aggiornare i badge delle carte scoperte. Vi aggiungiamo la chiamata all'indicatore.

- [ ] **Step 1: Aggiungi `setStackIndicator` dentro `setNumberOfCards()`**

Sostituisci il metodo esistente (riga 659–668):

```kotlin
    private fun setNumberOfCards(cardsList: List<String>, position: String) {
        val textView = findViewById<TextView>(getTextViewByName(position))
        if (cardsList.size > 1) {
            textView.text = cardsList.size.toString()
            textView.visibility = View.VISIBLE
        } else {
            textView.text = ""
            textView.visibility = View.INVISIBLE
        }
        // stack indicator: show when > 1 card in slot
        val cardViewId = resources.getIdentifier("subDeck$position", "id", packageName)
        if (cardViewId != 0) {
            setStackIndicator(findViewById(cardViewId), cardsList.size > 1)
        }
    }
```

- [ ] **Step 2: Verifica che compili**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/GameActivity.kt
git commit -m "feat(GameActivity): wire stack indicator to setNumberOfCards for scoperte"
```

---

### Task 5: Indicatore + badge per le coperte (`updateSourceDeck`)

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/GameActivity.kt`

`updateSourceDeck(line)` aggiorna sia il badge numerico del mazzo sorgente sia il suo indicatore grafico. Viene chiamato da `dealCard()` ogni volta che `subDeckMap[line]` cambia.

- [ ] **Step 1: Aggiungi il metodo `updateSourceDeck()`**

Subito dopo `setStackIndicator()`:

```kotlin
    /**
     * Updates the source-deck badge count and stack indicator for [line] ("1"–"4").
     * Call whenever subDeckMap[line] changes.
     */
    private fun updateSourceDeck(line: String) {
        val size = subDeckMap[line]?.size ?: 0
        // badge
        val badgeId = resources.getIdentifier("textViewDeck$line", "id", packageName)
        val badge = findViewById<TextView>(badgeId)
        if (size > 0) {
            badge.text = size.toString()
            badge.visibility = View.VISIBLE
        } else {
            badge.text = ""
            badge.visibility = View.INVISIBLE
        }
        // indicator: show when 2+ cards remain
        val deckId = resources.getIdentifier("subDeck$line", "id", packageName)
        setStackIndicator(findViewById(deckId), size > 1)
    }
```

- [ ] **Step 2: Chiama `updateSourceDeck(line)` in `dealCard()` dopo `subDeckMap[line] = subDeck`**

Trova la riga `subDeckMap[line] = subDeck` (riga ~815) e aggiungi subito dopo:

```kotlin
        subDeckMap[line] = subDeck
        updateSourceDeck(line)   // ← aggiungi questa riga
```

- [ ] **Step 3: Verifica che compili**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/GameActivity.kt
git commit -m "feat(GameActivity): add updateSourceDeck() for coperte badge + indicator"
```

---

### Task 6: Indicatore end deck (`updateEndDeckIndicator`)

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/GameActivity.kt`

L'indicatore appare sull'end deck quando il rank della carta accumulata è > 1 (es. "b3" → rank 3 → ci sono b1 e b2 sotto).

- [ ] **Step 1: Aggiungi il metodo `updateEndDeckIndicator()`**

Subito dopo `updateSourceDeck()`:

```kotlin
    /**
     * Updates the end-deck stack indicator for [line] ("1"–"4").
     * Shows indicator when rank of accumulated card > 1 (i.e. cards are below it).
     * Call whenever endDeckList[line] changes.
     */
    private fun updateEndDeckIndicator(line: String) {
        val card = endDeckList[line] ?: "zero"
        val rank = if (card == "zero") 0 else card.substring(1).toIntOrNull() ?: 0
        val endDeckId = resources.getIdentifier("subDeck${line}4", "id", packageName)
        if (endDeckId != 0) {
            setStackIndicator(findViewById(endDeckId), rank > 1)
        }
    }
```

- [ ] **Step 2: Aggiungi la chiamata dopo `endDeckList[line.toString()] = sourceCard` (riga ~505)**

In `handleCardClick()` (o nella funzione che contiene quel blocco), trova:
```kotlin
endDeckList[line.toString()] = sourceCard
```
e aggiungi subito dopo:
```kotlin
updateEndDeckIndicator(line.toString())
```

- [ ] **Step 3: Aggiungi la chiamata in `forceCardsEndDeck()` dopo `endDeckList[line] = endDeckCard` (riga ~559)**

Trova:
```kotlin
endDeckList[line] = endDeckCard
```
e aggiungi subito dopo:
```kotlin
updateEndDeckIndicator(line)
```

- [ ] **Step 4: Verifica che compili**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/GameActivity.kt
git commit -m "feat(GameActivity): add updateEndDeckIndicator() for end deck stack visual"
```

---

### Task 7: `clearAllStackIndicators()` + hook in `zeroFill()`

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/GameActivity.kt`

`zeroFill()` resetta visivamente il tavolo all'inizio di ogni partita. Va azzerato anche lo stato degli indicatori e dei badge sorgente.

- [ ] **Step 1: Aggiungi `clearAllStackIndicators()`**

```kotlin
    /** Removes stack indicator overlays from all 20 card slots and source decks. */
    private fun clearAllStackIndicators() {
        for (row in 1..4) {
            // table slots (col 1–4)
            for (col in 1..4) {
                val id = resources.getIdentifier("subDeck$row$col", "id", packageName)
                if (id != 0) findViewById<View>(id)?.overlay?.clear()
            }
            // source decks
            val deckId = resources.getIdentifier("subDeck$row", "id", packageName)
            if (deckId != 0) findViewById<View>(deckId)?.overlay?.clear()
        }
    }
```

- [ ] **Step 2: Azzera i badge sorgente in `zeroFill()`**

In `zeroFill()` (riga ~1052), all'inizio del metodo aggiungi:

```kotlin
    private fun zeroFill() {
        clearAllStackIndicators()   // ← aggiungi come prima riga
        // reset source deck badges
        for (line in listOf("1", "2", "3", "4")) {
            val badgeId = resources.getIdentifier("textViewDeck$line", "id", packageName)
            val badge = findViewById<TextView>(badgeId)
            badge.text = ""
            badge.visibility = View.INVISIBLE
        }
        // ... resto del metodo invariato ...
```

- [ ] **Step 3: Verifica che compili**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/GameActivity.kt
git commit -m "feat(GameActivity): clear indicators and source badges in zeroFill"
```

---

### Task 8: `refreshAllStackIndicators()` + hook su tutti i path di init/restore

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/GameActivity.kt`

Dopo ogni operazione che (ri)costruisce il tavolo — nuova partita, retry, tutorial, restore — tutti gli indicatori devono essere sincronizzati con lo stato corrente.

- [ ] **Step 1: Aggiungi `refreshAllStackIndicators()`**

```kotlin
    /**
     * Syncs all stack indicators and source-deck badges with current game state.
     * Call after any full board rebuild: new game, retry, restore, tutorial start.
     */
    private fun refreshAllStackIndicators() {
        // scoperte (columns 1–3, all 4 rows)
        for (row in 1..4) {
            for (col in 1..3) {
                val pos = "$row$col"
                val cards = cardTableMap[pos] ?: emptyList()
                setNumberOfCards(cards, pos)
            }
        }
        // source decks
        for (line in listOf("1", "2", "3", "4")) {
            updateSourceDeck(line)
        }
        // end decks
        for (line in listOf("1", "2", "3", "4")) {
            updateEndDeckIndicator(line)
        }
    }
```

- [ ] **Step 2: Aggiungi `refreshAllStackIndicators()` nell'`onComplete` di `startNewGame()`**

Trova (riga ~221):
```kotlin
onComplete = {
    isIntroAnimating = false
    isInitializing   = false
    gameRoot.setOnClickListener(null)
    startTimer()
}
```
Diventa:
```kotlin
onComplete = {
    isIntroAnimating = false
    isInitializing   = false
    gameRoot.setOnClickListener(null)
    startTimer()
    refreshAllStackIndicators()
}
```

- [ ] **Step 3: Aggiungi nell'`onComplete` di `retryGame()`**

Trova (riga ~369):
```kotlin
onComplete = {
    isIntroAnimating = false
    isInitializing   = false
    gameRoot.setOnClickListener(null)
    startTimer()
}
```
Diventa:
```kotlin
onComplete = {
    isIntroAnimating = false
    isInitializing   = false
    gameRoot.setOnClickListener(null)
    startTimer()
    refreshAllStackIndicators()
}
```

- [ ] **Step 4: Aggiungi in `restoreGameFromViewModel()` dopo i loop di ripristino**

Trova (riga ~1435–1438):
```kotlin
        for ((line, card) in endDeckList) {
            val endDeckId = resources.getIdentifier("subDeck${line}4", "id", this.packageName)
            setImage(endDeckId, card)
        }
```
Aggiungi subito dopo:
```kotlin
        refreshAllStackIndicators()
```

- [ ] **Step 5: Aggiungi in `startTutorial()` dopo `prepareTable()`**

Trova (riga ~247):
```kotlin
        prepareTable()
```
Aggiungi subito dopo:
```kotlin
        refreshAllStackIndicators()
```

- [ ] **Step 6: Verifica che compili e linka**

```bash
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL, APK generato.

- [ ] **Step 7: Commit finale**

```bash
git add app/src/main/java/com/bottazzini/trasloco/GameActivity.kt
git commit -m "feat(GameActivity): add refreshAllStackIndicators(), wire to all init/restore paths"
```

---

## Verifica manuale

Dopo il build, installa e testa questi scenari:

1. **Nuova partita**: i mazzi sorgente mostrano il badge col numero di carte e le linee. Le scoperte con 2+ carte mostrano le linee.
2. **Deal carta**: il badge del mazzo sorgente si decrementa. Quando arriva a 1, le linee spariscono (1 sola carta, nessuna sotto). Quando arriva a 0, badge sparisce.
3. **Muovi carta su end deck**: le linee appaiono sull'end deck quando rank ≥ 2. A rank 1 (asso appena piazzato) nessuna linea.
4. **Muovi carta su scoperta**: le linee appaiono sulla scoperta di destinazione quando la pila supera 1 carta; spariscono quando si svuota a 1 o 0.
5. **Retry**: tutto si azzera e si ricalcola correttamente.
6. **Resume** (ripristino da DB): indicatori e badge sincronizzati con lo stato salvato.
7. **Tutorial**: indicatori presenti correttamente.
