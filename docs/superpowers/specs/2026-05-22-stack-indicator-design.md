# Stack Indicator — Design Spec
_Date: 2026-05-22_

## Obiettivo

Aggiungere un indicatore visivo sottile su ogni carta che ha carte sotto di sé, per aiutare il giocatore a capire la profondità dello stack a colpo d'occhio. Il badge numerico esistente rimane invariato.

---

## Comportamento

L'indicatore è **binario**: visibile se ci sono carte sotto, invisibile altrimenti. Non scala con il numero di carte (per quello c'è già il badge).

| Tipo carta | ImageView IDs | Condizione per mostrare l'indicatore |
|---|---|---|
| **Scoperta** (colonne 1–3) | `subDeck11`…`subDeck43` (12 slot) | `cardTableMap[pos].size > 1` |
| **Coperta** (mazzo sorgente) | `subDeck1`…`subDeck4` (4 slot) | `subDeckMap[line].size > 1` |
| **EndDeck** (colonna 4) | `subDeck14`…`subDeck44` (4 slot) | rank della carta accumulata > 1 |

Per l'endDeck il rank viene estratto dal nome carta: es. `"b3".substring(1) = "3"` → rank 3 → 2 carte sotto → indicatore ON. Se il valore è `"zero"` o rank `1`, indicatore OFF.

---

## Visual Design

**Stile scelto: linee orizzontali sottili al bordo inferiore** (Opzione A dal mockup).

Due linee bianche semitrasparenti sovrapposte alla carta:

| Linea | Spessore | Distanza dal bordo inf. | Opacità | Margine lat. |
|---|---|---|---|---|
| 1 (più visibile) | 2 dp | 7 dp | ~30 % (alpha 76/255) | 4 dp |
| 2 (più sottile) | 1.5 dp | 3 dp | ~18 % (alpha 46/255) | 4 dp |

Tutte le misure sono in `dp`, convertite in pixel a runtime tramite `context.resources.displayMetrics.density`.

---

## Architettura

### Nuovo file: `CardStackIndicatorDrawable.kt`

```
app/src/main/java/com/bottazzini/trasloco/utils/CardStackIndicatorDrawable.kt
```

Classe che estende `Drawable`. Disegna le due linee nel metodo `draw(canvas)` usando i bounds assegnati. Non ha stato mutabile — è stateless oltre ai valori di inizializzazione calcolati dal density.

```kotlin
class CardStackIndicatorDrawable(context: Context) : Drawable() {
    // line1: 2dp thick, 7dp from bottom
    // line2: 1.5dp thick, 3dp from bottom
    // margins: 4dp left/right
    override fun draw(canvas: Canvas) { ... }
    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(cf: ColorFilter?) {}
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}
```

### Metodo helper in `GameActivity`

```kotlin
private fun setStackIndicator(view: View, show: Boolean) {
    view.overlay.clear()
    if (!show) return
    fun add() {
        val d = CardStackIndicatorDrawable(this)
        d.setBounds(0, 0, view.width, view.height)
        view.overlay.add(d)
    }
    if (view.isLaidOut && view.width > 0) add() else view.doOnLayout { add() }
}
```

**Perché `ViewOverlay`:** non tocca né `background` né `foreground` esistenti sulla View. I mazzi sorgente (`subDeck1`–`subDeck4`) hanno già `android:foreground="@drawable/subdeck_background_selector"` per il press-state — usare overlay evita qualsiasi conflitto.

---

## Punti di integrazione in `GameActivity`

### 1. Scoperte — dentro `setNumberOfCards()` (già esistente)

```kotlin
private fun setNumberOfCards(cardsList: List<String>, position: String) {
    // ... badge logic invariata ...
    val view = findCardViewByPosition(position)
    setStackIndicator(view, cardsList.size > 1)
}
```

`findCardViewByPosition(position: String)` è un helper da aggiungere che mappa la stringa di posizione (es. `"11"`, `"23"`) all'ImageView corrispondente tramite `resources.getIdentifier("subDeck$position", "id", packageName)`.

### 2. Coperte — nuovo metodo `updateSourceDeckIndicator(line: String)`

```kotlin
private fun updateSourceDeckIndicator(line: String) {
    val view = findViewById<View>(
        resources.getIdentifier("subDeck$line", "id", packageName)
    )
    setStackIndicator(view, (subDeckMap[line]?.size ?: 0) > 1)
}
```

Chiamato ogni volta che `subDeckMap[line]` viene modificato (dopo ogni deal di carta dal mazzo sorgente).

### 3. EndDeck — nuovo metodo `updateEndDeckIndicator(line: String)`

```kotlin
private fun updateEndDeckIndicator(line: String) {
    val posId = "${line}4"
    val view = findViewById<View>(
        resources.getIdentifier("subDeck$posId", "id", packageName)
    )
    val card = endDeckList[line] ?: "zero"
    val rank = if (card == "zero") 0 else card.substring(1).toIntOrNull() ?: 0
    setStackIndicator(view, rank > 1)
}
```

Chiamato ogni volta che `endDeckList[line]` viene aggiornato.

### 4. Inizializzazione completa

Dopo ogni `prepareTable()` (nuova partita, retry, ripristino stato) vanno aggiornati tutti gli indicatori. Un metodo `refreshAllStackIndicators()` itera su tutte le 20 posizioni e chiama i metodi sopra.

---

## Nessuna modifica al layout XML

Tutto il codice è Kotlin. Zero nuove View nel layout. Il badge numerico (`textView11`…`textView43`) non viene toccato.

---

## File modificati / creati

| File | Tipo modifica |
|---|---|
| `utils/CardStackIndicatorDrawable.kt` | **Nuovo** |
| `GameActivity.kt` | **Modificato**: aggiunto `setStackIndicator()`, `updateSourceDeckIndicator()`, `updateEndDeckIndicator()`, `refreshAllStackIndicators()`, hook in `setNumberOfCards()` e negli altri punti di aggiornamento stato |

---

## Out of scope

- Animazione dell'indicatore (fade in/out) — non richiesta
- Indicatore scalabile per profondità — il badge numerico già lo gestisce
- Modifica al `SettingsActivity` — nessuna nuova preferenza
