# Multi-deck support — Design Spec
**Date:** 2026-05-15  
**Status:** Approved

---

## Obiettivo

Aggiungere supporto a 5 nuovi tipi di mazzi regionali italiani (Bergamasche, Siciliane, Trevisane, Bresciane, Sarde) portando il totale a 8 deck. Sostituire le tile hardcodate con un carosello ViewPager2 con peek, sia nella schermata di primo avvio (`DeckPickerActivity`) che nelle Impostazioni (`SettingsActivity`).

---

## 1. Architettura e modello dati

Problema attuale: aggiungere un deck richiede modifiche in 4-5 posti separati (2 XML, 2 Kotlin, 3 file stringhe). Il design introduce un registro centralizzato.

**Nuovo file: `utils/CardDeck.kt`**

```kotlin
data class CardDeck(val id: String, val labelRes: Int, val available: Boolean = true)

object CardDeckRegistry {
    val ALL = listOf(
        CardDeck("piacentine",  R.string.card_type_piacentine),
        CardDeck("napoletane",  R.string.card_type_napoletane),
        CardDeck("francesi",    R.string.card_type_francesi),
        CardDeck("bergamasche", R.string.card_type_bergamasche),
        CardDeck("siciliane",   R.string.card_type_siciliane),
        CardDeck("trevisane",   R.string.card_type_trevisane),
        CardDeck("bresciane",   R.string.card_type_bresciane),
        CardDeck("sarde",       R.string.card_type_sarde),
    )
}
```

Le 3 carte di anteprima sono sempre `{id}_b1`, `{id}_c1`, `{id}_d1` — derivate dall'`id`, nessun campo extra. Aggiungere un deck in futuro = una riga in `CardDeckRegistry.ALL` + 40 asset + 3 stringhe localizzate.

---

## 2. Componenti UI — Carosello ViewPager2

### DeckCarouselAdapter

Adapter `RecyclerView.Adapter` condiviso tra le due schermate. Riceve `List<CardDeck>` e produce un item per ogni deck con:
- Nome del mazzo (da `labelRes`)
- 3 `ImageView` di anteprima (`b1`, `c1`, `d1`)
- Se `available = false`: anteprime in grigio/alpha ridotta + label "Prossimamente", non selezionabile

### DeckPickerActivity

Sostituisce le tile XML fisse con un `ViewPager2`. Configurazione peek:

```kotlin
viewPager.offscreenPageLimit = 1
viewPager.setPadding(48.dp, 0, 48.dp, 0)
viewPager.clipToPadding = false
viewPager.clipChildren = false
```

Un `DotsIndicator` (o `TabLayoutMediator`) sotto il ViewPager mostra la posizione corrente. Il pulsante "Inizia" si abilita al primo render (deck 0 = piacentine come default visivo). La selezione corrente viene salvata in `savedInstanceState` per sopravvivere alla rotazione.

### SettingsActivity

La sezione "Tipo di carte" (attuale `HorizontalScrollView` con 3 tile fisse) viene sostituita con lo stesso `ViewPager2` + `DeckCarouselAdapter` in modalità compatta (altezza ridotta). La selezione è istantanea al tap. Il deck salvato nel DB determina la pagina iniziale del ViewPager.

**Dipendenze:** `ViewPager2` è già nel classpath standard Android. Per il dots indicator usare `TabLayoutMediator` dalla libreria Material già presente nel progetto (no dipendenze aggiuntive).

---

## 3. Sourcing degli asset

### Fonti da esplorare (ordine di priorità)

1. **Wikimedia Commons** — mazzi regionali italiani spesso CC0/PD (preferita per Siciliane, Sarde)
2. **GitHub (app Briscola/Scopa open source)** — asset già ritagliati per uso programmatico (verificare licenza del repo)
3. **OpenGameArt.org / itch.io** — fallback

### Naming convention

```
{prefix}_{suit}{number}.png
```

| Suit | Codice |
|------|--------|
| Bastoni | `b` |
| Coppe | `c` |
| Denari | `d` |
| Spade | `s` |

Numeri: 1–10. Totale: 40 file per deck.  
Destinazione: `app/src/main/res/drawable-xxhdpi/`

### Standard visivo degli asset

Le immagini devono seguire lo stesso standard delle Piacentine esistenti: **soggetto molto centrato** nell'immagine, margini uniformi su tutti i lati, senza ritagli asimmetrici o spazi eccessivi su un lato. Prima di integrare un set di asset, verificare visivamente che l'allineamento sia coerente con le Piacentine (usare `magick identify` + anteprima comparativa).

### Deck senza asset disponibili

Se dopo la ricerca un mazzo non ha asset con licenza chiara, viene aggiunto a `CardDeckRegistry` con `available = false`. Il tile compare nel carosello con label "Prossimamente" e non è selezionabile. Il codice è pronto — basta aggiungere gli asset quando si trovano.

### Credits

Aggiornare `credits_text` nei 3 file `strings.xml` (`values/`, `values-it/`, `values-pt/`) con fonte e licenza di ogni nuovo mazzo integrato.

---

## 4. Error handling

| Scenario | Comportamento |
|---|---|
| `getIdentifier` ritorna 0 a runtime | Usare `zero.png` (già esistente) come placeholder, no crash |
| Deck `available = false` | Tap ignorato, nessun salvataggio, label "Prossimamente" |
| Impostazione esistente nel DB | Valida senza migrazione — ViewPager si apre alla pagina dell'`id` salvato |
| Asset mancante in anteprima (Settings/DeckPicker) | `ImageView` nascosta (`gone`) o placeholder neutro |

---

## 5. Localizzazioni

Aggiungere in tutti e 3 i file strings (`values/`, `values-it/`, `values-pt/`):

Per ogni locale il nome del mazzo è identico (nomi propri regionali), tranne `card_type_coming_soon`:

| Locale | `card_type_coming_soon` |
|--------|------------------------|
| `values/` (EN) | `Coming soon` |
| `values-it/` | `Prossimamente` |
| `values-pt/` | `Em breve` |

Campi da aggiungere in tutti e 3 i file:
```xml
<string name="card_type_bergamasche">Bergamasche</string>
<string name="card_type_siciliane">Siciliane</string>
<string name="card_type_trevisane">Trevisane</string>
<string name="card_type_bresciane">Bresciane</string>
<string name="card_type_sarde">Sarde</string>
<string name="card_type_coming_soon"><!-- valore per locale, vedi tabella sopra --></string>
```

---

## 6. Test manuali

- [ ] Sfogliare il carosello in DeckPickerActivity: peek visibile, dots aggiornati
- [ ] Selezionare un deck e premere "Inizia": impostazione salvata correttamente
- [ ] Aprire Settings: carosello pre-posizionato sul deck corrente
- [ ] Cambiare deck in Settings: hero preview si aggiorna immediatamente
- [ ] Deck `available = false`: non selezionabile, label "Prossimamente" visibile
- [ ] Rotazione schermo in DeckPickerActivity: selezione mantenuta
- [ ] Asset centrati: confronto visivo con Piacentine di riferimento
