# Achievement Expansion — Design Spec
**Data:** 2026-05-19  
**Progetto:** Trasloco (Android)  
**Scope:** Aggiunta di 21 nuovi achievement al sistema esistente

---

## Obiettivo

Espandere il catalogo da 34 a 55 achievement aggiungendo:
- Una serie di achievement sarcastici basati su sconfitte consecutive ("presa in giro")
- Achievement di stile di gioco (lentezza, dipendenza dai suggerimenti, perfezione)
- Achievement legati a orari e giorni speciali
- Nuove festività italiane
- Achievement basati su sessioni intensive

---

## Catalogo nuovi achievement (21)

### Sconfitte consecutive — trigger `GAME_LOST`
| ID | Icona | Nome | Descrizione |
|---|---|---|---|
| `loss_2` | 😬 | Ci risiamo | Perdi 2 partite di fila. Coincidenza? |
| `loss_3` | 🤦 | Tre è il limite | 3 sconfitte consecutive. O forse no |
| `loss_5` | 💀 | Sei sicuro di saper giocare? | 5 sconfitte di fila. Magari un tutorial? |
| `loss_7` | 🫣 | Forse è il telefono | 7 di fila. Non è il telefono |
| `loss_10` | 🃏 | Maestro della sconfitta | 10 sconfitte consecutive. Talento raro |
| `big_loser` | 🏳️ | Perseveranza infinita | 100 sconfitte totali |

### Rimonte e stile — trigger `GAME_WON`
| ID | Icona | Nome | Descrizione |
|---|---|---|---|
| `comeback_2` | 😤 | Stavo solo scaldando | Vinci dopo 2 sconfitte di fila |
| `slow_win` | 🐢 | Chi va piano... | Vinci in più di 15 minuti |
| `hint_hero` | 💡 | Con un po' d'aiuto | Vinci usando 5 o più suggerimenti |
| `hint_addict` | 🧪 | Suggerimento-dipendente | Usa suggerimenti nelle ultime 5 partite di fila |
| `perfectionist` | 🎭 | Perfezionista | Vinci 3 partite di fila senza hint né auto-mosse |
| `speed_freak` | ⚡ | Veloce e furioso | Vinci 3 partite consecutive in meno di 2 minuti |
| `lunch_win` | 🍝 | Pausa pranzo produttiva | Vinci tra le 12:00 e 14:00 |
| `sunday_player` | ☕ | Domenica rilassata | Vinci di domenica |
| `night_owl_3` | 🦉 | Nottambulo accanito | Gioca 3 partite consecutive dopo mezzanotte |
| `same_day_3` | 📅 | Non mi fermo | 3 vittorie nello stesso giorno |
| `same_day_5` | 🔁 | Giornata da pro | 5 partite giocate nello stesso giorno |
| `new_year_eve` | 🥂 | Aspettando la mezzanotte | Gioca una partita il 31 dicembre |

### Festività — trigger `APP_OPENED`
| ID | Icona | Nome | Descrizione |
|---|---|---|---|
| `new_year` | 🎆 | Anno nuovo, carte nuove | Apri l'app il 1° gennaio |
| `halloween` | 🎃 | Dolcetto o scherzetto | Apri l'app il 31 ottobre |
| `ferragosto` | ☀️ | Ferragosto | Apri l'app il 15 agosto |

---

## Architettura — modifiche al codice

### File da modificare

**`AchievementCatalog.kt`**  
Aggiungere 21 nuove voci `AchievementDef(...)` nelle sezioni appropriate.

**`AchievementEngine.kt`**  
- `getLastN(4)` → `getLastN(10)` nel metodo `evaluate()`
- Parametro `lastFourGames: List<GameLog>` → `recentGames: List<GameLog>` in `evaluateConditions()`
- Aggiungere tutti i nuovi check alle sezioni `GAME_LOST`, `GAME_WON`, `APP_OPENED`

Nessuna modifica a `GameLogRepository` — `totalLosses = totalGames - totalWins` è già calcolabile con i dati esistenti.

**`strings.xml`** × 3 lingue (IT, PT, EN)  
42 nuove stringhe per lingua (21 nomi + 21 descrizioni).

**`AchievementEngineTest.kt`**  
Nuovi casi di test per ogni condizione aggiunta.

---

## Logica dettagliata delle condizioni

### `GAME_LOST`

```kotlin
// Sconfitte consecutive
listOf(2, 3, 5, 7, 10).forEach { n ->
    if (recentGames.size >= n && recentGames.take(n).all { !it.won })
        candidates.add("loss_$n")
}

// Perseveranza infinita
if (totalGames - totalWins >= 100) candidates.add("big_loser")
```

### `GAME_WON`

```kotlin
// Stavo solo scaldando
if (recentGames.size >= 3 && recentGames[0].won &&
    !recentGames[1].won && !recentGames[2].won)
    candidates.add("comeback_2")

// Vittoria lenta (> 15 min)
if (lastGame.durationMs > 15 * 60 * 1000L) candidates.add("slow_win")

// Hint abbondanti
if (lastGame.hintsUsed >= 5) candidates.add("hint_hero")

// Hint in 5 partite di fila
if (recentGames.size >= 5 && recentGames.take(5).all { it.hintsUsed > 0 })
    candidates.add("hint_addict")

// 3 vittorie di fila senza hint né auto-mosse
if (recentGames.size >= 3 && recentGames.take(3).all {
        it.won && it.hintsUsed == 0 && it.autoMoves == 0 })
    candidates.add("perfectionist")

// 3 vittorie di fila in meno di 2 minuti
if (recentGames.size >= 3 && recentGames.take(3).all {
        it.won && it.durationMs < 2 * 60 * 1000L })
    candidates.add("speed_freak")

// Orari e giorni
val cal = Calendar.getInstance().apply { timeInMillis = lastGame.timestamp }
val hour = cal.get(Calendar.HOUR_OF_DAY)
val dow  = cal.get(Calendar.DAY_OF_WEEK)
val month = cal.get(Calendar.MONTH)
val day   = cal.get(Calendar.DAY_OF_MONTH)

if (hour in 12..13) candidates.add("lunch_win")
if (dow == Calendar.SUNDAY) candidates.add("sunday_player")
if (month == Calendar.DECEMBER && day == 31) candidates.add("new_year_eve")

// 3 partite di fila dopo mezzanotte
if (recentGames.size >= 3 && recentGames.take(3).all {
        Calendar.getInstance().apply { timeInMillis = it.timestamp }
            .get(Calendar.HOUR_OF_DAY) in 0..5 })
    candidates.add("night_owl_3")

// 3 vittorie nello stesso giorno di calendario
if (recentGames.size >= 3) {
    val wins = recentGames.filter { it.won }.take(3)
    if (wins.size == 3 && wins.map { dayKey(it.timestamp) }.distinct().size == 1)
        candidates.add("same_day_3")
}

// 5 partite nello stesso giorno di calendario
if (recentGames.size >= 5 &&
    recentGames.take(5).map { dayKey(it.timestamp) }.distinct().size == 1)
    candidates.add("same_day_5")
```

`dayKey(ts)` è un helper privato che restituisce `"YYYY-MM-DD"` dal timestamp, usato solo in `evaluateConditions`.

### `APP_OPENED`

```kotlin
val cal = Calendar.getInstance().apply { timeInMillis = now }
val month = cal.get(Calendar.MONTH)
val day   = cal.get(Calendar.DAY_OF_MONTH)

if (month == Calendar.JANUARY  && day == 1)  candidates.add("new_year")
if (month == Calendar.OCTOBER  && day == 31) candidates.add("halloween")
if (month == Calendar.AUGUST   && day == 15) candidates.add("ferragosto")
if (month == Calendar.DECEMBER && day == 31) candidates.add("new_year_eve")
```

---

## Stringhe da aggiungere (IT — riferimento)

```xml
<!-- loss streak -->
<string name="achievement_loss_2_name">Ci risiamo</string>
<string name="achievement_loss_2_desc">Perdi 2 partite di fila. Coincidenza?</string>
<string name="achievement_loss_3_name">Tre è il limite</string>
<string name="achievement_loss_3_desc">3 sconfitte consecutive. O forse no</string>
<string name="achievement_loss_5_name">Sei sicuro di saper giocare?</string>
<string name="achievement_loss_5_desc">5 sconfitte di fila. Magari un tutorial?</string>
<string name="achievement_loss_7_name">Forse è il telefono</string>
<string name="achievement_loss_7_desc">7 di fila. Non è il telefono</string>
<string name="achievement_loss_10_name">Maestro della sconfitta</string>
<string name="achievement_loss_10_desc">10 sconfitte consecutive. Talento raro</string>
<!-- stile -->
<string name="achievement_comeback_2_name">Stavo solo scaldando</string>
<string name="achievement_comeback_2_desc">Vinci dopo 2 sconfitte di fila</string>
<string name="achievement_slow_win_name">Chi va piano...</string>
<string name="achievement_slow_win_desc">Vinci in più di 15 minuti</string>
<string name="achievement_hint_hero_name">Con un po\' d\'aiuto</string>
<string name="achievement_hint_hero_desc">Vinci usando 5 o più suggerimenti</string>
<string name="achievement_hint_addict_name">Suggerimento-dipendente</string>
<string name="achievement_hint_addict_desc">Usa suggerimenti nelle ultime 5 partite di fila</string>
<string name="achievement_perfectionist_name">Perfezionista</string>
<string name="achievement_perfectionist_desc">Vinci 3 partite di fila senza hint né auto-mosse</string>
<string name="achievement_speed_freak_name">Veloce e furioso</string>
<string name="achievement_speed_freak_desc">Vinci 3 partite consecutive in meno di 2 minuti</string>
<string name="achievement_big_loser_name">Perseveranza infinita</string>
<string name="achievement_big_loser_desc">100 sconfitte totali</string>
<string name="achievement_lunch_win_name">Pausa pranzo produttiva</string>
<string name="achievement_lunch_win_desc">Vinci tra le 12:00 e 14:00</string>
<string name="achievement_sunday_player_name">Domenica rilassata</string>
<string name="achievement_sunday_player_desc">Vinci di domenica</string>
<string name="achievement_night_owl_3_name">Nottambulo accanito</string>
<string name="achievement_night_owl_3_desc">Gioca 3 partite consecutive dopo mezzanotte</string>
<string name="achievement_same_day_3_name">Non mi fermo</string>
<string name="achievement_same_day_3_desc">3 vittorie nello stesso giorno</string>
<string name="achievement_same_day_5_name">Giornata da pro</string>
<string name="achievement_same_day_5_desc">5 partite giocate nello stesso giorno</string>
<string name="achievement_new_year_eve_name">Aspettando la mezzanotte</string>
<string name="achievement_new_year_eve_desc">Gioca una partita il 31 dicembre</string>
<!-- festività -->
<string name="achievement_new_year_name">Anno nuovo, carte nuove</string>
<string name="achievement_new_year_desc">Apri l\'app il 1° gennaio</string>
<string name="achievement_halloween_name">Dolcetto o scherzetto</string>
<string name="achievement_halloween_desc">Apri l\'app il 31 ottobre</string>
<string name="achievement_ferragosto_name">Ferragosto</string>
<string name="achievement_ferragosto_desc">Apri l\'app il 15 agosto</string>
```

---

## Test da aggiungere

Per ogni nuova condizione aggiungere un test in `AchievementEngineTest.kt` che verifica:
- La condizione **positiva** (achievement sbloccato)
- La condizione **negativa** (achievement non sbloccato con dati borderline)

Esempi chiave:
- `loss_10`: lista di 10 `won=false` → candidato; 9 false + 1 true → non candidato
- `perfectionist`: 3 win con hints=0 autoMoves=0 → candidato; 2 win + 1 loss → non candidato
- `same_day_3`: 3 wins stesso giorno → candidato; 3 wins giorni diversi → non candidato
- `big_loser`: totalGames=150, totalWins=49 (losses=101) → candidato

---

## Fix banner APP_OPENED (timing)

**Problema rilevato:** In `MainActivity.onCreate()` il banner viene accodato immediatamente, ma:
1. Il layout non è ancora stato misurato — `bannerRoot.height == 0`, il banner parte da `-200f` invece dell'altezza reale
2. La transizione `fade_in` da `SplashActivity` dura ~300ms — il banner si sovrappone alla dissolvenza

**Fix in `MainActivity.onCreate()`:**
```kotlin
// Prima (problematico)
achievementBanner.enqueue(newAchievements)

// Dopo
bannerRoot.postDelayed({ achievementBanner.enqueue(newAchievements) }, 500L)
```

I 500ms coprono sia la fine della transizione che il primo layout pass del menu.

---

## Impatto

- Nessuna modifica al DB
- Fix minore in `MainActivity.onCreate()` per il timing del banner
- `stats_trophies_header` passa da `34` a `55` (stringa con `%1$d` — si aggiorna automaticamente)
- Retrocompatibilità completa: gli achievement non sbloccati restano locked per chi ha già la app
