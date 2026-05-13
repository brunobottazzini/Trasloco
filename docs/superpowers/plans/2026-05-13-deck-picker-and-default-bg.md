# Deck Picker & Default Background Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show a one-time full-screen deck selection screen to new users before the main menu, and make the main menu background dynamic (defaulting to bordeaux).

**Architecture:** `SplashActivity` (LAUNCHER) → `MainActivity` (checks `deck_chosen` flag, redirects new users to `DeckPickerActivity`) → `DeckPickerActivity` (saves choice, starts `MainActivity` fresh). `MainActivity` reads `BACKGROUND` setting dynamically like `StatsActivity` already does. `SettingsHandler.insertDefaultSettings()` changes BACKGROUND default from `"tappeto"` to `"bordeaux"`.

**Tech Stack:** Kotlin, Android SDK, existing `SettingsHandler` / `Configuration` enum, `SharedPreferences("trasloco_prefs")`, existing drawables (`piacentine_b1/c1/d1`, `napoletane_b1/c1/d1`, `francesi_b1/c1/d1`, `casino_tile_bg`, `casino_tile_bg_primary`, `bordeaux`), `ResourceUtils.getDrawableByName`, `WindowInsetsUtils.applySystemBarInsets`.

---

## File Map

| File | Action |
|------|--------|
| `app/src/main/res/values/strings.xml` | Add 3 deck picker strings |
| `app/src/main/res/values-it/strings.xml` | Add 3 deck picker strings |
| `app/src/main/res/values-pt/strings.xml` | Add 3 deck picker strings |
| `app/src/main/res/layout/activity_deck_picker.xml` | Create — full-screen picker layout |
| `app/src/main/java/com/bottazzini/trasloco/DeckPickerActivity.kt` | Create — picker logic |
| `app/src/main/AndroidManifest.xml` | Add `DeckPickerActivity` with `noHistory` |
| `app/src/main/java/com/bottazzini/trasloco/MainActivity.kt` | Add deck-chosen gate + dynamic background |
| `app/src/main/res/layout/activity_main.xml` | Remove hardcoded background from ScrollView |
| `app/src/main/java/com/bottazzini/trasloco/settings/SettingsHandler.kt` | Change BACKGROUND default to `"bordeaux"` |

---

### Task 1: Add deck picker strings

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-it/strings.xml`
- Modify: `app/src/main/res/values-pt/strings.xml`

No tests — string resources are verified at compile time.

- [ ] **Step 1: Add strings to values/strings.xml**

Find the closing `</resources>` tag and insert before it:

```xml
    <string name="deck_picker_title">Choose your deck</string>
    <string name="deck_picker_subtitle">You can change this later in Settings</string>
    <string name="deck_picker_button">Start</string>
```

- [ ] **Step 2: Add strings to values-it/strings.xml**

Find the closing `</resources>` tag and insert before it:

```xml
    <string name="deck_picker_title">Scegli il tuo mazzo</string>
    <string name="deck_picker_subtitle">Potrai cambiarlo nelle Impostazioni</string>
    <string name="deck_picker_button">Inizia</string>
```

- [ ] **Step 3: Add strings to values-pt/strings.xml**

Find the closing `</resources>` tag and insert before it:

```xml
    <string name="deck_picker_title">Escolha seu baralho</string>
    <string name="deck_picker_subtitle">Você pode mudar isso nas Configurações</string>
    <string name="deck_picker_button">Começar</string>
```

- [ ] **Step 4: Verify build compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/values/strings.xml \
        app/src/main/res/values-it/strings.xml \
        app/src/main/res/values-pt/strings.xml
git commit -m "feat(deck-picker): add strings for deck picker screen (EN/IT/PT)"
```

---

### Task 2: Create deck picker layout

**Files:**
- Create: `app/src/main/res/layout/activity_deck_picker.xml`

- [ ] **Step 1: Create `activity_deck_picker.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/deckPickerScrollView"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@drawable/bordeaux"
    android:fillViewport="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="24dp"
        android:gravity="center_horizontal">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="32dp"
            android:gravity="center"
            android:text="@string/deck_picker_title"
            android:textSize="26sp"
            style="@style/CasinoTitle" />

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:gravity="center"
            android:text="@string/deck_picker_subtitle"
            android:textSize="12sp"
            style="@style/CasinoSubtitle" />

        <!-- Piacentine tile -->
        <LinearLayout
            android:id="@+id/tilePiacentine"
            android:tag="piacentine"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="24dp"
            android:background="@drawable/casino_tile_bg"
            android:orientation="vertical"
            android:padding="16dp"
            android:clickable="true"
            android:focusable="true">

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:gravity="center"
                android:text="@string/card_type_piacentine"
                android:textSize="14sp"
                android:fontFamily="serif"
                android:textStyle="italic|bold"
                android:textColor="@color/casino_gold"
                android:layout_marginBottom="12dp" />

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal">

                <ImageView
                    android:layout_width="0dp"
                    android:layout_height="90dp"
                    android:layout_weight="1"
                    android:src="@drawable/piacentine_b1"
                    android:scaleType="centerInside"
                    android:layout_marginHorizontal="4dp" />

                <ImageView
                    android:layout_width="0dp"
                    android:layout_height="90dp"
                    android:layout_weight="1"
                    android:src="@drawable/piacentine_c1"
                    android:scaleType="centerInside"
                    android:layout_marginHorizontal="4dp" />

                <ImageView
                    android:layout_width="0dp"
                    android:layout_height="90dp"
                    android:layout_weight="1"
                    android:src="@drawable/piacentine_d1"
                    android:scaleType="centerInside"
                    android:layout_marginHorizontal="4dp" />
            </LinearLayout>
        </LinearLayout>

        <!-- Napoletane tile -->
        <LinearLayout
            android:id="@+id/tileNapoletane"
            android:tag="napoletane"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            android:background="@drawable/casino_tile_bg"
            android:orientation="vertical"
            android:padding="16dp"
            android:clickable="true"
            android:focusable="true">

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:gravity="center"
                android:text="@string/card_type_napoletane"
                android:textSize="14sp"
                android:fontFamily="serif"
                android:textStyle="italic|bold"
                android:textColor="@color/casino_gold"
                android:layout_marginBottom="12dp" />

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal">

                <ImageView
                    android:layout_width="0dp"
                    android:layout_height="90dp"
                    android:layout_weight="1"
                    android:src="@drawable/napoletane_b1"
                    android:scaleType="centerInside"
                    android:layout_marginHorizontal="4dp" />

                <ImageView
                    android:layout_width="0dp"
                    android:layout_height="90dp"
                    android:layout_weight="1"
                    android:src="@drawable/napoletane_c1"
                    android:scaleType="centerInside"
                    android:layout_marginHorizontal="4dp" />

                <ImageView
                    android:layout_width="0dp"
                    android:layout_height="90dp"
                    android:layout_weight="1"
                    android:src="@drawable/napoletane_d1"
                    android:scaleType="centerInside"
                    android:layout_marginHorizontal="4dp" />
            </LinearLayout>
        </LinearLayout>

        <!-- Francesi tile -->
        <LinearLayout
            android:id="@+id/tileFrancesi"
            android:tag="francesi"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            android:background="@drawable/casino_tile_bg"
            android:orientation="vertical"
            android:padding="16dp"
            android:clickable="true"
            android:focusable="true">

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:gravity="center"
                android:text="@string/card_type_francesi"
                android:textSize="14sp"
                android:fontFamily="serif"
                android:textStyle="italic|bold"
                android:textColor="@color/casino_gold"
                android:layout_marginBottom="12dp" />

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal">

                <ImageView
                    android:layout_width="0dp"
                    android:layout_height="90dp"
                    android:layout_weight="1"
                    android:src="@drawable/francesi_b1"
                    android:scaleType="centerInside"
                    android:layout_marginHorizontal="4dp" />

                <ImageView
                    android:layout_width="0dp"
                    android:layout_height="90dp"
                    android:layout_weight="1"
                    android:src="@drawable/francesi_c1"
                    android:scaleType="centerInside"
                    android:layout_marginHorizontal="4dp" />

                <ImageView
                    android:layout_width="0dp"
                    android:layout_height="90dp"
                    android:layout_weight="1"
                    android:src="@drawable/francesi_d1"
                    android:scaleType="centerInside"
                    android:layout_marginHorizontal="4dp" />
            </LinearLayout>
        </LinearLayout>

        <Button
            android:id="@+id/buttonDeckConfirm"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="28dp"
            android:layout_marginBottom="32dp"
            android:minWidth="160dp"
            android:background="@drawable/casino_tile_bg_primary"
            android:fontFamily="serif"
            android:padding="14dp"
            android:text="@string/deck_picker_button"
            android:textColor="@color/casino_gold"
            android:textSize="16sp"
            android:textStyle="italic|bold"
            android:enabled="false"
            android:alpha="0.4" />

    </LinearLayout>
</ScrollView>
```

- [ ] **Step 2: Verify build**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (no resource errors)

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/layout/activity_deck_picker.xml
git commit -m "feat(deck-picker): add deck picker layout with 3 tile options"
```

---

### Task 3: Create DeckPickerActivity

**Files:**
- Create: `app/src/main/java/com/bottazzini/trasloco/DeckPickerActivity.kt`

- [ ] **Step 1: Create `DeckPickerActivity.kt`**

```kotlin
package com.bottazzini.trasloco

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bottazzini.trasloco.settings.Configuration
import com.bottazzini.trasloco.settings.SettingsHandler
import com.bottazzini.trasloco.utils.WindowInsetsUtils

class DeckPickerActivity : AppCompatActivity() {

    private lateinit var settingsHandler: SettingsHandler
    private var selectedTag: String? = null

    private lateinit var tiles: List<LinearLayout>
    private lateinit var confirmButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        supportActionBar?.hide()
        setContentView(R.layout.activity_deck_picker)
        WindowInsetsUtils.applySystemBarInsets(window, findViewById(R.id.deckPickerScrollView))

        settingsHandler = SettingsHandler(applicationContext)

        tiles = listOf(
            findViewById(R.id.tilePiacentine),
            findViewById(R.id.tileNapoletane),
            findViewById(R.id.tileFrancesi)
        )
        confirmButton = findViewById(R.id.buttonDeckConfirm)

        tiles.forEach { tile ->
            tile.setOnClickListener { onTileSelected(tile) }
        }

        confirmButton.setOnClickListener { confirmSelection() }
    }

    private fun onTileSelected(selected: LinearLayout) {
        tiles.forEach { tile ->
            tile.background = ContextCompat.getDrawable(this, R.drawable.casino_tile_bg)
        }
        selected.background = ContextCompat.getDrawable(this, R.drawable.casino_tile_bg_primary)
        selectedTag = selected.tag as String
        confirmButton.isEnabled = true
        confirmButton.alpha = 1f
    }

    private fun confirmSelection() {
        val tag = selectedTag ?: return
        settingsHandler.updateSetting(Configuration.CARD_TYPE.value, tag)
        getSharedPreferences("trasloco_prefs", MODE_PRIVATE)
            .edit().putBoolean("deck_chosen", true).apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
```

- [ ] **Step 2: Verify build**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/DeckPickerActivity.kt
git commit -m "feat(deck-picker): implement DeckPickerActivity with tile selection"
```

---

### Task 4: Wire manifest, MainActivity, and default background

**Files:**
- Modify: `app/src/main/AndroidManifest.xml` (add DeckPickerActivity)
- Modify: `app/src/main/java/com/bottazzini/trasloco/MainActivity.kt` (gate + dynamic bg)
- Modify: `app/src/main/res/layout/activity_main.xml` (remove hardcoded background)
- Modify: `app/src/main/java/com/bottazzini/trasloco/settings/SettingsHandler.kt` (change BACKGROUND default)

- [ ] **Step 1: Add DeckPickerActivity to AndroidManifest.xml**

After the line `<activity android:name=".StatsActivity" android:theme="@style/Theme.Trasloco" />`, add:

```xml
        <activity android:name=".DeckPickerActivity" android:theme="@style/Theme.Trasloco" android:noHistory="true" />
```

- [ ] **Step 2: Change BACKGROUND default in SettingsHandler.kt**

In `insertDefaultSettings()`, change:
```kotlin
setDefaultSetting(Configuration.BACKGROUND.value, "tappeto")
```
to:
```kotlin
setDefaultSetting(Configuration.BACKGROUND.value, "bordeaux")
```

- [ ] **Step 3: Remove hardcoded background from activity_main.xml**

In `activity_main.xml`, find the `<ScrollView` element and remove the line:
```
android:background="@drawable/verde"
```

- [ ] **Step 4: Add deck-chosen gate and dynamic background to MainActivity.kt**

The full `onCreate` method becomes (replace the existing one):

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // One-time deck picker gate (skip for existing users who already played)
    val prefs = getSharedPreferences("trasloco_prefs", MODE_PRIVATE)
    if (prefs.getBoolean("tutorial_seen", false) && !prefs.getBoolean("deck_chosen", false)) {
        prefs.edit().putBoolean("deck_chosen", true).apply()
    }
    if (!prefs.getBoolean("deck_chosen", false)) {
        startActivity(Intent(this, DeckPickerActivity::class.java))
        finish()
        return
    }

    enableEdgeToEdge()
    requestWindowFeature(Window.FEATURE_NO_TITLE)
    setContentView(R.layout.activity_main)
    WindowInsetsUtils.applySystemBarInsets(window, findViewById(R.id.mainScrollView))
    supportActionBar?.hide()

    // Dynamic background (follows user setting, defaults to bordeaux for new users)
    settingsHandler = SettingsHandler(applicationContext)
    val bg = settingsHandler.readValue(Configuration.BACKGROUND.value) ?: "bordeaux"
    val bgDrawable = com.bottazzini.trasloco.utils.ResourceUtils.getDrawableByName(resources, packageName, bg)
    findViewById<View>(R.id.mainScrollView).background = ContextCompat.getDrawable(this, bgDrawable)

    settingsHandler.insertDefaultSettings()
    settingsHandler.migrateRemovedBackgrounds()
    gameStateRepo = com.bottazzini.trasloco.settings.GameStateRepository(applicationContext)
    recordsHandler = RecordsHandler(applicationContext)
    recordsHandler.insertDefaultSettings()
    val achievementBanner = com.bottazzini.trasloco.utils.AchievementBanner(
        this, findViewById(R.id.mainBannerAchievement)
    )
    val newAchievements = com.bottazzini.trasloco.utils.AchievementEngine.create(applicationContext)
        .evaluate(com.bottazzini.trasloco.utils.AchievementTrigger.APP_OPENED)
    achievementBanner.enqueue(newAchievements)

    findViewById<View>(R.id.textViewTitle).setOnClickListener {
        handleTripleTap()
    }
}
```

Also add the missing import at the top of `MainActivity.kt` if not already present:
```kotlin
import androidx.core.content.ContextCompat
import com.bottazzini.trasloco.settings.Configuration
```

- [ ] **Step 5: Verify build**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Manual smoke test**

Install a fresh build (clear app data first or use a fresh emulator):
1. Open app → SplashActivity plays → MainActivity detects `deck_chosen=false` → DeckPickerActivity opens
2. Tap Napoletane tile → gold border appears, "Inizia" button enabled
3. Tap "Inizia" → MainActivity opens with bordeaux background
4. Tap "Nuova Partita" → tutorial prompt appears (normal flow unchanged)
5. Close and reopen app → DeckPickerActivity is NOT shown again
6. Open Settings → change background to verde → go back → main menu now shows verde

- [ ] **Step 7: Commit**

```bash
git add app/src/main/AndroidManifest.xml \
        app/src/main/java/com/bottazzini/trasloco/MainActivity.kt \
        app/src/main/res/layout/activity_main.xml \
        app/src/main/java/com/bottazzini/trasloco/settings/SettingsHandler.kt
git commit -m "feat(deck-picker): wire gate in MainActivity, dynamic bg, bordeaux default"
```
