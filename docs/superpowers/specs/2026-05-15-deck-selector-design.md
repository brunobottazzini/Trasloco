# Deck Selector — Design Spec
**Data:** 2026-05-15  
**Scope:** Sostituzione del selettore mazzo (ViewPager2 carousel) con una griglia a tab geografici, in `DeckPickerActivity` e `SettingsActivity`.

---

## Problema

Con 15 mazzi regionali il carousel ViewPager2 è scomodo: bisogna scorrere uno a uno senza avere visione d'insieme. In più, con font di sistema grandi (impostazione comune tra utenti anziani) il layout attuale si distorce perché i nomi dei mazzi sono dentro tile piccoli.

---

## Soluzione

Griglia a 3 colonne con tab geografici. I tile mostrano **solo le anteprime delle carte** (nessun testo dentro, dimensioni in `dp`). Il nome del mazzo selezionato è mostrato in un label dedicato sopra la griglia (testo in `sp`), che scala correttamente con qualsiasi font di sistema.

---

## Raggruppamenti geografici

| Tab | Mazzi |
|---|---|
| **Nord** (11) | Piacentine, Bergamasche, Bolognesi, Bresciane, Genovesi, Milanesi, Piemontesi, Romagnole, Trentine, Trevisane, Triestine |
| **Sud & Isole** (3) | Napoletane, Sarde, Siciliane |
| **Internazionali** (1) | Francesi |

---

## DeckPickerActivity (schermo intero)

### Struttura layout (`activity_deck_picker.xml`)
1. Titolo + sottotitolo (invariati)
2. `TabLayout` — 3 tab: Nord / Sud & Isole / Internazionali
3. `TextView` "Mazzo selezionato: [nome]" — testo in `sp`, aggiornato dinamicamente
4. `RecyclerView` con `GridLayoutManager(3)` — tile senza testo
5. `Button` "Gioca con le [nome] →" — sempre visibile in fondo

### Comportamento
- **Apertura**: tab Nord attivo, Piacentine preselezionate (bordo dorato), bottone abilitato
- **Tap su tile**: aggiorna la selezione (bordo dorato), aggiorna label nome e testo bottone
- **Cambio tab**: la selezione corrente rimane; se il mazzo selezionato è in un altro tab, label e bottone mantengono il nome corretto
- **Bottone**: abilitato solo se un mazzo è selezionato; salva la scelta, imposta `deck_chosen = true`, avvia `MainActivity`
- Mazzi `available = false`: tile opaco al 40%, non cliccabile (invariato rispetto a oggi)

---

## SettingsActivity (sezione embedded)

### Struttura nella sezione mazzo (`settings.xml`)
1. Label sezione "Mazzo di carte" (invariata)
2. `TextView` "Mazzo selezionato: [nome]" — testo in `sp`
3. `TabLayout` — stessi 3 tab
4. `RecyclerView` con `GridLayoutManager(3)` — tile senza testo
5. **Nessun bottone di conferma**

### Comportamento
- **Apertura**: tab attivo = quello che contiene il mazzo attualmente salvato; tile corrispondente evidenziato
- **Tap su tile**: salva immediatamente la scelta (`SettingsHandler.updateSetting`), aggiorna label nome e hero preview (comportamento invariato)
- Altezza sezione totale: ~160dp (tab ~36dp + griglia ~120dp)

---

## Architettura — file coinvolti

### Nuovi file
| File | Descrizione |
|---|---|
| `utils/DeckRegion.kt` | Enum `NORD`, `SUD_ISOLE`, `INTERNAZIONALI` |
| `utils/DeckGridAdapter.kt` | RecyclerView adapter per la griglia; espone callback `onDeckSelected: (CardDeck) -> Unit` |
| `res/layout/item_deck_grid_tile.xml` | Tile: 3 `ImageView` orizzontali, nessun `TextView`, dimensioni in `dp` |

### File modificati
| File | Cambiamento |
|---|---|
| `utils/CardDeck.kt` | Aggiunta property `val region: DeckRegion` |
| `utils/CardDeckRegistry.kt` | Regione per ogni mazzo; aggiunto helper `fun byRegion(r: DeckRegion): List<CardDeck>` |
| `res/layout/activity_deck_picker.xml` | Rimozione `ViewPager2` e `TabLayout` dots; aggiunta struttura tab + label + RecyclerView |
| `res/layout/settings.xml` | Sostituzione `ViewPager2` con label + `TabLayout` + `RecyclerView` nella sezione mazzo |
| `DeckPickerActivity.kt` | Riscrittura: gestione tab, selezione tile, aggiornamento label/bottone |
| `SettingsActivity.kt` | Aggiornamento sezione mazzo: tab + griglia, salvataggio immediato al tap |

### File eliminati
| File | Motivo |
|---|---|
| `utils/DeckCarouselAdapter.kt` | Sostituito completamente da `DeckGridAdapter` |

---

## Accessibilità

- Testo in tile: **assente** — i tile sono puri `dp`, non risentono del font di sistema
- Nome mazzo: `TextView` a larghezza piena in `sp` — scala con qualsiasi impostazione di accessibilità
- Testo tab: in `sp` — il sistema gestisce il wrapping automaticamente
- Touch target tile: minimo 48dp (linea guida Material) garantito dalla griglia 3 colonne su schermi ≥360dp
- Contrasto: bordo dorato su sfondo scuro (#d4af37 su #3a1a1a) — invariato rispetto all'app attuale

---

## Cosa non cambia

- Hero preview nelle impostazioni (aggiornamento al cambio mazzo: invariato)
- Logica `insetX` / `insetY` per il ritaglio delle anteprime carte (invariata)
- Mazzi `available = false` (opacità 40%, non cliccabili: invariato)
- Salvataggio con `SettingsHandler` e `Configuration.CARD_TYPE` (invariato)
- Tutti gli altri controlli in `SettingsActivity` (dorso carte, sfondo, switch)
