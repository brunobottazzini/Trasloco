# Localizzazione — Design Spec
**Data:** 2026-05-20
**Progetto:** Trasloco (Android)
**Scope:** Aggiunta di 14 nuovi file di stringhe localizzate

---

## Obiettivo

Espandere il supporto linguistico da 3 lingue (EN, IT, PT) a 16, aggiungendo:
- 4 lingue europee occidentali con script latino
- 2 varianti del portoghese (brasiliano + europeo)
- 8 lingue con script non-latino (Cirillico, Hangul, CJK, Devanagari, Thai)

Nessuna modifica a codice Kotlin o layout XML. Solo file di risorse.

---

## File da creare

| Cartella Android | Lingua | Script |
|---|---|---|
| `app/src/main/res/values-es/strings.xml` | Spagnolo | Latino |
| `app/src/main/res/values-fr/strings.xml` | Francese | Latino |
| `app/src/main/res/values-de/strings.xml` | Tedesco | Latino |
| `app/src/main/res/values-nl/strings.xml` | Olandese | Latino |
| `app/src/main/res/values-pt-rBR/strings.xml` | Portoghese brasiliano | Latino |
| `app/src/main/res/values-pt-rPT/strings.xml` | Portoghese europeo | Latino |
| `app/src/main/res/values-ru/strings.xml` | Russo | Cirillico |
| `app/src/main/res/values-tr/strings.xml` | Turco | Latino |
| `app/src/main/res/values-pl/strings.xml` | Polacco | Latino |
| `app/src/main/res/values-ko/strings.xml` | Coreano | Hangul |
| `app/src/main/res/values-ja/strings.xml` | Giapponese | CJK |
| `app/src/main/res/values-zh-rCN/strings.xml` | Cinese semplificato | CJK |
| `app/src/main/res/values-hi/strings.xml` | Hindi | Devanagari |
| `app/src/main/res/values-th/strings.xml` | Tailandese | Thai |

`values-pt/` rimane invariato come fallback generico portoghese.

---

## Struttura di ogni file

Ogni `strings.xml` è una copia di `values/strings.xml` con tutte le stringhe tradotte, **eccetto** quelle con `translatable="false"`:
- `app_name` → invariato ("Trasloco")
- `title` → invariato ("Trasloco")
- `trasloco` → invariato ("Trasloco")
- `watermark_studio` → invariato ("Bottazzini Softworks")

Totale stringhe da tradurre per file: ~235 (su 239 totali).

---

## Regole di traduzione

### Nomi propri italiani — invariati in tutte le lingue
I nomi dei mazzi di carte sono nomi propri regionali italiani, non si traducono:
- `card_type_piacentine`, `card_type_napoletane`, `card_type_francesi`, `card_type_bergamasche`, ecc.
- Tradurre solo eventuali label UI adiacenti, non i nomi stessi.

### Achievement "Ferragosto"
- `achievement_ferragosto_name` → "Ferragosto" in tutte le lingue (nome proprio della festività)
- `achievement_ferragosto_desc` → tradurre la descrizione ("Open the app on August 15th")

### Tono degli achievement
- **Sconfitte consecutive** (`loss_2` … `loss_10`): sarcastici/ironici — mantenere il tono scherzoso
- **Stile** (`hint_addict`, `perfectionist`, ecc.): spiritosi ma non crudeli
- **UI generale**: neutro e chiaro

### Escape XML
- Apostrofi in stringhe: `\'`
- Ampersand: `&amp;`
- I caratteri Unicode (cirillico, CJK, thai, devanagari) non richiedono escape

### Variabili di formato
Le stringhe con `%1$s`, `%2$s`, `%1$d` devono mantenere i placeholder invariati:
- `time_taken` → `"Time: %1$s Best time: %2$s"` (solo le parole intorno cambiano)
- `victory_in_a_row` → mantieni `%1$s` e `%2$s`
- `stats_trophies_header` → `(%1$d / 55)` invariato
- `stats_unlocked_on` → mantieni `%1$s` e `%2$s`
- `deck_picker_play_with` → mantieni `%s`

---

## PT-BR vs PT-PT — differenze chiave

Le differenze più rilevanti tra le due varianti:

| Stringa | PT-BR | PT-PT |
|---|---|---|
| `achievement_loss_7_desc` | "Não é o celular" | "Não é o telemóvel" |
| `achievement_night_owl_3_desc` | "Jogue 3 partidas após meia-noite" | "Jogue 3 partidas após meia-noite" |
| Pronome formale | "você" | "você" o "tu" |
| `preparing_game` | "Preparando jogo…" | "A preparar o jogo…" |

L'attuale `values-pt/` ha contenuto brasiliano — `values-pt-rBR` può riciclare quasi tutto; `values-pt-rPT` avrà le variazioni europee.

---

## Nessun test richiesto

Le stringhe non sono coperte da unit test. Nessuna modifica al DB o alla logica di gioco.

---

## Esecuzione consigliata

I 14 file sono completamente indipendenti. Eseguire in gruppi paralleli:
- Gruppo A (script latino): ES, FR, DE, NL, TR, PL → 6 subagent in parallelo
- Gruppo B (PT split): PT-BR, PT-PT → 2 subagent in parallelo
- Gruppo C (script non-latino): RU, KO, JA, ZH-CN, HI, TH → 6 subagent in parallelo
