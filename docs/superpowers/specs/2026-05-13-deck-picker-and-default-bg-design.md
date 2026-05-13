# Deck Picker & Default Background Design

**Goal:** Show a full-screen deck selection screen to new users before the main menu, and change the default background to bordeaux so the main menu respects the user's setting.

**Architecture:** A new `DeckPickerActivity` acts as a one-time gate checked from `MainActivity.onCreate()`. The background system is extended to `MainActivity` using the same pattern already used by `StatsActivity` and `GameActivity`.

**Tech Stack:** Kotlin, Android SDK, existing `SettingsHandler` / `Configuration`, SharedPreferences (`trasloco_prefs`), existing drawables (`piacentine_b1/c1/d1`, `napoletane_b1/c1/d1`, `francesi_b1/c1/d1`).

---

## Feature 1: DeckPickerActivity

### Detection & Flow

`MainActivity.onCreate()` runs this logic before doing anything else:

```kotlin
val prefs = getSharedPreferences("trasloco_prefs", MODE_PRIVATE)
// Migration: existing users (tutorial_seen=true) skip the picker automatically
if (prefs.getBoolean("tutorial_seen", false) && !prefs.getBoolean("deck_chosen", false)) {
    prefs.edit().putBoolean("deck_chosen", true).apply()
}
if (!prefs.getBoolean("deck_chosen", false)) {
    startActivity(Intent(this, DeckPickerActivity::class.java))
    finish()
    return
}
```

`DeckPickerActivity` after the user confirms:
1. Save `CARD_TYPE` via `settingsHandler.updateSetting(Configuration.CARD_TYPE.value, chosenTag)`
2. Set `deck_chosen = true` in SharedPreferences
3. `startActivity(Intent(this, MainActivity::class.java))`
4. `finish()`

`android:noHistory="true"` in the manifest prevents back-navigation returning to the picker.

### Layout: `activity_deck_picker.xml`

- Root: `FrameLayout`, full screen
- Background: `@drawable/bordeaux`
- Inside: `ScrollView` → `LinearLayout` (vertical, padding 24dp)
  - Title `TextView`: `@string/deck_picker_title`, style `CasinoTitle`, 26sp, centered
  - Subtitle `TextView`: `@string/deck_picker_subtitle`, style `CasinoSubtitle`, 13sp, centered, marginTop 6dp
  - 3 deck tiles (`LinearLayout`, vertical, `@drawable/casino_tile_bg`, marginTop 20dp, padding 16dp, clickable):
    - Each tile has tag `"piacentine"` / `"napoletane"` / `"francesi"`
    - Deck name `TextView` (CasinoSubtitle, 14sp, gold, marginBottom 10dp)
    - Horizontal `LinearLayout` with 3 `ImageView`s (0dp weight=1 each, height 80dp, scaleType centerInside) showing `${tag}_b1`, `${tag}_c1`, `${tag}_d1`
  - Selected tile uses `@drawable/casino_tile_bg_primary` (gold border highlight)
  - "Inizia" `Button` (`@string/deck_picker_button`), `@drawable/casino_tile_bg_primary`, disabled alpha 0.4f until a tile is selected, marginTop 28dp, width 60% constrained

### Selection State

`DeckPickerActivity` keeps a `selectedTag: String?`. On tile tap:
- Reset all 3 tiles to `@drawable/casino_tile_bg`
- Set tapped tile to `@drawable/casino_tile_bg_primary`
- Update `selectedTag`
- Enable the button (`isEnabled = true`, `alpha = 1f`)

Button click calls `confirmSelection()` which executes the flow described above.

### Strings (EN / IT / PT)

| Key | EN | IT | PT |
|-----|----|----|-----|
| `deck_picker_title` | Choose your deck | Scegli il tuo mazzo | Escolha seu baralho |
| `deck_picker_subtitle` | You can change this later in Settings | Potrai cambiarlo nelle Impostazioni | Você pode mudar isso nas Configurações |
| `deck_picker_button` | Start | Inizia | Começar |

### Manifest

```xml
<activity
    android:name=".DeckPickerActivity"
    android:theme="@style/Theme.Trasloco"
    android:noHistory="true" />
```

---

## Feature 2: Dynamic Background in MainActivity + New Default

### Default Change

In `SettingsHandler.insertDefaultSettings()`:
```kotlin
setDefaultSetting(Configuration.BACKGROUND.value, "bordeaux")  // was "tappeto"
```

`setDefaultSetting` is idempotent (only writes if key absent), so existing users with any background already set are unaffected.

### MainActivity Reads Background Dynamically

In `MainActivity.onCreate()`, after `setContentView`, add:
```kotlin
val bg = settingsHandler.readValue(Configuration.BACKGROUND.value) ?: "bordeaux"
val drawable = ResourceUtils.getDrawableByName(resources, packageName, bg)
findViewById<View>(R.id.mainScrollView).background = ContextCompat.getDrawable(this, drawable)
```

Remove the hardcoded `android:background="@drawable/verde"` from `activity_main.xml`'s ScrollView.

---

## What Does NOT Change

- `tutorial_seen` logic and tutorial prompt dialog — unchanged
- Settings screen deck selection — unchanged
- Game/Stats background reading — unchanged
- All existing achievements, records, game logic — unchanged
