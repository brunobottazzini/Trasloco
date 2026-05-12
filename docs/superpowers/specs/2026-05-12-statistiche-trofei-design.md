# G4d + G4e — Statistiche & Trofei

**Data:** 2026-05-12
**Stato:** Approvato
**Riferimento design base:** `docs/implementation/2026-05-11-ui-casino-redesign-design.md` § 9 (Wishlist G4d, G4e)

---

## 1. Obiettivo

Implementare un'unica schermata **Statistiche & Trofei** che sostituisce la `RecordActivity` esistente, offrendo:

- **G4d** — 34 achievement con sblocco in-game tramite banner animato
- **G4e** — statistiche estese (6 metriche aggregate + grafico storico tempi con MPAndroidChart)

---

## 2. Scope

### In scope
- Nuova `StatsActivity` (sostituisce `RecordActivity`)
- Grafico storico ultimi 30 partite (MPAndroidChart `LineChart`)
- 34 achievement con condizioni, icone, nomi e date di sblocco
- `AchievementEngine` — valutazione e persistenza achievement
- Banner in-game in `GameActivity` per notifica sblocco
- `BottomSheetDialog` per dettaglio singolo achievement
- Migrazione DB v3 → v4 (due nuove tabelle)
- Raccolta dati per-partita (`hints_used`, `auto_moves`) in `GameActivity`

### Fuori scope
- Leaderboard online (G4f)
- Achievement basati su cloud / condivisione social
- Animazioni elaborate sui trofei (particelle, confetti)
- Notifiche push per achievement

---

## 3. Data Model — DB v4

### 3.1 Migrazione `onUpgrade` v3 → v4

Crea due nuove tabelle. Nessun dato esistente viene modificato o perso.

```sql
CREATE TABLE IF NOT EXISTS game_log (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp   INTEGER NOT NULL,   -- epoch ms, fine partita
    duration_ms INTEGER NOT NULL,   -- durata partita in ms
    won         INTEGER NOT NULL,   -- 1=vinta, 0=persa
    hints_used  INTEGER NOT NULL,   -- tap su pulsante hint
    auto_moves  INTEGER NOT NULL    -- carte spostate da auto-move
);

CREATE TABLE IF NOT EXISTS achievements (
    id          TEXT PRIMARY KEY,   -- es. "speed_2min"
    unlocked_at INTEGER NOT NULL    -- epoch ms
);
```

La tabella `records` esistente (best time, consecutive, total_wins) rimane invariata e continua ad essere usata da `RecordsHandler`.

### 3.2 Raccolta dati in-game

`GameActivity` aggiunge due contatori di istanza:
- `private var hintsUsedThisGame: Int = 0` — incrementato ogni tap su 💡
- `private var autoMovesThisGame: Int = 0` — incrementato in ogni auto-move

Al termine partita (vinta o persa) viene inserita una riga in `game_log` tramite `GameLogRepository`.

---

## 4. Architettura

### Nuovi file

| File | Responsabilità |
|---|---|
| `db/columns/GameLogColumns.kt` | Costanti colonne tabella `game_log` |
| `db/columns/AchievementsColumns.kt` | Costanti colonne tabella `achievements` |
| `settings/GameLogRepository.kt` | Insert/query su `game_log` |
| `settings/AchievementsRepository.kt` | Insert/query su `achievements` |
| `utils/AchievementEngine.kt` | Valuta condizioni, restituisce achievement appena sbloccati |
| `utils/AchievementBanner.kt` | Gestisce la coda e l'animazione del banner in-game |
| `StatsActivity.kt` | Schermata Statistiche & Trofei |
| `res/layout/activity_stats.xml` | Layout StatsActivity |
| `res/layout/item_achievement.xml` | Cella griglia trofeo (sbloccato / locked) |
| `res/layout/sheet_achievement_detail.xml` | BottomSheet dettaglio achievement |
| `res/layout/banner_achievement.xml` | Banner overlay in-game |

### File modificati

| File | Modifica |
|---|---|
| `db/DatabaseHandler.kt` | Bump version → 4, `onUpgrade` crea le due nuove tabelle |
| `GameActivity.kt` | Aggiunge contatori, inserisce `game_log`, chiama `AchievementEngine`, mostra banner |
| `YouWonActivity.kt` | Chiama `AchievementEngine` dopo la registrazione delle stats |
| `MainActivity.kt` | Collegamento tile Records → `StatsActivity` |
| `AndroidManifest.xml` | Dichiara `StatsActivity` |
| `res/values/strings.xml` | Nomi e descrizioni achievement, label nuove stats |

### Flusso sblocco achievement

```
Partita finisce (won/lost)
    │
    ├─► GameLogRepository.insert(gameLog)
    │
    ├─► RecordsHandler.update(...)       ← già esistente
    │
    └─► AchievementEngine.evaluate(context)
            │
            ├─ legge game_log, records, achievements
            ├─ calcola quali achievement ora soddisfatti ma non ancora in achievements
            └─ per ogni nuovo achievement:
                   AchievementsRepository.insert(id, now)
                   AchievementBanner.enqueue(achievement)  ← solo se in GameActivity
```

---

## 5. AchievementEngine

`AchievementEngine` è un `object` (singleton stateless). Espone:

```kotlin
object AchievementEngine {
    // Restituisce la lista degli achievement appena sbloccati (non ancora in DB).
    // Chiama AchievementsRepository.insert() per ognuno.
    fun evaluate(
        context: Context,
        triggerEvent: AchievementTrigger
    ): List<AchievementDef>
}

enum class AchievementTrigger { GAME_WON, GAME_LOST, TUTORIAL_COMPLETED, APP_OPENED }
```

`AchievementDef` è una `data class` con `id`, `icon`, `name`, `description`, `condition`.
La lista completa di `AchievementDef` è definita in `AchievementCatalog.kt` (file separato, lista statica).

### Fonti dati per la valutazione

| Achievement category | Fonte |
|---|---|
| Vittorie totali | `RecordsHandler.getTotalWins()` |
| Streak | `RecordsHandler.readValue(Type.CONSECUTIVE)` |
| Velocità | ultima riga `game_log` con `won=1` |
| Partite giocate | `COUNT(*)` su `game_log` |
| `hint_free` | ultima riga `game_log`: `won=1 AND hints_used=0` |
| `no_assist` | ultima riga `game_log`: `won=1 AND hints_used=0 AND auto_moves=0` |
| `resilient` | ultime 4 righe `game_log`: le 3 precedenti `won=0`, l'ultima `won=1` |
| morning / midnight | `Calendar` sul `timestamp` dell'ultima riga |
| christmas | `Calendar` su `System.currentTimeMillis()` al trigger `APP_OPENED` |
| tutorial_done | flag `SharedPreferences` già esistente `tutorial_seen` |
| new_record | flag `isNew` da `RecordsHandler.readNew(Type.TIME)` |

---

## 6. Lista Achievement Completa (34)

### Vittorie totali (6)

| ID | Icona | Nome | Condizione |
|---|---|---|---|
| `first_win` | 🎉 | Prima vittoria | 1 vittoria totale |
| `wins_10` | 🃏 | Giocatore | 10 vittorie totali |
| `wins_50` | 🎰 | Assiduo | 50 vittorie totali |
| `wins_100` | 🏆 | Campione | 100 vittorie totali |
| `wins_500` | 👑 | Leggenda | 500 vittorie totali |
| `wins_1000` | ♾️ | Eterno | 1000 vittorie totali |

### Streak consecutive (12)

| ID | Icona | Nome | Condizione |
|---|---|---|---|
| `streak_3` | 🔥 | In forma | 3 vittorie consecutive |
| `streak_6` | 🔥 | Ritmo | 6 vittorie consecutive |
| `streak_9` | 🔥 | Concentrato | 9 vittorie consecutive |
| `streak_12` | 💥 | Inarrestabile | 12 vittorie consecutive |
| `streak_15` | 💥 | Dominatore | 15 vittorie consecutive |
| `streak_18` | 💥 | Maestro | 18 vittorie consecutive |
| `streak_21` | ⚡ | Fenomeno | 21 vittorie consecutive |
| `streak_24` | ⚡ | Leggendario | 24 vittorie consecutive |
| `streak_27` | ⚡ | Assoluto | 27 vittorie consecutive |
| `streak_30` | 🌟 | Intoccabile | 30 vittorie consecutive |
| `streak_50` | 🌟 | Immortale | 50 vittorie consecutive |
| `streak_100` | 💎 | Dio del Trasloco | 100 vittorie consecutive |

### Velocità (4)

| ID | Icona | Nome | Condizione |
|---|---|---|---|
| `speed_3min` | ⏱ | Fulmine | Vinci in < 3 minuti |
| `speed_2min` | 🚀 | Velocista | Vinci in < 2 minuti |
| `speed_1min` | ✈️ | Supersonico | Vinci in < 1 minuto |
| `speed_45s` | 🌪️ | Uragano | Vinci in < 45 secondi |

### Stile di gioco (8)

| ID | Icona | Nome | Condizione |
|---|---|---|---|
| `tutorial_done` | 📚 | Autodidatta | Completa il tutorial |
| `first_loss` | 😅 | Ci vuole pazienza | Perdi la prima partita |
| `games_50` | 🏋️ | Allenamento | 50 partite giocate (vinte o perse) |
| `games_200` | 🎪 | Maratoneta | 200 partite giocate |
| `games_500` | 🌍 | Ossessione | 500 partite giocate |
| `hint_free` | 🎯 | Purista | Vinci una partita senza usare 💡 |
| `no_assist` | 🧘 | Zen | Vinci senza 💡 né auto-mossa |
| `resilient` | 💪 | Resiliente | Vinci dopo 3 sconfitte consecutive |

### Speciali (4)

| ID | Icona | Nome | Condizione |
|---|---|---|---|
| `morning` | 🌅 | Mattiniero | Vinci una partita prima delle 7:00 |
| `midnight` | 🌙 | Nottambulo | Vinci una partita dopo mezzanotte |
| `christmas` | 🎄 | Festivo | Apri l'app il 25 dicembre |
| `new_record` | ⭐ | Record assoluto | Batti il tuo record personale di velocità |

---

## 7. StatsActivity — Layout

Schermata scrollabile verticale in stile casino. Sostituisce `RecordActivity`; il tile "Records" nel menu principale punta a `StatsActivity`.

### 7.1 Sezione Stats aggregate

Card in stile `casino_tile_bg` con 6 righe:

| Label | Fonte |
|---|---|
| ⏱ Miglior tempo | `RecordsHandler.getBestTime()` |
| 🔥 Streak record | `RecordsHandler.readValue(Type.CONSECUTIVE)` |
| 🏆 Vittorie totali | `RecordsHandler.getTotalWins()` |
| 🎮 Partite giocate | `COUNT(*)` su `game_log` |
| ✅ % vittorie | `COUNT(won=1) / COUNT(*) * 100` su `game_log` |
| ⌛ Tempo medio | `AVG(duration_ms) WHERE won=1` su `game_log` |

### 7.2 Sezione Grafico

`MPAndroidChart LineChart` — ultime 30 partite ordinate per `timestamp ASC`.
- Asse X: indice partita (1…n)
- Asse Y: durata in minuti (float)
- Punti: cerchio verde se `won=1`, rosso se `won=0`
- Tap su punto: marker con durata esatta e esito
- Stato vuoto (< 1 partita in log): testo centrato "Gioca la tua prima partita"
- Colori asse/griglia: `casino_gold` alpha 0.3, sfondo trasparente

### 7.3 Sezione Trofei

Header: "Trofei (X / 34)" con `X` = count righe in `achievements`.
Griglia `RecyclerView` con `GridLayoutManager(3)`.

**Cella sbloccata:** sfondo `casino_tile_bg`, icona emoji grande (32sp), nome sotto (10sp serif italic oro), nessun lucchetto.
**Cella locked:** stessa struttura ma alpha 0.35, icona 🔒 sovrapposta in basso a destra, testo grigio.

**Tap su cella → `AchievementDetailBottomSheet`:**
- Titolo: nome achievement
- Icona grande (48sp)
- Descrizione condizione completa
- Se sbloccato: "Sbloccato il GG/MM/AAAA alle HH:MM"
- Se locked: "Non ancora sbloccato"

---

## 8. Banner Achievement

View con layout `banner_achievement.xml` inclusa via `<include>` in:
- `game.xml` / `game-land/game.xml` — per sblocchi su sconfitta e tutorial completato
- `activity_you_won.xml` — per sblocchi su vittoria (la navigazione avviene prima che il banner possa mostrarsi in GameActivity)

```xml
[🏆]  Achievement sbloccato!
[icona]  Nome achievement
         Descrizione breve
```

**Dove si valutano gli achievement per trigger:**

| Trigger | Activity che valuta | Activity che mostra il banner |
|---|---|---|
| `GAME_WON` | `YouWonActivity` | `YouWonActivity` |
| `GAME_LOST` | `GameActivity` (lostOverlay) | `GameActivity` |
| `TUTORIAL_COMPLETED` | `GameActivity` | `GameActivity` |
| `APP_OPENED` | `MainActivity` | nessun banner (solo `christmas`) |

**`AchievementBanner`** gestisce una coda (`Queue<AchievementDef>`):
- `enqueue(achievement)`: aggiunge alla coda e avvia la sequenza se idle
- Animazione: `translationY` da `-height` a `0` (slide-in 300ms), attesa 2500ms, slide-out 300ms
- Dopo lo slide-out: se la coda non è vuota, mostra il prossimo
- Background: `casino_tutorial_banner_bg` (già esistente, 80% opacità)

---

## 9. Dipendenze

Aggiungere in `app/build.gradle`:

```groovy
implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
```

E nel `settings.gradle` (se non già presente):

```groovy
maven { url 'https://jitpack.io' }
```

---

## 10. Internazionalizzazione

Aggiungere nomi e descrizioni di tutti i 34 achievement in:
- `res/values/strings.xml` (en)
- `res/values-it/strings.xml` (it)
- `res/values-pt/strings.xml` (pt)

Pattern chiavi: `achievement_<id>_name` e `achievement_<id>_desc`.
Esempio: `achievement_streak_10_name` = "Inarrestabile", `achievement_streak_10_desc` = "Vinci 10 partite di fila".

---

## 11. Rischi

| Rischio | Probabilità | Impatto | Mitigazione |
|---|---|---|---|
| MPAndroidChart non compatibile con compileSdk 36 | Bassa | Alto | Verificare alla prima build; fallback: Canvas custom semplice |
| `game_log` cresce indefinitamente | Media | Basso | Tenere solo ultime 500 righe; `DELETE` delle più vecchie dopo ogni insert se `COUNT > 500` |
| Achievement valutati su ogni evento rallentano la UI | Bassa | Basso | `AchievementEngine.evaluate()` in coroutine `Dispatchers.IO` |
| Utenti esistenti con dati in `records` non hanno `game_log` | Certa | Basso | Stats derivate da `game_log` mostrano "–" o 0 fino alla prima partita con v nuova; best time e streak dall'esistente `records` non ne risentono |
| `christmas` achievement triggered senza giocare | Voluto | — | È un easter egg — basta aprire l'app il 25/12 |
