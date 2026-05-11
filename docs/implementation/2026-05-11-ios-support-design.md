# Supporto iPhone via Kotlin Multiplatform + Compose Multiplatform

**Data:** 2026-05-11
**Stato:** Analisi approvata — pronta per writing-plans
**Approccio scelto:** KMP + Compose Multiplatform, migrazione incrementale (M3)

---

## 1. Obiettivo

Portare l'app Android **Trasloco** (gioco di carte, oggi v1.9.0 su Play Store, ~3.900 righe Kotlin) su iPhone, mantenendo una sola codebase per entrambe le piattaforme. Lo scopo è evitare di dover mantenere due codebase native parallele, sfruttando il fatto che il progetto è già interamente in Kotlin.

## 2. Decisioni di alto livello

| Decisione | Scelta | Motivazione |
|---|---|---|
| Strategia | Cross-platform rewrite | Evitare duplicazione futura |
| Framework | **Kotlin Multiplatform + Compose Multiplatform** | Riusa Kotlin esistente; UI Compose una volta sola; meno riscrittura rispetto a Flutter/RN |
| Approccio migrazione | **M3 — Estrazione incrementale** | Due fasi rilasciabili: prima logica condivisa con UI Android invariata, poi UI Compose cross-platform |
| Min iOS | iOS 14 | Compose Multiplatform stabile; copertura device ~99% |
| Min Android | `minSdk 24` (invariato) | KMP supporta senza problemi |

## 3. Architettura target

Struttura del repo dopo la migrazione:

```
Trasloco/
├── composeApp/                  ← nuovo modulo, sostituisce :app
│   └── src/
│       ├── commonMain/          ← 90% del codice futuro
│       │   ├── kotlin/          (logica, ViewModel, repository, UI Compose)
│       │   └── composeResources/ (PNG carte, MP3, GIF, stringhe i18n)
│       ├── androidMain/         (MainActivity wrapper, DriverFactory SQLDelight Android)
│       ├── iosMain/             (MainViewController, DriverFactory SQLDelight iOS)
│       └── commonTest/          (unit test condivisi)
├── iosApp/                      ← progetto Xcode (Swift host minimale ~30 righe)
│   ├── iosApp/                  (ContentView.swift → MainViewController KMP)
│   └── iosApp.xcodeproj
├── docs/
└── gradle/, settings.gradle.kts (Groovy → Kotlin DSL)
```

**Note architetturali:**

- Durante la **Fase 1** il vecchio modulo `:app` (Android/XML) continua a esistere e consuma il modulo `:shared` per la logica.
- Alla **Fase 2** `:shared` viene promosso a `:composeApp` (aggiunti target Compose) e sostituisce `:app`.
- `iosApp/` è un wrapper Xcode minimale; tutta la UI vive in `commonMain`.
- Build script convertito da Groovy a Kotlin DSL (`.gradle` → `.gradle.kts`), standard KMP.

## 4. Mappatura codice esistente → moduli target

| File attuale | Righe | Destinazione | Azione |
|---|---|---|---|
| `utils/DeckSetup.kt` | 36 | `commonMain` | **MOVE** — puro Kotlin |
| `utils/CardNameTranslator.kt` | 22 | `commonMain` | **MOVE** (verificare niente Android) |
| `utils/TimeUtils.kt` | 20 | `commonMain` | **MOVE** (verificare niente Android) |
| `GameViewModel.kt` | 19 | `commonMain` | **REFACTOR** — usare `androidx.lifecycle:lifecycle-viewmodel` 2.8+ (KMP) |
| `YouWonViewModel.kt` | 7 | `commonMain` | **REFACTOR** — idem |
| `GameActivity.kt` | **1060** | `commonMain` | **REFACTOR pesante** — spezzare (vedi sotto) |
| `MainActivity`, `RecordActivity`, `RulesActivity`, `SettingsActivity`, `YouWonActivity` | 603 | `commonMain` come Composable | **REWRITE** in Compose |
| `db/DatabaseHandler.kt`, `db/columns/*` | 62 | `commonMain` | **REPLACE** con SQLDelight + driver `expect/actual` |
| `settings/SettingsHandler.kt` | 87 | `commonMain` | **REPLACE** con `multiplatform-settings` |
| `settings/RecordsHandler.kt` | 141 | `commonMain` | **REPLACE** con SQLDelight |
| `utils/ResourceUtils.kt` | 9 | — | **DROP** (Android `R.id`-based) |
| `utils/WindowInsetsUtils.kt` | 36 | `androidMain` o sostituire | **REPLACE** con Compose `WindowInsets` API |
| `utils/PartyGifs.kt` | 123 | `commonMain` | **MOVE** lista URL; **REPLACE** caricamento Glide → `coil3` |
| Audio (`MediaPlayer`) | dentro Activities | `commonMain` interface + `expect/actual` | **REPLACE** — Android `MediaPlayer`, iOS `AVAudioPlayer` |
| `res/layout/*.xml` (6 file) | — | — | **DROP** — sostituiti da Composable |
| `res/values*/strings.xml` (it, pt, default) | — | `composeResources/values*/` | **MIGRATE** |
| `res/drawable*/*.png` (120 file: carte, sfondi) | — | `composeResources/drawable/` | **MIGRATE** |
| `res/raw/*.mp3` (5 file) | — | `composeResources/files/` | **MIGRATE** |
| `res/drawable/ending.gif` | — | `composeResources/files/` | **MIGRATE** + loader nuovo |
| `AndroidManifest.xml` | — | `androidMain/` | **SLIM** — solo 1 Activity wrapper |

### 4.1 Decomposizione di `GameActivity.kt` (1060 righe)

Logica e UI oggi sono accoppiate. Riscrittura in Compose è l'occasione per spezzare:

| Nuovo file | Righe stimate | Modulo | Contenuto |
|---|---|---|---|
| `GameEngine.kt` | ~300 | `commonMain` | Regole, validazione mosse, esiti |
| `GameState.kt` | ~100 | `commonMain` | Stato osservabile (`StateFlow`) |
| `GameViewModel.kt` | ~150 | `commonMain` | Orchestrazione |
| `GameScreen.kt` | ~300 | `commonMain` | Composable principale + drag&drop |
| `SoundPlayer.kt` (`expect`/`actual`) | ~50 + 50 + 50 | common + android + ios | Audio platform-specific |

Drag&drop in Compose Multiplatform: `Modifier.pointerInput` + `detectDragGestures` + ghost composable; funziona uguale su Android e iOS.

## 5. Stack di dipendenze

### 5.1 Cosa esce

| Dipendenza attuale | Motivo |
|---|---|
| `androidx.appcompat`, `material`, `constraintlayout` | Android-only → Compose Multiplatform |
| `androidx.core:core-ktx`, `splashscreen` | Splash gestito da Composable iniziale; per Android nativo resta in `androidMain` |
| `com.github.bumptech.glide:5.0.4` | Android-only → coil3 |
| `pl.droidsonroids.gif:android-gif-drawable` | Android-only → coil3 + decoder gif |
| `android.media.MediaPlayer` | Android-only → `expect/actual SoundPlayer` |
| `SQLiteOpenHelper` | Android-only → SQLDelight |
| Espresso, AndroidJUnitRunner | Restano per UI test Android; commonTest userà `kotlin.test` |

### 5.2 Cosa entra

| Dipendenza | A cosa serve |
|---|---|
| `org.jetbrains.compose.runtime/foundation/material3` | UI Compose cross-platform |
| `org.jetbrains.compose.components:resources` | Risorse cross-platform (genera `Res.drawable.francesi_b1` ecc.) |
| `androidx.lifecycle:lifecycle-viewmodel` (2.8+ KMP) | ViewModel cross-platform |
| `app.cash.sqldelight:2.x` + driver Android + Native iOS | DB locale cross-platform |
| `com.russhwolf:multiplatform-settings-no-arg` | Key-value (SharedPreferences / NSUserDefaults) |
| `io.coil-kt.coil3:coil-compose` + `coil-gif` | Immagini PNG/GIF cross-platform |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core` | Async (timer, audio) |

### 5.3 Toolchain & plugin Gradle

- `org.jetbrains.kotlin.multiplatform` (sostituisce `kotlin-android`)
- `org.jetbrains.compose`
- `app.cash.sqldelight` (genera classi dai `.sq`)
- Build script: Groovy → Kotlin DSL

## 6. Roadmap M3 a fasi

Stime in **giornate-uomo (gg)** per uno sviluppatore full-time, esperto Kotlin/Android ma alla prima esperienza KMP+CMP+iOS. **Aggiungere ~30% se part-time.**

### Fase 0 — Setup & prerequisiti *(3-5 gg)*

| Task | gg |
|---|---|
| Apple Developer Account (registrazione + attesa 24-48h) | 0.5 |
| Aggiornamento tooling: Xcode, CocoaPods, KDoctor | 0.5 |
| Conversione build script Groovy → Kotlin DSL | 1 |
| Aggiunta plugin KMP + Compose al progetto (senza toccare codice) | 1 |
| Verifica build Android verde dopo upgrade | 0.5 |
| Branch `feature/kmp-migration` con tag di rollback | 0.1 |

**Deliverable:** build Android verde su toolchain KMP-ready.

### Fase 1 — Estrazione logica condivisa *(8-12 gg)*

UI Android resta su XML/Views. Si estrae solo la logica in `:shared`.

| Task | gg |
|---|---|
| Creazione modulo `:shared` con `commonMain`/`androidMain`/`iosMain` | 0.5 |
| Migrazione `DeckSetup`, `CardNameTranslator`, `TimeUtils` → `commonMain` | 0.5 |
| Setup SQLDelight: `.sq`, classi generate, DriverFactory `expect/actual` | 2 |
| Setup `multiplatform-settings` + migrazione `SettingsHandler` | 1 |
| Migrazione `RecordsHandler` a SQLDelight + adattamento `RecordActivity` | 1.5 |
| Estrazione `GameEngine` da `GameActivity` (validazione mosse, end deck, win/lose) | 3-4 |
| Unit test in `commonTest` per `GameEngine`, `DeckSetup`, repository | 1.5 |
| Smoke test Android | 1 |

**🎯 Milestone Fase 1:** release Android **v1.10.0** su Play Store con backend KMP invisibile all'utente. Valida SQLDelight + settings su utenti reali.

### Fase 2 — UI Compose Multiplatform + app iOS *(15-22 gg)*

| Task | gg |
|---|---|
| Promozione `:shared` → `:composeApp` | 0.5 |
| Migrazione asset (120 PNG, 5 MP3, 1 GIF, i18n) in `composeResources/` | 1 |
| Theming Material3 + dark mode | 1 |
| `MainScreen` Composable | 1 |
| `RulesScreen`, `RecordScreen`, `SettingsScreen` | 2 |
| `GameScreen` (layout tappeto + 4 subdeck + 4 endDeck) | 4 |
| Drag&drop: `pointerInput` + `detectDragGestures` + ghost + hover | 3 |
| Animazioni: shuffle, flip, vittoria (ending.gif + giphy) | 2 |
| Audio: `expect/actual SoundPlayer` (Android MediaPlayer / iOS AVAudioPlayer) | 1 |
| Splash screen iOS + AppIcon | 0.5 |
| `YouWonScreen` con gif party | 1 |
| Setup Xcode `iosApp/` con `MainViewController` da KMP | 1 |
| Test simulatore (iPhone SE, 15 Pro, 15 Pro Max) | 1.5 |
| Sostituzione `:app` legacy con `:composeApp` Android + parità funzionale | 1.5 |
| Buffer integrazione/imprevisti | 2 |

**🎯 Milestone Fase 2:** app gira su iPhone (simulatore) e Android con parità funzionale v1.9.0.

### Fase 3 — Rilascio iOS *(3-5 gg)*

| Task | gg |
|---|---|
| Test su device fisico iPhone | 1 |
| App Store Connect: certificati, provisioning, bundle ID, screenshot, descrizione | 1 |
| Privacy policy URL | 0.5 |
| TestFlight con 2-3 tester | 1 |
| Submission App Store + attesa review (1-7 giorni, non blocca) | 0.5 |
| Release Android v2.0.0 con UI Compose | 0.5 |

### Totale

| Fase | Range |
|---|---|
| Fase 0 — Setup | 3-5 gg |
| Fase 1 — Logica condivisa | 8-12 gg |
| Fase 2 — UI Compose + iOS | 15-22 gg |
| Fase 3 — Rilascio iOS | 3-5 gg |
| **TOTALE** | **29-44 gg** (≈ **6-9 settimane full-time**, o **3-5 mesi part-time**) |

**Le Fasi 1 e 2 sono rilasciabili individualmente** — in caso di pausa imprevista nessun lavoro va sprecato.

## 7. Rischi

| Rischio | Probabilità | Impatto | Mitigazione |
|---|---|---|---|
| Compose Multiplatform iOS ha quirks su API edge (text input avanzato, gesture complesse) | Media | Medio | Buffer 2 gg in Fase 2; fallback `expect/actual` con UIKit per il singolo componente |
| Drag&drop iOS si comporta diversamente (back-swipe edge ha priorità sistema) | Media | Medio | Test simulatore presto in Fase 2; `consume()` esplicito sui pointer events |
| Audio MP3 simultaneo con latenza diversa su iOS | Bassa | Basso | `AVAudioPlayer` precaricato nei ViewModel |
| Performance rendering 40 carte animate | Bassa | Medio | `key()` nei layout, `remember` per i bitmap |
| GIF giphy bloccate da privacy review Apple | Bassa | Alto | Disclaimer privacy policy; alternativa: bundlare GIF locali |
| Migrazione SQLite → SQLDelight: schema mismatch su utenti esistenti | Media | Alto | v1.10.0 mantiene lo schema esistente; migration v2→v3 idempotente; test con DB reali |
| CI macOS necessaria | Bassa | Basso | All'inizio build locale; CI post-launch |

## 8. Costi

| Voce | Costo | Frequenza |
|---|---|---|
| Apple Developer Program | $99 | **annuale ricorrente** |
| Google Play (già pagato) | $25 | una tantum |
| Device iPhone fisico (raccomandato) | $200-800 | una tantum, opzionale |
| TestFlight | gratis | — |
| CI macOS (opzionale) | $10-50/mese | mensile |

**Anno 1:** $99 (+ eventuale iPhone). **Anni successivi:** $99/anno solo per restare su App Store.

## 9. Prerequisiti utente (stato attuale)

| Prerequisito | Stato | Note |
|---|---|---|
| Mac fisico | ✅ Disponibile | |
| Xcode + Command Line Tools | ✅ Installato | |
| Apple Developer Account | ❌ Da valutare | $99/anno ricorrente — da decidere prima Fase 0 |
| iPhone fisico per testing | ❌ Solo simulatore | Sufficiente per Fase 2; raccomandato per Fase 3 |

## 10. Open Questions

Da risolvere prima di iniziare Fase 0 (in writing-plans o successivamente).

1. **GIF di vittoria da giphy.com**: caricamento remoto vs bundle locale? Pro remoto = varietà; pro locale = niente dipendenza rete, niente disclosure privacy.
2. **Risoluzione asset carte**: oggi solo `drawable-xxhdpi`. iPhone moderni hanno `@3x`. Rifare asset a risoluzione superiore in Fase 2?
3. **Feature dal `TODO` esistente** (card selection visiva, resume game, hint, configurabilità "auto-move to final deck"): confermare che restano **fuori scope** e si fanno dopo la migrazione.
4. **Localizzazione**: mantenere `it`, `pt`, default? Aggiungere lingue sfruttando il rifacimento?
5. **Naming iOS**: bundle id `com.bottazzini.trasloco` (stesso package Android)? Nome app store = `Trasloco`?

## 11. Fuori scope di questa analisi

- Feature dal `TODO` esistente
- Backend / sync online / leaderboard cross-device
- iPad-specific layout (girerà come "iPhone app")
- Apple Watch / macOS Catalyst
- Refactoring qualità del codice non strettamente legato alla migrazione

## 12. Prossimo passo

Invocare `writing-plans` per produrre il piano di implementazione dettagliato della **Fase 0**, dopo aver risolto le Open Questions critiche (in particolare #3 e #5).
