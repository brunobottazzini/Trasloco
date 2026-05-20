# Localization — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add 14 new `strings.xml` resource files so Trasloco supports ES, FR, DE, NL, TR, PL, PT-BR, PT-PT, RU, KO, JA, ZH-CN, HI, and TH.

**Architecture:** Each language is a standalone Android resource folder (`values-XX/`) containing a translated copy of `app/src/main/res/values/strings.xml`. No Kotlin or layout changes needed. `values-pt/` stays as generic Portuguese fallback; `values-pt-rBR` and `values-pt-rPT` add the regional variants.

**Tech Stack:** Android XML string resources, UTF-8, standard Android resource qualifiers.

---

## Translation rules (apply to every task)

**Do NOT translate — leave exactly as-is:**
- `app_name` (`translatable="false"`) → `"Trasloco"`
- `title` (`translatable="false"`) → `"Trasloco"`
- `trasloco` (`translatable="false"`) → `"Trasloco"`
- `watermark_studio` (`translatable="false"`) → `"Bottazzini Softworks"`
- `achievement_ferragosto_name` → `"Ferragosto"` in every language (proper name of the Italian holiday)
- Italian regional card deck names — `card_type_piacentine`, `card_type_napoletane`, `card_type_bergamasche`, `card_type_siciliane`, `card_type_trevisane`, `card_type_bresciane`, `card_type_sarde`, `card_type_bolognesi`, `card_type_genovesi`, `card_type_milanesi`, `card_type_piemontesi`, `card_type_romagnole`, `card_type_trentine`, `card_type_triestine` → keep the Italian names as-is
- `card_type_francesi` → keep as-is (it's a regional Italian deck name, not the adjective "French")
- `credits_text` → keep in English in all languages (legal attribution text)
- `subtitle_card_game` → translate "CARD GAME" part, keep the `~` decoration

**Preserve format placeholders exactly:** `%1$s`, `%2$s`, `%1$d`, `%s` must appear unchanged in the translated string.

**XML escaping:**
- Apostrophes: `\'`
- Ampersand: `&amp;`
- Unicode characters (Cyrillic, CJK, Thai, Devanagari, Hangul) need no escaping

**Tone for achievements:**
- Loss streak (`loss_2` through `loss_10`): sarcastic, mock the player gently. EN examples: "Here We Go Again", "Are You Sure You Know How to Play?", "Maybe It's the Phone / It's not the phone". Match this ironic tone.
- `big_loser`: deadpan — "100 total losses"
- `comeback_2`, `slow_win`, `hint_hero`: mildly playful
- `perfectionist`, `speed_freak`: admiring
- `hint_addict`: teasing

**Validation:** after creating each file, run:
```
./gradlew :app:testDebugUnitTest
```
Expected: BUILD SUCCESSFUL (one pre-existing test `isSolvable_returnsFalseWhenBudgetIsNegligible` may fail — that is unrelated and acceptable).

---

## Task 1: Spanish (ES)

**Files:**
- Create: `app/src/main/res/values-es/strings.xml`

- [ ] **Step 1: Create the file**

Create `app/src/main/res/values-es/strings.xml` as a complete translation of `app/src/main/res/values/strings.xml` into Spanish. Apply all translation rules above. Key tone anchors:

```xml
<string name="achievement_loss_2_name">Otra Vez</string>
<string name="achievement_loss_2_desc">Pierdes 2 partidas seguidas. ¿Coincidencia?</string>
<string name="achievement_loss_3_name">Tres es un Patrón</string>
<string name="achievement_loss_3_desc">3 derrotas consecutivas. O quizás no</string>
<string name="achievement_loss_5_name">¿Seguro que sabes jugar?</string>
<string name="achievement_loss_5_desc">5 derrotas seguidas. ¿Un tutorial quizás?</string>
<string name="achievement_loss_7_name">Quizás es el Móvil</string>
<string name="achievement_loss_7_desc">7 seguidas. No es el móvil</string>
<string name="achievement_loss_10_name">Maestro de la Derrota</string>
<string name="achievement_loss_10_desc">10 derrotas consecutivas. Talento especial</string>
<string name="achievement_ferragosto_name">Ferragosto</string>
<string name="achievement_ferragosto_desc">Abre la app el 15 de agosto</string>
<string name="stats_trophies_header">Trofeos (%1$d / 55)</string>
```

- [ ] **Step 2: Run tests**

```
./gradlew :app:testDebugUnitTest
```

- [ ] **Step 3: Commit**

```
git add app/src/main/res/values-es/strings.xml
git commit -m "feat: add Spanish (ES) localization"
```

---

## Task 2: French (FR)

**Files:**
- Create: `app/src/main/res/values-fr/strings.xml`

- [ ] **Step 1: Create the file**

Create `app/src/main/res/values-fr/strings.xml` as a complete translation into French. Apply all translation rules. Key tone anchors:

```xml
<string name="achievement_loss_2_name">Encore Raté</string>
<string name="achievement_loss_2_desc">Tu perds 2 parties d\'affilée. Coïncidence ?</string>
<string name="achievement_loss_3_name">Trois, c\'est une Tendance</string>
<string name="achievement_loss_3_desc">3 défaites consécutives. Ou peut-être pas</string>
<string name="achievement_loss_5_name">Tu sais vraiment jouer ?</string>
<string name="achievement_loss_5_desc">5 défaites d\'affilée. Un tutoriel peut-être ?</string>
<string name="achievement_loss_7_name">C\'est sûrement le Téléphone</string>
<string name="achievement_loss_7_desc">7 d\'affilée. Ce n\'est pas le téléphone</string>
<string name="achievement_loss_10_name">Maître de la Défaite</string>
<string name="achievement_loss_10_desc">10 défaites consécutives. Un talent rare</string>
<string name="achievement_ferragosto_name">Ferragosto</string>
<string name="achievement_ferragosto_desc">Ouvre l\'appli le 15 août</string>
<string name="stats_trophies_header">Trophées (%1$d / 55)</string>
```

- [ ] **Step 2: Run tests**

```
./gradlew :app:testDebugUnitTest
```

- [ ] **Step 3: Commit**

```
git add app/src/main/res/values-fr/strings.xml
git commit -m "feat: add French (FR) localization"
```

---

## Task 3: German (DE)

**Files:**
- Create: `app/src/main/res/values-de/strings.xml`

- [ ] **Step 1: Create the file**

Create `app/src/main/res/values-de/strings.xml` as a complete translation into German. Apply all translation rules. Key tone anchors:

```xml
<string name="achievement_loss_2_name">Schon wieder</string>
<string name="achievement_loss_2_desc">2 Niederlagen hintereinander. Zufall?</string>
<string name="achievement_loss_3_name">Drei ist ein Muster</string>
<string name="achievement_loss_3_desc">3 aufeinanderfolgende Niederlagen. Oder doch nicht</string>
<string name="achievement_loss_5_name">Kannst du wirklich spielen?</string>
<string name="achievement_loss_5_desc">5 Niederlagen am Stück. Vielleicht ein Tutorial?</string>
<string name="achievement_loss_7_name">Liegt\'s am Handy</string>
<string name="achievement_loss_7_desc">7 am Stück. Es liegt nicht am Handy</string>
<string name="achievement_loss_10_name">Meister der Niederlage</string>
<string name="achievement_loss_10_desc">10 Niederlagen hintereinander. Seltenes Talent</string>
<string name="achievement_ferragosto_name">Ferragosto</string>
<string name="achievement_ferragosto_desc">Öffne die App am 15. August</string>
<string name="stats_trophies_header">Trophäen (%1$d / 55)</string>
```

- [ ] **Step 2: Run tests**

```
./gradlew :app:testDebugUnitTest
```

- [ ] **Step 3: Commit**

```
git add app/src/main/res/values-de/strings.xml
git commit -m "feat: add German (DE) localization"
```

---

## Task 4: Dutch (NL)

**Files:**
- Create: `app/src/main/res/values-nl/strings.xml`

- [ ] **Step 1: Create the file**

Create `app/src/main/res/values-nl/strings.xml` as a complete translation into Dutch. Apply all translation rules. Key tone anchors:

```xml
<string name="achievement_loss_2_name">Weer Mis</string>
<string name="achievement_loss_2_desc">Verliest 2 spellen op rij. Toeval?</string>
<string name="achievement_loss_3_name">Drie is een Patroon</string>
<string name="achievement_loss_3_desc">3 opeenvolgende verlies. Of misschien niet</string>
<string name="achievement_loss_5_name">Weet je zeker dat je kunt spelen?</string>
<string name="achievement_loss_5_desc">5 op rij verloren. Misschien een tutorial?</string>
<string name="achievement_loss_7_name">Misschien is het de Telefoon</string>
<string name="achievement_loss_7_desc">7 op rij. Het is niet de telefoon</string>
<string name="achievement_loss_10_name">Meester van de Nederlaag</string>
<string name="achievement_loss_10_desc">10 opeenvolgende verlies. Zeldzaam talent</string>
<string name="achievement_ferragosto_name">Ferragosto</string>
<string name="achievement_ferragosto_desc">Open de app op 15 augustus</string>
<string name="stats_trophies_header">Trofeeën (%1$d / 55)</string>
```

- [ ] **Step 2: Run tests**

```
./gradlew :app:testDebugUnitTest
```

- [ ] **Step 3: Commit**

```
git add app/src/main/res/values-nl/strings.xml
git commit -m "feat: add Dutch (NL) localization"
```

---

## Task 5: Turkish (TR)

**Files:**
- Create: `app/src/main/res/values-tr/strings.xml`

- [ ] **Step 1: Create the file**

Create `app/src/main/res/values-tr/strings.xml` as a complete translation into Turkish. Apply all translation rules. Key tone anchors:

```xml
<string name="achievement_loss_2_name">Yine mi?</string>
<string name="achievement_loss_2_desc">Arka arkaya 2 oyun kaybettin. Tesadüf mü?</string>
<string name="achievement_loss_3_name">Üç Bir Örüntü</string>
<string name="achievement_loss_3_desc">3 art arda mağlubiyet. Ya da belki değil</string>
<string name="achievement_loss_5_name">Gerçekten Oynamasını Biliyor musun?</string>
<string name="achievement_loss_5_desc">Arka arkaya 5 mağlubiyet. Belki bir eğitim?</string>
<string name="achievement_loss_7_name">Belki Telefondur</string>
<string name="achievement_loss_7_desc">7 art arda. Telefon değil</string>
<string name="achievement_loss_10_name">Yenilgi Ustası</string>
<string name="achievement_loss_10_desc">10 art arda mağlubiyet. Nadir bir yetenek</string>
<string name="achievement_ferragosto_name">Ferragosto</string>
<string name="achievement_ferragosto_desc">15 Ağustos\'ta uygulamayı aç</string>
<string name="stats_trophies_header">Kupalar (%1$d / 55)</string>
```

- [ ] **Step 2: Run tests**

```
./gradlew :app:testDebugUnitTest
```

- [ ] **Step 3: Commit**

```
git add app/src/main/res/values-tr/strings.xml
git commit -m "feat: add Turkish (TR) localization"
```

---

## Task 6: Polish (PL)

**Files:**
- Create: `app/src/main/res/values-pl/strings.xml`

- [ ] **Step 1: Create the file**

Create `app/src/main/res/values-pl/strings.xml` as a complete translation into Polish. Apply all translation rules. Key tone anchors:

```xml
<string name="achievement_loss_2_name">I znowu to samo</string>
<string name="achievement_loss_2_desc">Przegrywasz 2 gry z rzędu. Zbieg okoliczności?</string>
<string name="achievement_loss_3_name">Trzy to wzorzec</string>
<string name="achievement_loss_3_desc">3 kolejne porażki. A może nie</string>
<string name="achievement_loss_5_name">Czy na pewno umiesz grać?</string>
<string name="achievement_loss_5_desc">5 porażek z rzędu. Może samouczek?</string>
<string name="achievement_loss_7_name">Może to telefon</string>
<string name="achievement_loss_7_desc">7 z rzędu. To nie telefon</string>
<string name="achievement_loss_10_name">Mistrz Porażki</string>
<string name="achievement_loss_10_desc">10 kolejnych porażek. Rzadki talent</string>
<string name="achievement_ferragosto_name">Ferragosto</string>
<string name="achievement_ferragosto_desc">Otwórz aplikację 15 sierpnia</string>
<string name="stats_trophies_header">Trofea (%1$d / 55)</string>
```

- [ ] **Step 2: Run tests**

```
./gradlew :app:testDebugUnitTest
```

- [ ] **Step 3: Commit**

```
git add app/src/main/res/values-pl/strings.xml
git commit -m "feat: add Polish (PL) localization"
```

---

## Task 7: Brazilian Portuguese (PT-BR)

**Files:**
- Create: `app/src/main/res/values-pt-rBR/strings.xml`

- [ ] **Step 1: Create the file**

Create `app/src/main/res/values-pt-rBR/strings.xml`. Start from `app/src/main/res/values-pt/strings.xml` and refine for Brazilian Portuguese. Brazilian-specific terms: "celular" (not "telemóvel"), "você" as pronoun, natural BR register.

Key differences from generic PT to verify/fix:
```xml
<!-- BR uses "celular" -->
<string name="achievement_loss_7_desc">7 seguidas. Não é o celular</string>

<!-- BR progressive form -->
<string name="preparing_game">Preparando o jogo…</string>

<!-- Trophy header — verify it shows 55 -->
<string name="stats_trophies_header">Troféus (%1$d / 55)</string>
```

Apply all translation rules. `achievement_ferragosto_name` stays `"Ferragosto"`.

- [ ] **Step 2: Run tests**

```
./gradlew :app:testDebugUnitTest
```

- [ ] **Step 3: Commit**

```
git add app/src/main/res/values-pt-rBR/strings.xml
git commit -m "feat: add Brazilian Portuguese (PT-BR) localization"
```

---

## Task 8: European Portuguese (PT-PT)

**Files:**
- Create: `app/src/main/res/values-pt-rPT/strings.xml`

- [ ] **Step 1: Create the file**

Create `app/src/main/res/values-pt-rPT/strings.xml` for European Portuguese. Start from `app/src/main/res/values-pt/strings.xml` and adapt for PT-PT register and vocabulary.

Key PT-PT differences:
```xml
<!-- PT uses "telemóvel" not "celular" -->
<string name="achievement_loss_7_desc">7 seguidas. Não é o telemóvel</string>

<!-- PT infinitive / gerund forms differ -->
<string name="preparing_game">A preparar o jogo…</string>

<!-- Gerund → infinitive constructions typical in PT-PT -->
<string name="achievement_night_owl_3_desc">Joga 3 partidas consecutivas depois da meia-noite</string>
<string name="achievement_same_day_3_desc">3 vitórias no mesmo dia</string>

<!-- Trophy header -->
<string name="stats_trophies_header">Troféus (%1$d / 55)</string>
```

Apply all translation rules. `achievement_ferragosto_name` stays `"Ferragosto"`.

- [ ] **Step 2: Run tests**

```
./gradlew :app:testDebugUnitTest
```

- [ ] **Step 3: Commit**

```
git add app/src/main/res/values-pt-rPT/strings.xml
git commit -m "feat: add European Portuguese (PT-PT) localization"
```

---

## Task 9: Russian (RU)

**Files:**
- Create: `app/src/main/res/values-ru/strings.xml`

- [ ] **Step 1: Create the file**

Create `app/src/main/res/values-ru/strings.xml` as a complete translation into Russian (Cyrillic). Apply all translation rules. Key tone anchors:

```xml
<string name="achievement_loss_2_name">Снова двойка</string>
<string name="achievement_loss_2_desc">Проигрываешь 2 партии подряд. Совпадение?</string>
<string name="achievement_loss_3_name">Три — это уже тенденция</string>
<string name="achievement_loss_3_desc">3 поражения подряд. Или нет</string>
<string name="achievement_loss_5_name">Ты точно умеешь играть?</string>
<string name="achievement_loss_5_desc">5 поражений подряд. Может, сначала обучение?</string>
<string name="achievement_loss_7_name">Это точно телефон виноват</string>
<string name="achievement_loss_7_desc">7 подряд. Телефон ни при чём</string>
<string name="achievement_loss_10_name">Мастер поражений</string>
<string name="achievement_loss_10_desc">10 поражений подряд. Редкий талант</string>
<string name="achievement_ferragosto_name">Ferragosto</string>
<string name="achievement_ferragosto_desc">Открой приложение 15 августа</string>
<string name="stats_trophies_header">Трофеи (%1$d / 55)</string>
```

- [ ] **Step 2: Run tests**

```
./gradlew :app:testDebugUnitTest
```

- [ ] **Step 3: Commit**

```
git add app/src/main/res/values-ru/strings.xml
git commit -m "feat: add Russian (RU) localization"
```

---

## Task 10: Korean (KO)

**Files:**
- Create: `app/src/main/res/values-ko/strings.xml`

- [ ] **Step 1: Create the file**

Create `app/src/main/res/values-ko/strings.xml` as a complete translation into Korean (Hangul). Apply all translation rules. Key tone anchors:

```xml
<string name="achievement_loss_2_name">또 졌네</string>
<string name="achievement_loss_2_desc">2연패. 우연일까?</string>
<string name="achievement_loss_3_name">3번은 패턴이야</string>
<string name="achievement_loss_3_desc">3연속 패배. 아닐 수도 있지만</string>
<string name="achievement_loss_5_name">진짜 게임 할 줄 알아?</string>
<string name="achievement_loss_5_desc">5연패. 튜토리얼 어때?</string>
<string name="achievement_loss_7_name">폰 탓인가</string>
<string name="achievement_loss_7_desc">7연패. 폰 탓 아니야</string>
<string name="achievement_loss_10_name">패배의 달인</string>
<string name="achievement_loss_10_desc">10연속 패배. 희귀한 재능</string>
<string name="achievement_ferragosto_name">Ferragosto</string>
<string name="achievement_ferragosto_desc">8월 15일에 앱 열기</string>
<string name="stats_trophies_header">트로피 (%1$d / 55)</string>
```

- [ ] **Step 2: Run tests**

```
./gradlew :app:testDebugUnitTest
```

- [ ] **Step 3: Commit**

```
git add app/src/main/res/values-ko/strings.xml
git commit -m "feat: add Korean (KO) localization"
```

---

## Task 11: Japanese (JA)

**Files:**
- Create: `app/src/main/res/values-ja/strings.xml`

- [ ] **Step 1: Create the file**

Create `app/src/main/res/values-ja/strings.xml` as a complete translation into Japanese. Apply all translation rules. Key tone anchors:

```xml
<string name="achievement_loss_2_name">またか…</string>
<string name="achievement_loss_2_desc">2連敗。偶然？</string>
<string name="achievement_loss_3_name">3回はパターンだよ</string>
<string name="achievement_loss_3_desc">3連敗。まあ、そうでもないかも</string>
<string name="achievement_loss_5_name">本当にルール知ってる？</string>
<string name="achievement_loss_5_desc">5連敗。チュートリアルやってみては？</string>
<string name="achievement_loss_7_name">スマホのせいにしてみる？</string>
<string name="achievement_loss_7_desc">7連敗。スマホのせいじゃないよ</string>
<string name="achievement_loss_10_name">敗北のマエストロ</string>
<string name="achievement_loss_10_desc">10連敗。稀有な才能</string>
<string name="achievement_ferragosto_name">Ferragosto</string>
<string name="achievement_ferragosto_desc">8月15日にアプリを開く</string>
<string name="stats_trophies_header">トロフィー (%1$d / 55)</string>
```

- [ ] **Step 2: Run tests**

```
./gradlew :app:testDebugUnitTest
```

- [ ] **Step 3: Commit**

```
git add app/src/main/res/values-ja/strings.xml
git commit -m "feat: add Japanese (JA) localization"
```

---

## Task 12: Simplified Chinese (ZH-CN)

**Files:**
- Create: `app/src/main/res/values-zh-rCN/strings.xml`

- [ ] **Step 1: Create the file**

Create `app/src/main/res/values-zh-rCN/strings.xml` as a complete translation into Simplified Chinese. Apply all translation rules. Key tone anchors:

```xml
<string name="achievement_loss_2_name">又来了</string>
<string name="achievement_loss_2_desc">连输2局。巧合？</string>
<string name="achievement_loss_3_name">三次是规律</string>
<string name="achievement_loss_3_desc">连续3次失败。也许吧</string>
<string name="achievement_loss_5_name">你真的会玩吗？</string>
<string name="achievement_loss_5_desc">连输5局。要不要看看教程？</string>
<string name="achievement_loss_7_name">一定是手机的问题</string>
<string name="achievement_loss_7_desc">连输7局。不是手机的问题</string>
<string name="achievement_loss_10_name">失败大师</string>
<string name="achievement_loss_10_desc">连续10次失败。罕见的天赋</string>
<string name="achievement_ferragosto_name">Ferragosto</string>
<string name="achievement_ferragosto_desc">在8月15日打开应用</string>
<string name="stats_trophies_header">奖杯 (%1$d / 55)</string>
```

- [ ] **Step 2: Run tests**

```
./gradlew :app:testDebugUnitTest
```

- [ ] **Step 3: Commit**

```
git add app/src/main/res/values-zh-rCN/strings.xml
git commit -m "feat: add Simplified Chinese (ZH-CN) localization"
```

---

## Task 13: Hindi (HI)

**Files:**
- Create: `app/src/main/res/values-hi/strings.xml`

- [ ] **Step 1: Create the file**

Create `app/src/main/res/values-hi/strings.xml` as a complete translation into Hindi (Devanagari script). Apply all translation rules. Key tone anchors:

```xml
<string name="achievement_loss_2_name">फिर से हार</string>
<string name="achievement_loss_2_desc">लगातार 2 गेम हारे। संयोग?</string>
<string name="achievement_loss_3_name">तीन तो पैटर्न है</string>
<string name="achievement_loss_3_desc">लगातार 3 हार। या शायद नहीं</string>
<string name="achievement_loss_5_name">क्या सच में खेलना आता है?</string>
<string name="achievement_loss_5_desc">लगातार 5 हार। ट्यूटोरियल देखें?</string>
<string name="achievement_loss_7_name">शायद फ़ोन का दोष है</string>
<string name="achievement_loss_7_desc">7 लगातार। फ़ोन का दोष नहीं है</string>
<string name="achievement_loss_10_name">हार के उस्ताद</string>
<string name="achievement_loss_10_desc">लगातार 10 हार। दुर्लभ प्रतिभा</string>
<string name="achievement_ferragosto_name">Ferragosto</string>
<string name="achievement_ferragosto_desc">15 अगस्त को ऐप खोलें</string>
<string name="stats_trophies_header">ट्रॉफ़ी (%1$d / 55)</string>
```

- [ ] **Step 2: Run tests**

```
./gradlew :app:testDebugUnitTest
```

- [ ] **Step 3: Commit**

```
git add app/src/main/res/values-hi/strings.xml
git commit -m "feat: add Hindi (HI) localization"
```

---

## Task 14: Thai (TH)

**Files:**
- Create: `app/src/main/res/values-th/strings.xml`

- [ ] **Step 1: Create the file**

Create `app/src/main/res/values-th/strings.xml` as a complete translation into Thai. Apply all translation rules. Key tone anchors:

```xml
<string name="achievement_loss_2_name">อีกแล้ว</string>
<string name="achievement_loss_2_desc">แพ้ 2 เกมติดกัน บังเอิญ?</string>
<string name="achievement_loss_3_name">สามครั้งคือรูปแบบ</string>
<string name="achievement_loss_3_desc">แพ้ติดกัน 3 ครั้ง หรือเปล่าก็ไม่รู้</string>
<string name="achievement_loss_5_name">เล่นเป็นจริงๆ หรอ?</string>
<string name="achievement_loss_5_desc">แพ้ติดกัน 5 ครั้ง ลองดูบทแนะนำ?</string>
<string name="achievement_loss_7_name">คงเป็นโทรศัพท์แน่ๆ</string>
<string name="achievement_loss_7_desc">7 ครั้งติดกัน ไม่ใช่โทรศัพท์หรอก</string>
<string name="achievement_loss_10_name">ราชาแห่งความพ่ายแพ้</string>
<string name="achievement_loss_10_desc">แพ้ติดกัน 10 ครั้ง พรสวรรค์หายาก</string>
<string name="achievement_ferragosto_name">Ferragosto</string>
<string name="achievement_ferragosto_desc">เปิดแอปวันที่ 15 สิงหาคม</string>
<string name="stats_trophies_header">ถ้วยรางวัล (%1$d / 55)</string>
```

- [ ] **Step 2: Run tests**

```
./gradlew :app:testDebugUnitTest
```

- [ ] **Step 3: Commit**

```
git add app/src/main/res/values-th/strings.xml
git commit -m "feat: add Thai (TH) localization"
```
