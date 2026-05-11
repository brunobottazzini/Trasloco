# Tutorial guidato — Design

**Date:** 2026-05-11
**Status:** Draft (awaiting user review)
**Target version:** post-2.1.0

## Goal

Sostituire la pagina statica delle "Regole" con un **tutorial interattivo giocato**: l'utente esegue 3 mosse reali su un deck deterministico, accompagnato da istruzioni passo-passo in un banner inferiore. Il tutorial è proposto una sola volta al primo "Nuova partita" su installazione pulita, ed è sempre rilanciabile dal link del menu.

## Entry points

### Primo tap di "Nuova partita" su installazione pulita

`MainActivity` legge il flag SharedPreferences `tutorial_seen` (default `false`).

- Se `tutorial_seen == false` → al tap su tile "Nuova partita" si apre un **AlertDialog stile casino** con:
  - Titolo: "Prima volta?"
  - Testo: "Vuoi una breve guida di 3 mosse o vai dritto al gioco?"
  - Bottone primario "🎓 Tutorial" → setta `tutorial_seen = true`, lancia `TutorialActivity`
  - Bottone secondario "▶ Gioca" → setta `tutorial_seen = true`, lancia `GameActivity`
- Se `tutorial_seen == true` → comportamento attuale (lancia `GameActivity` diretto)

Il flag passa a `true` qualunque sia la scelta: il prompt non viene mai mostrato una seconda volta.

### Link "📜 Tutorial" nel menu

Il link nell'angolo in alto a destra di `MainActivity` cambia label da `📜 Regole` a `📜 Tutorial` (string `rules_corner_label`).

Il tap lancia sempre `TutorialActivity`, indipendentemente da `tutorial_seen`. Permette di rivedere il tutorial in qualsiasi momento.

### Cleanup

`RulesActivity` viene rimossa insieme al suo layout (`activity_rules.xml`), al manifest entry, alla string-array `game_rules_array`, e alle stringhe `title_activity_rules`, `game_rules_title`, `got_it_button`.

## `TutorialActivity`

`TutorialActivity` riusa il layout `game.xml` (stesso tavolo, stesse view) per non duplicare l'UI. Aggiunge:

- Un nuovo overlay `tutorialBanner` nel game.xml
- Un `TutorialEngine` che orchestra gli step
- Filtraggio degli input nei card click/drag handler di `GameActivity` quando in tutorial mode

`GameActivity` viene esteso (non duplicato) con un flag `isTutorialMode` letto dall'intent extra `EXTRA_TUTORIAL_MODE`. Quando attivo:

- `shuffleSolvable()` è sostituito da `DeckSetup.setTutorialDeck()`
- `tutorialBanner` viene mostrato e popolato dal `TutorialEngine`
- Top bar (back/timer/pause/hint) nascosta
- Sub-deck click disabilitato
- Auto-move disabilitato
- I click/drag delle carte chiamano `tutorialEngine.isMoveAllowed(card, target)`; se `false`, il gesto viene ignorato silenziosamente
- `recordsHandler` non viene mai aggiornato, `gameStartTimeMillis` non parte
- Il timer view è nascosto (parte della top bar nascosta)
- Stato di gioco non viene persistito (`gameStateRepo` non chiamato)

### Banner inferiore (`tutorialBanner`)

Nuovo `LinearLayout` overlay aggiunto a `layout/game.xml` e `layout-land/game.xml`:

- Posizione: full-width, ancorato in basso sopra i sub-deck
- Sfondo: `@drawable/casino_pause_overlay_bg` (coerente con `pauseOverlay`, `lostOverlay`)
- `visibility="gone"` di default, mostrato solo in tutorial mode
- Contenuto orizzontale:
  - `TextView` istruzione: serif italic, casino_gold, ~14sp, `layout_weight=1`
  - `Button "Avanti →"` (id `tutorialNextButton`): casino_tile_bg_primary, inizialmente `gone`, diventa `visible` quando lo step corrente è completo (mossa eseguita o intro/outro)
  - `ImageButton "✕"` (id `tutorialExitButton`): in alto a destra del banner, sempre visibile, apre il dialog "Esci dal tutorial?"

### Highlight

Riutilizzo del drawable esistente `@drawable/hint_pulse_highlight` (già usato dal `HintEngine`). Il `TutorialEngine` applica il background pulse-highlight alle 1-2 view target dello step corrente, e lo rimuove quando si passa allo step successivo.

### `TutorialEngine`

Nuova classe in `app/src/main/java/com/bottazzini/trasloco/utils/TutorialEngine.kt`:

```kotlin
data class TutorialStep(
    val instructionResId: Int,
    val highlightTargets: List<String>,  // card tags or slot IDs to pulse
    val requiredMove: TutorialMove?,      // null = no move (intro/outro)
    val confirmationResId: Int?           // shown after move completes; null on intro/outro
)

data class TutorialMove(
    val sourceCard: String,    // e.g. "c5"
    val targetSlotOrCard: String  // e.g. "c6" or "endDeck1"
)

class TutorialEngine(steps: List<TutorialStep>) {
    fun currentStep(): TutorialStep
    fun isMoveAllowed(source: String, target: String): Boolean
    fun onMoveExecuted(source: String, target: String)  // advances if matches required
    fun advanceToNext()  // called by "Avanti →" tap
    fun isComplete(): Boolean
}
```

## Script (5 step)

I testi qui sono in italiano; per la versione finale si useranno string resources in `values/`, `values-it/`, `values-pt/`.

**Step 0 — Intro**
> Banner: *"Benvenuto al Trasloco. Obiettivo: portare tutte le carte sui 4 mazzetti finali in alto, dall'Asso al Re, divise per seme. Tocca Avanti."*
- No mossa richiesta. "Avanti →" visibile subito.

**Step 1 — Mossa colonna→colonna**
- Highlight: `c5` (top di una colonna), `c6` (top di un'altra colonna)
- Banner: *"Tocca il 5 di Coppe, poi tocca il 6 di Coppe per impilarlo sopra. Sulle colonne le carte vanno in ordine decrescente, stesso seme."*
- Required move: source=`c5` → target=`c6`
- Confirmation: *"✓ Bene. Decrescente, stesso seme."*

**Step 2 — Asso sulla foundation**
- Highlight: `d1` (top di una colonna), un endDeck slot vuoto
- Banner: *"Porta l'Asso di Denari su un mazzetto finale vuoto in alto. Ogni mazzetto finale parte da un Asso."*
- Required move: source=`d1` → target=endDeck vuoto
- Confirmation: *"✓ L'Asso è al posto giusto."*

**Step 3 — 2 sulla foundation**
- Highlight: `d2` (top di una colonna), l'endDeck con l'Asso appena messo
- Banner: *"Tocca il 2 di Denari e portalo sul mazzetto finale dell'Asso. I mazzetti crescono Asso → 2 → ... → Re, stesso seme."*
- Required move: source=`d2` → target=endDeck dell'Asso
- Confirmation: *"✓ Perfetto."*

**Step 4 — Outro**
> Banner: *"🎉 Hai imparato il nucleo. Quando giochi davvero, usa il suggerimento (💡) in alto se ti blocchi. Buona partita."*
- "Avanti →" diventa "Fine"; tap → `finish()` → torna a `MainActivity`.

## Deck deterministico

Nuovo metodo `DeckSetup.setTutorialDeck()` che imposta `randomDeck` con un ordine hard-coded.

**Vincoli funzionali** (l'ordine concreto è scelto in fase implementativa):

- Dopo il deal iniziale tramite `prepareTable()`, queste 4 carte devono essere top di 4 colonne distinte:
  - `c5` (5 Coppe) — su una colonna
  - `c6` (6 Coppe) — su un'altra colonna
  - `d1` (Asso Denari) — su una terza colonna
  - `d2` (2 Denari) — su una quarta colonna
- Sotto a `c5`, dopo che il 5 viene spostato, può comparire qualsiasi carta (non rilevante per gli step 2/3)
- Tutti gli endDeck devono partire vuoti (è già il comportamento di default)

## Uscita dal tutorial

Il bottone `tutorialExitButton` (✕ nel banner) e il tasto fisico Back aprono un `AlertDialog`:

- Titolo: "Esci dal tutorial?"
- Bottoni: "Sì, esci" → `finish()` → `MainActivity` / "Continua" → dismiss

A fine tutorial (step 4 "Fine"), nessuna conferma: `finish()` diretto.

## Stringhe nuove

Da aggiungere in `values/strings.xml`, `values-it/strings.xml`, `values-pt/strings.xml`:

```
tutorial_corner_label       "🎓 Tutorial"  (sostituisce rules_corner_label)
tutorial_prompt_title       "Prima volta?"
tutorial_prompt_message     "Vuoi una breve guida di 3 mosse o vai dritto al gioco?"
tutorial_prompt_yes         "🎓 Tutorial"
tutorial_prompt_no          "▶ Gioca"
tutorial_next               "Avanti →"
tutorial_finish             "Fine"
tutorial_exit_title         "Esci dal tutorial?"
tutorial_exit_confirm       "Sì, esci"
tutorial_exit_cancel        "Continua"
tutorial_step_intro         "Benvenuto al Trasloco. ..."
tutorial_step1_instruction  "Tocca il 5 di Coppe, ..."
tutorial_step1_confirm      "✓ Bene. Decrescente, stesso seme."
tutorial_step2_instruction  "Porta l'Asso di Denari ..."
tutorial_step2_confirm      "✓ L'Asso è al posto giusto."
tutorial_step3_instruction  "Tocca il 2 di Denari ..."
tutorial_step3_confirm      "✓ Perfetto."
tutorial_step_outro         "🎉 Hai imparato il nucleo. ..."
```

## File toccati

**Nuovi:**
- `app/src/main/java/com/bottazzini/trasloco/TutorialActivity.kt`
- `app/src/main/java/com/bottazzini/trasloco/utils/TutorialEngine.kt`

Nessun nuovo drawable: l'highlight riusa `@drawable/hint_pulse_highlight`; il banner riusa `@drawable/casino_pause_overlay_bg`; i bottoni del prompt riusano `casino_tile_bg_primary` / `casino_tile_bg`.

**Modificati:**
- `app/src/main/AndroidManifest.xml` — aggiungere `TutorialActivity`, rimuovere `RulesActivity`
- `app/src/main/java/com/bottazzini/trasloco/MainActivity.kt` — gestire flag `tutorial_seen`, prompt dialog, link "Tutorial"
- `app/src/main/java/com/bottazzini/trasloco/GameActivity.kt` — flag `isTutorialMode`, integrazione `TutorialEngine`
- `app/src/main/java/com/bottazzini/trasloco/utils/DeckSetup.kt` — aggiungere `setTutorialDeck()`
- `app/src/main/res/layout/game.xml`, `layout-land/game.xml` — aggiungere `tutorialBanner`
- `app/src/main/res/layout/activity_main.xml` — label link cambia da Regole a Tutorial
- `app/src/main/res/values/strings.xml`, `values-it/strings.xml`, `values-pt/strings.xml` — stringhe nuove, rimozione `game_rules_array`

**Eliminati:**
- `app/src/main/java/com/bottazzini/trasloco/RulesActivity.kt`
- `app/src/main/res/layout/activity_rules.xml`

## Non-goals

- Tutorial avanzato con sub-deck, sequenze multiple, mosse non valide (esplicitamente fuori scope; opzione B della discussione iniziale)
- Animazioni guidate (le carte non si muovono da sole; il giocatore esegue il drag/tap)
- Localizzazione di lingue oltre IT/EN/PT
- Telemetria (quante persone scelgono tutorial vs gioca, quanti lo completano)

## Open questions / risk

- **Robustezza dello strict mode**: se durante un drag il giocatore rilascia su un target non-valido, il sistema deve fallire silenziosamente senza confondere lo stato del deck/UI. Va verificato in fase di test.
- **Sblocco "Avanti →"**: deve apparire solo quando la mossa è effettivamente confermata (carta arrivata a destinazione), non al solo tap di selezione.
- **Riavvio durante tutorial**: se il sistema chiude l'app a metà tutorial, al rilancio si riparte da capo (no resume del tutorial). Accettabile data la durata ~1 min.
