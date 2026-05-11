# UI Casino Redesign — v1.11 + residui Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) o superpowers:executing-plans. Steps usano checkbox (`- [ ]`) syntax. Branch già esistente: continua sopra di esso.

**Goal:** Completare il residuo di v1.10 (watermark main menu), la release v1.11 completa (polish in-game P1 + top bar C1 + hint engine + win screen W3 + stats DB), e il residuo di v1.12 (auto-move + toggle). Bump versione **1.10.0 → 1.11.0** (versionCode 22).

**Architecture:** Android XML/Views, paradigma esistente. Niente nuove dipendenze di build (resta `mavenCentral()` solo). JSON serialization già disponibile (introdotta in v1.10 per Resume).

**Tech Stack:** Kotlin, AndroidX, ConstraintLayout, ObjectAnimator, SQLite via SQLiteOpenHelper (esistente).

**Spec di riferimento:** `docs/implementation/2026-05-11-ui-casino-redesign-design.md`
**Stato precedente:** `docs/implementation/2026-05-11-ui-casino-redesign-v1.10-plan.md` (eseguito; 43 commit su `feature/v1.10-casino-redesign` dopo `main`)

**Branch:** continua su `feature/v1.10-casino-redesign`. Niente fork: la nomenclatura del branch è puramente storica.

**Commit policy:** 1 commit per task verificabile + build verify obbligatoria (`./gradlew assembleDebug` deve passare). **NESSUN trailer "Co-Authored-By: Claude"** nei commit.

**Open Questions risolte (default da spec sezione 10):**
- Q3 Toggle "Suggerimenti" default → **ON**
- Q4 Auto-move default → **OFF**
- Q5 Suono "Nuovo record!" → **SI**, riusa `R.raw.youwin` esistente (no pitch shift, audio identico al normale "vinci")
- Q7 Migrazione utenti v1.x stats → mantieni `consecutive_wins`; `best_time` parte da `null`; `total_wins` parte da `0` per utenti esistenti

**Caveat noto da v1.10:** dopo modifiche a drawables (in particolare swap PNG→XML), AAPT può segnalare `error: resource drawable/X not found` per cache di merge stale. Soluzione: `./gradlew clean` una volta, poi `assembleDebug`. Documentato nel piano predecessore.

---

## Pre-flight

### Task 0: Verifica stato branch

**Files:** N/A (operazioni git)

- [ ] **Step 0.1: Verifica branch corrente**

```
git branch --show-current
```
Expected: `feature/v1.10-casino-redesign`. Se diverso, `git checkout feature/v1.10-casino-redesign`.

- [ ] **Step 0.2: Verifica build verde dopo clean**

```
./gradlew clean assembleDebug
```
Expected: `BUILD SUCCESSFUL`. Se fallisce: STOP, investigare, segnalare BLOCKED.

- [ ] **Step 0.3: Tag baseline per rollback**

```
git tag -a v1.10-pre-v1.11 -m "Baseline before v1.11 polish + hint + stats"
```

---

## Phase R — Residuo v1.10: Watermark main menu

### Task R.1: Stringa watermark

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-it/strings.xml`
- Modify: `app/src/main/res/values-pt/strings.xml`

- [ ] **Step R.1.1: Aggiungere stringa in default**

In `app/src/main/res/values/strings.xml`, prima di `</resources>`:
```xml
    <string name="watermark_studio" translatable="false">Bottazzini Softworks</string>
```

- [ ] **Step R.1.2: Aggiungere in it e pt** (stessa stringa, `translatable="false"` rende inutile la duplicazione ma per coerenza aggiungiamo)

In `values-it/strings.xml` e `values-pt/strings.xml`:
```xml
    <string name="watermark_studio" translatable="false">Bottazzini Softworks</string>
```

- [ ] **Step R.1.3: Commit**

```
git add app/src/main/res/values/strings.xml app/src/main/res/values-it/strings.xml app/src/main/res/values-pt/strings.xml
git commit -m "feat(i18n): add watermark string for main menu"
```

### Task R.2: TextView watermark nel main menu

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`

- [ ] **Step R.2.1: Aggiungere il TextView watermark in fondo al ConstraintLayout**

Aggiungere prima del tag chiusura `</androidx.constraintlayout.widget.ConstraintLayout>`, dopo l'ultimo `<LinearLayout>` (tileSettings):

```xml
        <TextView
            android:id="@+id/textViewWatermark"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginBottom="8dp"
            android:text="@string/watermark_studio"
            android:textSize="9sp"
            android:fontFamily="serif"
            android:textStyle="italic"
            android:textColor="@color/casino_gold_alpha_25"
            android:alpha="0.6"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent" />
```

NOTA: `casino_gold_alpha_25` × `alpha=0.6` = effettivo ~15% opaco. Molto discreto.

NOTA 2: il `tileSettings` esistente ha `app:layout_constraintBottom_toBottomOf="parent"`. Per evitare conflitti, sostituire quella constraint del tile con `app:layout_constraintBottom_toTopOf="@id/textViewWatermark"` e aggiungere `android:layout_marginBottom="8dp"` se non c'è già.

- [ ] **Step R.2.2: Build verify**

```
./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step R.2.3: Commit**

```
git add app/src/main/res/layout/activity_main.xml
git commit -m "feat(menu): add discreet 'Bottazzini Softworks' watermark to main menu"
```

---

## Phase S — DB schema upgrade per stats v1.11

Prima di v1.11 features, alziamo lo schema del DB per le nuove statistiche. Questo permette di scrivere stats sui giochi attuali e di abilitare la migrazione utenti v1.10→v1.11.

### Task S.1: Aggiungere colonne best_time + total_wins

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/db/columns/RecordsColumns.kt` (potrebbe non esistere, verificare prima)
- Modify: `app/src/main/java/com/bottazzini/trasloco/db/DatabaseHandler.kt`
- Modify: `app/src/main/java/com/bottazzini/trasloco/settings/RecordsHandler.kt`

Lo schema attuale (v2) ha tabella `records` con colonne: `name TEXT`, `value INTEGER`, `is_new INTEGER`, `current_value INTEGER`. È usata per `consecutive_wins`.

Per v1.11 aggiungiamo due "tipi di record" come righe nella stessa tabella, NON nuove colonne. Approccio: usare la chiave `name` con valori `"best_time"` e `"total_wins"`. Cambia solo come `RecordsHandler` li scrive/legge.

Schema bump: da `DATABASE_VERSION = 2` a `3`. Migrazione idempotente (le righe nuove si inseriscono on-demand).

- [ ] **Step S.1.1: Leggere `RecordsHandler.kt` per capire l'API esistente**

```
cat app/src/main/java/com/bottazzini/trasloco/settings/RecordsHandler.kt
```

Identificare: come legge/scrive `consecutive_wins`. Probabilmente con metodi tipo `getConsecutiveWins()`, `incrementConsecutiveWins()`, `resetConsecutiveWins()`. Verifica.

- [ ] **Step S.1.2: Aggiungere bump version in DatabaseHandler**

In `app/src/main/java/com/bottazzini/trasloco/db/DatabaseHandler.kt`:

```kotlin
private const val DATABASE_VERSION = 3
```
(precedente: `2`)

Aggiornare `onUpgrade` per gestire migration 2→3 in modo non-distruttivo. Sostituire il blocco `onUpgrade` esistente con:

```kotlin
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        Log.w(
            TAG, "onUpgrade: from version $oldVersion to $newVersion"
        )
        if (oldVersion < 3) {
            // v3: no schema changes — just bump.
            // New records 'best_time' and 'total_wins' inserted on-demand by RecordsHandler.
        }
        // For destructive upgrades only:
        if (oldVersion < 2) {
            db?.execSQL(SQL_DELETE_RECORDS)
            db?.execSQL(SQL_DELETE_SETTINGS)
            onCreate(db)
        }
    }
```

NOTA: l'oldVersion `<2` branch è per device pre-v1.9 (probabilmente irrilevante). Il punto è non eseguire DROP TABLE durante 2→3.

- [ ] **Step S.1.3: Aggiungere metodi in RecordsHandler**

In `app/src/main/java/com/bottazzini/trasloco/settings/RecordsHandler.kt`, aggiungere (struttura — adattare ai pattern esistenti del file):

```kotlin
    // Best time = millis (Long). null se nessun record (nessuna vittoria ancora).
    fun getBestTime(): Long? {
        // SELECT current_value FROM records WHERE name = 'best_time'
        // return null if no row exists
        // implementare seguendo il pattern di getConsecutiveWins
    }

    // Aggiorna se nuovoTempo < bestCorrente OR best non esiste.
    // Returns true se è un nuovo record (per trigger UI "Nuovo Record!").
    fun maybeUpdateBestTime(newTime: Long): Boolean {
        val current = getBestTime()
        if (current == null || newTime < current) {
            // INSERT OR UPDATE record name='best_time', value=newTime
            // (usare il pattern existente di RecordsHandler per scrivere)
            return true
        }
        return false
    }

    // Total wins counter (default 0).
    fun getTotalWins(): Long {
        // SELECT current_value FROM records WHERE name = 'total_wins'; return 0 if missing
    }

    fun incrementTotalWins() {
        val current = getTotalWins()
        // INSERT OR UPDATE record name='total_wins', value=current+1
    }
```

L'implementazione concreta dipende dal pattern già usato per `consecutive_wins`. Leggere prima il file e replicare.

- [ ] **Step S.1.4: Build verify + commit**

```
./gradlew assembleDebug
git add app/src/main/java/com/bottazzini/trasloco/db/DatabaseHandler.kt app/src/main/java/com/bottazzini/trasloco/settings/RecordsHandler.kt
git commit -m "feat(stats): add best_time and total_wins to records DB (schema v3)"
```

---

## Phase H — Hint Engine + Top Bar C1

Top bar C1: barra in alto con 3 icone (back-to-menu sx, timer al centro, pausa + hint dx). Sostituisce il singolo TextView timer attuale.

Hint Engine: trova la prima mossa valida e la evidenzia con pulse dorato per 1.5s. Toggle "Suggerimenti" in settings (default ON) abilita/disabilita la disponibilità del bottone.

### Task H.1: Stringhe top bar + hint

**Files:**
- Modify: `values/strings.xml`, `values-it/strings.xml`, `values-pt/strings.xml`

- [ ] **Step H.1.1: Aggiungere stringhe**

values/strings.xml:
```xml
    <string name="settings_label_hint">Hints</string>
    <string name="settings_label_auto_move">Auto-move to final deck</string>
    <string name="hint_no_moves">No moves available</string>
    <string name="game_paused">PAUSED</string>
    <string name="tap_to_resume">Tap to resume</string>
    <string name="cd_back">Back to menu</string>
    <string name="cd_pause">Pause/Resume</string>
    <string name="cd_hint">Hint</string>
    <string name="hint_with_icon">💡 Hints</string>
    <string name="auto_move_with_icon">🎯 Auto-move to final deck</string>
```

values-it/strings.xml:
```xml
    <string name="settings_label_hint">Suggerimenti</string>
    <string name="settings_label_auto_move">Auto-muovi su end deck</string>
    <string name="hint_no_moves">Nessuna mossa disponibile</string>
    <string name="game_paused">PAUSATO</string>
    <string name="tap_to_resume">Tap per riprendere</string>
    <string name="cd_back">Torna al menu</string>
    <string name="cd_pause">Pausa/Riprendi</string>
    <string name="cd_hint">Suggerimento</string>
    <string name="hint_with_icon">💡 Suggerimenti</string>
    <string name="auto_move_with_icon">🎯 Auto-muovi su end deck</string>
```

values-pt/strings.xml:
```xml
    <string name="settings_label_hint">Dicas</string>
    <string name="settings_label_auto_move">Auto-mover para baralho final</string>
    <string name="hint_no_moves">Sem jogadas disponíveis</string>
    <string name="game_paused">PAUSADO</string>
    <string name="tap_to_resume">Toque para continuar</string>
    <string name="cd_back">Voltar ao menu</string>
    <string name="cd_pause">Pausa/Continuar</string>
    <string name="cd_hint">Dica</string>
    <string name="hint_with_icon">💡 Dicas</string>
    <string name="auto_move_with_icon">🎯 Auto-mover para baralho final</string>
```

- [ ] **Step H.1.2: Commit**

```
git add app/src/main/res/values/strings.xml app/src/main/res/values-it/strings.xml app/src/main/res/values-pt/strings.xml
git commit -m "feat(i18n): add strings for top bar, hint, pause overlay, settings v1.11"
```

### Task H.2: Configuration enum + default settings

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/settings/SettingsHandler.kt`

- [ ] **Step H.2.1: Estendere enum Configuration**

In `SettingsHandler.kt`, l'enum `Configuration` attualmente ha: `FAST_DEAL`, `CARD_BACK`, `BACKGROUND`, `CARD_TYPE`. Aggiungere:

```kotlin
enum class Configuration(val value: String) {
    FAST_DEAL("fastDeal"),
    CARD_BACK("cardBack"),
    BACKGROUND("background"),
    CARD_TYPE("cardType"),
    HINT_ENABLED("hintEnabled"),
    AUTO_MOVE("autoMove")
}
```

- [ ] **Step H.2.2: Estendere `insertDefaultSettings`**

Nel metodo `insertDefaultSettings()`, aggiungere:

```kotlin
        setDefaultSetting(Configuration.HINT_ENABLED.value, "enabled")  // default ON
        setDefaultSetting(Configuration.AUTO_MOVE.value, "disabled")    // default OFF
```

- [ ] **Step H.2.3: Build + commit**

```
./gradlew assembleDebug
git add app/src/main/java/com/bottazzini/trasloco/settings/SettingsHandler.kt
git commit -m "feat(settings): add HINT_ENABLED (ON) and AUTO_MOVE (OFF) defaults"
```

### Task H.3: Drawable icone tonde top bar

**Files:**
- Create: `app/src/main/res/drawable/casino_icon_button.xml`

- [ ] **Step H.3.1: Drawable bottone tondo dorato**

Creare `app/src/main/res/drawable/casino_icon_button.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_pressed="true">
        <shape android:shape="oval">
            <solid android:color="@color/casino_gold_alpha_25" />
            <stroke android:width="1dp" android:color="@color/casino_gold" />
        </shape>
    </item>
    <item android:state_enabled="false">
        <shape android:shape="oval">
            <solid android:color="@android:color/transparent" />
            <stroke android:width="1dp" android:color="@color/casino_gold_alpha_25" />
        </shape>
    </item>
    <item>
        <shape android:shape="oval">
            <solid android:color="@color/casino_gold_alpha_08" />
            <stroke android:width="1dp" android:color="@color/casino_gold_alpha_50" />
        </shape>
    </item>
</selector>
```

- [ ] **Step H.3.2: Drawable pill timer**

Creare `app/src/main/res/drawable/casino_timer_pill.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#80000000" />
    <stroke android:width="1dp" android:color="@color/casino_gold_alpha_50" />
    <corners android:radius="14dp" />
</shape>
```

- [ ] **Step H.3.3: Commit**

```
git add app/src/main/res/drawable/casino_icon_button.xml app/src/main/res/drawable/casino_timer_pill.xml
git commit -m "feat(theme): add casino round icon button + timer pill drawables"
```

### Task H.4: Riscrivere top bar in game.xml + game-land.xml

**Files:**
- Modify: `app/src/main/res/layout/game.xml`
- Modify: `app/src/main/res/layout-land/game.xml`

Il layout attuale ha probabilmente un singolo `TextView` `textViewGameTimer` in cima. Da sostituire con:
- ImageView/TextView icona `←` (back) a sinistra (id `iconBack`)
- Timer al centro (id `textViewGameTimer`, con background pill)
- ImageView/TextView icona `⏸` (pausa) a destra (id `iconPause`)
- ImageView/TextView icona `💡` (hint) accanto a pausa (id `iconHint`)

- [ ] **Step H.4.1: Leggere il layout attuale**

```
sed -n '1,60p' app/src/main/res/layout/game.xml
```

Identificare la struttura attuale del timer.

- [ ] **Step H.4.2: Sostituire timer-only con top bar**

Trovare il blocco TextView `textViewGameTimer` in game.xml e in game-land.xml. Sostituirlo con:

```xml
        <LinearLayout
            android:id="@+id/topBar"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:layout_marginHorizontal="12dp"
            android:gravity="center_vertical"
            android:orientation="horizontal"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent">

            <TextView
                android:id="@+id/iconBack"
                android:layout_width="40dp"
                android:layout_height="40dp"
                android:background="@drawable/casino_icon_button"
                android:gravity="center"
                android:text="←"
                android:textSize="20sp"
                android:textColor="@color/casino_gold"
                android:contentDescription="@string/cd_back"
                android:onClick="onClickBack"
                android:clickable="true"
                android:focusable="true" />

            <TextView
                android:id="@+id/textViewGameTimer"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:layout_marginHorizontal="8dp"
                android:background="@drawable/casino_timer_pill"
                android:gravity="center"
                android:paddingHorizontal="14dp"
                android:paddingVertical="6dp"
                android:text="00:00"
                android:textSize="14sp"
                android:textStyle="italic"
                android:fontFamily="serif"
                android:textColor="@color/casino_gold" />

            <TextView
                android:id="@+id/iconPause"
                android:layout_width="40dp"
                android:layout_height="40dp"
                android:layout_marginEnd="6dp"
                android:background="@drawable/casino_icon_button"
                android:gravity="center"
                android:text="⏸"
                android:textSize="18sp"
                android:textColor="@color/casino_gold"
                android:contentDescription="@string/cd_pause"
                android:onClick="onClickPause"
                android:clickable="true"
                android:focusable="true" />

            <TextView
                android:id="@+id/iconHint"
                android:layout_width="40dp"
                android:layout_height="40dp"
                android:background="@drawable/casino_icon_button"
                android:gravity="center"
                android:text="💡"
                android:textSize="18sp"
                android:contentDescription="@string/cd_hint"
                android:onClick="onClickHint"
                android:clickable="true"
                android:focusable="true" />
        </LinearLayout>
```

Aggiornare i constraint dei view sotto (subdeck/cardTable rows) per agganciarsi a `@id/topBar` invece che al vecchio textViewGameTimer.

- [ ] **Step H.4.3: Stesso in layout-land/game.xml**

Stesso ribaltamento: rimuovere il vecchio timer, inserire la top bar in cima.

- [ ] **Step H.4.4: Build + commit**

```
./gradlew assembleDebug
git add app/src/main/res/layout/game.xml app/src/main/res/layout-land/game.xml
git commit -m "feat(game): replace single timer with top bar C1 (back/timer/pause/hint)"
```

### Task H.5: Pause overlay drawable + layout

**Files:**
- Create: `app/src/main/res/drawable/casino_pause_overlay_bg.xml`

- [ ] **Step H.5.1: Drawable overlay scuro semi-trasparente**

Creare `app/src/main/res/drawable/casino_pause_overlay_bg.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#CC000000" />
</shape>
```

- [ ] **Step H.5.2: Aggiungere overlay nel game.xml**

In `app/src/main/res/layout/game.xml` (e land), aggiungere come ULTIMO figlio del ConstraintLayout root (sopra tutto):

```xml
        <LinearLayout
            android:id="@+id/pauseOverlay"
            android:layout_width="0dp"
            android:layout_height="0dp"
            android:background="@drawable/casino_pause_overlay_bg"
            android:gravity="center"
            android:orientation="vertical"
            android:visibility="gone"
            android:clickable="true"
            android:focusable="true"
            android:onClick="onClickResumeFromPause"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/game_paused"
                android:textSize="42sp"
                android:textStyle="italic"
                android:fontFamily="serif"
                android:textColor="@color/casino_gold"
                android:letterSpacing="0.3" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="12dp"
                android:text="@string/tap_to_resume"
                android:textSize="14sp"
                android:fontFamily="serif"
                android:textColor="@color/casino_gold_alpha_50" />
        </LinearLayout>
```

- [ ] **Step H.5.3: Commit**

```
git add app/src/main/res/drawable/casino_pause_overlay_bg.xml app/src/main/res/layout/game.xml app/src/main/res/layout-land/game.xml
git commit -m "feat(game): add pause overlay UI"
```

### Task H.6: HintEngine class

**Files:**
- Create: `app/src/main/java/com/bottazzini/trasloco/HintEngine.kt`

L'algoritmo dipende dalle regole di Trasloco. **Prima di scrivere**, l'AI executor deve leggere `GameActivity.kt` e identificare le funzioni di validazione delle mosse (probabilmente nomi come `isValidMove`, `canMoveToEndDeck`, `canStackOn`, ecc.). Adottare la stessa logica.

Pseudo-algoritmo:
```
1. Per ogni carta visibile in cima a cardTableMap (16 slot), in ordine deterministico:
   1a. Per ogni endDeck (4 slot), verifica se la carta può andarci
       (logica: se endDeck è vuoto e la carta è un Asso dello stesso seme del deck;
        oppure se endDeck top + 1 = carta, stesso seme)
       Se sì → ritorna (cardOrigin, endDeckTarget)
   1b. Per ogni altro slot tavolo (15 altri), verifica se la carta può accatastarsi
       (logica: top destinazione = carta + 1, stesso seme)
       Se sì → ritorna (cardOrigin, tableSlotTarget)
2. Se nessuna mossa trovata → ritorna null
```

- [ ] **Step H.6.1: Scrivere HintEngine**

Creare `app/src/main/java/com/bottazzini/trasloco/HintEngine.kt`:

```kotlin
package com.bottazzini.trasloco

data class HintMove(
    val sourceCardId: String,        // es. "b7"
    val sourceSlotId: Int,           // R.id.cardTable11 oppure R.id.subDeck1
    val targetSlotId: Int,           // R.id.endDeckB oppure R.id.cardTable22
    val targetType: HintTargetType
)

enum class HintTargetType { END_DECK, TABLE_STACK }

class HintEngine {

    /**
     * Trova la prima mossa valida disponibile.
     * @return HintMove o null se nessuna mossa è possibile.
     *
     * NOTA: la logica di validazione DEVE coincidere con quella di GameActivity.
     * L'executor: prima di implementare, leggere i metodi di validazione mossa in
     * GameActivity.kt e riutilizzarli (estrarli come fun statiche se necessario).
     */
    fun findFirstValidMove(
        cardTableMap: Map<String, List<String>>,
        endDeckList: Map<String, String>,
        subDeckMap: Map<String, List<String>>
    ): HintMove? {
        // Step 1: prova ogni carta in cima delle 16 colonne tavolo verso endDeck
        for ((tableKey, cards) in cardTableMap) {
            if (cards.isEmpty()) continue
            val topCard = cards.last()
            for ((endDeckKey, endDeckTopCard) in endDeckList) {
                if (canMoveToEndDeck(topCard, endDeckKey, endDeckTopCard)) {
                    return HintMove(
                        sourceCardId = topCard,
                        sourceSlotId = resolveTableSlotId(tableKey),
                        targetSlotId = resolveEndDeckSlotId(endDeckKey),
                        targetType = HintTargetType.END_DECK
                    )
                }
            }
        }
        // Step 2: prova ogni carta verso un altro slot tavolo
        for ((sourceKey, sourceCards) in cardTableMap) {
            if (sourceCards.isEmpty()) continue
            val topCard = sourceCards.last()
            for ((targetKey, targetCards) in cardTableMap) {
                if (sourceKey == targetKey) continue
                if (targetCards.isEmpty()) continue
                val targetTop = targetCards.last()
                if (canStackOn(topCard, targetTop)) {
                    return HintMove(
                        sourceCardId = topCard,
                        sourceSlotId = resolveTableSlotId(sourceKey),
                        targetSlotId = resolveTableSlotId(targetKey),
                        targetType = HintTargetType.TABLE_STACK
                    )
                }
            }
        }
        return null
    }

    // canMoveToEndDeck: card può andare nell'endDeck identificato da endDeckKey?
    // endDeckTopCard è la carta in cima all'endDeck (o stringa vuota se vuoto)
    private fun canMoveToEndDeck(card: String, endDeckKey: String, endDeckTopCard: String): Boolean {
        // Implementare in coerenza con GameActivity. Esempio probabile:
        // - Se endDeckTopCard è vuoto, card deve essere "X1" dove X coincide col seme del deck
        //   (endDeckKey probabilmente codifica il seme: "endDeckB" per bastoni, etc.)
        // - Altrimenti, card deve essere stesso seme + valore = top+1
        // L'AI executor: leggere il metodo esistente di GameActivity e replicare qui.
        return false  // placeholder — DEVE essere implementato leggendo GameActivity
    }

    // canStackOn: card può essere posata su targetTop (sul tavolo, sequenza decrescente stesso seme)?
    private fun canStackOn(card: String, targetTop: String): Boolean {
        // Logica: stesso seme && valore(card) = valore(targetTop) - 1
        // I valori sono in stringa "b1".."b10" — il prefisso è il seme, il suffisso è il numero.
        return false  // placeholder — implementare
    }

    private fun resolveTableSlotId(tableKey: String): Int {
        // tableKey es. "11", "12"... "44". Restituire R.id.cardTable{tableKey}.
        // Implementare con resources.getIdentifier o switch lookup.
        return 0  // placeholder
    }

    private fun resolveEndDeckSlotId(endDeckKey: String): Int {
        return 0  // placeholder
    }
}
```

L'AI executor: completare i metodi `canMoveToEndDeck`, `canStackOn`, `resolveTableSlotId`, `resolveEndDeckSlotId` leggendo le regole esatte da `GameActivity.kt`.

- [ ] **Step H.6.2: Build + commit**

```
./gradlew assembleDebug
git add app/src/main/java/com/bottazzini/trasloco/HintEngine.kt
git commit -m "feat(game): add HintEngine class (find-first-valid-move algorithm)"
```

### Task H.7: Drawable hint pulse animation

**Files:**
- Create: `app/src/main/res/drawable/hint_pulse.xml`
- Create: `app/src/main/res/anim/hint_pulse_anim.xml`

- [ ] **Step H.7.1: Drawable per highlight hint**

Creare `app/src/main/res/drawable/hint_pulse.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item>
        <shape android:shape="rectangle">
            <stroke android:width="3dp" android:color="@color/casino_gold" />
            <corners android:radius="6dp" />
        </shape>
    </item>
</layer-list>
```

- [ ] **Step H.7.2: Commit**

```
git add app/src/main/res/drawable/hint_pulse.xml
git commit -m "feat(game): add hint pulse highlight drawable"
```

### Task H.8: Integrare HintEngine + Pause + Back in GameActivity

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/GameActivity.kt`

- [ ] **Step H.8.1: Aggiungere campi**

```kotlin
    private lateinit var hintEngine: HintEngine
    private var hintEnabled: Boolean = true
```

In `onCreate`, dopo l'init di `settingsHandler`:
```kotlin
        hintEngine = HintEngine()
        hintEnabled = settingsHandler.readValue(Configuration.HINT_ENABLED.value) == "enabled"
```

- [ ] **Step H.8.2: Aggiungere metodi onClick (referenziati dai bottoni in game.xml)**

```kotlin
    fun onClickBack(view: View) {
        // Save state already happens in onPause if game active.
        finish()
    }

    fun onClickPause(view: View) {
        if (isTimerPaused) resumeTimerAndHideOverlay() else pauseTimerAndShowOverlay()
    }

    fun onClickResumeFromPause(view: View) {
        if (isTimerPaused) resumeTimerAndHideOverlay()
    }

    fun onClickHint(view: View) {
        if (!hintEnabled) {
            android.widget.Toast.makeText(this, "Suggerimenti disabilitati nelle impostazioni", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val hint = hintEngine.findFirstValidMove(cardTableMap, endDeckList, subDeckMap)
        if (hint == null) {
            android.widget.Toast.makeText(this, getString(R.string.hint_no_moves), android.widget.Toast.LENGTH_SHORT).show()
            // vibrate?
            return
        }
        highlightHintMove(hint)
    }

    private fun pauseTimerAndShowOverlay() {
        // pause timer logic — adattare al timer esistente
        isTimerPaused = true
        timerPausedTimeMillis = System.currentTimeMillis()
        timerHandler.removeCallbacks(timerRunnable)
        findViewById<View>(R.id.pauseOverlay).visibility = View.VISIBLE
    }

    private fun resumeTimerAndHideOverlay() {
        isTimerPaused = false
        // adjust gameStartTimeMillis to account for paused interval
        val pausedDuration = System.currentTimeMillis() - timerPausedTimeMillis
        gameStartTimeMillis += pausedDuration
        timerHandler.post(timerRunnable)
        findViewById<View>(R.id.pauseOverlay).visibility = View.GONE
    }

    private fun highlightHintMove(hint: HintMove) {
        val sourceView = findViewById<View>(hint.sourceSlotId) ?: return
        val targetView = findViewById<View>(hint.targetSlotId) ?: return
        sourceView.setBackgroundResource(R.drawable.hint_pulse)
        targetView.setBackgroundResource(R.drawable.hint_pulse)
        // Pulse animation: scale 1 → 1.1 → 1 con repeat=3
        val anim1 = android.animation.ObjectAnimator.ofPropertyValuesHolder(
            sourceView,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 1.1f, 1f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 1.1f, 1f)
        ).apply {
            duration = 500
            repeatCount = 2
        }
        val anim2 = android.animation.ObjectAnimator.ofPropertyValuesHolder(
            targetView,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 1.1f, 1f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 1.1f, 1f)
        ).apply {
            duration = 500
            repeatCount = 2
        }
        anim1.start()
        anim2.start()
        // Remove highlight after 1.5s
        timerHandler.postDelayed({
            sourceView.background = null
            targetView.background = null
        }, 1500)
    }
```

NOTA: l'AI executor deve adattare al timer/handler esistente. Probabilmente `timerRunnable` è già definito; il timing della pausa deve essere consistente con la logica esistente di calcolo del tempo trascorso.

- [ ] **Step H.8.3: Build + commit**

```
./gradlew assembleDebug
git add app/src/main/java/com/bottazzini/trasloco/GameActivity.kt
git commit -m "feat(game): wire HintEngine + pause overlay + back-to-menu into GameActivity"
```

### Task H.9: Toggle "Suggerimenti" e "Auto-muovi" in settings

**Files:**
- Modify: `app/src/main/res/layout/settings.xml`
- Modify: `app/src/main/java/com/bottazzini/trasloco/SettingsActivity.kt`

- [ ] **Step H.9.1: Aggiungere 2 nuove righe toggle nel settings layout**

Sotto l'esistente `fastDealRow` in `settings.xml`, aggiungere due nuove LinearLayout simili:

```xml
        <LinearLayout
            android:id="@+id/hintRow"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:padding="12dp"
            android:background="@drawable/casino_tile_bg"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toBottomOf="@id/fastDealRow">

            <TextView
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="@string/hint_with_icon"
                style="@style/CasinoBodyText" />

            <Switch
                android:id="@+id/switchHint"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:thumb="@drawable/casino_gold_switch_thumb"
                android:track="@drawable/casino_gold_switch_track"
                android:onClick="changeHintEnabled" />
        </LinearLayout>

        <LinearLayout
            android:id="@+id/autoMoveRow"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:padding="12dp"
            android:background="@drawable/casino_tile_bg"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toBottomOf="@id/hintRow">

            <TextView
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="@string/auto_move_with_icon"
                style="@style/CasinoBodyText" />

            <Switch
                android:id="@+id/switchAutoMove"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:thumb="@drawable/casino_gold_switch_thumb"
                android:track="@drawable/casino_gold_switch_track"
                android:onClick="changeAutoMove" />
        </LinearLayout>
```

Anche aggiornare il constraint del `textViewCredits` perché ora puntava a `@id/fastDealRow`: cambiare in `app:layout_constraintTop_toBottomOf="@id/autoMoveRow"`.

- [ ] **Step H.9.2: Aggiungere logica in SettingsActivity**

Aggiungere a `SettingsActivity.kt`:

```kotlin
    fun changeHintEnabled(view: View) {
        val switch = findViewById<Switch>(R.id.switchHint)
        val value = if (switch.isChecked) "enabled" else "disabled"
        settingsHandler.updateSetting(Configuration.HINT_ENABLED.value, value)
    }

    fun changeAutoMove(view: View) {
        val switch = findViewById<Switch>(R.id.switchAutoMove)
        val value = if (switch.isChecked) "enabled" else "disabled"
        settingsHandler.updateSetting(Configuration.AUTO_MOVE.value, value)
    }
```

E in `readConfigurations`:

```kotlin
        val hint = settingsHandler.readValue(Configuration.HINT_ENABLED.value) ?: "enabled"
        findViewById<Switch>(R.id.switchHint).isChecked = (hint == "enabled")

        val autoMove = settingsHandler.readValue(Configuration.AUTO_MOVE.value) ?: "disabled"
        findViewById<Switch>(R.id.switchAutoMove).isChecked = (autoMove == "enabled")
```

- [ ] **Step H.9.3: Build + commit**

```
./gradlew assembleDebug
git add app/src/main/res/layout/settings.xml app/src/main/java/com/bottazzini/trasloco/SettingsActivity.kt
git commit -m "feat(settings): add Hints and Auto-move toggles"
```

---

## Phase P — In-game polish P1

P1 = sobrio (frame sottili oro sui slot, glow soft su carta selezionata già fatto, drop zone illuminata durante drag).

### Task P.1: Drawable slot frame

**Files:**
- Create: `app/src/main/res/drawable/casino_slot_frame.xml`
- Create: `app/src/main/res/drawable/casino_drop_zone_valid.xml`

- [ ] **Step P.1.1: Slot frame (sottile + alpha)**

Creare `app/src/main/res/drawable/casino_slot_frame.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#33000000" />
    <stroke android:width="1dp" android:color="@color/casino_gold_alpha_25" />
    <corners android:radius="4dp" />
</shape>
```

- [ ] **Step P.1.2: Drop zone valida (highlight oro)**

Creare `app/src/main/res/drawable/casino_drop_zone_valid.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@color/casino_gold_alpha_25" />
            <stroke android:width="1.5dp" android:color="@color/casino_gold" />
            <corners android:radius="4dp" />
        </shape>
    </item>
</layer-list>
```

- [ ] **Step P.1.3: Commit**

```
git add app/src/main/res/drawable/casino_slot_frame.xml app/src/main/res/drawable/casino_drop_zone_valid.xml
git commit -m "feat(theme): add slot frame and valid drop zone drawables"
```

### Task P.2: Applicare slot frame in game.xml

**Files:**
- Modify: `app/src/main/res/layout/game.xml`
- Modify: `app/src/main/res/layout-land/game.xml`

- [ ] **Step P.2.1: Aggiungere `android:background="@drawable/casino_slot_frame"` su tutti gli ImageView slot**

Tutti gli ImageView dei 4 endDeck (`endDeck1`/`endDeckB`/`endDeckC`/etc — verificare i nomi reali) e dei 16 cardTable slot devono avere il frame come background (oltre al src delle carte).

Tecnica: edit XML, cerca/sostituisci per ogni ImageView slot. Il pattern è:
```xml
<ImageView
    android:id="@+id/subDeck11"
    ...
    android:background="@drawable/casino_slot_frame"
    app:srcCompat="@drawable/zero" />
```

Verifica visivamente che il frame appaia su tutti gli slot (non solo dove c'è una carta).

- [ ] **Step P.2.2: Build + commit**

```
./gradlew assembleDebug
git add app/src/main/res/layout/game.xml app/src/main/res/layout-land/game.xml
git commit -m "feat(game): add casino slot frames to all card slots"
```

### Task P.3: Drop zone glow durante drag

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/GameActivity.kt`

Il drag&drop attuale è gestito in GameActivity (drag-touch handlers, ghost view, etc.). Il piano: durante un drag, quando l'utente passa sopra un drop target valido, evidenziarlo con `casino_drop_zone_valid`.

- [ ] **Step P.3.1: Identificare la logica di drag hover**

Il codice esistente ha `dragHoverTarget: View? = null`. Si aggiorna durante `MotionEvent.ACTION_MOVE` per identificare il drop target sotto il dito.

Modificare la logica di hover:
- Quando un nuovo target diventa hover: applicare `setBackgroundResource(R.drawable.casino_drop_zone_valid)` SE il drop sarebbe valido; ripristinare `casino_slot_frame` sul precedente.
- Quando il drag finisce (ACTION_UP): ripristinare tutti gli slot a `casino_slot_frame`.

Pseudo-code dell'inserzione:
```kotlin
// Inside drag MOVE handler:
val newTarget = findSlotUnderTouch(event)
if (newTarget != dragHoverTarget) {
    // Reset old
    dragHoverTarget?.setBackgroundResource(R.drawable.casino_slot_frame)
    // Set new (only if valid drop)
    if (newTarget != null && isValidDropTarget(dragTouchView, newTarget)) {
        newTarget.setBackgroundResource(R.drawable.casino_drop_zone_valid)
    }
    dragHoverTarget = newTarget
}

// In ACTION_UP/ACTION_CANCEL handler:
dragHoverTarget?.setBackgroundResource(R.drawable.casino_slot_frame)
dragHoverTarget = null
```

L'AI executor: trovare le esatte chiamate ai listener di drag in GameActivity e iniettare lì.

- [ ] **Step P.3.2: Build + commit**

```
./gradlew assembleDebug
git add app/src/main/java/com/bottazzini/trasloco/GameActivity.kt
git commit -m "feat(game): highlight valid drop zones during drag"
```

### Task P.4: Smooth card move animations

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/GameActivity.kt`

Quando una carta si muove (per click-source/click-target o per drag&drop), aggiungere uno smooth slide invece di teletrasporto.

- [ ] **Step P.4.1: Funzione helper `animateCardMove`**

Aggiungere a GameActivity:

```kotlin
    private fun animateCardMove(sourceView: View, targetView: View, onComplete: () -> Unit) {
        val source = IntArray(2).also { sourceView.getLocationOnScreen(it) }
        val target = IntArray(2).also { targetView.getLocationOnScreen(it) }
        val dx = (target[0] - source[0]).toFloat()
        val dy = (target[1] - source[1]).toFloat()

        sourceView.animate()
            .translationX(dx)
            .translationY(dy)
            .setDuration(200)
            .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
            .withEndAction {
                sourceView.translationX = 0f
                sourceView.translationY = 0f
                onComplete()
            }
            .start()
    }
```

- [ ] **Step P.4.2: Chiamare `animateCardMove` quando una carta passa da slot a slot**

Trovare i punti in cui GameActivity attualmente fa `srcCompat` swap diretto su una mossa click (non drag). Wrap in `animateCardMove`:

Pseudo:
```kotlin
// Prima:
targetImageView.setImageResource(getCardDrawable(card))
sourceImageView.setImageResource(...)

// Dopo:
animateCardMove(sourceImageView, targetImageView) {
    targetImageView.setImageResource(getCardDrawable(card))
    sourceImageView.setImageResource(...)
}
```

NOTA: questo è delicato perché il drag handler probabilmente fa già un movimento manuale. Applicare solo a mosse triggered da CLICK (selezione carta + click target), non a drag-drop (dove il ghost è già visibile).

- [ ] **Step P.4.3: Build + commit**

```
./gradlew assembleDebug
git add app/src/main/java/com/bottazzini/trasloco/GameActivity.kt
git commit -m "feat(game): smooth slide animation on click-based card moves"
```

---

## Phase A — Auto-move (residuo v1.12)

### Task A.1: Algoritmo detect uniquely placeable card

L'auto-move scatta dopo OGNI mossa valida. Logica:
- Per ogni endDeck, calcola la "carta successiva" (es. se endDeck=`b5`, successiva è `b6`)
- Se quella carta è disponibile in cima di UN SOLO slot del tavolo, muovila automaticamente
- Loop finché nessuna carta soddisfa la condizione

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/GameActivity.kt`

- [ ] **Step A.1.1: Aggiungere logica auto-move**

Aggiungere a GameActivity:

```kotlin
    private var autoMoveEnabled: Boolean = false

    // In onCreate, dopo settings init:
    // autoMoveEnabled = settingsHandler.readValue(Configuration.AUTO_MOVE.value) == "enabled"

    /**
     * Dopo ogni mossa, controlla se c'è una carta univocamente piazzabile in endDeck.
     * Se sì, la muove con animazione. Loop fino a esaurimento.
     */
    private fun triggerAutoMoveCycle() {
        if (!autoMoveEnabled) return
        val nextMove = findUniquelyPlaceableCard() ?: return
        executeAutoMove(nextMove) {
            // Recursive after animation completes
            timerHandler.postDelayed({ triggerAutoMoveCycle() }, 350)
        }
    }

    private fun findUniquelyPlaceableCard(): Pair<String, String>? {
        // Return (sourceTableKey, endDeckKey) for the move, or null.
        // Implementare:
        // - Per ogni endDeck, identifica la carta successiva attesa
        // - Cerca quante volte essa appare come top di un tableSlot
        // - Se esattamente 1, ritornala
        return null  // placeholder
    }

    private fun executeAutoMove(move: Pair<String, String>, onComplete: () -> Unit) {
        // Eseguire la mossa con animazione (riusare animateCardMove).
        // Aggiornare cardTableMap e endDeckList.
        // Suono soft.
        onComplete()
    }
```

L'AI executor: completare `findUniquelyPlaceableCard` e `executeAutoMove` leggendo l'esistente logica di mossa di GameActivity.

- [ ] **Step A.1.2: Hook auto-move dopo ogni mossa utente**

Trovare il punto in GameActivity in cui una mossa utente viene confermata e i map aggiornati. Subito dopo, chiamare:

```kotlin
        triggerAutoMoveCycle()
```

- [ ] **Step A.1.3: Build + commit**

```
./gradlew assembleDebug
git add app/src/main/java/com/bottazzini/trasloco/GameActivity.kt
git commit -m "feat(game): auto-move uniquely placeable cards to end deck when enabled"
```

---

## Phase W — Win screen W3

Win screen W3: trofeo grande + stat card (tempo / best time / striscia / totale vittorie) + gif party piccola + bottoni "Nuova partita" e "Menu". Quando il tempo batte il best precedente, mostra sparkle + suono dedicato.

### Task W.1: Strings per win screen W3

**Files:**
- Modify: `values/strings.xml`, `values-it/strings.xml`, `values-pt/strings.xml`

- [ ] **Step W.1.1: Aggiungere strings**

values/strings.xml:
```xml
    <string name="win_title">Victory!</string>
    <string name="win_stat_time">⏱ Time</string>
    <string name="win_stat_best">🏅 Best time</string>
    <string name="win_stat_streak">🔥 Streak</string>
    <string name="win_stat_total">📊 Total wins</string>
    <string name="win_new_record">New record! ✨</string>
    <string name="win_button_new">▶ New game</string>
    <string name="win_button_menu">← Menu</string>
    <string name="lose_title">You lost...</string>
    <string name="lose_button_retry">↻ Retry</string>
    <string name="streak_format">%1$s in a row</string>
```

values-it/strings.xml:
```xml
    <string name="win_title">Vittoria!</string>
    <string name="win_stat_time">⏱ Tempo</string>
    <string name="win_stat_best">🏅 Miglior tempo</string>
    <string name="win_stat_streak">🔥 Striscia</string>
    <string name="win_stat_total">📊 Vittorie totali</string>
    <string name="win_new_record">Nuovo record! ✨</string>
    <string name="win_button_new">▶ Nuova partita</string>
    <string name="win_button_menu">← Menu</string>
    <string name="lose_title">Hai perso...</string>
    <string name="lose_button_retry">↻ Riprova</string>
    <string name="streak_format">%1$s di fila</string>
```

values-pt/strings.xml:
```xml
    <string name="win_title">Vitória!</string>
    <string name="win_stat_time">⏱ Tempo</string>
    <string name="win_stat_best">🏅 Melhor tempo</string>
    <string name="win_stat_streak">🔥 Sequência</string>
    <string name="win_stat_total">📊 Vitórias totais</string>
    <string name="win_new_record">Novo recorde! ✨</string>
    <string name="win_button_new">▶ Nova partida</string>
    <string name="win_button_menu">← Menu</string>
    <string name="lose_title">Você perdeu...</string>
    <string name="lose_button_retry">↻ Tentar novamente</string>
    <string name="streak_format">%1$s seguidas</string>
```

- [ ] **Step W.1.2: Commit**

```
git add app/src/main/res/values/strings.xml app/src/main/res/values-it/strings.xml app/src/main/res/values-pt/strings.xml
git commit -m "feat(i18n): add strings for win screen W3"
```

### Task W.2: Drawable per win screen

**Files:**
- Create: `app/src/main/res/drawable/casino_stat_card.xml`
- Create: `app/src/main/res/drawable/casino_gif_frame.xml`

- [ ] **Step W.2.1: Drawable stat card background**

Creare `app/src/main/res/drawable/casino_stat_card.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#4D000000" />
    <stroke android:width="1dp" android:color="@color/casino_gold_alpha_50" />
    <corners android:radius="6dp" />
</shape>
```

- [ ] **Step W.2.2: Drawable cornice gif**

Creare `app/src/main/res/drawable/casino_gif_frame.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <stroke android:width="2dp" android:color="@color/casino_gold" />
    <solid android:color="#33000000" />
    <corners android:radius="4dp" />
</shape>
```

- [ ] **Step W.2.3: Commit**

```
git add app/src/main/res/drawable/casino_stat_card.xml app/src/main/res/drawable/casino_gif_frame.xml
git commit -m "feat(theme): add stat card and gif frame drawables for win screen"
```

### Task W.3: Riscrivere activity_you_won.xml

**Files:**
- Modify: `app/src/main/res/layout/activity_you_won.xml`
- Modify: `app/src/main/res/layout-land/activity_you_won.xml` (verificare se esiste)

- [ ] **Step W.3.1: Sostituire activity_you_won.xml con W3 layout**

Sovrascrivere `app/src/main/res/layout/activity_you_won.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/youWonScrollView"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@drawable/verde"
    android:fillViewport="true">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:id="@+id/youWonConstraintLayout"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="20dp"
        tools:context=".YouWonActivity">

        <TextView
            android:id="@+id/trophyIcon"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="24dp"
            android:text="🏆"
            android:textSize="56sp"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent" />

        <TextView
            android:id="@+id/winTitle"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:gravity="center"
            android:text="@string/win_title"
            android:textSize="32sp"
            style="@style/CasinoTitle"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toBottomOf="@id/trophyIcon" />

        <TextView
            android:id="@+id/newRecordBadge"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="6dp"
            android:paddingHorizontal="12dp"
            android:paddingVertical="4dp"
            android:background="@drawable/casino_corner_link"
            android:text="@string/win_new_record"
            android:textSize="12sp"
            android:fontFamily="serif"
            android:textStyle="italic|bold"
            android:textColor="@color/casino_gold"
            android:visibility="gone"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toBottomOf="@id/winTitle" />

        <LinearLayout
            android:id="@+id/statCard"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginTop="20dp"
            android:background="@drawable/casino_stat_card"
            android:orientation="vertical"
            android:padding="14dp"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toBottomOf="@id/newRecordBadge"
            app:layout_constraintWidth_percent="0.85">

            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="horizontal" android:paddingVertical="6dp">
                <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:text="@string/win_stat_time" style="@style/CasinoSubtitle" android:textSize="11sp" />
                <TextView android:id="@+id/statTimeValue" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="--:--" style="@style/CasinoBodyText" android:textStyle="italic" />
            </LinearLayout>

            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="horizontal" android:paddingVertical="6dp">
                <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:text="@string/win_stat_best" style="@style/CasinoSubtitle" android:textSize="11sp" />
                <TextView android:id="@+id/statBestValue" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="--:--" style="@style/CasinoBodyText" android:textStyle="italic" android:textColor="@color/casino_gold" />
            </LinearLayout>

            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="horizontal" android:paddingVertical="6dp">
                <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:text="@string/win_stat_streak" style="@style/CasinoSubtitle" android:textSize="11sp" />
                <TextView android:id="@+id/statStreakValue" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="0" style="@style/CasinoBodyText" android:textStyle="italic" />
            </LinearLayout>

            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="horizontal" android:paddingVertical="6dp">
                <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:text="@string/win_stat_total" style="@style/CasinoSubtitle" android:textSize="11sp" />
                <TextView android:id="@+id/statTotalValue" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="0" style="@style/CasinoBodyText" android:textStyle="italic" />
            </LinearLayout>
        </LinearLayout>

        <pl.droidsonroids.gif.GifImageView
            android:id="@+id/partyGif"
            android:layout_width="0dp"
            android:layout_height="80dp"
            android:layout_marginTop="20dp"
            android:background="@drawable/casino_gif_frame"
            android:padding="3dp"
            android:scaleType="centerCrop"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toBottomOf="@id/statCard"
            app:layout_constraintWidth_percent="0.5" />

        <Button
            android:id="@+id/buttonNewGame"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginTop="20dp"
            android:layout_marginHorizontal="20dp"
            android:background="@drawable/casino_tile_bg_primary"
            android:fontFamily="serif"
            android:onClick="onClickNewGame"
            android:padding="12dp"
            android:text="@string/win_button_new"
            android:textColor="@color/casino_gold"
            android:textSize="16sp"
            android:textStyle="italic|bold"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toBottomOf="@id/partyGif"
            app:layout_constraintWidth_percent="0.6" />

        <Button
            android:id="@+id/buttonMenu"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:layout_marginBottom="24dp"
            android:background="@drawable/casino_tile_bg"
            android:fontFamily="serif"
            android:onClick="onClickMenu"
            android:padding="12dp"
            android:text="@string/win_button_menu"
            android:textColor="@color/casino_gold"
            android:textSize="14sp"
            android:textStyle="italic"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toBottomOf="@id/buttonNewGame"
            app:layout_constraintWidth_percent="0.6" />

    </androidx.constraintlayout.widget.ConstraintLayout>
</ScrollView>
```

- [ ] **Step W.3.2: Stesso per layout-land/activity_you_won.xml**

Verificare se esiste (probabilmente sì). Adattare il layout sopra al landscape (probabilmente fai split: trofeo + stat a sinistra, gif + bottoni a destra). Per ora, lo stesso layout copiato funziona ragionevolmente — l'AI executor può raffinare se necessario.

Se preferisce semplice: rimuovere il file `layout-land/activity_you_won.xml` se esiste, e il portrait verrà usato anche in landscape (come abbiamo fatto per il main menu).

- [ ] **Step W.3.3: Build + commit**

```
./gradlew assembleDebug
git add app/src/main/res/layout/activity_you_won.xml
# Se hai rimosso il land variant:
# git rm app/src/main/res/layout-land/activity_you_won.xml
git commit -m "feat(win): rewrite YouWon layout as W3 (trophy + stat card + gif party)"
```

### Task W.4: Aggiornare YouWonActivity per binding nuove stats

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/YouWonActivity.kt`

- [ ] **Step W.4.1: Leggere file esistente**

```
cat app/src/main/java/com/bottazzini/trasloco/YouWonActivity.kt
```

L'attuale Activity legge probabilmente `time_taken` da intent extra. Aggiungere lettura/scrittura di best_time + total_wins.

- [ ] **Step W.4.2: Riscrivere YouWonActivity con logica W3**

Sovrascrivere completamente (adattando al pattern esistente — l'AI executor dovrebbe leggere prima):

```kotlin
package com.bottazzini.trasloco

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bottazzini.trasloco.settings.RecordsHandler
import com.bottazzini.trasloco.utils.PartyGifs
import com.bottazzini.trasloco.utils.TimeUtils
import com.bottazzini.trasloco.utils.WindowInsetsUtils
import com.bumptech.glide.Glide

class YouWonActivity : AppCompatActivity() {

    private lateinit var recordsHandler: RecordsHandler
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_you_won)
        WindowInsetsUtils.applySystemBarInsets(window, findViewById(R.id.youWonScrollView))
        supportActionBar?.hide()

        recordsHandler = RecordsHandler(applicationContext)

        // Read time from intent (existing pattern)
        val currentTimeMillis = intent.getLongExtra("time_taken", 0L)

        // Update best time if improved
        val isNewRecord = recordsHandler.maybeUpdateBestTime(currentTimeMillis)
        recordsHandler.incrementTotalWins()
        // consecutive_wins is handled by existing pattern (probably already incremented elsewhere)
        val streak = recordsHandler.getConsecutiveWins()  // existing method
        val totalWins = recordsHandler.getTotalWins()
        val bestTime = recordsHandler.getBestTime() ?: currentTimeMillis

        findViewById<TextView>(R.id.statTimeValue).text = TimeUtils.formatMillis(currentTimeMillis)
        findViewById<TextView>(R.id.statBestValue).text = TimeUtils.formatMillis(bestTime)
        findViewById<TextView>(R.id.statStreakValue).text = getString(R.string.streak_format, streak.toString())
        findViewById<TextView>(R.id.statTotalValue).text = totalWins.toString()

        findViewById<TextView>(R.id.newRecordBadge).visibility = if (isNewRecord) View.VISIBLE else View.GONE

        // Load random party gif via Glide
        val randomGifUrl = PartyGifs.partyGifUrls.random()
        Glide.with(this).load(randomGifUrl).into(findViewById(R.id.partyGif))

        // Play win sound (with optional "new record" emphasis — for now same youwin.mp3)
        playWinSound()

        // If user came here with intent extra "result=lost", show lose variant
        val didLose = intent.getBooleanExtra("did_lose", false)
        if (didLose) {
            findViewById<TextView>(R.id.trophyIcon).text = "😔"
            findViewById<TextView>(R.id.winTitle).setText(R.string.lose_title)
            findViewById<TextView>(R.id.newRecordBadge).visibility = View.GONE
            // Hide best time stat if losing? Or show as-is. For now show as-is.
            findViewById<Button>(R.id.buttonNewGame).setText(R.string.lose_button_retry)
        }
    }

    fun onClickNewGame(view: View) {
        // Clear saved state (no resume possible after win/lose anyway, already done in GameActivity).
        val intent = Intent(this, GameActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }

    fun onClickMenu(view: View) {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        recordsHandler.close()  // if method exists
        super.onDestroy()
    }

    private fun playWinSound() {
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.youwin)
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
```

NOTA: aggiustare al pattern esistente per `time_taken` / `did_lose` (verificare come GameActivity attualmente lancia YouWonActivity con quale intent extra).

- [ ] **Step W.4.3: Build + commit**

```
./gradlew assembleDebug
git add app/src/main/java/com/bottazzini/trasloco/YouWonActivity.kt
git commit -m "feat(win): implement W3 with stat tracking, best time, gif party"
```

---

## Phase F — Finalization

### Task F.1: Version bump 1.10.0 → 1.11.0

**Files:**
- Modify: `app/build.gradle`

- [ ] **Step F.1.1: Bump versione**

```
versionCode 22
versionName "1.11.0"
```
(precedente: 21 / "1.10.0")

- [ ] **Step F.1.2: Build + commit**

```
./gradlew assembleDebug
git add app/build.gradle
git commit -m "chore(release): bump version to 1.11.0 (versionCode 22)"
```

### Task F.2: Aggiornare TODO

**Files:**
- Modify: `TODO`

- [ ] **Step F.2.1: Update TODO**

Sovrascrivere `TODO` con:

```
- (Future v1.12+ wishlist) Time mode / countdown challenges
- (Future v1.12+ wishlist) Difficulty levels
- (Future v1.12+ wishlist) Daily challenge
- (Future v1.12+ wishlist) Achievements / trophies
- (Future v1.12+ wishlist) Online leaderboard
```

- [ ] **Step F.2.2: Commit**

```
git add TODO
git commit -m "chore: TODO reflects v1.11 completion (all v1.10/v1.11/v1.12 items now done)"
```

### Task F.3: Smoke test checklist (output per user)

Generare un report finale che l'utente userà per smoke test manuale. Non un task di commit; solo un memo strutturato che l'agente comunica al user nel reply finale.

Checklist da presentare:

**Top bar in game:**
- [ ] Icone back/timer/pausa/hint visibili
- [ ] Back → main menu (con save state intatto)
- [ ] Tap pausa → overlay scuro "PAUSATO" + tap-to-resume funziona
- [ ] Timer non avanza durante pausa
- [ ] Hint → evidenzia source + target con pulse oro 1.5s
- [ ] Hint con `Suggerimenti` OFF in settings → toast "Suggerimenti disabilitati"
- [ ] Hint quando nessuna mossa possibile → toast "Nessuna mossa disponibile"

**Polish:**
- [ ] Slot vuoti hanno frame oro sottile
- [ ] Carta selezionata ha glow oro doppio (già v1.10)
- [ ] Drag&drop → slot bersaglio valido si illumina dorato
- [ ] Mosse click → carta scivola con animazione 200ms

**Auto-move:**
- [ ] Toggle `Auto-muovi` ON in settings
- [ ] Mossa che lascia un Asso disponibile univoco → carta si muove sola
- [ ] Sequenza auto-move continua finché possibile (es. Asso → 2 → 3...)
- [ ] Toggle OFF → comportamento normale (mosse solo manuali)

**Win screen:**
- [ ] Vittoria → trofeo + "Vittoria!" + stat card popolata correttamente
- [ ] Tempo vittoria = effettivo (non include pausa)
- [ ] Best time mostra precedente record (o uguale al corrente se prima vittoria)
- [ ] Quando batti best precedente → badge "Nuovo record! ✨"
- [ ] Gif party piccola in cornice oro carica
- [ ] Bottone "Nuova partita" → game fresca
- [ ] Bottone "Menu" → main menu

**Lose screen:**
- [ ] Sconfitta → "Hai perso..." + 😔 + streak azzerato

**Settings:**
- [ ] Toggle "Suggerimenti" funziona (default ON)
- [ ] Toggle "Auto-muovi" funziona (default OFF)

**Main menu:**
- [ ] Watermark "Bottazzini Softworks" molto leggero in basso

---

## Riepilogo task

| Phase | Tasks | Stima |
|---|---|---|
| 0 — Pre-flight | 1 | ~10 min |
| R — Watermark | 2 | ~30 min |
| S — DB schema | 1 | ~1 ora |
| H — Hint + Top bar | 9 | ~5-6 ore |
| P — In-game polish | 4 | ~2-3 ore |
| A — Auto-move | 1 | ~3-4 ore |
| W — Win screen W3 | 4 | ~3-4 ore |
| F — Finalization | 3 | ~30 min |
| **TOTALE** | **25 task** | **~15-18 ore** (~3 gg full-time, ~1-2 settimane part-time) |

## Fuori scope

Già coperti da v1.10:
- Splash animata
- Main menu M3
- Settings L3
- Carta selezionata sempre visibile
- Card backs nuovi
- Riprendi partita (anticipato da v1.12)

Wishlist G4 (futuro, brainstorming separato):
- Modalità a tempo
- Daily challenge
- Livelli di difficoltà
- Achievement
- Statistiche estese standalone
- Leaderboard online
