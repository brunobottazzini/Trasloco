# UI Casino Redesign — v1.10 Implementation Plan

> ⚠️ **STATO: ESEGUITO IL 2026-05-11**. Branch `feature/v1.10-casino-redesign` (43 commit dopo `main`). Vedere sezione "Esecuzione retrospettiva" in cima per cosa è stato realmente fatto vs cosa è ancora pendente. **Le cose ancora aperte sono nel piano successore** `2026-05-11-ui-casino-redesign-v1.11-plan.md`.

---

## Esecuzione retrospettiva (aggiunta dopo l'esecuzione)

### ✅ Tutti i task del piano completati
Phase 0 (setup) → Phase A (design tokens) → Phase B (background rework) → Phase C (splash) → Phase D (main menu) → Phase E (settings) → Phase F (selected card) → Phase G (version bump).

### 📝 Deviazioni e aggiunte rispetto al piano originale

Durante l'esecuzione sono emerse modifiche non originariamente nel piano. Documentate qui per il prossimo esecutore:

1. **Pre-flight fix** (`50251f4`): `jcenter()` → `mavenCentral()` in `build.gradle`. JCenter spento da marzo 2022, la build falliva con SSL handshake error. Fix obbligatorio per qualunque environment fresh.

2. **Phase D / fix landscape** (`7ac1dda`): rimosso `app/src/main/res/layout-land/activity_main.xml`. Il file conteneva ancora il vecchio menu con riferimenti a `R.id.c4` (logo legacy). Dopo la riscrittura M3 di Phase D, sarebbe stato NPE in landscape. Soluzione: rimuovere il variante; il portrait M3 si adatta a entrambe le orientazioni.

3. **Splash con 3 brand intros** (`181dc35`, `63c178f`): l'utente ha chiesto durante l'esecuzione di anteporre **Bottazzini Softworks logo** (0.0-0.6s) e **logo gioco app icon** (0.6-1.2s) prima dell'animazione carte (1.2-3.2s). Durata totale splash passa da ~2s a ~3.2s.

4. **Post-review fix** (`834798a`, `32b6347`, `81cc115`): final code review ha trovato 3 issue:
   - SplashActivity con `Theme.App.Starting` (splashscreen parent) ma senza `installSplashScreen()` → potenziale hang su Android 12+. Fix: theme cambiato a `Theme.Trasloco`.
   - SplashActivity animazioni con valori in px raw (`-240f`) interpretati come pixel non dp → animazione cramped su xxhdpi. Fix: helper `dp(value)` per conversione density-aware.
   - MainActivity con dead imports dopo riscrittura M3. Fix: rimossi.

5. **Bug critico post-implementazione: MainActivity crash AppCompat** (`26e998c`): dopo la rimozione di `installSplashScreen()` da MainActivity (Phase C.8), MainActivity ereditava il theme app-level (`Theme.App.Starting`, parente di `Theme.SplashScreen` — non AppCompat-compatibile) e crashava su `setContentView`. Fix: aggiunto `android:theme="@style/Theme.Trasloco"` esplicito nel manifest.

6. **Card backs sostituiti** (`ef9b423`): aggiunta non nel piano. L'utente ha chiesto card back nuovi in mood casino. Risultato: 3 PNG (`bg.png`/`bg2.png`/`bg3.png`) sostituiti con XML drawable (`bg.xml` Royal Bordeaux, `bg2.xml` Tavolo Verde, `bg3.xml` Onyx Classico). Nomi preservati per non rompere le scelte utente già salvate nel DB.

7. **Riprendi partita anticipato da v1.12** (`94e2c27`, `74a0af8`, `099cb76`, `d36bc56`): l'utente ha chiesto di anticipare la feature Resume Game. Risultato: `GameStateRepository.kt` con serializzazione JSON, save su `onPause`, restore su `Intent.resume=true`, clear su win/lose, Riprendi tile abilitata condizionalmente in main menu. Riusa la pipeline esistente `restoreGameFromViewModel()` per il rendering.

### ⚠️ Caveat tecnici noti

- **Stale build cache dopo PNG→XML swap**: AAPT può fallire con `error: resource drawable/X not found` dopo aver convertito un asset da PNG a XML mantenendo lo stesso nome. Fix: `./gradlew clean` una volta. Causa: cache incremental merger non invalidata correttamente. Applicabile a future swap asset.

- **`overridePendingTransition` deprecata** dall'API 34. Usata in `SplashActivity.navigateToMain()`. Non bloccante ma da migrare a `overrideActivityTransition` in futuro (richiede min API 34).

- **Warnings Kotlin pre-esistenti**: `GameActivity.kt:1017/1094`, `YouWonActivity.kt:160` — condition is always true. Non introdotti da queste modifiche.

### 📋 Aperto / non eseguito (rimandato al piano successore)

1. **Watermark "Bottazzini Softworks" nel main menu** — discusso durante esecuzione ma mai implementato.
2. **v1.11 completo** — polish gameplay P1, top bar C1, hint engine, win screen W3, stats DB (best_time, total_wins).
3. **Auto-move e relativo toggle** (residuo v1.12).

→ Tutto questo è coperto da `2026-05-11-ui-casino-redesign-v1.11-plan.md`.

---

## Piano originale (sotto)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementare il primo release del restyling Casino Classico: nuovi design tokens, asset sfondi, splash animata, main menu tile-grid, settings con hero preview, carta selezionata sempre visibile in-game. Bump versione 1.9.0 → 1.10.0.

**Architecture:** Android XML/Views (paradigma attuale, no Compose). Tutti i componenti pensati per essere portati 1:1 a Compose Multiplatform in futuro (vedi spec iOS migration). Theming via colors.xml + drawable XML gradients + custom styles.

**Tech Stack:** Kotlin, AndroidX Material3, ConstraintLayout, ObjectAnimator (per splash), drawable XML (gradient + layer-list).

**Spec di riferimento:** `docs/implementation/2026-05-11-ui-casino-redesign-design.md`

**Commit policy:** 1 commit per task verificabile. **NESSUN trailer "Co-Authored-By: Claude"** nei commit (preferenza utente).

---

## Pre-flight

### Task 0: Setup branch + baseline

**Files:**
- N/A (operazioni git)

- [ ] **Step 0.1: Verifica che la working tree sia pulita**

Run: `git status`
Expected: `nothing to commit, working tree clean` (o solo i due spec untracked in `docs/implementation/`)

- [ ] **Step 0.2: Crea branch di lavoro**

Run: `git checkout -b feature/v1.10-casino-redesign`

- [ ] **Step 0.3: Verifica che la build Android funzioni come baseline**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 0.4: Tag della release attuale per rollback**

Run: `git tag -a v1.9.0-pre-redesign -m "Baseline before casino redesign"`

---

## Phase A — Design tokens (foundation)

### Task A.1: Aggiungere palette colori casino

**Files:**
- Modify: `app/src/main/res/values/colors.xml`

- [ ] **Step A.1.1: Aggiungere i colori casino sotto i colori esistenti**

In `app/src/main/res/values/colors.xml`, prima del tag `</resources>` finale, aggiungere:

```xml
    <!-- Casino Classico palette -->
    <color name="casino_green_dark">#FF0A3520</color>
    <color name="casino_green">#FF1A6638</color>
    <color name="casino_green_light">#FF2A7A48</color>
    <color name="casino_gold">#FFD4AF37</color>
    <color name="casino_gold_light">#FFF5E6B3</color>
    <color name="casino_gold_dark">#FF8B7129</color>
    <color name="casino_bordeaux">#FF8B0000</color>
    <color name="casino_bordeaux_dark">#FF4A0000</color>
    <color name="casino_brown">#FF5A3A1C</color>
    <color name="casino_ink">#FF1A1A1A</color>
    <!-- Alpha-on-gold helper -->
    <color name="casino_gold_alpha_08">#14D4AF37</color>
    <color name="casino_gold_alpha_25">#40D4AF37</color>
    <color name="casino_gold_alpha_50">#80D4AF37</color>
```

- [ ] **Step A.1.2: Commit**

```bash
git add app/src/main/res/values/colors.xml
git commit -m "feat(theme): add casino palette colors"
```

### Task A.2: Aggiungere text styles casino

**Files:**
- Modify: `app/src/main/res/values/themes.xml`

- [ ] **Step A.2.1: Aggiungere stili testo serif italic casino**

In `app/src/main/res/values/themes.xml`, prima del tag `</resources>` finale, aggiungere:

```xml
    <!-- Casino text styles -->
    <style name="CasinoTitle">
        <item name="android:fontFamily">serif</item>
        <item name="android:textStyle">italic</item>
        <item name="android:textColor">@color/casino_gold</item>
        <item name="android:letterSpacing">0.15</item>
        <item name="android:shadowColor">#80000000</item>
        <item name="android:shadowDy">2</item>
        <item name="android:shadowRadius">8</item>
    </style>

    <style name="CasinoSubtitle">
        <item name="android:fontFamily">serif</item>
        <item name="android:textColor">@color/casino_gold_alpha_50</item>
        <item name="android:letterSpacing">0.3</item>
        <item name="android:textSize">10sp</item>
    </style>

    <style name="CasinoSectionLabel">
        <item name="android:fontFamily">serif</item>
        <item name="android:textStyle">italic</item>
        <item name="android:textColor">@color/casino_gold</item>
        <item name="android:letterSpacing">0.2</item>
        <item name="android:textSize">12sp</item>
        <item name="android:textAllCaps">true</item>
    </style>

    <style name="CasinoBodyText">
        <item name="android:fontFamily">serif</item>
        <item name="android:textColor">@color/casino_gold_light</item>
        <item name="android:textSize">14sp</item>
    </style>
```

- [ ] **Step A.2.2: Commit**

```bash
git add app/src/main/res/values/themes.xml
git commit -m "feat(theme): add casino text styles"
```

---

## Phase B — Background asset rework

### Task B.1: Sostituire `verde.png` con gradient XML

Il drawable `verde.png` attuale è una tinta piatta. Lo sostituiamo con un gradient radiale XML **mantenendo lo stesso nome** (`verde`), così tutti i riferimenti esistenti (`@drawable/verde`, lookup dinamico `getIdentifier("verde", ...)`) puntano automaticamente al nuovo gradient.

**Files:**
- Delete: `app/src/main/res/drawable/verde.png`
- Create: `app/src/main/res/drawable/verde.xml`

- [ ] **Step B.1.1: Rimuovere il PNG esistente**

Run: `rm app/src/main/res/drawable/verde.png`

- [ ] **Step B.1.2: Creare il file XML con gradient radiale verde casino**

Creare `app/src/main/res/drawable/verde.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <gradient
        android:type="radial"
        android:centerX="0.5"
        android:centerY="0.5"
        android:gradientRadius="800"
        android:startColor="@color/casino_green"
        android:endColor="@color/casino_green_dark" />
</shape>
```

- [ ] **Step B.1.3: Build di verifica**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step B.1.4: Commit**

```bash
git add -A app/src/main/res/drawable/verde.png app/src/main/res/drawable/verde.xml
git commit -m "feat(theme): replace verde.png with radial gradient XML"
```

### Task B.2: Creare drawable `bordeaux.xml` (nuovo sfondo)

**Files:**
- Create: `app/src/main/res/drawable/bordeaux.xml`

- [ ] **Step B.2.1: Creare il file gradient bordeaux**

Creare `app/src/main/res/drawable/bordeaux.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <gradient
        android:type="radial"
        android:centerX="0.5"
        android:centerY="0.5"
        android:gradientRadius="800"
        android:startColor="@color/casino_bordeaux"
        android:endColor="@color/casino_bordeaux_dark" />
</shape>
```

- [ ] **Step B.2.2: Commit**

```bash
git add app/src/main/res/drawable/bordeaux.xml
git commit -m "feat(theme): add bordeaux gradient drawable"
```

### Task B.3: Aggiungere stringhe per nuovo sfondo `bordeaux`

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-it/strings.xml`
- Modify: `app/src/main/res/values-pt/strings.xml`

- [ ] **Step B.3.1: Aggiungere `bordeaux` in default (en)**

In `app/src/main/res/values/strings.xml`, aggiungere prima di `</resources>`:

```xml
    <string name="bordeaux">Bordeaux</string>
```

- [ ] **Step B.3.2: Aggiungere `bordeaux` in it**

In `app/src/main/res/values-it/strings.xml`, aggiungere prima di `</resources>`:

```xml
    <string name="bordeaux">Bordeaux</string>
```

- [ ] **Step B.3.3: Aggiungere `bordeaux` in pt**

In `app/src/main/res/values-pt/strings.xml`, aggiungere prima di `</resources>`:

```xml
    <string name="bordeaux">Bordeaux</string>
```

- [ ] **Step B.3.4: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/values-it/strings.xml app/src/main/res/values-pt/strings.xml
git commit -m "feat(i18n): add 'bordeaux' background string"
```

### Task B.4: Migrazione utenti con sfondo rimosso (sabbia, tavolo)

Aggiungere logica in `SettingsHandler` che al primo avvio dopo l'upgrade controlla se il background corrente è uno tra `sabbia` o `tavolo` e lo sostituisce con `verde`.

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/settings/SettingsHandler.kt`

- [ ] **Step B.4.1: Aggiungere metodo `migrateRemovedBackgrounds`**

In `app/src/main/java/com/bottazzini/trasloco/settings/SettingsHandler.kt`, dopo il metodo `insertDefaultSettings()`, aggiungere:

```kotlin
    fun migrateRemovedBackgrounds() {
        val current = readValue(Configuration.BACKGROUND.value)
        val removed = setOf("sabbia", "tavolo")
        if (current != null && current in removed) {
            updateSetting(Configuration.BACKGROUND.value, "verde")
        }
    }
```

- [ ] **Step B.4.2: Chiamare la migrazione da `MainActivity.onCreate`**

In `app/src/main/java/com/bottazzini/trasloco/MainActivity.kt`, dentro `onCreate`, dopo la riga `settingsHandler.insertDefaultSettings()`, aggiungere:

```kotlin
        settingsHandler.migrateRemovedBackgrounds()
```

- [ ] **Step B.4.3: Build di verifica**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step B.4.4: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/settings/SettingsHandler.kt app/src/main/java/com/bottazzini/trasloco/MainActivity.kt
git commit -m "feat(settings): migrate removed backgrounds (sabbia, tavolo) to verde"
```

### Task B.5: Rimuovere drawable sfondi deprecati

Rimuovere i file PNG di `sabbia.png` e `tavolo.png` (sfondi rimossi dalle scelte utente). NOTA: `bg.png`, `bg2.png`, `bg3.png` sono **retro carte**, non sfondi — NON rimuoverli. `zero.png` non è referenziato nelle settings ma verifichiamo prima.

**Files:**
- Delete: `app/src/main/res/drawable-v24/sabbia.png`
- Delete: `app/src/main/res/drawable-v24/tavolo.png`

- [ ] **Step B.5.1: Verifica che `sabbia` e `tavolo` non siano referenziati in altri file XML**

Run: `grep -rn "drawable/sabbia\|drawable/tavolo\|@drawable/sabbia\|@drawable/tavolo" app/src/main/res/ app/src/main/java/`
Expected: solo riferimenti in `app/src/main/res/layout/settings.xml` (saranno rimossi nella Task E)

Se trovi altri riferimenti, fermati e segnala — non rimuovere ancora i drawable.

- [ ] **Step B.5.2: Verifica uso di `zero.png`**

Run: `grep -rn "drawable/zero\|@drawable/zero" app/src/main/res/ app/src/main/java/`

Se non referenziato in modo significativo (es. solo in altri commenti/test), può essere rimosso. Se è ancora usato come placeholder slot vuoto in game.xml, NON rimuoverlo.

- [ ] **Step B.5.3: Rimuovere i file PNG (solo se Step B.5.1 conferma uso solo in settings.xml)**

Run:
```bash
rm app/src/main/res/drawable-v24/sabbia.png
rm app/src/main/res/drawable-v24/tavolo.png
```

- [ ] **Step B.5.4: Build di verifica**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL` (potrebbe fallire se `settings.xml` referenzia ancora `@drawable/sabbia`/`tavolo` — in tal caso la build fallirà, fermati e procedi prima con Task E)

Se la build fallisce per riferimenti in settings.xml, ripristina i file (`git restore app/src/main/res/drawable-v24/sabbia.png app/src/main/res/drawable-v24/tavolo.png`), salta questo task e riprendilo dopo Task E.

- [ ] **Step B.5.5: Commit**

```bash
git add -A app/src/main/res/drawable-v24/
git commit -m "chore(theme): remove deprecated background drawables (sabbia, tavolo)"
```

---

## Phase C — Splash screen animata (S1 Shuffle & Deal)

### Task C.1: Creare SplashActivity skeleton

**Files:**
- Create: `app/src/main/java/com/bottazzini/trasloco/SplashActivity.kt`

- [ ] **Step C.1.1: Creare la classe Activity vuota**

Creare `app/src/main/java/com/bottazzini/trasloco/SplashActivity.kt`:

```kotlin
package com.bottazzini.trasloco

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.Window
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var skipEnabled = false
    private var hasNavigated = false

    private val skipEnableDelayMs = 500L
    private val totalDurationMs = 2000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        supportActionBar?.hide()
        setContentView(R.layout.activity_splash)

        handler.postDelayed({ skipEnabled = true }, skipEnableDelayMs)
        handler.postDelayed({ navigateToMain() }, totalDurationMs)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN && skipEnabled) {
            navigateToMain()
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun navigateToMain() {
        if (hasNavigated) return
        hasNavigated = true
        handler.removeCallbacksAndMessages(null)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
```

- [ ] **Step C.1.2: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/SplashActivity.kt
git commit -m "feat(splash): add SplashActivity skeleton with timing logic"
```

### Task C.2: Creare layout splash con 4 ImageView placeholder + titolo

**Files:**
- Create: `app/src/main/res/layout/activity_splash.xml`

- [ ] **Step C.2.1: Creare il layout XML**

Creare `app/src/main/res/layout/activity_splash.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/splashRoot"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@drawable/verde">

    <ImageView
        android:id="@+id/splashCard1"
        android:layout_width="80dp"
        android:layout_height="120dp"
        android:layout_gravity="center"
        android:src="@drawable/bg"
        android:alpha="0" />

    <ImageView
        android:id="@+id/splashCard2"
        android:layout_width="80dp"
        android:layout_height="120dp"
        android:layout_gravity="center"
        android:src="@drawable/bg"
        android:alpha="0" />

    <ImageView
        android:id="@+id/splashCard3"
        android:layout_width="80dp"
        android:layout_height="120dp"
        android:layout_gravity="center"
        android:src="@drawable/bg"
        android:alpha="0" />

    <ImageView
        android:id="@+id/splashCard4"
        android:layout_width="80dp"
        android:layout_height="120dp"
        android:layout_gravity="center"
        android:src="@drawable/bg"
        android:alpha="0" />

    <TextView
        android:id="@+id/splashTitle"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center_horizontal|top"
        android:layout_marginTop="120dp"
        android:text="@string/trasloco"
        android:textSize="48sp"
        android:alpha="0"
        style="@style/CasinoTitle" />

</FrameLayout>
```

- [ ] **Step C.2.2: Build di verifica**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step C.2.3: Commit**

```bash
git add app/src/main/res/layout/activity_splash.xml
git commit -m "feat(splash): add splash layout with 4 cards + title placeholders"
```

### Task C.3: Implementare animazione fase 1 (stack iniziale)

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/SplashActivity.kt`

- [ ] **Step C.3.1: Aggiungere import e logica fase stack iniziale**

In `SplashActivity.kt`, aggiungere import:

```kotlin
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
```

Dopo `setContentView(R.layout.activity_splash)`, aggiungere:

```kotlin
        startAnimation()
```

E aggiungere il metodo (per ora solo fase iniziale: stack visibile al centro):

```kotlin
    private fun startAnimation() {
        val card1 = findViewById<ImageView>(R.id.splashCard1)
        val card2 = findViewById<ImageView>(R.id.splashCard2)
        val card3 = findViewById<ImageView>(R.id.splashCard3)
        val card4 = findViewById<ImageView>(R.id.splashCard4)

        // Phase 1 (0.0s - 0.6s): stacked cards fade in with slight rotation
        val stackCards = listOf(card1, card2, card3, card4)
        stackCards.forEachIndexed { idx, card ->
            card.alpha = 0f
            card.translationY = -(idx * 2f)
            card.rotation = (idx - 1.5f) * 2f
            card.animate()
                .alpha(1f)
                .setDuration(400)
                .setStartDelay(idx * 50L)
                .start()
        }
    }
```

- [ ] **Step C.3.2: Build di verifica**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step C.3.3: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/SplashActivity.kt
git commit -m "feat(splash): implement phase 1 (stacked cards fade in)"
```

### Task C.4: Implementare animazione fase 2 (shuffle in aria)

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/SplashActivity.kt`

- [ ] **Step C.4.1: Aggiungere fase shuffle**

In `SplashActivity.kt`, alla fine del metodo `startAnimation()`, aggiungere:

```kotlin
        // Phase 2 (0.6s - 1.4s): shuffle in air — random rotations + translations
        handler.postDelayed({
            shuffleCard(card1, -180f, -120f, -40f)
            shuffleCard(card2, 90f, 100f, 25f)
            shuffleCard(card3, -60f, 60f, -15f)
            shuffleCard(card4, 200f, -80f, 35f)
        }, 600L)
    }

    private fun shuffleCard(card: ImageView, dx: Float, dy: Float, rot: Float) {
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(card, "translationX", 0f, dx, 0f),
                ObjectAnimator.ofFloat(card, "translationY", card.translationY, dy, 0f),
                ObjectAnimator.ofFloat(card, "rotation", card.rotation, rot, 0f)
            )
            duration = 800
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }
```

- [ ] **Step C.4.2: Build di verifica**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step C.4.3: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/SplashActivity.kt
git commit -m "feat(splash): implement phase 2 (shuffle in air)"
```

### Task C.5: Implementare fase 3 (deal a ventaglio + title reveal)

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/SplashActivity.kt`

- [ ] **Step C.5.1: Aggiungere fase deal-out + title**

In `SplashActivity.kt`, alla fine del metodo `startAnimation()` (dopo `handler.postDelayed` di Phase 2), aggiungere:

```kotlin
        // Phase 3 (1.4s - 2.0s): fan deal-out + title reveal
        handler.postDelayed({
            dealCard(card1, -240f, 100f, -30f)
            dealCard(card2, -80f, 100f, -10f)
            dealCard(card3, 80f, 100f, 10f)
            dealCard(card4, 240f, 100f, 30f)

            val title = findViewById<TextView>(R.id.splashTitle)
            title.alpha = 0f
            title.translationY = -40f
            title.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }, 1400L)
```

E aggiungere il metodo `dealCard`:

```kotlin
    private fun dealCard(card: ImageView, finalX: Float, finalY: Float, finalRot: Float) {
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(card, "translationX", card.translationX, finalX),
                ObjectAnimator.ofFloat(card, "translationY", card.translationY, finalY),
                ObjectAnimator.ofFloat(card, "rotation", card.rotation, finalRot)
            )
            duration = 600
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }
```

- [ ] **Step C.5.2: Build di verifica**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step C.5.3: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/SplashActivity.kt
git commit -m "feat(splash): implement phase 3 (fan deal-out + title reveal)"
```

### Task C.6: Suono shuffle durante l'animazione

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/SplashActivity.kt`

- [ ] **Step C.6.1: Aggiungere riproduzione `shuffle.mp3` all'inizio animazione**

In `SplashActivity.kt`, aggiungere import:

```kotlin
import android.media.MediaPlayer
```

Aggiungere campo nella classe:

```kotlin
    private var mediaPlayer: MediaPlayer? = null
```

In `startAnimation()`, all'inizio del metodo, aggiungere:

```kotlin
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.shuffle)
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
```

Modificare `onDestroy()`:

```kotlin
    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }
```

E in `navigateToMain()`, prima di `startActivity(...)`, aggiungere:

```kotlin
        mediaPlayer?.release()
        mediaPlayer = null
```

- [ ] **Step C.6.2: Build di verifica**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step C.6.3: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/SplashActivity.kt
git commit -m "feat(splash): play shuffle sound during animation"
```

### Task C.7: Aggiornare AndroidManifest (SplashActivity = launcher)

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step C.7.1: Aggiungere SplashActivity come launcher + rimuovere intent-filter da MainActivity**

In `app/src/main/AndroidManifest.xml`, sostituire il blocco MainActivity esistente:

```xml
        <activity android:name=".MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
```

Con:

```xml
        <activity
            android:name=".SplashActivity"
            android:exported="true"
            android:theme="@style/Theme.App.Starting">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        <activity android:name=".MainActivity" android:exported="false" />
```

- [ ] **Step C.7.2: Build di verifica**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step C.7.3: Commit**

```bash
git add app/src/main/AndroidManifest.xml
git commit -m "feat(splash): set SplashActivity as launcher, MainActivity exported=false"
```

### Task C.8: Rimuovere `installSplashScreen` da MainActivity

`MainActivity` non è più launcher e non deve gestire splash system.

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/MainActivity.kt`

- [ ] **Step C.8.1: Rimuovere import e chiamata `installSplashScreen`**

In `app/src/main/java/com/bottazzini/trasloco/MainActivity.kt`:

Rimuovere queste righe:

```kotlin
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
```

Sostituire il blocco:

```kotlin
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setOnExitAnimationListener {
            Thread.sleep(700)
            it.remove()
        }
        super.onCreate(savedInstanceState)
```

Con:

```kotlin
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
```

- [ ] **Step C.8.2: Build di verifica**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step C.8.3: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/MainActivity.kt
git commit -m "refactor(main): remove installSplashScreen (handled by SplashActivity now)"
```

### Task C.9: Smoke test splash su emulatore

- [ ] **Step C.9.1: Installare l'APK sull'emulatore**

Run: `./gradlew installDebug`

- [ ] **Step C.9.2: Lanciare l'app e verificare manualmente**

Aprire l'app sull'emulatore. Verificare:
- Le 4 carte appaiono al centro con effetto stack
- Si mescolano in aria per ~0.8s
- Si dispongono a ventaglio + titolo "Trasloco" appare
- Suono `shuffle.mp3` durante l'animazione
- Dopo ~2s passa automaticamente a MainActivity
- Tap dopo 0.5s skippa correttamente

Se tutto OK, prosegui. Se c'è un bug, fixalo prima del prossimo task.

---

## Phase D — Main menu M3 (Tile grid 2×2)

### Task D.1: Drawable `casino_tile_bg_primary.xml`

**Files:**
- Create: `app/src/main/res/drawable/casino_tile_bg_primary.xml`

- [ ] **Step D.1.1: Creare il drawable con bordo dorato + glow + gradient oro morbido**

Creare `app/src/main/res/drawable/casino_tile_bg_primary.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item>
        <shape android:shape="rectangle">
            <gradient
                android:angle="270"
                android:startColor="@color/casino_gold_alpha_25"
                android:endColor="@color/casino_gold_alpha_08" />
            <stroke android:width="2dp" android:color="@color/casino_gold" />
            <corners android:radius="8dp" />
        </shape>
    </item>
</layer-list>
```

- [ ] **Step D.1.2: Commit**

```bash
git add app/src/main/res/drawable/casino_tile_bg_primary.xml
git commit -m "feat(theme): add casino primary tile background drawable"
```

### Task D.2: Drawable `casino_tile_bg.xml` (secondary)

**Files:**
- Create: `app/src/main/res/drawable/casino_tile_bg.xml`

- [ ] **Step D.2.1: Creare il drawable secondary**

Creare `app/src/main/res/drawable/casino_tile_bg.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/casino_gold_alpha_08" />
    <stroke android:width="1.5dp" android:color="@color/casino_gold" />
    <corners android:radius="6dp" />
</shape>
```

- [ ] **Step D.2.2: Commit**

```bash
git add app/src/main/res/drawable/casino_tile_bg.xml
git commit -m "feat(theme): add casino secondary tile background drawable"
```

### Task D.3: Drawable `casino_corner_link.xml` (pill per "Regole")

**Files:**
- Create: `app/src/main/res/drawable/casino_corner_link.xml`

- [ ] **Step D.3.1: Creare il drawable pill**

Creare `app/src/main/res/drawable/casino_corner_link.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@android:color/transparent" />
    <stroke android:width="1dp" android:color="@color/casino_gold_alpha_50" />
    <corners android:radius="12dp" />
</shape>
```

- [ ] **Step D.3.2: Commit**

```bash
git add app/src/main/res/drawable/casino_corner_link.xml
git commit -m "feat(theme): add casino corner link pill drawable"
```

### Task D.4: Aggiungere stringhe per main menu rinnovato

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-it/strings.xml`
- Modify: `app/src/main/res/values-pt/strings.xml`

- [ ] **Step D.4.1: Aggiungere stringhe in `values/strings.xml`**

Prima di `</resources>`:

```xml
    <string name="subtitle_card_game">~ CARD GAME ~</string>
    <string name="riprendi_partita">Resume</string>
    <string name="nuova_partita_label">New\nGame</string>
    <string name="riprendi_label">Resume</string>
    <string name="records_label">Records</string>
    <string name="impostazioni_label">Settings</string>
    <string name="rules_corner_label">📜 Rules</string>
```

- [ ] **Step D.4.2: Aggiungere stringhe in `values-it/strings.xml`**

```xml
    <string name="subtitle_card_game">~ GIOCO DI CARTE ~</string>
    <string name="riprendi_partita">Riprendi</string>
    <string name="nuova_partita_label">Nuova\nPartita</string>
    <string name="riprendi_label">Riprendi</string>
    <string name="records_label">Record</string>
    <string name="impostazioni_label">Impostazioni</string>
    <string name="rules_corner_label">📜 Regole</string>
```

- [ ] **Step D.4.3: Aggiungere stringhe in `values-pt/strings.xml`**

```xml
    <string name="subtitle_card_game">~ JOGO DE CARTAS ~</string>
    <string name="riprendi_partita">Continuar</string>
    <string name="nuova_partita_label">Novo\nJogo</string>
    <string name="riprendi_label">Continuar</string>
    <string name="records_label">Recordes</string>
    <string name="impostazioni_label">Definições</string>
    <string name="rules_corner_label">📜 Regras</string>
```

- [ ] **Step D.4.4: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/values-it/strings.xml app/src/main/res/values-pt/strings.xml
git commit -m "feat(i18n): add strings for new M3 main menu layout"
```

### Task D.5: Riscrivere `activity_main.xml` con tile grid 2×2

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`

- [ ] **Step D.5.1: Sostituire interamente activity_main.xml**

Sovrascrivere `app/src/main/res/layout/activity_main.xml` con:

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/mainScrollView"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@drawable/verde"
    android:fillViewport="true">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:id="@+id/mainConstraintLayout"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="20dp"
        tools:context=".MainActivity">

        <TextView
            android:id="@+id/textViewTitle"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginTop="32dp"
            android:gravity="center"
            android:text="@string/trasloco"
            android:textSize="48sp"
            style="@style/CasinoTitle"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent" />

        <TextView
            android:id="@+id/textViewSubtitle"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginTop="4dp"
            android:gravity="center"
            android:text="@string/subtitle_card_game"
            style="@style/CasinoSubtitle"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toBottomOf="@id/textViewTitle" />

        <TextView
            android:id="@+id/buttonRules"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="32dp"
            android:layout_marginEnd="4dp"
            android:paddingHorizontal="12dp"
            android:paddingVertical="6dp"
            android:background="@drawable/casino_corner_link"
            android:text="@string/rules_corner_label"
            android:textSize="11sp"
            android:textStyle="italic"
            android:fontFamily="serif"
            android:textColor="@color/casino_gold"
            android:onClick="showRules"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintTop_toBottomOf="@id/textViewSubtitle" />

        <!-- Tile 1: Nuova Partita (primary) -->
        <LinearLayout
            android:id="@+id/tileNuovaPartita"
            android:layout_width="0dp"
            android:layout_height="0dp"
            android:layout_marginTop="40dp"
            android:layout_marginEnd="6dp"
            android:background="@drawable/casino_tile_bg_primary"
            android:gravity="center"
            android:orientation="vertical"
            android:onClick="startGame"
            android:clickable="true"
            android:focusable="true"
            app:layout_constraintDimensionRatio="1:1"
            app:layout_constraintEnd_toStartOf="@id/tileRiprendi"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toBottomOf="@id/buttonRules"
            app:layout_constraintWidth_percent="0.42">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="▶"
                android:textSize="32sp"
                android:textColor="@color/casino_gold" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:gravity="center"
                android:layout_marginTop="8dp"
                android:text="@string/nuova_partita_label"
                android:textSize="14sp"
                android:textStyle="italic|bold"
                android:fontFamily="serif"
                android:textColor="@color/casino_gold" />
        </LinearLayout>

        <!-- Tile 2: Riprendi (disabled placeholder in v1.10) -->
        <LinearLayout
            android:id="@+id/tileRiprendi"
            android:layout_width="0dp"
            android:layout_height="0dp"
            android:layout_marginTop="40dp"
            android:layout_marginStart="6dp"
            android:background="@drawable/casino_tile_bg"
            android:gravity="center"
            android:orientation="vertical"
            android:alpha="0.4"
            android:clickable="false"
            app:layout_constraintDimensionRatio="1:1"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toEndOf="@id/tileNuovaPartita"
            app:layout_constraintTop_toBottomOf="@id/buttonRules"
            app:layout_constraintWidth_percent="0.42">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="↻"
                android:textSize="32sp"
                android:textColor="@color/casino_gold_alpha_50" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:gravity="center"
                android:layout_marginTop="8dp"
                android:text="@string/riprendi_label"
                android:textSize="14sp"
                android:textStyle="italic"
                android:fontFamily="serif"
                android:textColor="@color/casino_gold_alpha_50" />
        </LinearLayout>

        <!-- Tile 3: Records -->
        <LinearLayout
            android:id="@+id/tileRecords"
            android:layout_width="0dp"
            android:layout_height="0dp"
            android:layout_marginTop="12dp"
            android:layout_marginEnd="6dp"
            android:background="@drawable/casino_tile_bg"
            android:gravity="center"
            android:orientation="vertical"
            android:onClick="showRecords"
            android:clickable="true"
            android:focusable="true"
            app:layout_constraintDimensionRatio="1:1"
            app:layout_constraintEnd_toStartOf="@id/tileSettings"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toBottomOf="@id/tileNuovaPartita"
            app:layout_constraintWidth_percent="0.42">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="🏆"
                android:textSize="28sp" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:gravity="center"
                android:layout_marginTop="8dp"
                android:text="@string/records_label"
                android:textSize="14sp"
                android:textStyle="italic"
                android:fontFamily="serif"
                android:textColor="@color/casino_gold_light" />
        </LinearLayout>

        <!-- Tile 4: Impostazioni -->
        <LinearLayout
            android:id="@+id/tileSettings"
            android:layout_width="0dp"
            android:layout_height="0dp"
            android:layout_marginTop="12dp"
            android:layout_marginStart="6dp"
            android:layout_marginBottom="24dp"
            android:background="@drawable/casino_tile_bg"
            android:gravity="center"
            android:orientation="vertical"
            android:onClick="openSettings"
            android:clickable="true"
            android:focusable="true"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintDimensionRatio="1:1"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toEndOf="@id/tileRecords"
            app:layout_constraintTop_toBottomOf="@id/tileRiprendi"
            app:layout_constraintWidth_percent="0.42">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="⚙"
                android:textSize="28sp"
                android:textColor="@color/casino_gold" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:gravity="center"
                android:layout_marginTop="8dp"
                android:text="@string/impostazioni_label"
                android:textSize="14sp"
                android:textStyle="italic"
                android:fontFamily="serif"
                android:textColor="@color/casino_gold_light" />
        </LinearLayout>

    </androidx.constraintlayout.widget.ConstraintLayout>
</ScrollView>
```

NOTA: il vecchio "easter egg triple-tap sul logo (mainImage `c4`)" è perso con questa riscrittura. La logica `handleTripleTap` resta in MainActivity ma non è più collegata a nulla.

- [ ] **Step D.5.2: Build di verifica**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step D.5.3: Commit**

```bash
git add app/src/main/res/layout/activity_main.xml
git commit -m "feat(menu): rewrite main menu as M3 tile grid 2x2"
```

### Task D.6: Riattivare triple-tap version-toast su titolo

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/MainActivity.kt`

- [ ] **Step D.6.1: Spostare il listener di triple-tap dal vecchio `c4` al titolo**

In `MainActivity.kt`, sostituire:

```kotlin
        val mainImage: ImageView = findViewById(R.id.c4)

        mainImage.setOnClickListener {
            handleTripleTap()
        }
```

Con:

```kotlin
        findViewById<View>(R.id.textViewTitle).setOnClickListener {
            handleTripleTap()
        }
```

E rimuovere l'import:

```kotlin
import android.widget.ImageView
```

- [ ] **Step D.6.2: Build di verifica**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step D.6.3: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/MainActivity.kt
git commit -m "feat(menu): re-attach version-toast triple-tap to title"
```

### Task D.7: Smoke test main menu

- [ ] **Step D.7.1: Installa + verifica manualmente**

Run: `./gradlew installDebug`

Aprire l'app, dopo splash vedere:
- Titolo "Trasloco" in serif italico oro al centro
- Sottotitolo "~ GIOCO DI CARTE ~"
- Pulsante "📜 Regole" piccolo in alto a destra (apre regole)
- Griglia 2×2:
  - In alto a sx: "Nuova Partita" con bordo dorato pieno (parte la partita)
  - In alto a dx: "Riprendi" semi-trasparente, non cliccabile (placeholder)
  - In basso a sx: "🏆 Records" (apre records)
  - In basso a dx: "⚙ Impostazioni" (apre settings)
- Triple-tap sul titolo mostra versione

---

## Phase E — Settings L3 (Hero preview + selettori carte illustrate + switch oro)

### Task E.1: Drawable `casino_gold_switch_track.xml` + `casino_gold_switch_thumb.xml`

**Files:**
- Create: `app/src/main/res/drawable/casino_gold_switch_track.xml`
- Create: `app/src/main/res/drawable/casino_gold_switch_thumb.xml`

- [ ] **Step E.1.1: Creare il track del switch (binario)**

Creare `app/src/main/res/drawable/casino_gold_switch_track.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_checked="true">
        <shape android:shape="rectangle">
            <gradient
                android:angle="270"
                android:startColor="@color/casino_gold"
                android:endColor="@color/casino_gold_dark" />
            <stroke android:width="1dp" android:color="@color/casino_gold" />
            <corners android:radius="14dp" />
            <size android:width="56dp" android:height="28dp" />
        </shape>
    </item>
    <item>
        <shape android:shape="rectangle">
            <gradient
                android:angle="270"
                android:startColor="@color/casino_ink"
                android:endColor="@color/casino_green_dark" />
            <stroke android:width="1dp" android:color="@color/casino_gold" />
            <corners android:radius="14dp" />
            <size android:width="56dp" android:height="28dp" />
        </shape>
    </item>
</selector>
```

- [ ] **Step E.1.2: Creare il thumb del switch (knob)**

Creare `app/src/main/res/drawable/casino_gold_switch_thumb.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_checked="true">
        <shape android:shape="oval">
            <gradient
                android:type="radial"
                android:gradientRadius="14"
                android:centerX="0.3"
                android:centerY="0.3"
                android:startColor="@color/casino_gold_light"
                android:endColor="@color/casino_gold_dark" />
            <stroke android:width="1dp" android:color="@color/casino_brown" />
            <size android:width="22dp" android:height="22dp" />
        </shape>
    </item>
    <item>
        <shape android:shape="oval">
            <gradient
                android:type="radial"
                android:gradientRadius="14"
                android:centerX="0.3"
                android:centerY="0.3"
                android:startColor="#FF888888"
                android:endColor="#FF222222" />
            <stroke android:width="1dp" android:color="#FF111111" />
            <size android:width="22dp" android:height="22dp" />
        </shape>
    </item>
</selector>
```

- [ ] **Step E.1.3: Commit**

```bash
git add app/src/main/res/drawable/casino_gold_switch_track.xml app/src/main/res/drawable/casino_gold_switch_thumb.xml
git commit -m "feat(theme): add gold switch (track + thumb) drawables"
```

### Task E.2: Drawable `casino_card_tile_selectable.xml` (selettore carta tile)

**Files:**
- Create: `app/src/main/res/drawable/casino_card_tile_selectable.xml`
- Create: `app/src/main/res/drawable/casino_card_tile_selected.xml`
- Create: `app/src/main/res/drawable/casino_card_tile_unselected.xml`

- [ ] **Step E.2.1: Creare drawable selected**

Creare `app/src/main/res/drawable/casino_card_tile_selected.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@android:color/transparent" />
    <stroke android:width="2dp" android:color="@color/casino_gold" />
    <corners android:radius="6dp" />
</shape>
```

- [ ] **Step E.2.2: Creare drawable unselected**

Creare `app/src/main/res/drawable/casino_card_tile_unselected.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@android:color/transparent" />
    <stroke android:width="1dp" android:color="@color/casino_gold_alpha_25" />
    <corners android:radius="6dp" />
</shape>
```

- [ ] **Step E.2.3: Creare il selector**

Creare `app/src/main/res/drawable/casino_card_tile_selectable.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_selected="true" android:drawable="@drawable/casino_card_tile_selected" />
    <item android:state_checked="true" android:drawable="@drawable/casino_card_tile_selected" />
    <item android:drawable="@drawable/casino_card_tile_unselected" />
</selector>
```

- [ ] **Step E.2.4: Commit**

```bash
git add app/src/main/res/drawable/casino_card_tile_selectable.xml app/src/main/res/drawable/casino_card_tile_selected.xml app/src/main/res/drawable/casino_card_tile_unselected.xml
git commit -m "feat(theme): add selectable card tile drawables"
```

### Task E.3: Drawable `casino_hero_preview_frame.xml`

**Files:**
- Create: `app/src/main/res/drawable/casino_hero_preview_frame.xml`

- [ ] **Step E.3.1: Creare frame cornice oro per anteprima**

Creare `app/src/main/res/drawable/casino_hero_preview_frame.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item>
        <shape android:shape="rectangle">
            <stroke android:width="1.5dp" android:color="@color/casino_gold" />
            <corners android:radius="6dp" />
            <solid android:color="@android:color/transparent" />
        </shape>
    </item>
</layer-list>
```

- [ ] **Step E.3.2: Commit**

```bash
git add app/src/main/res/drawable/casino_hero_preview_frame.xml
git commit -m "feat(theme): add hero preview frame drawable"
```

### Task E.4: Aggiungere stringhe settings rinnovate

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-it/strings.xml`
- Modify: `app/src/main/res/values-pt/strings.xml`

- [ ] **Step E.4.1: Aggiungere stringhe label sezioni in default**

In `app/src/main/res/values/strings.xml`, prima di `</resources>`:

```xml
    <string name="settings_label_card_deck">Card Deck</string>
    <string name="settings_label_card_back">Card Back</string>
    <string name="settings_label_background">Table</string>
    <string name="settings_label_preview">PREVIEW</string>
    <string name="settings_title_decorated">~  Settings  ~</string>
    <string name="fast_deal_with_icon">⚡ Transfer entire deck</string>
```

- [ ] **Step E.4.2: Aggiungere stringhe in it**

In `app/src/main/res/values-it/strings.xml`:

```xml
    <string name="settings_label_card_deck">Mazzo carte</string>
    <string name="settings_label_card_back">Retro carte</string>
    <string name="settings_label_background">Tappeto</string>
    <string name="settings_label_preview">ANTEPRIMA</string>
    <string name="settings_title_decorated">~  Impostazioni  ~</string>
    <string name="fast_deal_with_icon">⚡ Trasferisci intero mazzo</string>
```

- [ ] **Step E.4.3: Aggiungere stringhe in pt**

In `app/src/main/res/values-pt/strings.xml`:

```xml
    <string name="settings_label_card_deck">Baralho</string>
    <string name="settings_label_card_back">Verso das cartas</string>
    <string name="settings_label_background">Tapete</string>
    <string name="settings_label_preview">PRÉVIA</string>
    <string name="settings_title_decorated">~  Definições  ~</string>
    <string name="fast_deal_with_icon">⚡ Transferir baralho inteiro</string>
```

- [ ] **Step E.4.4: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/values-it/strings.xml app/src/main/res/values-pt/strings.xml
git commit -m "feat(i18n): add section labels for new settings layout"
```

### Task E.5: Riscrivere `settings.xml` con layout L3 (hero preview + sezioni)

**Files:**
- Modify: `app/src/main/res/layout/settings.xml`

- [ ] **Step E.5.1: Sostituire interamente settings.xml**

Sovrascrivere `app/src/main/res/layout/settings.xml` con:

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/settingsScrollView"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@drawable/verde"
    android:fillViewport="true">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:id="@+id/settingsConstraintLayout"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:paddingHorizontal="20dp"
        android:paddingBottom="24dp"
        tools:context=".SettingsActivity">

        <TextView
            android:id="@+id/textViewTitle"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginTop="24dp"
            android:gravity="center"
            android:text="@string/settings_title_decorated"
            android:textSize="28sp"
            style="@style/CasinoTitle"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent" />

        <!-- Hero preview container -->
        <FrameLayout
            android:id="@+id/heroPreview"
            android:layout_width="0dp"
            android:layout_height="140dp"
            android:layout_marginTop="20dp"
            android:background="@drawable/casino_hero_preview_frame"
            android:padding="8dp"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toBottomOf="@id/textViewTitle">

            <TextView
                android:id="@+id/heroPreviewLabel"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_gravity="top|center_horizontal"
                android:layout_marginTop="2dp"
                android:text="@string/settings_label_preview"
                style="@style/CasinoSubtitle" />

            <ImageView
                android:id="@+id/heroBackgroundImage"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:layout_gravity="center"
                android:scaleType="centerCrop"
                android:layout_marginTop="18dp"
                android:layout_marginHorizontal="8dp"
                android:layout_marginBottom="8dp" />

            <LinearLayout
                android:id="@+id/heroCardRow"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_gravity="center"
                android:layout_marginTop="18dp"
                android:orientation="horizontal">

                <ImageView
                    android:id="@+id/heroCard1"
                    android:layout_width="36dp"
                    android:layout_height="54dp"
                    android:layout_marginEnd="4dp"
                    android:scaleType="fitXY" />

                <ImageView
                    android:id="@+id/heroCard2"
                    android:layout_width="36dp"
                    android:layout_height="54dp"
                    android:layout_marginHorizontal="4dp"
                    android:scaleType="fitXY" />

                <ImageView
                    android:id="@+id/heroCard3"
                    android:layout_width="36dp"
                    android:layout_height="54dp"
                    android:layout_marginHorizontal="4dp"
                    android:scaleType="fitXY" />

                <ImageView
                    android:id="@+id/heroCardBack"
                    android:layout_width="36dp"
                    android:layout_height="54dp"
                    android:layout_marginStart="4dp"
                    android:scaleType="fitXY" />
            </LinearLayout>
        </FrameLayout>

        <!-- Card Deck section -->
        <TextView
            android:id="@+id/labelCardDeck"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="24dp"
            android:text="@string/settings_label_card_deck"
            style="@style/CasinoSectionLabel"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toBottomOf="@id/heroPreview" />

        <LinearLayout
            android:id="@+id/cardTypeRow"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:orientation="horizontal"
            android:gravity="center"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toBottomOf="@id/labelCardDeck">

            <FrameLayout
                android:id="@+id/cardTypePiacentine"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginHorizontal="6dp"
                android:padding="3dp"
                android:background="@drawable/casino_card_tile_selectable"
                android:tag="piacentine"
                android:onClick="selectCardType"
                android:clickable="true"
                android:focusable="true">

                <ImageView
                    android:layout_width="56dp"
                    android:layout_height="80dp"
                    android:src="@drawable/piacentine_b1"
                    android:scaleType="fitXY" />
            </FrameLayout>

            <FrameLayout
                android:id="@+id/cardTypeNapoletane"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginHorizontal="6dp"
                android:padding="3dp"
                android:background="@drawable/casino_card_tile_selectable"
                android:tag="napoletane"
                android:onClick="selectCardType"
                android:clickable="true"
                android:focusable="true">

                <ImageView
                    android:layout_width="56dp"
                    android:layout_height="80dp"
                    android:src="@drawable/napoletane_b1"
                    android:scaleType="fitXY" />
            </FrameLayout>

            <FrameLayout
                android:id="@+id/cardTypeFrancesi"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginHorizontal="6dp"
                android:padding="3dp"
                android:background="@drawable/casino_card_tile_selectable"
                android:tag="francesi"
                android:onClick="selectCardType"
                android:clickable="true"
                android:focusable="true">

                <ImageView
                    android:layout_width="56dp"
                    android:layout_height="80dp"
                    android:src="@drawable/francesi_b1"
                    android:scaleType="fitXY" />
            </FrameLayout>
        </LinearLayout>

        <!-- Card Back section -->
        <TextView
            android:id="@+id/labelCardBack"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="24dp"
            android:text="@string/settings_label_card_back"
            style="@style/CasinoSectionLabel"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toBottomOf="@id/cardTypeRow" />

        <LinearLayout
            android:id="@+id/cardBackRow"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:orientation="horizontal"
            android:gravity="center"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toBottomOf="@id/labelCardBack">

            <FrameLayout
                android:id="@+id/cardBackBg1"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginHorizontal="6dp"
                android:padding="3dp"
                android:background="@drawable/casino_card_tile_selectable"
                android:tag="bg"
                android:onClick="selectCardBack"
                android:clickable="true"
                android:focusable="true">

                <ImageView
                    android:layout_width="56dp"
                    android:layout_height="80dp"
                    android:src="@drawable/bg"
                    android:scaleType="fitXY" />
            </FrameLayout>

            <FrameLayout
                android:id="@+id/cardBackBg2"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginHorizontal="6dp"
                android:padding="3dp"
                android:background="@drawable/casino_card_tile_selectable"
                android:tag="bg2"
                android:onClick="selectCardBack"
                android:clickable="true"
                android:focusable="true">

                <ImageView
                    android:layout_width="56dp"
                    android:layout_height="80dp"
                    android:src="@drawable/bg2"
                    android:scaleType="fitXY" />
            </FrameLayout>

            <FrameLayout
                android:id="@+id/cardBackBg3"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginHorizontal="6dp"
                android:padding="3dp"
                android:background="@drawable/casino_card_tile_selectable"
                android:tag="bg3"
                android:onClick="selectCardBack"
                android:clickable="true"
                android:focusable="true">

                <ImageView
                    android:layout_width="56dp"
                    android:layout_height="80dp"
                    android:src="@drawable/bg3"
                    android:scaleType="fitXY" />
            </FrameLayout>
        </LinearLayout>

        <!-- Background section -->
        <TextView
            android:id="@+id/labelBackground"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="24dp"
            android:text="@string/settings_label_background"
            style="@style/CasinoSectionLabel"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toBottomOf="@id/cardBackRow" />

        <LinearLayout
            android:id="@+id/backgroundRow"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:orientation="horizontal"
            android:gravity="center"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toBottomOf="@id/labelBackground">

            <FrameLayout
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginHorizontal="4dp"
                android:padding="3dp"
                android:background="@drawable/casino_card_tile_selectable"
                android:tag="verde"
                android:onClick="selectBackground"
                android:clickable="true"
                android:focusable="true">

                <ImageView
                    android:layout_width="44dp"
                    android:layout_height="44dp"
                    android:background="@drawable/verde" />
            </FrameLayout>

            <FrameLayout
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginHorizontal="4dp"
                android:padding="3dp"
                android:background="@drawable/casino_card_tile_selectable"
                android:tag="bordeaux"
                android:onClick="selectBackground"
                android:clickable="true"
                android:focusable="true">

                <ImageView
                    android:layout_width="44dp"
                    android:layout_height="44dp"
                    android:background="@drawable/bordeaux" />
            </FrameLayout>

            <FrameLayout
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginHorizontal="4dp"
                android:padding="3dp"
                android:background="@drawable/casino_card_tile_selectable"
                android:tag="tappeto"
                android:onClick="selectBackground"
                android:clickable="true"
                android:focusable="true">

                <ImageView
                    android:layout_width="44dp"
                    android:layout_height="44dp"
                    android:src="@drawable/tappeto"
                    android:scaleType="centerCrop" />
            </FrameLayout>

            <FrameLayout
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginHorizontal="4dp"
                android:padding="3dp"
                android:background="@drawable/casino_card_tile_selectable"
                android:tag="legno"
                android:onClick="selectBackground"
                android:clickable="true"
                android:focusable="true">

                <ImageView
                    android:layout_width="44dp"
                    android:layout_height="44dp"
                    android:src="@drawable/legno"
                    android:scaleType="centerCrop" />
            </FrameLayout>

            <FrameLayout
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginHorizontal="4dp"
                android:padding="3dp"
                android:background="@drawable/casino_card_tile_selectable"
                android:tag="panno"
                android:onClick="selectBackground"
                android:clickable="true"
                android:focusable="true">

                <ImageView
                    android:layout_width="44dp"
                    android:layout_height="44dp"
                    android:src="@drawable/panno"
                    android:scaleType="centerCrop" />
            </FrameLayout>
        </LinearLayout>

        <!-- Fast deal switch -->
        <LinearLayout
            android:id="@+id/fastDealRow"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginTop="24dp"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:padding="12dp"
            android:background="@drawable/casino_tile_bg"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toBottomOf="@id/backgroundRow">

            <TextView
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="@string/fast_deal_with_icon"
                style="@style/CasinoBodyText" />

            <Switch
                android:id="@+id/switchFastDeal"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:thumb="@drawable/casino_gold_switch_thumb"
                android:track="@drawable/casino_gold_switch_track"
                android:onClick="changeFastDeal" />
        </LinearLayout>

        <!-- Credits -->
        <TextView
            android:id="@+id/textViewCredits"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginTop="32dp"
            android:gravity="center"
            android:text="@string/credits_text"
            android:textColor="@color/casino_gold_alpha_50"
            android:textSize="10sp"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toBottomOf="@id/fastDealRow" />

    </androidx.constraintlayout.widget.ConstraintLayout>
</ScrollView>
```

NOTA — riferimenti drawable carta usati nel layout: `piacentine_b1`, `napoletane_b1`, `francesi_b1`. Verifica che esistano:

Run: `ls app/src/main/res/drawable-xxhdpi/ | grep -E '(piacentine_b1|napoletane_b1|francesi_b1)'`

Se uno o più non esistono, scegli un'altra carta del mazzo corrispondente (es. `piacentine_c1`, `napoletane_c1`, `francesi_c1`) e aggiorna il riferimento `android:src` nel layout sopra.

- [ ] **Step E.5.2: Build di verifica**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

Se la build fallisce per drawable mancante (es. `piacentine_b1` non esiste), aggiorna i riferimenti drawable nel layout con carte realmente presenti.

- [ ] **Step E.5.3: Commit**

```bash
git add app/src/main/res/layout/settings.xml
git commit -m "feat(settings): rewrite as L3 layout (hero preview + card tiles + gold switch)"
```

### Task E.6: Riscrivere `SettingsActivity.kt` per il nuovo layout

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/SettingsActivity.kt`

- [ ] **Step E.6.1: Sostituire il contenuto di SettingsActivity**

Sovrascrivere `app/src/main/java/com/bottazzini/trasloco/SettingsActivity.kt` con:

```kotlin
package com.bottazzini.trasloco

import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bottazzini.trasloco.settings.Configuration
import com.bottazzini.trasloco.settings.SettingsHandler
import com.bottazzini.trasloco.utils.ResourceUtils
import com.bottazzini.trasloco.utils.WindowInsetsUtils

class SettingsActivity : AppCompatActivity() {

    private lateinit var settingsHandler: SettingsHandler

    private val cardTypeTileIds = listOf(R.id.cardTypePiacentine, R.id.cardTypeNapoletane, R.id.cardTypeFrancesi)
    private val cardBackTileIds = listOf(R.id.cardBackBg1, R.id.cardBackBg2, R.id.cardBackBg3)
    private lateinit var backgroundTileIds: List<Int>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.settings)
        WindowInsetsUtils.applySystemBarInsets(window, findViewById(R.id.settingsScrollView))
        supportActionBar?.hide()

        settingsHandler = SettingsHandler(applicationContext)

        // Discover all FrameLayouts in backgroundRow (they have tags)
        val bgRow = findViewById<ViewGroup>(R.id.backgroundRow)
        val ids = mutableListOf<Int>()
        for (i in 0 until bgRow.childCount) {
            val child = bgRow.getChildAt(i)
            if (child.id != View.NO_ID) ids.add(child.id)
        }
        backgroundTileIds = ids

        readConfigurations()
    }

    fun selectCardType(view: View) {
        val tag = view.tag?.toString() ?: return
        settingsHandler.updateSetting(Configuration.CARD_TYPE.value, tag)
        updateSelection(cardTypeTileIds, tag)
        updateHeroPreview()
    }

    fun selectCardBack(view: View) {
        val tag = view.tag?.toString() ?: return
        settingsHandler.updateSetting(Configuration.CARD_BACK.value, tag)
        updateSelection(cardBackTileIds, tag)
        updateHeroPreview()
    }

    fun selectBackground(view: View) {
        val tag = view.tag?.toString() ?: return
        settingsHandler.updateSetting(Configuration.BACKGROUND.value, tag)
        updateSelection(backgroundTileIds, tag)
        applyScreenBackground(tag)
        updateHeroPreview()
    }

    fun changeFastDeal(view: View) {
        val switch = findViewById<Switch>(R.id.switchFastDeal)
        val value = if (switch.isChecked) "enabled" else "disabled"
        settingsHandler.updateSetting(Configuration.FAST_DEAL.value, value)
    }

    override fun onDestroy() {
        settingsHandler.close()
        super.onDestroy()
    }

    private fun updateSelection(tileIds: List<Int>, selectedTag: String) {
        tileIds.forEach { id ->
            val view = findViewById<View>(id)
            view.isSelected = (view.tag?.toString() == selectedTag)
        }
    }

    private fun applyScreenBackground(backgroundTag: String) {
        val drawableId = resources.getIdentifier(backgroundTag, "drawable", packageName)
        val root = findViewById<View>(R.id.settingsScrollView)
        if (drawableId != 0) {
            root.background = ContextCompat.getDrawable(this, drawableId)
        }
    }

    private fun updateHeroPreview() {
        val backgroundTag = settingsHandler.readValue(Configuration.BACKGROUND.value) ?: "verde"
        val cardTypeTag = settingsHandler.readValue(Configuration.CARD_TYPE.value) ?: "piacentine"
        val cardBackTag = settingsHandler.readValue(Configuration.CARD_BACK.value) ?: "bg2"

        // Update hero background
        val heroBg = findViewById<ImageView>(R.id.heroBackgroundImage)
        val bgDrawableId = resources.getIdentifier(backgroundTag, "drawable", packageName)
        if (bgDrawableId != 0) {
            heroBg.setImageDrawable(ContextCompat.getDrawable(this, bgDrawableId))
        }

        // Update 3 cards (use card 1, 2, 3 of card type) + 1 card back
        val cardIds = listOf(R.id.heroCard1, R.id.heroCard2, R.id.heroCard3)
        val sampleCards = listOf("${cardTypeTag}_b1", "${cardTypeTag}_c1", "${cardTypeTag}_d1")
        cardIds.forEachIndexed { idx, viewId ->
            val img = findViewById<ImageView>(viewId)
            val drawableId = resources.getIdentifier(sampleCards[idx], "drawable", packageName)
            if (drawableId != 0) {
                img.setImageDrawable(ContextCompat.getDrawable(this, drawableId))
            } else {
                img.setImageDrawable(null)
            }
        }

        // Update card back
        val backImg = findViewById<ImageView>(R.id.heroCardBack)
        val backDrawableId = resources.getIdentifier(cardBackTag, "drawable", packageName)
        if (backDrawableId != 0) {
            backImg.setImageDrawable(ContextCompat.getDrawable(this, backDrawableId))
        }
    }

    private fun readConfigurations() {
        val fastDeal = settingsHandler.readValue(Configuration.FAST_DEAL.value) ?: "disabled"
        findViewById<Switch>(R.id.switchFastDeal).isChecked = (fastDeal == "enabled")

        val cardType = settingsHandler.readValue(Configuration.CARD_TYPE.value) ?: "piacentine"
        updateSelection(cardTypeTileIds, cardType)

        val cardBack = settingsHandler.readValue(Configuration.CARD_BACK.value) ?: "bg2"
        updateSelection(cardBackTileIds, cardBack)

        val background = settingsHandler.readValue(Configuration.BACKGROUND.value) ?: "verde"
        updateSelection(backgroundTileIds, background)

        applyScreenBackground(background)
        updateHeroPreview()
    }
}
```

NOTA: assicurarsi che i nomi delle carte sample (`${cardTypeTag}_b1` etc.) corrispondano a drawable esistenti. Se in `piacentine` non esiste `piacentine_b1` ma esiste `piacentine_c1`, modificare i `sampleCards` in modo appropriato. Verificare anche per `napoletane`.

- [ ] **Step E.6.2: Verifica drawable carte per i 3 tipi**

Run:
```bash
ls app/src/main/res/drawable-xxhdpi/ | grep -E '^piacentine_(b|c|d)1\.' | head
ls app/src/main/res/drawable-xxhdpi/ | grep -E '^napoletane_(b|c|d)1\.' | head
ls app/src/main/res/drawable-xxhdpi/ | grep -E '^francesi_(b|c|d)1\.' | head
```

Se per `napoletane` esistono solo carte con suffisso diverso (es. `c1`, non `b1`), modifica `sampleCards` di conseguenza nel file appena scritto.

- [ ] **Step E.6.3: Build di verifica**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step E.6.4: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/SettingsActivity.kt
git commit -m "feat(settings): implement L3 with hero preview + card tile selectors"
```

### Task E.7: Smoke test settings

- [ ] **Step E.7.1: Installa + verifica manualmente**

Run: `./gradlew installDebug`

Aprire l'app, tap su "⚙ Impostazioni". Verificare:
- Titolo "~ Impostazioni ~" in serif italico oro
- Anteprima in cima con cornice dorata: mostra sfondo corrente + 3 mini-carte del tipo selezionato + retro selezionato
- Sezione "MAZZO CARTE" con 3 tile clickabili (Piacentine, Napoletane, Francesi); quella selezionata ha bordo dorato
- Sezione "RETRO CARTE" con 3 mini-retri clickabili
- Sezione "TAPPETO" con 5 swatch (verde, bordeaux, tappeto, legno, panno)
- Switch "⚡ Trasferisci intero mazzo" con thumb dorato lucido
- Cambiando un setting, l'anteprima si aggiorna immediatamente
- Cambiando sfondo, anche lo sfondo della schermata cambia
- Tornare al menu, riaprire settings → le scelte sono persistite

---

## Phase F — Carta selezionata sempre visibile (G3a)

### Task F.1: Drawable `selected_card_glow.xml` (sostituisce `selected_border.xml`)

Verifica prima cosa fa il drawable esistente `selected_border.xml`:

**Files:**
- Modify: `app/src/main/res/drawable/selected_border.xml`

- [ ] **Step F.1.1: Aggiornare il drawable selected_border per glow oro casino**

Sovrascrivere `app/src/main/res/drawable/selected_border.xml` con:

```xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- Outer glow (soft) -->
    <item>
        <shape android:shape="rectangle">
            <stroke android:width="4dp" android:color="@color/casino_gold_alpha_50" />
            <corners android:radius="6dp" />
        </shape>
    </item>
    <!-- Inner solid gold border -->
    <item android:left="2dp" android:top="2dp" android:right="2dp" android:bottom="2dp">
        <shape android:shape="rectangle">
            <stroke android:width="2dp" android:color="@color/casino_gold" />
            <corners android:radius="4dp" />
        </shape>
    </item>
</layer-list>
```

- [ ] **Step F.1.2: Build di verifica**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step F.1.3: Commit**

```bash
git add app/src/main/res/drawable/selected_border.xml
git commit -m "feat(game): update selected card glow to casino gold double border"
```

### Task F.2: Verifica visiva selezione carta in-game

L'highlight della carta selezionata in `GameActivity.kt` usa già `selected_border` (verifica con grep). Se sì, basta il drawable change per veder funzionare.

- [ ] **Step F.2.1: Verificare uso di `selected_border` in GameActivity**

Run: `grep -n "selected_border\|R.drawable.selected_border" app/src/main/java/com/bottazzini/trasloco/GameActivity.kt`

Se trova riferimenti: il drawable cambia automaticamente l'highlight, no codice da modificare.
Se NON trova riferimenti: la selezione è probabilmente solo testuale (`selectedCard: String?`). In tal caso, procedere con F.3 per aggiungerla.

- [ ] **Step F.2.2: Installa + verifica visiva**

Run: `./gradlew installDebug`

Aprire l'app → Nuova partita → tap su una carta del tavolo. La carta selezionata deve avere ora il glow oro doppio bordo.

Se la selezione visiva non appare, procedere con Task F.3. Altrimenti saltare a Task F.4.

### Task F.3 (condizionale): Aggiungere highlight visivo se non c'è

Da eseguire SOLO se Task F.2 mostra che la selezione attualmente non è visualizzata.

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/GameActivity.kt`

- [ ] **Step F.3.1: Identificare il metodo di selezione carta**

Run: `grep -n "selectedCard\|setBackgroundResource\|setBackground" app/src/main/java/com/bottazzini/trasloco/GameActivity.kt | head -30`

Identificare il punto in cui `selectedCard` viene impostato. Lì, recuperare la `ImageView` corrispondente e applicare:

```kotlin
imageView.setBackgroundResource(R.drawable.selected_border)
```

E quando la selezione viene rimossa (`selectedCard = null`):

```kotlin
imageView.setBackgroundResource(0)
```

NOTA: questa modifica richiede di adattare il codice esistente — il piano qui è strutturale, l'implementazione precisa dipende dal layout esatto del game. Da fare in singolo commit con messaggio:

```bash
git commit -m "feat(game): highlight selected card with casino gold border"
```

### Task F.4: Smoke test partita completa

- [ ] **Step F.4.1: Verifica end-to-end**

Aprire l'app → Nuova partita. Fare una partita completa (anche persa). Verificare:
- Splash animato OK
- Main menu con tile grid OK
- Inizia partita → tavolo carte familiare
- Tap carta → highlight oro visibile
- Drag&drop carta verso slot OK
- Vittoria/sconfitta → schermata esistente (W3 non in v1.10)

---

## Phase G — Versione + cleanup finale

### Task G.1: Bump versionCode + versionName

**Files:**
- Modify: `app/build.gradle`

- [ ] **Step G.1.1: Aggiornare versione**

In `app/build.gradle`, modificare:

```
versionCode 20
versionName "1.9.0"
```

In:

```
versionCode 21
versionName "1.10.0"
```

- [ ] **Step G.1.2: Build di verifica**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step G.1.3: Commit**

```bash
git add app/build.gradle
git commit -m "chore(release): bump version to 1.10.0 (versionCode 21)"
```

### Task G.2: Smoke test finale completo

- [ ] **Step G.2.1: Build release**

Run: `./gradlew assembleRelease`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step G.2.2: Installa + smoke completo**

Run: `./gradlew installDebug`

Eseguire l'intero flusso utente:
1. Apri app → splash animata OK
2. Tap durante splash dopo 0.5s → skip OK
3. Main menu casinò OK
4. 📜 Regole (corner) OK
5. Triple tap sul titolo → mostra versione 1.10.0
6. 🏆 Records OK
7. ⚙ Impostazioni → tutti i selettori funzionano, anteprima dal vivo OK, switch dorato OK
8. ▶ Nuova Partita → tavolo OK
9. Selezione carta → glow oro visibile
10. Fai una mossa → corretto
11. Back to menu → "Riprendi" sempre disabilitato (v1.10 placeholder)

- [ ] **Step G.2.3: Verifica APK release**

Run: `ls -lh app/build/outputs/apk/release/`
Expected: file APK presente, dimensione ragionevole (< 15MB)

### Task G.3: Aggiornare TODO (rimuovere voci coperte)

**Files:**
- Modify: `TODO`

- [ ] **Step G.3.1: Aggiornare il TODO file**

Il file `TODO` attuale ha:

```
- Show card selection visually instead of a text
- Add settings to choose card types, back card types and mat
- Add confiuration to enable or disable "move all the cards once it is ready for the final deck"
- Allow to resume previous game
- Add button to make a TIP of a play (is it really needed?)
```

Sostituire con:

```
- Add configuration to enable or disable "move all the cards once it is ready for the final deck"  [v1.12]
- Allow to resume previous game  [v1.12]
- Add button to make a TIP of a play (hint)  [v1.11]
```

(Le voci "card selection visual" e "settings card types/back/mat" sono completate in v1.10.)

- [ ] **Step G.3.2: Commit**

```bash
git add TODO
git commit -m "chore: update TODO with version milestones"
```

### Task G.4: PR / merge readiness

- [ ] **Step G.4.1: Verifica branch pulito**

Run: `git status`
Expected: `nothing to commit, working tree clean`

- [ ] **Step G.4.2: Verifica log commit**

Run: `git log --oneline main..feature/v1.10-casino-redesign`
Expected: lista ordinata di ~35-40 commit, ciascuno con messaggio descrittivo

- [ ] **Step G.4.3: Verifica nessun "Co-Authored-By: Claude"**

Run: `git log main..feature/v1.10-casino-redesign | grep -i "co-authored" || echo "OK no co-author trailers"`
Expected: `OK no co-author trailers`

- [ ] **Step G.4.4: Annuncia completamento**

Riporta all'utente:
- Numero commit
- Eventuali deviazioni dal piano (es. drawable name mismatch corretti durante l'esecuzione)
- Comando per merge: `git checkout main && git merge --no-ff feature/v1.10-casino-redesign`
- Comando per release: dipende dal flusso Play Store dell'utente

NOTA: NON merge-are né push-are senza esplicita richiesta utente.

---

## Riepilogo task

| Phase | Tasks | Stima |
|---|---|---|
| 0 — Pre-flight | 1 | ~10 min |
| A — Design tokens | 2 | ~30 min |
| B — Background asset rework | 5 | ~2-3 ore |
| C — Splash animata | 9 | ~6-8 ore |
| D — Main menu M3 | 7 | ~4-6 ore |
| E — Settings L3 | 7 | ~8-10 ore |
| F — Carta selezionata visibile | 4 | ~2-4 ore |
| G — Versione + finalizzazione | 4 | ~2-3 ore |
| **TOTALE** | **39 task** | **~10-12 gg** |

## Fuori scope di v1.10 (per memoria)

Da v1.11+ (in plan future):
- Polish P1 in-game (frames, drop zone glow durante drag, animazioni mosse)
- Top bar C1 (back / timer / pausa / hint)
- Hint engine (G3b)
- Auto-move (G3c)
- Resume game (G3d) — abilitazione bottone "Riprendi" placeholder
- Win screen W3 (trofeo + stat)
- Stats tracking esteso (best_time, total_wins)
