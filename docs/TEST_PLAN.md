# E2E Test Plan — Trasloco

Tag legend: **[AUTO]** = coperto da Espresso suite in `app/src/androidTest/`. **[MANUAL]** = test manuale residuo.

Run automated suite: `./gradlew connectedAndroidTest`

---

## 0 · Pre-flight (build)

- **[MANUAL]** `versionCode` e `versionName` incrementati in `app/build.gradle`
- **[MANUAL]** Build release firmato (`./gradlew bundleRelease`)
- **[MANUAL]** Smoke test su build **release** (R8/ProGuard può rompere reflection)
- **[MANUAL]** Manifest privo di `screenOrientation` o `resizeableActivity` espliciti
- **[MANUAL]** `targetSdk=36`, `minSdk=24`
- **[MANUAL]** Verifica supporto **16 KB page size**

## 1 · Smoke

- **[AUTO]** `SmokeNavigationTest.mainActivity_displaysAllMenuButtons`
- **[AUTO]** `SmokeNavigationTest.mainActivity_navigatesToRules`
- **[AUTO]** `SmokeNavigationTest.mainActivity_navigatesToRecords`
- **[AUTO]** `SmokeNavigationTest.mainActivity_navigatesToSettings`
- **[AUTO]** `SmokeNavigationTest.mainActivity_navigatesToGame`
- **[MANUAL]** Splash screen visibile + transition fluida (non automatizzabile facilmente)
- **[MANUAL]** Triple tap su immagine centrale → toast versione (Toast post API 30 richiede UiAutomator)

## 2 · Flusso di gioco

- **[AUTO]** `GameInteractionTest.gameStartsWithSubDecksAndDealtCards`
- **[AUTO]** `GameInteractionTest.gameEndDeckSlotsStartEmpty`
- **[AUTO]** `GameInteractionTest.gameUndoDisabledOnFreshGame`
- **[AUTO]** `GameInteractionTest.gameTable_isFullyDealtAtStart`
- **[AUTO]** `GameInteractionTest.cardClick_setsSelectionForeground`
- **[AUTO]** `GameInteractionTest.firstClickOnEndDeckSlot_doesNotSelect`
- **[AUTO]** `GameInteractionTest.pileCounters_areInvisibleAtGameStart`
- **[AUTO]** `GameInteractionTest.pileCounter_displaysSizeAndBecomesVisibleWhenStackGrows`
- **[MANUAL]** Catena card → end deck (sequenza A→2→3...→10) per ogni seme
- **[MANUAL]** Fast Deal abilitato: dopo carta in end deck, la successiva sale auto
- **[MANUAL]** Win condition (4× 10 in end deck) → naviga a YouWonActivity
- **[MANUAL]** Lost condition → mostra "Hai perso" + Riprova + Nuova partita
- **[MANUAL]** Undo ripristina ultima mossa
- **[MANUAL]** Retry ridistribuisce le stesse carte dell'inizio partita
- **[MANUAL]** Drag-and-drop: long-press carta game → trascina su slot valido → mossa eseguita
- **[MANUAL]** Drag-and-drop: long-press su slot vuoto / end deck → niente drag avviato
- **[MANUAL]** Drag-and-drop: target valido evidenziato col bordo durante il drag (`ACTION_DRAG_ENTERED`)
- **[MANUAL]** Drag-and-drop: drop su target invalido → niente movimento + testo "mossa non valida"
- **[MANUAL]** Drag-and-drop: drop su end deck con sequenza valida (A,2,3,...,10) → carta accettata, fast deal reagisce

## 3 · Impostazioni

- **[AUTO]** `SettingsPersistenceTest.fastDealToggle_persistsAcrossActivityRestart`
- **[AUTO]** `SettingsPersistenceTest.cardBackSelectionPersists_AcrossActivityRestart`
- **[AUTO]** `SettingsPersistenceTest.backgroundSelection_persistsAcrossActivityRestart`
- **[AUTO]** `SettingsPersistenceTest.cardTypeDefaultIsPiacentine` (regression: default Piacentine)
- **[AUTO]** `SettingsPersistenceTest.cardTypeSelection_persistsAcrossActivityRestart`
- **[MANUAL]** Cambio "Tipo di carte" → torna in gioco → carte mostrate con il nuovo set (piacentine/napoletane/francesi)
- **[MANUAL]** Verifica visiva delle 40 carte di ciascun set (no carte mancanti, mapping seme/numero corretto)
- **[AUTO]** `SettingsPersistenceTest.selectingBackgroundInRow2_clearsRow1Selection`
- **[MANUAL]** Sfondo applicato visivamente nelle altre activity (escluso Game/YouWon)

## 4 · Record

- **[AUTO]** `RecordActivityTest.recordActivity_showsNoRecordsMessageOnFirstLaunch`
- **[AUTO]** `RecordActivityTest.recordActivity_titleIsRendered`
- **[MANUAL]** Vittoria → bestTime aggiornato se inferiore al precedente
- **[MANUAL]** Vittoria → consecutiveWins +1 e "Nuovo record" se supera
- **[MANUAL]** Sconfitta → consecutiveWins corrente azzerato
- **[MANUAL]** Format del tempo (mm:ss) corretto
- **[AUTO]** `RecordActivityTest.recordActivity_showsRecordsWhenPopulated`
- **[AUTO]** `RecordActivityTest.recordActivity_showsOnlyConsecutiveWhenTimeMissing`
- **[AUTO]** `YouWonActivityTest.youWonActivity_displaysAllRequiredViews`
- **[AUTO]** `YouWonActivityTest.youWonActivity_displaysNonEmptyTimeAndConsecutiveText`

## 5 · Edge-to-edge & insets

- **[MANUAL]** Su device con barra di stato e gesture nav (Pixel 6+) e con notch:
  - MainActivity, Settings, Records: titoli mai sotto status bar; bottoni mai sotto nav bar
  - RulesActivity: titolo non tagliato in alto, "Got it" non tagliato in basso, ScrollView scrollabile
  - GameActivity / YouWonActivity: fullscreen immersive, swipe rivela transient bars
  - Foldable aperto con notch laterale: contenuto rispetta il cutout
- **[MANUAL]** Confronto visivo screenshot pre/post fix (manualmente o con tool come Shot)

## 6 · Orientamento (rotazione)

- **[AUTO]** `RotationStateTest.gameState_survivesActivityRecreation`
- **[AUTO]** `RotationStateTest.selection_survivesActivityRecreation`
- **[AUTO]** `RotationStateTest.subDeckPick_survivesActivityRecreation`
- **[AUTO]** `RotationStateTest.multipleRecreations_doNotCorruptState`
- **[AUTO]** `RotationStateTest.gameTimer_doesNotResetAfterRecreation`
- **[AUTO]** `YouWonActivityTest.youWonActivity_timeTextSurvivesRecreation` (regression: previously DB was reset to 0)
- **[AUTO]** `YouWonActivityTest.youWonActivity_consecutiveTextSurvivesRecreation`
- **[AUTO]** `YouWonActivityTest.youWonActivity_buttonsClickableAfterRecreation`
- **[AUTO]** `YouWonActivityTest.youWonActivity_gifUrlSurvivesRecreation` (regression: previously a new random GIF was picked on each rotation)
- **[AUTO]** `RulesActivityTest.rulesActivity_gotItButtonFinishesActivity`
- **[AUTO]** `RulesActivityTest.rulesActivity_loadsRulesIntoContainer`
- **[MANUAL]** Layout `layout-land/game.xml` e `layout-land/activity_you_won.xml` caricati in landscape (verifica visiva)
- **[MANUAL]** Stato "Hai perso" preservato post-rotazione, timer non riparte

## 7 · Ciclo di vita

- **[AUTO]** Coperto in parte da `RotationStateTest` (recreate simula config change)
- **[MANUAL]** Home button mid-game → ritorno: timer corretto, partita intatta
- **[MANUAL]** Lock screen mid-game (5+s) → unlock: timer corretto
- **[MANUAL]** Chiamata in arrivo simulata → ritorno: stato preservato
- **[MANUAL]** Kill da recents → riapertura: nuova partita
- **[MANUAL]** Audio non si sovrappone tra suoni concorrenti

## 8 · Multi-device / form factor

- **[MANUAL]** Smartphone piccolo (≤5"), medio (Pixel 7), tablet portrait/landscape
- **[MANUAL]** Foldable aperto/chiuso, ChromeOS desktop mode
- **[AUTO future]** Aggiungere screenshot test (Paparazzi / Roborazzi) per regression visiva

## 9 · API levels

- **[MANUAL]** API 24 (minSdk), 30, 33, 34, 35+
- **[AUTO future]** Eseguire suite con `--abi`/`--api-level` su Firebase Test Lab o emulator orchestrator

## 10 · Localizzazione

- **[MANUAL]** IT (default), EN (fallback), PT (`values-pt`)
- **[MANUAL]** Nessun overflow su lingue lunghe

## 11 · Audio

- **[MANUAL]** Suoni corretti su shuffle / flipcard / youwon / youlost / change_activity
- **[MANUAL]** Volume rispetta media di sistema; mute → app silenziosa, no crash

## 12 · Network (YouWonActivity GIF)

- **[MANUAL]** Online: GIF random caricato; offline: placeholder + fallback
- **[MANUAL]** Cache Glide attiva: secondo lancio offline mostra GIF cachato

## 13 · Performance & stabilità

- **[MANUAL]** 10 partite consecutive: memoria stabile (Profiler ≤ 80MB)
- **[MANUAL]** Timer running 30+ minuti senza drift
- **[MANUAL]** Nessun ANR / memory leak su rotazione ripetuta

## 14 · Accessibilità

- **[MANUAL]** TalkBack: menu navigabile, bottoni annunciati, carte annunciate
- **[MANUAL]** Touch target ≥ 48dp, contrasto leggibile
- **[MANUAL]** Font scaling +200% senza overflow critico

## 15 · Play Store readiness

- **[MANUAL]** AAB su internal track
- **[MANUAL]** Screenshot Play Store aggiornati (portrait + tablet, ora anche landscape)
- **[MANUAL]** Listing description, privacy policy, content rating

## 16 · Regression — issues recenti

- **[AUTO]** Edge-to-edge (coperto indirettamente: nessun crash su layout con insets)
- **[AUTO]** ViewModel state (RotationStateTest)
- **[MANUAL]** 16 KB page size, triple tap version, accessibility fix

---

## Workflow consigliato

| Fase | Test | Trigger |
|---|---|---|
| Pre-commit / PR | `./gradlew connectedAndroidTest` | CI |
| Pre-rilascio internal | tutti gli [AUTO] + sezioni 5, 6 (visivo), 14 manuali | dev |
| Pre-rilascio production | suite completa su 3 device fisici (smartphone piccolo, smartphone moderno, tablet) + sezioni 8/9/10/11/12/13/15 | dev/QA |
