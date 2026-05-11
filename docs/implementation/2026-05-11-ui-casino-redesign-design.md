# Restyling UI "Casino classico" + miglioramenti gameplay

**Data:** 2026-05-11
**Stato:** Analisi approvata
**Mood:** Casino classico (verde tappeto + oro + bordeaux + serif italico)
**Documento correlato:** [Supporto iPhone via KMP](2026-05-11-ios-support-design.md)

---

## 1. Obiettivo

Trasformare Trasloco da app "funzionale" ad app "professional": restyling completo di splash, main menu, impostazioni e schermata vittoria; polish del tavolo di gioco; introduzione di tre nuove feature dal `TODO` (carta selezionata sempre visibile, hint, auto-move, resume partita).

L'investimento è progettato per essere "una volta sola": il design viene implementato ora su Android (XML/Views) e resterà valido quando l'app passerà a Compose Multiplatform (vedi spec iOS).

## 2. Scope

### In scope
- Restyling completo: splash, main menu, settings, win screen
- Polish del tavolo di gioco (frames, evidenziazione selezione)
- Rifacimento asset **sfondi** (solo gli sfondi, le carte restano)
- **G3 completo**: carta selezionata visibile, hint, auto-move verso end deck, resume game
- Nuove statistiche per win screen: miglior tempo, vittorie totali

### Fuori scope (vedi sezione 9 — Wishlist)
- G4 — nuove meccaniche (modalità a tempo, daily challenge, difficoltà, achievement, statistiche estese standalone, leaderboard online)
- Rifacimento asset carte (Piacentine/Francesi/retri restano)
- Audio nuovo / suoni aggiuntivi
- Refactoring qualità del codice fuori dal perimetro del restyling

## 3. Lingua visiva (design language)

### Palette principale
| Ruolo | Valore | Note |
|---|---|---|
| Verde tappeto (sfondo primario) | `#0a3520` → `#1a6638` (gradient radiale) | Sostituisce il piatto `verde.png` |
| Oro principale | `#d4af37` | Frame, titoli, selezioni, accenti |
| Oro chiaro | `#f5e6b3` | Testo su sfondi scuri |
| Oro scuro / bronzo | `#8b7129` | Gradient dell'oro, bordi pressed |
| Bordeaux | `#8b0000` | Accenti, badge, "Riprendi", vittorie consecutive |
| Marrone / cuoio | `#5a3a1c` | Bordi carta, ombre |
| Nero / inchiostro | `#1a1a1a` → `#0a3520` | Componenti scuri, switch OFF |

### Tipografia
- Font primario: **serif italico** (Georgia / Playfair / equivalente)
- Titoli: italico, letter-spacing 2-4px, spesso shadow morbida
- Testo settings/label: italic small caps, letter-spacing 2px
- Numeri (timer, stat): serif italico

### Principi
- **Sobrio**: niente cornici triple, niente texture aggressive (vedi polish P1)
- **Coerenza**: stessa palette, stessa famiglia di componenti su tutte le schermate
- **Mobile-first**: area di tap ≥ 44pt, controlli a portata di pollice
- **WYSIWYG**: l'utente vede sempre cosa sta scegliendo (anteprime dal vivo)

## 4. Design per schermata

### 4.1 Splash screen — concept S1 "Shuffle & Deal"

**Durata:** ~2s (con possibilità di tap-to-skip dopo 0.5s)

**Animazione (3 fasi):**
1. **0.0s — 0.6s**: pila di 4 carte coperte al centro, leggera oscillazione, sound `shuffle.mp3` parte
2. **0.6s — 1.4s**: le carte si "mescolano" in aria con motion blur, rotazioni casuali (-30°/+30°), spread radiale
3. **1.4s — 2.0s**: le carte atterrano scoperte a ventaglio nella metà bassa dello schermo; il titolo "Trasloco" emerge dall'alto con fade-in + glow oro

**Sfondo:** gradient verde radiale (`#1a6638` → `#0a3520`)
**Sound:** `shuffle.mp3` esistente
**Skip:** singolo tap = skip animazione e vai a main menu

**Implementazione Android:** `MainActivity` con `MotionLayout` o `ObjectAnimator`. La splash di sistema (`Theme.App.Starting`) rimane per il "cold start" Android (apre subito → MainActivity gestisce l'animazione vera).

### 4.2 Main menu — layout M3 "Tile grid 2×2"

```
┌─────────────────────────────┐
│       Trasloco              │
│   ~ GIOCO DI CARTE ~        │   📜 Regole
│                             │
│  ┌──────────┐  ┌──────────┐ │
│  │    ▶     │  │    ↻     │ │
│  │  Nuova   │  │ Riprendi │ │ ← Riprendi con badge ✦
│  │ Partita  │  │  02:14   │ │   condizionale
│  └──────────┘  └──────────┘ │
│  ┌──────────┐  ┌──────────┐ │
│  │    🏆    │  │    ⚙     │ │
│  │  Records │  │ Imposta- │ │
│  │          │  │  zioni   │ │
│  └──────────┘  └──────────┘ │
└─────────────────────────────┘
```

- **4 tile principali** (Nuova / Riprendi / Records / Impostazioni) in griglia 2×2
- "Nuova Partita" enfatizzato con gradient oro morbido + glow
- "Riprendi" appare con bordo dorato pieno + badge bordeaux ✦ **solo se esiste una partita salvata**; mostra il tempo già consumato come sub-label
- **Regole** spostato in alto a destra come link compatto (lo guardi una volta nella vita)
- Sfondo: verde radiale standard
- Titolo "Trasloco" in serif italico oro, 32sp, letter-spacing 4px

### 4.3 Settings — layout L3 + selettori B + toggle T1

**Struttura:**

```
┌─────────────────────────────┐
│      ~ Impostazioni ~       │   ← titolo serif italico
│                             │
│  ┌─── ANTEPRIMA ───────┐    │   ← hero preview dal vivo
│  │ [mini tavolo coi    │    │     che riflette i settings
│  │  4 subdeck + carte] │    │     correnti
│  └─────────────────────┘    │
│                             │
│  MAZZO CARTE                │   ← label oro maiuscolo
│  [🃏 Piacentine✓] [🂠 Fran.]│   ← selettore B (mini-carta)
│                             │
│  RETRO CARTE                │
│  [retro1✓] [retro2] [retro3]│   ← mini-carte clickabili
│                             │
│  TAPPETO                    │
│  [verde✓][bord.][legno]...  │   ← mini-tappeti
│                             │
│  ⚡ Distribuzione veloce  ●○│   ← toggle T1 gold switch
│  💡 Suggerimenti          ●○│
│  🎯 Auto-muovi su end deck●○│
└─────────────────────────────┘
```

**Hero preview** (componente nuovo): un riquadro in cima che mostra una versione miniaturizzata del tavolo di gioco con i settings correnti applicati (carte del tipo scelto, sul tappeto scelto, con il retro scelto). Cambi un setting → l'anteprima si aggiorna in tempo reale.

**Selettori (stile B "carte illustrate"):**
- Ogni opzione = mini-anteprima reale (mini-carta vera per cardType, mini-retro per cardBack, mini-tappeto per background)
- Selezionata: sollevamento di 2-3px + glow dorato + badge "✓" o cornice dorata
- Non selezionata: opacità 0.85, bordo grigio
- Tap = selezione + flip leggero (animazione 200ms)

**Toggle (stile T1 "switch dorato"):**
- Knob lucido oro su binario gold/dark
- ON: gradient oro pieno + knob a destra + glow leggero
- OFF: scuro + knob a sinistra
- Animazione: slide 200ms

**Settings inclusi nella sezione:**
- Mazzo carte (cardType: Piacentine/Francesi)
- Retro carte (cardBack: bg, bg2, bg3)
- Tappeto (background: verde, tappeto, panno, sabbia, legno, tavolo + nuovi)
- Distribuzione veloce (fastDeal — esistente)
- 💡 Suggerimenti (nuovo — G3)
- 🎯 Auto-muovi su end deck (nuovo — G3)

### 4.4 Game screen — polish P1 + controlli C1 + G3 features

**Top bar (C1):**
```
[←]              02:14              [⏸] [💡]
```
- 3 icone tonde dorate (32×32dp): back-to-menu, pausa, hint
- Timer al centro in pill dorata
- Pausa: ferma timer, mostra overlay "PAUSATO" con tap-to-resume
- Hint (💡): se attivo nei settings, evidenzia una mossa valida con glow dorato pulsante per 1.5s

**Polish P1 (sobrio):**
- Frame slot subdeck/endDeck: bordo 1px oro semi-trasparente (alpha 0.35)
- Carta selezionata: glow oro morbido (shadow blur 6px) + colore icone tinta oro
- Drop zone valida durante drag: illuminazione interna del bordo (nessun cambio drammatico)
- Animazione mossa carta: smooth slide con curva ease-out (200ms)
- Niente cornici doppie/triple, niente texture diagonali
- Niente camera shake / particelle in-game (riservate alla schermata vittoria)

**G3 features in-game:**

| Feature | Trigger | Comportamento |
|---|---|---|
| Carta selezionata sempre visibile | Sempre on | Quando una carta è selezionata, la carta è evidenziata con glow oro + icone tinta oro. Funziona sia per click che per drag-in-progress. Sostituisce il "selectedCard: String?" testuale con un highlight visivo. |
| Hint (💡) | Tap icona top-right (se abilitato in settings) | Engine cerca la prima mossa valida (carta tavolo → end deck) e la evidenzia con pulse dorato 1.5s. Se nessuna mossa = vibrazione + "Nessuna mossa disponibile" toast. |
| Auto-move su end deck | Auto-trigger se abilitato in settings | Quando la logica rileva che una carta del tavolo può andare direttamente su un end deck (carta giocabile univocamente), si muove da sola con animazione 300ms + soft sound. |
| Resume game | Trigger: aprire app con stato salvato | Game state serializzato in DB ad ogni mossa; main menu mostra "Riprendi" se DB ha uno stato attivo. Tap = riporta a GameScreen con stato + timer ripartiti. |

**Persistenza resume**: aggiungere tabella `game_state` (single row) con JSON serializzato di: `subDeckMap`, `cardTableMap`, `endDeckList`, `playList`, `selectedCard`, `gameStartTimeMillis`, `timerPausedTimeMillis`, `cardType`. Cancellata al "You won" o quando l'utente inizia una nuova partita.

### 4.5 Win screen — W3 "Stats-focused"

```
┌─────────────────────────────┐
│             🏆              │
│        Vittoria!            │
│                             │
│ ┌─────────────────────────┐ │
│ │ ⏱ Tempo          02:14  │ │
│ │ 🏅 Miglior tempo  01:48 │ │
│ │ 🔥 Striscia      3 di fila│
│ │ 📊 Vittorie totali   47 │ │
│ └─────────────────────────┘ │
│                             │
│       [ gif party piccola ] │
│                             │
│    ▶ Nuova partita          │
│      ← Menu                 │
└─────────────────────────────┘
```

- Trofeo 🏆 con drop-shadow oro
- Stat card con 4 righe (tempo / best / streak / totali)
- Se `tempo < miglior tempo` → "Miglior tempo" evidenziato in oro + sparkle ✨ + badge "Nuovo record!"
- Gif party (random da `PartyGifs`) in cornice dorata piccola (~90×60dp), decorativa
- Bottoni: "Nuova partita" (primary oro pieno) + "Menu" (secondary outlined)

**Schermata sconfitta (`gameLost`):** stesso template ma:
- Niente trofeo (icona 😔 o niente)
- Titolo "Hai perso..." rosso bordeaux
- Stat card mostra solo tempo + striscia (azzerata a 0)
- Bottoni: "Riprova" (primary) + "Menu"

## 5. Asset da rifare

### 5.1 Sfondi (G1 limitato)

Gli sfondi attuali (`tappeto`, `verde`, `bg`, `bg2`, `bg3`, `panno`, `sabbia`, `legno`, `tavolo`, `zero`) sono eterogenei e non tutti coerenti col mood casinò. Da rifare/integrare:

| Nome | Stato | Azione |
|---|---|---|
| `verde` | Tinta piatta `#0e6b3e` | **Rifare**: gradient radiale `#1a6638` → `#0a3520` (verde tappeto standard) |
| `tappeto` | Esistente | **Mantenere** se già adatto, altrimenti adattare al nuovo verde |
| `bordeaux` (nuovo) | — | **Aggiungere**: gradient radiale `#8b0000` → `#4a0000` |
| `legno` | Esistente | **Mantenere/rifinire**: texture legno scuro warm |
| `panno` | Esistente | **Valutare**: tenere se in tono col casinò |
| `sabbia`, `tavolo`, `zero`, `bg`, `bg2`, `bg3` | — | **Da decidere**: rimuovere quelli non coerenti o rifarli in palette |

Ogni sfondo deve fornire contrasto sufficiente perché le 120 carte attuali (che NON vengono rifatte) restino leggibili. Test obbligatorio per ogni sfondo: carta più chiara (es. 1 di denari) deve avere contrasto AAA contro lo sfondo.

### 5.2 Iconografia e frame

- Icone top bar (back, pausa, hint, settings): set vettoriale in oro `#d4af37`
- Trofeo 🏆 e icone stat: emoji o icon set serif-coerente
- Cornici dorate (per gif party, hero preview, stat card): drawable XML con `stroke 1px #d4af37`

## 6. Roadmap (priorità e fasi)

Stime in giornate-uomo (gg) full-time. Aggiungere ~30% se part-time.

### v1.10 — Restyling base + carta selezionata (≈ 10-12 gg)

| Task | gg |
|---|---|
| Asset rework sfondi (verde, bordeaux, valutazione altri) | 2-3 |
| Splash S1 (animazione MotionLayout/ObjectAnimator) | 2 |
| Main menu M3 (tile grid, no Riprendi attivo per ora) | 2 |
| Settings L3 (hero preview + selettori B + switch T1) — esclusi i nuovi toggle G3 | 3-4 |
| Carta selezionata sempre visibile (G3a — glow + icone tinta oro) | 1 |
| Test parità funzionale | 1 |

**🎯 Milestone:** release Play Store v1.10 con look casinò + carta selezionata visiva. Niente meccaniche nuove.

### v1.11 — Polish gameplay + Hint + Stats vittoria (≈ 11-13 gg)

| Task | gg |
|---|---|
| Polish tavolo P1 (frame, glow, drop zone illuminate) | 3 |
| Top bar C1 (back/timer/pausa+hint) | 2 |
| Hint engine (algoritmo: trova prima mossa valida) | 3-4 |
| Toggle "Suggerimenti" nei settings + persistenza | 0.5 |
| Win screen W3 (trofeo + stat card + best time tracking) | 2 |
| Stats tracking nel DB: best_time, total_wins (oltre a consecutive_wins esistente) | 2 |

**🎯 Milestone:** release Play Store v1.11 con polish in-game e win screen ricca.

### v1.12 — Auto-move + Resume game (≈ 5-7 gg)

| Task | gg |
|---|---|
| Auto-move su end deck (rilevazione carta univocamente piazzabile) | 2 |
| Toggle "Auto-muovi" nei settings | 0.5 |
| Resume game: serializzazione GameState in DB | 3 |
| Main menu: attivazione bottone "Riprendi" condizionale + badge tempo | 1 |
| Test integrazione | 0.5 |

**🎯 Milestone:** release Play Store v1.12 con G3 completo.

### Totale

| Release | Range |
|---|---|
| v1.10 — Restyling base | 10-12 gg |
| v1.11 — Polish + Hint + Stats | 11-13 gg |
| v1.12 — Auto-move + Resume | 5-7 gg |
| **TOTALE** | **26-32 gg** (≈ 5-6 settimane full-time, o 3-4 mesi part-time) |

Ogni release è autonoma e rilasciabile. In caso di pausa, niente è sprecato.

## 7. Continuità con KMP / iOS migration

Le scelte di design sono platform-agnostic:
- Palette, tipografia, layout sono trasferibili 1:1 a Compose Multiplatform
- Animazioni (splash shuffle, card flip, slide mosse) si traducono in `Modifier.animateXxx`/`AnimatedVisibility` Compose
- Hero preview, tile grid, switch dorato sono Composable già diffusi
- Gli sfondi rifatti come gradient/drawable usano risorse cross-platform

**Quando** si passerà a `:composeApp` (Fase 2 della migrazione iOS), il design viene **portato** non ridisegnato. Il lavoro di Fase 2 (riscrittura UI) ne risulta accelerato perché gli screen sono già pensati.

## 8. Rischi

| Rischio | Probabilità | Impatto | Mitigazione |
|---|---|---|---|
| Animazione splash S1 troppo "pesante" per device low-end (lag su 4-5 anni fa) | Media | Medio | Profilare su device entry-level prima del release; fallback "fast splash" (solo flip + logo) sotto soglia hardware |
| Hero preview dei settings introduce lag se aggiornato troppo frequentemente | Bassa | Basso | Debounce 100ms sugli aggiornamenti; preview usa Bitmap cache delle carte |
| Hint engine richiede tempi di calcolo non triviali (4 subdeck × 4 endDeck × N carte) | Bassa | Basso | Calcolo O(40) max — non è un problema; opzionalmente in coroutine background |
| Serializzazione GameState (resume) cresce con bug di stato non coperto | Media | Medio | Test JSON round-trip su tutti i campi; versionare il payload con `schema_version` per future migrazioni |
| Gli sfondi rifatti non si abbinano alle carte esistenti (contrasto) | Media | Alto | Test contrasto AAA obbligatorio per ogni nuovo sfondo; se una carta non legge, rifare lo sfondo (NON la carta) |
| Cambio drastico di look può disorientare utenti esistenti della v1.9 | Media | Basso | Release v1.10 con changelog dedicato; gli utenti che vedono il restyling al primo apri lo capiscono in 5 secondi |

## 9. Wishlist G4 (fuori scope, da valutare in futuro)

Decise come "futuro" durante il brainstorming. Da brainstorming separato se/quando interessano:

- **G4a — Modalità a tempo / sfida cronometro** (es. "vinci in <3min")
- **G4b — Livelli di difficoltà** (easy/standard/hard)
- **G4c — Daily challenge** (deck giornaliero deterministico, condivisibile)
- **G4d — Achievement / trofei** ("10 vittorie consecutive", "vittoria sotto 2min", ecc.)
- **G4e — Statistiche estese standalone** (schermata dedicata con grafici partite/tempi/vittorie nel tempo) — *attenzione: parzialmente già coperta da win screen W3 stats*
- **G4f — Leaderboard online** (sconsigliato — richiede backend, fuori dallo spirito "app locale")

## 10. Open Questions

Da risolvere prima di iniziare v1.10:

1. **Sfondi da mantenere/rimuovere**: dei 10 attuali, quali tieni? Quelli da rimuovere richiederebbero migrazione utenti che ce li hanno selezionati (fallback a default `verde`).
2. **Nome alternativo "Trasloco"**: il titolo serif italico funziona benissimo, ma vuoi affiancarlo con un sottotitolo fisso "~ GIOCO DI CARTE ~" (come mostrato nei mockup) o lasciarlo solo?
3. **Toggle "Suggerimenti" — comportamento default**: ON (sempre visibile, semplice ai nuovi giocatori) o OFF (puristi)?
4. **Auto-move on/off default**: ON (più rapido) o OFF (controllo totale)?
5. **Schermata vittoria — "Nuovo record!"**: quando battere il best time, oltre allo sparkle, vuoi anche suono dedicato (es. `youwin.mp3` esistente con pitch +20%)?
6. **Skip splash**: il tap durante l'animazione skippa? Si attiva dopo 0.5s (per non far skippare per sbaglio)?
7. **Cosa fare con gli utenti v1.9 che hanno `consecutiveWins` salvati**: portarli avanti come "vittorie totali" stimate, o partire da 0 con la nuova stat?

## 11. Riferimenti visivi

Tutti i mockup interattivi prodotti durante il brainstorming sono salvati in:

```
.superpowers/brainstorm/95202-1778492491/content/
├── aesthetic-direction.html      (A — Casino classico)
├── settings-layout.html          (L3 — Hero preview)
├── selector-style.html           (B — Carte illustrate)
├── toggle-style.html             (T1 — Switch dorato)
├── splash-concept.html           (S1 — Shuffle & Deal)
├── game-polish.html              (P1 — Sobrio)
├── main-menu.html                (M3 — Tile grid)
├── ingame-controls.html          (C1 — Top bar)
└── win-screen.html               (W3 — Stats)
```

(Aggiungere `.superpowers/` al `.gitignore` se non vuoi committarli.)

## 12. Prossimo passo

Risolvere le **Open Questions** (sezione 10), poi invocare `writing-plans` per produrre il piano di implementazione dettagliato di **v1.10** (Restyling base + carta selezionata).
