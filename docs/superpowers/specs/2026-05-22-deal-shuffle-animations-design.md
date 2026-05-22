# Design Spec: Deal & Shuffle Intro Animations

**Date:** 2026-05-22  
**Status:** Approved  

---

## Overview

Due nuove animazioni per l'inizio partita:

1. **Deal cascade** — le 12 carte iniziali volano una per una dai 4 talloni verso i 3 slot tavolo di ogni riga, in ordine a cascata verticale.
2. **Shuffle intro** — all'inizio di una New Game, un mazzo ghost appare al centro, si mescola visivamente, poi si divide in 4 ghost deck che volano verso le 4 posizioni tallone, dopodiché parte il deal cascade.

---

## Scopo

Rendere l'avvio della partita più visivamente coinvolgente, comunicando chiaramente che le carte sono state mescolate e distribuite.

---

## Architettura

### Nuovo componente: `DealAnimator`

**File:** `app/src/main/java/com/bottazzini/trasloco/utils/DealAnimator.kt`

Singleton `object` stateless (stesso pattern di `CardAnimator`). Espone tre metodi pubblici:

```kotlin
object DealAnimator {
    /** New Game: shuffle visivo + deal cascade */
    fun playNewGame(
        root: ViewGroup,
        talloneViews: List<ImageView>,        // subDeck1..4
        slotViews: Map<String, ImageView>,    // "11"→view, "12"→view, …, "43"→view
        drawables: Map<String, Drawable?>,    // "11"→drawable della carta, …
        backDrawable: Drawable?,              // retro carta per ghost deck
        handler: Handler,
        onComplete: () -> Unit
    )

    /** Retry: solo deal cascade (no shuffle visivo), stagger più veloce */
    fun playRetry(
        root: ViewGroup,
        talloneViews: List<ImageView>,
        slotViews: Map<String, ImageView>,
        drawables: Map<String, Drawable?>,
        handler: Handler,
        onComplete: () -> Unit
    )

    /** Cancella tutto immediatamente e chiama onComplete */
    fun skip()
}
```

Internamente mantiene:
- `val pendingRunnables: MutableList<Runnable>` — per cancel su skip
- `val ghostViews: MutableList<View>` — ghost ImageView da rimuovere su skip
- `var skipCallback: (() -> Unit)?` — callback da chiamare su skip
- `var rootRef: WeakReference<ViewGroup>?` — per rimozione ghost su skip

---

## Sequenza animazione — New Game (~2.5s totali)

### Fase 1 — Shuffle visivo (~600ms)

- Compare un ghost `ImageView` (retro carta, stessa dimensione di un tallone) al **centro** di `gameRoot`
- Animazione `ValueAnimator`: `scaleX` oscilla `1f → 0.85f → 1.05f → 1f` + traslazione X ±8dp (effetto riffle)
- Durata: 400ms
- Al termine della fase 1: nasconde il `loadingOverlay` (il tavolo diventa visibile, i talloni mostrano già il retro reale)

### Fase 2 — Divisione in 4 (~400ms)

- 4 ghost deck (retri carta, dimensione = tallone) partono dalla posizione del ghost centrale e volano verso ciascun tallone usando `CardAnimator.animateCardFlight`
- Stagger: riga1 a 0ms, riga2 a 60ms, riga3 a 120ms, riga4 a 180ms
- Durata volo per ghost: 220ms
- Al termine: il ghost centrale e i 4 ghost deck vengono rimossi (i talloni reali sono già visibili)

### Fase 3 — Deal cascade (~1.5s)

12 carte (3 slot × 4 righe) in ordine a cascata verticale:

| Tempo  | Carta         |
|--------|---------------|
| 0ms    | riga1 → slot1 |
| 150ms  | riga2 → slot1 |
| 300ms  | riga3 → slot1 |
| 450ms  | riga4 → slot1 |
| 700ms  | riga1 → slot2 |
| 850ms  | riga2 → slot2 |
| 1000ms | riga3 → slot2 |
| 1150ms | riga4 → slot2 |
| 1400ms | riga1 → slot3 |
| 1550ms | riga2 → slot3 |
| 1700ms | riga3 → slot3 |
| 1850ms | riga4 → slot3 |

- Ogni volo: 200ms, usa `CardAnimator.animateCardFlight`
- Callback `onComplete` al termine dell'ultima carta (t ≈ 2050ms dal deal start, ≈ 2450ms dal new game start)
- Slot senza carta (drawable null o "zero"): saltati silenziosamente

---

## Sequenza animazione — Retry (solo deal cascade)

- Identica alla Fase 3 sopra, stagger ridotto a **100ms** (invece di 150ms) per sensazione più snella
- Nessuna fase shuffle/divisione
- Total: ~1.3s

---

## Skip

- Un tap su `gameRoot` durante l'animazione chiama `DealAnimator.skip()`
- `skip()`:
  1. Cancella tutti i `Runnable` pendenti via `handler.removeCallbacks`
  2. Rimuove tutti i ghost `View` da root
  3. Chiama `onComplete()` (piazza le carte istantaneamente e setta `isInitializing = false`)
- Implementato come `OnClickListener` temporaneo su `gameRoot`, rimosso nel callback `onComplete`

---

## Integrazione con `GameActivity`

### `startNewGame()`

```
1. prePrepareTable() — azzera board visivamente
2. loadingOverlay → VISIBLE
3. Background thread: DeckSetup.shuffleSolvable()
4. UI thread:
   a. DeckSetup.prepareSubDecks()
   b. prepareTable() con isInitializing=true
      → stato aggiornato (cardTableMap, tag), immagini NON mostrate ancora
   c. Costruisce talloneViews, slotViews, drawables, backDrawable
   d. Registra skip listener su gameRoot
   e. DealAnimator.playNewGame(..., onComplete = {
        loadingOverlay → GONE
        isInitializing = false
        rimuovi skip listener
        startTimer()
      })
```

### `retryGame()`

```
1. prePrepareTable()
2. subDeckMap = coppiedSubDeckMap
3. prepareTable() con isInitializing=true
4. Costruisce slotViews, drawables
5. Registra skip listener
6. DealAnimator.playRetry(..., onComplete = {
     isInitializing = false
     rimuovi skip listener
     startTimer()
   })
```

### `onDestroy()`

- Chiama `DealAnimator.skip()` per cancellare ghost e Runnable pendenti
- Aggiunto accanto alla pulizia di `dealRunnables` già esistente

---

## Audio

| Momento             | Suono                    |
|---------------------|--------------------------|
| Inizio shuffle      | `R.raw.shuffle` (già chiamato in `startNewGame()`) |
| Ogni carta deal     | `playSoundAtomic(R.raw.flipcard)` nel callback di ogni ghost |

---

## Casi esclusi

| Caso                 | Comportamento                        |
|----------------------|--------------------------------------|
| Tutorial             | `startTutorial()` invariato, nessuna animazione intro |
| Rotation durante anim| `onDestroy` → `skip()` → nuova Activity mostra tavolo direttamente |
| Resume partita       | `restoreGameFromViewModel()` invariato, nessuna animazione intro |
| Carte mancanti (zero)| Ghost saltato silenziosamente per quello slot |

---

## File modificati

| File | Tipo modifica |
|------|--------------|
| `utils/DealAnimator.kt` | **Nuovo** |
| `GameActivity.kt` | Modifica `startNewGame()`, `retryGame()`, `onDestroy()` |

---

## Criteri di successo

- New Game: si vede sempre il shuffle visivo + deal cascade prima che il tavolo sia interagibile
- Retry: si vede il deal cascade, nessun shuffle visivo
- Skip funziona in qualsiasi fase: il tavolo appare istantaneamente in stato corretto
- Nessun ghost residuo sul root dopo lo skip o il completamento
- `isInitializing = false` viene settato solo dopo l'ultimo callback, prevenendo interazioni premature
