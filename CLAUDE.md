# CLAUDE.md

Ten plik dostarcza kontekst dla Claude Code (claude.ai/code) podczas pracy nad tym repozytorium.

## Status repozytorium

**Aplikacja jest zaimplementowana i rozwijana.** Repozytorium zawiera pełny, działający kod (Kotlin +
Jetpack Compose, pojedynczy moduł `:app`, pakiet `com.pairomatic`) oraz dokumentację w `docs/`. Wydania
buduje CI i publikuje jako APK w GitHub Releases. Bieżące zadania techniczne (bezpieczeństwo, stabilność,
wydajność, testy) są śledzone w [`docs/ROADMAP.md`](docs/ROADMAP.md).

### Komendy budowania i testowania

- `./gradlew testDebugUnitTest` — testy jednostkowe (czysta logika: `SelectionEngine`, `SchedulerRules`,
  `pairLetterIndices`, `CsvParser`). Uruchamiane też w CI.
- `./gradlew assembleDebug` — zbudowanie debugowego APK.
- `./gradlew lint` — statyczna analiza (opcjonalnie).

> Uwaga (środowisko web): SDK Androida bywa niedostępne lokalnie — kompilację i testy weryfikuje CI.

## Co to za projekt

**Letter Pairs Trainer** — natywna aplikacja na Androida do treningu skojarzeń *letter pairs* (mnemotechnika /
peg system). Każdej parze liter (np. `CT`) odpowiada słowo-obraz (np. *Cytryna*). Aplikacja uczy tych skojarzeń
głównie przez **powiadomienia** wyświetlane przy okazji odblokowywania telefonu — użytkownik utrwala parę w
mikro-sesji, a aplikacja śledzi które pary są opanowane i częściej pokazuje te słabsze.

Dwa filary produktu:
1. **Powiadomienia** — główny kanał nauki, działający w tle codziennego użytkowania telefonu.
2. **Aplikacja** — zarządzanie bazą par (400+), statystyki, ustawienia, import/eksport.

### Kluczowe założenia v1

- Jednoużytkownikowa, **w pełni offline**. Brak kont, chmury, synchronizacji.
- Baza **400+ par** — kod musi być wydajny przy przeglądaniu/doborze na tej skali.
- Obrazki są **przygotowane poza aplikacją** (mogą już zawierać wrysowane litery). Aplikacja tylko je wyświetla,
  nie generuje ani nie modyfikuje.
- Dystrybucja przez **sideload** — nie obowiązują ograniczenia Google Play (typy foreground service, polityki).
  Battery optimization użytkownik wyłącza ręcznie.
- `minSdk = 26` (Android 8.0), `targetSdk` = najnowszy stabilny. **Android 13+ wymaga runtime permission
  `POST_NOTIFICATIONS`.**

## Dwa tryby działania (wzajemnie wykluczające się)

Wybór trybu jest w ustawieniach; jednocześnie aktywny jest tylko jeden.

### Tryb testu (active recall) — domyślny

- **Powiadomienie zwinięte:** wyłącznie tekst pary (np. `CT`), bez obrazka i słowa — to jest test.
- **Powiadomienie rozwinięte:** słowo + obrazek + **trzy przyciski oceny**.
- **Brak automatycznej rotacji.** Kolejne powiadomienie pojawia się dopiero po odrzuceniu (swipe) lub ocenie
  bieżącego, z krótkim opóźnieniem. Tryb jest w pełni **zdarzeniowy** — nie wymaga timera ani foreground service.
- Trzy poziomy oceny i ich efekt na częstotliwość powtórek:

  | Przycisk | Znaczenie | Efekt |
  |---|---|---|
  | Znam bardzo dobrze | opanowane | prawie nie powtarzane |
  | Znam w miarę | średnio | powtarzane co jakiś czas |
  | Nie znałem | problem | powtarzane często |

- Kliknięcie przycisku zapisuje `level` + `lastSeen`, potem pokazuje kolejną parę.
- Swipe (odrzucenie bez oceny) = "dalej": pokazuje kolejną parę, **nie zmienia** oceny odrzuconej pary.

### Tryb immersji (bierna ekspozycja) — bez oceny

- Powiadomienie od razu pokazuje obrazek (z wrysowanym rysunkiem i literami), **bez przycisków**.
- **Nie zapisuje żadnych statystyk** — nie modyfikuje `level` ani `lastSeen`.
- **Jedyny tryb z automatyczną rotacją na timerze** (kadencja ~15 min lub rzadziej, konfigurowalna).
- Utrzymuje własną, ulotną (in-memory, bez zapisu do bazy) listę "ostatnio pokazanych", żeby ta sama para nie
  wracała od razu.

## Powiadomienia — szczegóły techniczne

- Styl rozwinięcia: `NotificationCompat.BigPictureStyle` dla obrazka.
- Tryb testu: trzy `Action` z `PendingIntent` → `BroadcastReceiver`, który zapisuje ocenę i wyzwala kolejne
  powiadomienie.
- Odrzucenie (swipe): `setDeleteIntent(PendingIntent)` → osobny `BroadcastReceiver`, pokazujący kolejną parę
  (z uwzględnieniem godzin ciszy i głównego przełącznika).
- Dedykowany `NotificationChannel`; rozważyć konfigurowalny poziom ważności (cichy vs. heads-up).
- Widoczność na ekranie blokady sterowana ustawieniami systemu — poza zakresem aplikacji.
- **Tryb testu nie wymaga foreground service.** Jedyny element czasowy w całej aplikacji to timer trybu immersji.

## Model danych

### Encja `Pair` (Room / SQLite)

| Pole | Typ | Opis |
|---|---|---|
| `id` | Long (PK) | identyfikator |
| `letters` | String | para liter, np. `"CT"` — unikalny klucz biznesowy |
| `word` | String | słowo-obraz, np. `"Cytryna"` |
| `imagePath` | String? | ścieżka/nazwa pliku obrazka w pamięci aplikacji (nullable) |
| `level` | Int? | `null` = nigdy nie oceniona; `0` = nie znałem; `1` = w miarę; `2` = bardzo dobrze |
| `lastSeen` | Long? | epoch millis ostatniej **oceny/kliknięcia** (nie dotyczy immersji); `null` = nigdy |
| `hardFlag` | Boolean | ręczne oznaczenie "nie wchodzi do głowy" |

> **Ważne dla implementacji:** `level = null` ("jeszcze nieklinięta") musi być rozróżnialne od `level = 0`
> ("nie znałem"). Pomylenie tych stanów zafałszuje liczniki statystyk.

Obrazki są plikami w pamięci wewnętrznej aplikacji; w bazie tylko ścieżka/nazwa. Tabela zdarzeń (log ocen w
czasie) jest **poza zakresem v1** — liczniki statystyk liczą się wprost z tabeli `Pair`.

## Algorytm doboru pary (`SelectionEngine`)

Losowanie ważone, nie sztywny harmonogram. Waga pary = iloczyn czterech czynników:

```
weight(pair):
    # 1. Cooldown — świeżo pokazana para chwilowo wypada z puli
    if pair.lastSeen != null and (now - pair.lastSeen) < COOLDOWN:
        return 0
    # 2. Poziom znajomości — im gorzej znana, tym większa waga
    base = LEVEL_WEIGHT[pair.level]        # null / 0 → wysoka, 1 → średnia, 2 → niska
    # 3. Świeżość — im dawniej widziana, tym większa waga (ograniczona z góry)
    recency = recencyMultiplier(now - pair.lastSeen)
    # 4. Ręczna flaga „nie wchodzi do głowy"
    boost = HARD_FLAG_BOOST if pair.hardFlag else 1.0
    return base * recency * boost

pickNext():
    candidates = pairs.filter { weight(it) > 0 }
    return weightedRandomChoice(candidates)   # P(wyboru) ∝ weight
```

Wartości startowe (strojalne w ustawieniach lub jako stałe):

| Stała | Wartość | Uwagi |
|---|---|---|
| `LEVEL_WEIGHT[null]` | 8 | nowe pary wchodzą do rotacji, ale nie zalewają jej naraz |
| `LEVEL_WEIGHT[0]` | 10 | „nie znałem" — najczęściej |
| `LEVEL_WEIGHT[1]` | 4 | „w miarę" — co jakiś czas |
| `LEVEL_WEIGHT[2]` | 1 | „bardzo dobrze" — rzadko, ale nie nigdy |
| `HARD_FLAG_BOOST` | 3 | mnożnik dla oflagowanych |
| `COOLDOWN` | 30 min | żeby ta sama para nie wróciła od razu |
| `recencyMultiplier` | `1 + min(godziny_od_lastSeen, CAP)`, `CAP = 24` | |

`SelectionEngine.pickNext()` musi być **czystą funkcją**, łatwą do pokrycia testami jednostkowymi — to
priorytet implementacyjny (patrz "Kolejność implementacji" niżej).

Do rozważenia: limit liczby *nowych* (`level = null`) par wprowadzanych na raz, żeby nie zrzucić od razu
wszystkich 400 nieznanych.

## Ekran statystyk

Liczniki wyliczane wprost z tabeli `Pair` (kategorie mogą się przecinać — to zamierzone, nie sumują się do
całości):

| Licznik | Definicja |
|---|---|
| Znam bardzo dobrze | `count(level == 2)` |
| Znam w miarę | `count(level == 1)` |
| Nie znam | `count(level == 0)` |
| Oznaczone „nie wchodzi do głowy" | `count(hardFlag == true)` |
| Jeszcze nieklinięte | `count(level == null)` |

## Ekran ustawień

- Wybór trybu: Test / Immersja (wzajemnie wykluczające).
- Główny włącznik powiadomień (wstrzymaj/wznów całą naukę).
- Godziny ciszy: okno (od–do), w którym powiadomienia się nie pojawiają.
- Kadencja immersji: interwał auto-rotacji (np. 15 / 30 / 60 min).
- Poziom ważności powiadomień: cichy (podmiana w miejscu) vs. heads-up.
- (Opcjonalnie) strojenie stałych algorytmu doboru.

## Godziny ciszy

Konfigurowalne okno (typowo nocne), w którym nie pojawiają się żadne powiadomienia w obu trybach. Sprawdzane
przy każdym wyzwoleniu kolejnego powiadomienia (swipe/ocena w trybie testu, tick timera w immersji) — jeśli
bieżący czas mieści się w oknie ciszy, powiadomienie jest pomijane. Nauka wznawia się przy najbliższym
zdarzeniu/ticku po zakończeniu okna.

## Zarządzanie bazą par

- Lista par z wyszukiwaniem i filtrowaniem (litery, poziom, flaga) — konieczne przy 400+ pozycjach.
- CRUD: dodawanie / edycja / usuwanie pary; ustawienie `word`, `imagePath`, przełączenie `hardFlag`.
- Przypisanie obrazka: wybór pliku i skopiowanie do pamięci aplikacji.

### Import / eksport

- **Eksport:** `.zip` z metadanymi (`JSON`) + folderem obrazków, przez **Storage Access Framework (SAF)**.
  Opcja: eksport ze statystykami (`level`, `lastSeen`, `hardFlag`) lub bez — do dzielenia się „gołą" talią.
- **Import:** odczyt `.zip`, wypakowanie obrazków, przepisanie ścieżek. Strategia konfliktów po kluczu
  `letters`: **scal** (aktualizuj istniejące) vs. **zastąp wszystko**.
- (Opcjonalnie) import/eksport `CSV` (`letters, word, image_filename`) do masowej edycji w arkuszu.

## Uprawnienia i ograniczenia Androida

| Zagadnienie | Rozwiązanie |
|---|---|
| `POST_NOTIFICATIONS` (Android 13+) | prośba o uprawnienie przy pierwszym uruchomieniu |
| Timer immersji | **WorkManager** (periodic, min. 15 min) — wystarczające przy kadencji ≥15 min. Alternatywnie `AlarmManager` z exact alarm (`SCHEDULE_EXACT_ALARM` na 12+) jeśli potrzeba punktualności |
| Zabójcy tła (Xiaomi/Huawei/Samsung/OPPO) | onboarding prowadzący przez wyłączenie optymalizacji baterii i autostartu |
| Foreground service | **niepotrzebny** — tryb testu jest zdarzeniowy, immersja korzysta z WorkManagera |
| Przechowywanie obrazków | pamięć wewnętrzna aplikacji; import/eksport przez SAF |

## Proponowana architektura

Kotlin + AndroidX. Struktura modułów/pakietów do ustalenia przy scaffoldingu, ale logicznie:

- **Room** — persystencja par.
- **Warstwa domenowa:** `SelectionEngine` (czysta funkcja `pickNext()`, testowalna jednostkowo),
  `SchedulerRules` (godziny ciszy, główny przełącznik).
- **Powiadomienia:** `NotificationHelper` (budowanie), `GradeReceiver` + `DismissReceiver` (obsługa akcji i
  swipe w trybie testu).
- **Immersja:** `ImmersionWorker` (WorkManager), wyzwalający kolejny obrazek.
- **UI:** ekrany — lista/edycja par, statystyki, ustawienia, import/eksport.

## Kolejność implementacji

Trzymaj się tej kolejności przy pracy nad kodem — każdy krok buduje na poprzednim i jest niezależnie
testowalny:

1. Room + encja `Pair` + podstawowy import (żeby wgrać bazę 400+ par).
2. Ekran listy/edycji par + import/eksport `.zip`.
3. `SelectionEngine` (dobór ważony) jako czysta, przetestowana funkcja.
4. Tryb testu — powiadomienia: pokaż `CT`, rozwinięcie z obrazkiem + 3 przyciski, ocena→zapis→następna,
   swipe→następna.
5. Ekran statystyk (5 liczników).
6. Tryb immersji + `ImmersionWorker` (auto-rotacja).
7. Ustawienia: wybór trybu, godziny ciszy, kadencja, główny przełącznik.
8. Onboarding battery optimization + prośba o `POST_NOTIFICATIONS`.

## Poza zakresem v1 (non-goals)

Nie implementuj tych rzeczy, chyba że użytkownik wyraźnie o to poprosi:

- Synchronizacja w chmurze, konta, wiele urządzeń.
- Publikacja w Google Play.
- Generowanie liter na obrazkach po stronie aplikacji (obrazki są gotowe, dostarczone z zewnątrz).
- Statystyki trendów w czasie / wykresy historyczne (możliwe później dzięki tabeli zdarzeń).
- Wybudzanie ekranu „na żądanie" — powiadomienie po prostu czeka na ekranie blokady.

## Otwarte kwestie (potwierdzić z użytkownikiem przed decyzjami architektonicznymi w tych obszarach)

- Docelowa wersja Androida / model urządzenia (wpływa na `minSdk` i szczegóły uprawnień).
- Wartości startowe stałych algorytmu doboru (sekcja "Algorytm doboru pary") — czy przyjmujemy sugerowane.
- Eksport domyślnie ze statystykami czy bez.
- Czy limitować liczbę nowych (`level = null`) par wprowadzanych na raz.
- Nazwa aplikacji (obecnie robocza: "Letter Pairs Trainer").
