# Deck Selector Grid Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the ViewPager2 carousel deck selector with a geographic-tab grid in both DeckPickerActivity and SettingsActivity, with full large-font accessibility support.

**Architecture:** A new `DeckGridAdapter` drives a `RecyclerView` with `GridLayoutManager(3)`. Tiles show only card previews (no text, sized in `dp`). The selected deck name is displayed in a separate `TextView` (in `sp`) above the grid. Three geographic `TabLayout` tabs (Nord / Sud & Isole / Internazionali) filter the grid content.

**Tech Stack:** Kotlin, Android Views, RecyclerView + GridLayoutManager, Material TabLayout, existing `casino_card_tile_selectable` drawable for selection state.

---

### Task 1: DeckRegion enum + CardDeck region property + CardDeckRegistry.byRegion()

**Files:**
- Create: `app/src/main/java/com/bottazzini/trasloco/utils/DeckRegion.kt`
- Modify: `app/src/main/java/com/bottazzini/trasloco/utils/CardDeck.kt`
- Create: `app/src/test/java/com/bottazzini/trasloco/utils/CardDeckRegistryTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/bottazzini/trasloco/utils/CardDeckRegistryTest.kt`:

```kotlin
package com.bottazzini.trasloco.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardDeckRegistryTest {

    @Test
    fun `byRegion NORD contains piacentine and 11 decks`() {
        val nord = CardDeckRegistry.byRegion(DeckRegion.NORD)
        assertEquals(11, nord.size)
        assertTrue(nord.any { it.id == "piacentine" })
        assertTrue(nord.all { it.region == DeckRegion.NORD })
    }

    @Test
    fun `byRegion SUD_ISOLE contains napoletane sarde siciliane`() {
        val sud = CardDeckRegistry.byRegion(DeckRegion.SUD_ISOLE)
        assertEquals(3, sud.size)
        assertTrue(sud.any { it.id == "napoletane" })
        assertTrue(sud.any { it.id == "sarde" })
        assertTrue(sud.any { it.id == "siciliane" })
    }

    @Test
    fun `byRegion INTERNAZIONALI contains only francesi`() {
        val intl = CardDeckRegistry.byRegion(DeckRegion.INTERNAZIONALI)
        assertEquals(1, intl.size)
        assertEquals("francesi", intl[0].id)
    }

    @Test
    fun `all 15 decks are covered across regions`() {
        val total = DeckRegion.values().sumOf { CardDeckRegistry.byRegion(it).size }
        assertEquals(15, total)
    }

    @Test
    fun `piacentine is first in NORD`() {
        assertEquals("piacentine", CardDeckRegistry.byRegion(DeckRegion.NORD)[0].id)
    }
}
```

- [ ] **Step 2: Run test — expect compile failure (DeckRegion not defined)**

```bash
./gradlew testDebugUnitTest --tests "com.bottazzini.trasloco.utils.CardDeckRegistryTest" 2>&1 | tail -20
```

Expected: compilation error mentioning `DeckRegion`.

- [ ] **Step 3: Create DeckRegion enum**

Create `app/src/main/java/com/bottazzini/trasloco/utils/DeckRegion.kt`:

```kotlin
package com.bottazzini.trasloco.utils

enum class DeckRegion { NORD, SUD_ISOLE, INTERNAZIONALI }
```

- [ ] **Step 4: Add region property to CardDeck and update CardDeckRegistry**

Replace entire content of `app/src/main/java/com/bottazzini/trasloco/utils/CardDeck.kt`:

```kotlin
package com.bottazzini.trasloco.utils

import com.bottazzini.trasloco.R

data class CardDeck(
    val id: String,
    val labelRes: Int,
    val available: Boolean = true,
    val insetX: Float = 0f,
    val insetY: Float = 0f,
    val region: DeckRegion = DeckRegion.NORD,
)

object CardDeckRegistry {
    val ALL = listOf(
        CardDeck("piacentine",  R.string.card_type_piacentine,  region = DeckRegion.NORD),
        CardDeck("bergamasche", R.string.card_type_bergamasche, region = DeckRegion.NORD),
        CardDeck("bolognesi",   R.string.card_type_bolognesi,   insetX = 0.04f, region = DeckRegion.NORD),
        CardDeck("bresciane",   R.string.card_type_bresciane,   insetX = 0.11f, region = DeckRegion.NORD),
        CardDeck("genovesi",    R.string.card_type_genovesi,    insetY = 0.14f, region = DeckRegion.NORD),
        CardDeck("milanesi",    R.string.card_type_milanesi,    region = DeckRegion.NORD),
        CardDeck("piemontesi",  R.string.card_type_piemontesi,  insetY = 0.10f, region = DeckRegion.NORD),
        CardDeck("romagnole",   R.string.card_type_romagnole,   insetY = 0.06f, region = DeckRegion.NORD),
        CardDeck("trentine",    R.string.card_type_trentine,    insetY = 0.06f, region = DeckRegion.NORD),
        CardDeck("trevisane",   R.string.card_type_trevisane,   region = DeckRegion.NORD),
        CardDeck("triestine",   R.string.card_type_triestine,   region = DeckRegion.NORD),
        CardDeck("napoletane",  R.string.card_type_napoletane,  insetY = 0.14f, region = DeckRegion.SUD_ISOLE),
        CardDeck("sarde",       R.string.card_type_sarde,       insetY = 0.14f, region = DeckRegion.SUD_ISOLE),
        CardDeck("siciliane",   R.string.card_type_siciliane,   insetY = 0.14f, region = DeckRegion.SUD_ISOLE),
        CardDeck("francesi",    R.string.card_type_francesi,    region = DeckRegion.INTERNAZIONALI),
    )

    fun indexOf(id: String): Int = ALL.indexOfFirst { it.id == id }.coerceAtLeast(0)
    fun byId(id: String): CardDeck = ALL.firstOrNull { it.id == id } ?: ALL[0]
    fun byRegion(region: DeckRegion): List<CardDeck> = ALL.filter { it.region == region }
}
```

- [ ] **Step 5: Run tests — expect PASS**

```bash
./gradlew testDebugUnitTest --tests "com.bottazzini.trasloco.utils.CardDeckRegistryTest" 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`, all 5 tests pass.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/utils/DeckRegion.kt \
        app/src/main/java/com/bottazzini/trasloco/utils/CardDeck.kt \
        app/src/test/java/com/bottazzini/trasloco/utils/CardDeckRegistryTest.kt
git commit -m "feat(decks): add DeckRegion enum and byRegion() to CardDeckRegistry"
```

---

### Task 2: New tile layout item_deck_grid_tile.xml

**Files:**
- Create: `app/src/main/res/layout/item_deck_grid_tile.xml`

- [ ] **Step 1: Create layout**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:background="@drawable/casino_card_tile_selectable"
    android:padding="6dp"
    android:gravity="center"
    android:clickable="true"
    android:focusable="true">

    <ImageView
        android:id="@+id/tilePrev1"
        android:layout_width="0dp"
        android:layout_weight="1"
        android:layout_height="56dp"
        android:scaleType="centerInside" />

    <ImageView
        android:id="@+id/tilePrev2"
        android:layout_width="0dp"
        android:layout_weight="1"
        android:layout_height="56dp"
        android:scaleType="centerInside" />

    <ImageView
        android:id="@+id/tilePrev3"
        android:layout_width="0dp"
        android:layout_weight="1"
        android:layout_height="56dp"
        android:scaleType="centerInside" />

</LinearLayout>
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/res/layout/item_deck_grid_tile.xml
git commit -m "feat(decks): add grid tile layout with 3 card previews, no text"
```

---

### Task 3: DeckGridAdapter

**Files:**
- Create: `app/src/main/java/com/bottazzini/trasloco/utils/DeckGridAdapter.kt`

- [ ] **Step 1: Create adapter**

```kotlin
package com.bottazzini.trasloco.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bottazzini.trasloco.R

class DeckGridAdapter(
    private val decks: List<CardDeck>,
    private val onDeckSelected: (CardDeck) -> Unit,
) : RecyclerView.Adapter<DeckGridAdapter.TileViewHolder>() {

    private var selectedId: String = ""

    fun setSelectedId(id: String) {
        val oldPos = decks.indexOfFirst { it.id == selectedId }
        val newPos = decks.indexOfFirst { it.id == id }
        selectedId = id
        if (oldPos >= 0) notifyItemChanged(oldPos)
        if (newPos >= 0) notifyItemChanged(newPos)
    }

    class TileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val root: View = itemView
        val prev1: ImageView = itemView.findViewById(R.id.tilePrev1)
        val prev2: ImageView = itemView.findViewById(R.id.tilePrev2)
        val prev3: ImageView = itemView.findViewById(R.id.tilePrev3)
    }

    override fun getItemCount() = decks.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_deck_grid_tile, parent, false)
        return TileViewHolder(view)
    }

    override fun onBindViewHolder(holder: TileViewHolder, position: Int) {
        val deck = decks[position]
        val ctx = holder.root.context
        val isSelected = deck.id == selectedId

        val names = listOf("${deck.id}_b1", "${deck.id}_c1", "${deck.id}_d1")
        val previews = listOf(holder.prev1, holder.prev2, holder.prev3)
        names.zip(previews).forEach { (name, img) ->
            val resId = ctx.resources.getIdentifier(name, "drawable", ctx.packageName)
            img.setImageDrawable(
                ContextCompat.getDrawable(ctx, if (resId != 0) resId else R.drawable.zero)
            )
            img.alpha = if (deck.available) 1f else 0.4f
        }

        holder.root.isSelected = isSelected
        holder.root.alpha = if (deck.available) 1f else 0.4f
        holder.root.isClickable = deck.available
        if (deck.available) {
            holder.root.setOnClickListener { onDeckSelected(deck) }
        } else {
            holder.root.setOnClickListener(null)
        }
    }
}
```

- [ ] **Step 2: Verify it compiles**

```bash
./gradlew compileDebugKotlin 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/utils/DeckGridAdapter.kt
git commit -m "feat(decks): add DeckGridAdapter for grid tile selection"
```

---

### Task 4: String resources (region tab labels + play-with button)

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-it/strings.xml`
- Modify: `app/src/main/res/values-pt/strings.xml`

Add the following strings immediately after the `card_type_coming_soon` line in each file.

- [ ] **Step 1: Add to values/strings.xml** (English)

After line `<string name="card_type_coming_soon">Coming soon</string>` add:

```xml
    <string name="region_nord">North</string>
    <string name="region_sud_isole">South &amp; Islands</string>
    <string name="region_internazionali">International</string>
```

Also replace the existing `deck_picker_button` line:

Old: `<string name="deck_picker_button">Start</string>`

New:
```xml
    <string name="deck_picker_button">Start</string>
    <string name="deck_picker_play_with">Play with %s</string>
```

- [ ] **Step 2: Add to values-it/strings.xml** (Italian)

After `<string name="card_type_coming_soon">Prossimamente</string>` add:

```xml
    <string name="region_nord">Nord</string>
    <string name="region_sud_isole">Sud &amp; Isole</string>
    <string name="region_internazionali">Internazionali</string>
```

After `<string name="deck_picker_button">Inizia</string>` add:

```xml
    <string name="deck_picker_play_with">Gioca con le %s</string>
```

- [ ] **Step 3: Add to values-pt/strings.xml** (Portuguese)

After `<string name="card_type_coming_soon">` line (find it in file) add:

```xml
    <string name="region_nord">Norte</string>
    <string name="region_sud_isole">Sul &amp; Ilhas</string>
    <string name="region_internazionali">Internacional</string>
```

After `<string name="deck_picker_button">Começar</string>` add:

```xml
    <string name="deck_picker_play_with">Jogar com %s</string>
```

- [ ] **Step 4: Verify compilation**

```bash
./gradlew compileDebugKotlin 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/values/strings.xml \
        app/src/main/res/values-it/strings.xml \
        app/src/main/res/values-pt/strings.xml
git commit -m "feat(decks): add region tab and deck selected label string resources"
```

---

### Task 5: Update activity_deck_picker.xml

**Files:**
- Modify: `app/src/main/res/layout/activity_deck_picker.xml`

Replace entire file content with:

- [ ] **Step 1: Replace layout**

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
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

        <com.google.android.material.tabs.TabLayout
            android:id="@+id/deckRegionTabs"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="24dp"
            app:tabTextColor="@color/casino_gold_alpha_50"
            app:tabSelectedTextColor="@color/casino_gold"
            app:tabIndicatorColor="@color/casino_gold"
            app:tabBackground="@android:color/transparent" />

        <TextView
            android:id="@+id/deckSelectedName"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            android:gravity="center"
            android:textSize="18sp"
            android:fontFamily="serif"
            android:textStyle="italic|bold"
            android:textColor="@color/casino_gold" />

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/deckGrid"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:clipToPadding="false"
            android:overScrollMode="never" />

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

- [ ] **Step 2: Verify compilation**

```bash
./gradlew compileDebugKotlin 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/layout/activity_deck_picker.xml
git commit -m "feat(decks): replace carousel with tab+grid layout in deck picker"
```

---

### Task 6: Rewrite DeckPickerActivity.kt

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/DeckPickerActivity.kt`

- [ ] **Step 1: Replace file content**

```kotlin
package com.bottazzini.trasloco

import android.content.Intent
import android.os.Bundle
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bottazzini.trasloco.settings.Configuration
import com.bottazzini.trasloco.settings.SettingsHandler
import com.bottazzini.trasloco.utils.CardDeck
import com.bottazzini.trasloco.utils.CardDeckRegistry
import com.bottazzini.trasloco.utils.DeckGridAdapter
import com.bottazzini.trasloco.utils.DeckRegion
import com.bottazzini.trasloco.utils.WindowInsetsUtils
import com.google.android.material.tabs.TabLayout

class DeckPickerActivity : AppCompatActivity() {

    private lateinit var settingsHandler: SettingsHandler
    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var selectedNameLabel: TextView
    private lateinit var confirmButton: Button

    private var selectedDeck: CardDeck? = null
    private val adapterByRegion = mutableMapOf<DeckRegion, DeckGridAdapter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        supportActionBar?.hide()
        setContentView(R.layout.activity_deck_picker)
        WindowInsetsUtils.applySystemBarInsets(window, findViewById(R.id.deckPickerScrollView))

        settingsHandler = SettingsHandler(applicationContext)

        tabLayout = findViewById(R.id.deckRegionTabs)
        recyclerView = findViewById(R.id.deckGrid)
        selectedNameLabel = findViewById(R.id.deckSelectedName)
        confirmButton = findViewById(R.id.buttonDeckConfirm)

        recyclerView.layoutManager = GridLayoutManager(this, 3)

        setupAdapters()
        setupTabs()

        val defaultDeck = CardDeckRegistry.byId("piacentine")
        selectDeck(defaultDeck)
        tabLayout.getTabAt(0)?.select()
        showRegion(DeckRegion.NORD)

        confirmButton.setOnClickListener { confirmSelection() }
    }

    private fun setupAdapters() {
        DeckRegion.values().forEach { region ->
            adapterByRegion[region] = DeckGridAdapter(CardDeckRegistry.byRegion(region)) { deck ->
                selectDeck(deck)
            }
        }
    }

    private fun setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText(R.string.region_nord))
        tabLayout.addTab(tabLayout.newTab().setText(R.string.region_sud_isole))
        tabLayout.addTab(tabLayout.newTab().setText(R.string.region_internazionali))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                showRegion(DeckRegion.values()[tab.position])
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun showRegion(region: DeckRegion) {
        val adapter = adapterByRegion[region] ?: return
        selectedDeck?.let { adapter.setSelectedId(it.id) }
        recyclerView.adapter = adapter
    }

    private fun selectDeck(deck: CardDeck) {
        selectedDeck = deck
        selectedNameLabel.text = getString(deck.labelRes)
        confirmButton.text = getString(R.string.deck_picker_play_with, getString(deck.labelRes))
        confirmButton.isEnabled = true
        confirmButton.alpha = 1f
        adapterByRegion.values.forEach { it.setSelectedId(deck.id) }
    }

    private fun confirmSelection() {
        val deck = selectedDeck ?: return
        settingsHandler.updateSetting(Configuration.CARD_TYPE.value, deck.id)
        getSharedPreferences("trasloco_prefs", MODE_PRIVATE)
            .edit().putBoolean("deck_chosen", true).apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew compileDebugKotlin 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/DeckPickerActivity.kt
git commit -m "feat(decks): rewrite DeckPickerActivity with geographic tab grid"
```

---

### Task 7: Update settings.xml — replace ViewPager2 with grid

**Files:**
- Modify: `app/src/main/res/layout/settings.xml`

The ViewPager2 block (lines 112–122 of current file) must be replaced. The constraint chain is:
`labelCardDeck` → *(new)* `settingsDeckSelectedName` → `settingsDeckTabLayout` → `settingsDeckGrid` → `labelCardBack`

- [ ] **Step 1: Replace ViewPager2 block**

Find and replace this block in `settings.xml`:

Old:
```xml
        <androidx.viewpager2.widget.ViewPager2
            android:id="@+id/viewPagerSettingsDecks"
            android:layout_width="0dp"
            android:layout_height="120dp"
            android:layout_marginTop="8dp"
            android:paddingHorizontal="48dp"
            android:clipToPadding="false"
            android:clipChildren="false"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toBottomOf="@id/labelCardDeck" />
```

New:
```xml
        <TextView
            android:id="@+id/settingsDeckSelectedName"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:gravity="center"
            android:textSize="16sp"
            android:fontFamily="serif"
            android:textStyle="italic|bold"
            android:textColor="@color/casino_gold"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toBottomOf="@id/labelCardDeck" />

        <com.google.android.material.tabs.TabLayout
            android:id="@+id/settingsDeckTabLayout"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginTop="4dp"
            app:tabTextColor="@color/casino_gold_alpha_50"
            app:tabSelectedTextColor="@color/casino_gold"
            app:tabIndicatorColor="@color/casino_gold"
            app:tabBackground="@android:color/transparent"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toBottomOf="@id/settingsDeckSelectedName" />

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/settingsDeckGrid"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginTop="4dp"
            android:clipToPadding="false"
            android:overScrollMode="never"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toBottomOf="@id/settingsDeckTabLayout" />
```

Also update `labelCardBack` constraint — change:
```xml
            app:layout_constraintTop_toBottomOf="@id/viewPagerSettingsDecks"
```
to:
```xml
            app:layout_constraintTop_toBottomOf="@id/settingsDeckGrid"
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew compileDebugKotlin 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/layout/settings.xml
git commit -m "feat(decks): replace ViewPager2 with tab+grid layout in settings"
```

---

### Task 8: Update SettingsActivity.kt

**Files:**
- Modify: `app/src/main/java/com/bottazzini/trasloco/SettingsActivity.kt`

- [ ] **Step 1: Replace file content**

```kotlin
package com.bottazzini.trasloco

import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bottazzini.trasloco.settings.Configuration
import com.bottazzini.trasloco.settings.SettingsHandler
import com.bottazzini.trasloco.utils.CardDeckRegistry
import com.bottazzini.trasloco.utils.DeckGridAdapter
import com.bottazzini.trasloco.utils.DeckRegion
import com.bottazzini.trasloco.utils.ThemeUtils
import com.bottazzini.trasloco.utils.WindowInsetsUtils
import com.google.android.material.tabs.TabLayout

class SettingsActivity : AppCompatActivity() {

    private lateinit var settingsHandler: SettingsHandler

    private val cardBackTileIds = listOf(R.id.cardBackBg1, R.id.cardBackBg2, R.id.cardBackBg3)
    private lateinit var backgroundTileIds: List<Int>

    private lateinit var deckTabLayout: TabLayout
    private lateinit var deckRecycler: RecyclerView
    private lateinit var deckSelectedName: TextView
    private val deckAdapterByRegion = mutableMapOf<DeckRegion, DeckGridAdapter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.settings)
        WindowInsetsUtils.applySystemBarInsets(window, findViewById(R.id.settingsScrollView))
        supportActionBar?.hide()

        settingsHandler = SettingsHandler(applicationContext)

        val bgRow = findViewById<ViewGroup>(R.id.backgroundRow)
        val ids = mutableListOf<Int>()
        for (i in 0 until bgRow.childCount) {
            val child = bgRow.getChildAt(i)
            if (child.id != View.NO_ID) ids.add(child.id)
        }
        backgroundTileIds = ids

        deckTabLayout = findViewById(R.id.settingsDeckTabLayout)
        deckRecycler = findViewById(R.id.settingsDeckGrid)
        deckSelectedName = findViewById(R.id.settingsDeckSelectedName)

        deckRecycler.layoutManager = GridLayoutManager(this, 3)

        setupDeckAdapters()
        setupDeckTabs()

        readConfigurations()
    }

    private fun setupDeckAdapters() {
        DeckRegion.values().forEach { region ->
            deckAdapterByRegion[region] = DeckGridAdapter(CardDeckRegistry.byRegion(region)) { deck ->
                if (!deck.available) return@DeckGridAdapter
                settingsHandler.updateSetting(Configuration.CARD_TYPE.value, deck.id)
                deckSelectedName.text = getString(deck.labelRes)
                deckAdapterByRegion.values.forEach { it.setSelectedId(deck.id) }
                updateHeroPreview()
            }
        }
    }

    private fun setupDeckTabs() {
        deckTabLayout.addTab(deckTabLayout.newTab().setText(R.string.region_nord))
        deckTabLayout.addTab(deckTabLayout.newTab().setText(R.string.region_sud_isole))
        deckTabLayout.addTab(deckTabLayout.newTab().setText(R.string.region_internazionali))

        deckTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                showDeckRegion(DeckRegion.values()[tab.position])
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun showDeckRegion(region: DeckRegion) {
        deckRecycler.adapter = deckAdapterByRegion[region]
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

    fun changeHintEnabled(view: View) {
        val switch = view as Switch
        val value = if (switch.isChecked) "enabled" else "disabled"
        settingsHandler.updateSetting(Configuration.HINT_ENABLED.value, value)
    }

    fun changeAutoMove(view: View) {
        val switch = view as Switch
        val value = if (switch.isChecked) "enabled" else "disabled"
        settingsHandler.updateSetting(Configuration.AUTO_MOVE.value, value)
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
        applyAccentColor(backgroundTag)
    }

    private fun applyAccentColor(bg: String) {
        val color = ThemeUtils.accentColor(bg, this)
        val dimColor = ThemeUtils.accentColorDim(bg, this)
        listOf(R.id.textViewTitle, R.id.labelCardDeck, R.id.labelCardBack, R.id.labelBackground)
            .forEach { findViewById<TextView>(it).setTextColor(color) }
        listOf(R.id.heroPreviewLabel, R.id.textViewCredits)
            .forEach { findViewById<TextView>(it).setTextColor(dimColor) }
    }

    private fun updateHeroPreview() {
        val backgroundTag = settingsHandler.readValue(Configuration.BACKGROUND.value) ?: "bordeaux"
        val cardTypeTag = settingsHandler.readValue(Configuration.CARD_TYPE.value) ?: "piacentine"
        val cardBackTag = settingsHandler.readValue(Configuration.CARD_BACK.value) ?: "bg2"

        val heroBg = findViewById<ImageView>(R.id.heroBackgroundImage)
        val bgDrawableId = resources.getIdentifier(backgroundTag, "drawable", packageName)
        if (bgDrawableId != 0) {
            heroBg.setImageDrawable(ContextCompat.getDrawable(this, bgDrawableId))
        }

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

        val backImg = findViewById<ImageView>(R.id.heroCardBack)
        val backDrawableId = resources.getIdentifier(cardBackTag, "drawable", packageName)
        if (backDrawableId != 0) {
            backImg.setImageDrawable(ContextCompat.getDrawable(this, backDrawableId))
        }
        val deck = CardDeckRegistry.byId(cardTypeTag)
        backImg.post {
            val px = (backImg.width * deck.insetX / 2f).toInt()
            val py = (backImg.height * deck.insetY / 2f).toInt()
            backImg.setPadding(px, py, px, py)
        }
    }

    private fun readConfigurations() {
        val fastDeal = settingsHandler.readValue(Configuration.FAST_DEAL.value) ?: "disabled"
        findViewById<Switch>(R.id.switchFastDeal).isChecked = (fastDeal == "enabled")

        val cardType = settingsHandler.readValue(Configuration.CARD_TYPE.value) ?: "piacentine"
        val currentDeck = CardDeckRegistry.byId(cardType)
        deckSelectedName.text = getString(currentDeck.labelRes)
        deckAdapterByRegion.values.forEach { it.setSelectedId(cardType) }
        val tabIndex = DeckRegion.values().indexOf(currentDeck.region)
        deckTabLayout.getTabAt(tabIndex)?.select()
        showDeckRegion(currentDeck.region)

        val cardBack = settingsHandler.readValue(Configuration.CARD_BACK.value) ?: "bg2"
        updateSelection(cardBackTileIds, cardBack)

        val background = settingsHandler.readValue(Configuration.BACKGROUND.value) ?: "bordeaux"
        updateSelection(backgroundTileIds, background)

        val hint = settingsHandler.readValue(Configuration.HINT_ENABLED.value) ?: "enabled"
        findViewById<Switch>(R.id.switchHint).isChecked = (hint == "enabled")

        val autoMove = settingsHandler.readValue(Configuration.AUTO_MOVE.value) ?: "disabled"
        findViewById<Switch>(R.id.switchAutoMove).isChecked = (autoMove == "enabled")

        applyScreenBackground(background)
        updateHeroPreview()
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew compileDebugKotlin 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bottazzini/trasloco/SettingsActivity.kt
git commit -m "feat(decks): update SettingsActivity to use geographic tab grid"
```

---

### Task 9: Delete DeckCarouselAdapter + full build

**Files:**
- Delete: `app/src/main/java/com/bottazzini/trasloco/utils/DeckCarouselAdapter.kt`

- [ ] **Step 1: Delete the file**

```bash
git rm app/src/main/java/com/bottazzini/trasloco/utils/DeckCarouselAdapter.kt
```

- [ ] **Step 2: Full build + unit tests**

```bash
./gradlew assembleDebug testDebugUnitTest 2>&1 | tail -30
```

Expected: `BUILD SUCCESSFUL`, all tests pass (including the new `CardDeckRegistryTest`).

- [ ] **Step 3: Commit**

```bash
git commit -m "chore(decks): remove DeckCarouselAdapter, replaced by DeckGridAdapter"
```
