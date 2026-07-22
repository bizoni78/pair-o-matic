# Roadmapa poprawek — Letter Pairs Trainer

Roadmapa powstała na podstawie audytu kodu. Jest rozbita na **drobne, niezależne zadania**,
pogrupowane w kamienie milowe. Każde zadanie ma być realizowane jako **osobny, mały PR**
(zgodnie z workflow projektu: PR → zielone CI → merge → build APK).

> **Docelowo cała roadmapa zostanie wdrożona.** Ten plik jest jednocześnie listą kontrolną —
> zaznaczaj `- [x]` przy ukończonych zadaniach i aktualizuj status.

## Legenda

- **Priorytet:** 🔴 krytyczny · 🟠 wysoki · 🟡 średni · 🔵 niski
- **Rozmiar:** S (≤½ dnia) · M (≈1 dzień) · L (kilka dni)
- **Status:** `TODO` · `IN PROGRESS` · `DONE`

## Podsumowanie kamieni milowych

| Kamień | Zakres | Zadania | Priorytet |
|---|---|---|---|
| M1 | Bezpieczeństwo i integralność danych | SEC-1…SEC-5 | 🔴 ✅ **DONE** |
| M2 | Stabilność i odporność | STA-1…STA-6 | 🟠 ◐ (STA-1/3/4/5/6 ✅; STA-2 TODO) |
| M3 | Wydajność i skalowanie | PERF-1…PERF-4 | 🟡 ◐ (PERF-3/4 ✅; PERF-1/2 TODO) |
| M4 | Testy i jakość | TEST-1…TEST-6 | 🟠 ◐ (TEST-1/3/4/5 ✅; TEST-2/6 TODO) |
| M5 | Refaktor i porządki | REF-1…REF-5 | 🔵 ✅ **DONE** |
| M6 | UX i drobne | UX-1…UX-3 | 🔵 |

**Sugerowana kolejność wdrażania:** M1 → M4 (testy równolegle, chronią kolejne zmiany) → M2 → M3 → M5 → M6.

---

## M1 — Bezpieczeństwo i integralność danych 🔴

> ✅ **Zrealizowane** w PR „M1 — hardening importu i backupu". Wszystkie kroki SEC-1…SEC-5 wdrożone.

### SEC-1 — Zip Slip: sanityzacja nazw plików przy imporcie ZIP
- **Priorytet/Rozmiar:** 🔴 / S · **Status:** ✅ DONE
- **Problem:** `PairRepository.importFromZip` zapisuje obrazki jako `File(imagesDir, name.removePrefix("images/"))` bez walidacji. Wpis `images/../../evil` zapisuje plik poza katalogiem aplikacji (path traversal).
- **Kroki:**
  - [x] Wyodrębnić samą nazwę pliku: `File(fileName).name` (odrzuca ścieżki).
  - [x] Odrzucać wpisy zawierające `..`, `/`, `\` lub prowadzące poza `imagesDir` (weryfikacja `canonicalPath.startsWith(imagesDir.canonicalPath)`).
  - [x] Pominąć (nie przerywać importu) wpisy niebezpieczne, zalogować liczbę pominiętych.
- **Pliki:** `data/PairRepository.kt`
- **Kryteria akceptacji:** ZIP z wpisem `images/../x` nie tworzy pliku poza `filesDir/images`; poprawne obrazki nadal się rozpakowują.

### SEC-2 — Atomowy import (transakcja)
- **Priorytet/Rozmiar:** 🔴 / M · **Status:** ✅ DONE
- **Problem:** `if (replaceAll) dao.deleteAll()` + pętla insertów nie jest transakcją. Przerwanie w połowie zostawia bazę pustą/częściową → utrata talii.
- **Kroki:**
  - [x] Dodać metodę `@Transaction suspend fun replaceAll(pairs: List<PairEntity>)` w DAO (deleteAll + insert w jednej transakcji) lub użyć `db.withTransaction { }`.
  - [x] Przepisać `importFromZip`/`importFromCsv`, aby zapis do bazy działał atomowo (najpierw sparsować całość do listy, potem jeden zapis).
  - [ ] (opcjonalne, pominięte) Obrazki wypakowywać do katalogu tymczasowego i przenosić dopiero po udanym zapisie bazy. Nie jest to konieczne — nadmiarowe pliki sprząta „Zdrowie talii" (osierocone obrazki).
- **Pliki:** `data/db/PairDao.kt`, `data/PairRepository.kt`, `data/db/AppDatabase.kt`
- **Kryteria akceptacji:** Wymuszony wyjątek w trakcie importu nie zostawia częściowej bazy; przy „scal" istniejące pary nie znikają.

### SEC-3 — Wyłączyć Auto Backup (spójność z „offline")
- **Priorytet/Rozmiar:** 🔴 / S · **Status:** ✅ DONE
- **Problem:** `allowBackup="true"` wysyła bazę + DataStore do Google Drive użytkownika — sprzeczne z założeniem „w pełni offline, brak chmury".
- **Kroki:**
  - [x] Ustawić `android:allowBackup="false"` **lub** dodać `dataExtractionRules` / `fullBackupContent` wykluczające bazę i DataStore.
  - [x] Zdecydować i udokumentować wybór w `docs/06-wymagania-techniczne.md`.
- **Pliki:** `AndroidManifest.xml`, ew. `res/xml/data_extraction_rules.xml`
- **Kryteria akceptacji:** Dane nie trafiają do chmury; decyzja opisana w docs.

### SEC-4 — Limity rozmiaru importu (ochrona przed OOM / zip bomb)
- **Priorytet/Rozmiar:** 🟠 / M · **Status:** ✅ DONE
- **Problem:** `zip.readBytes()` i wczytywanie całego CSV do stringa; brak limitów na liczbę/rozmiar wpisów.
- **Kroki:**
  - [x] Limit rozmiaru pojedynczego wpisu metadanych (np. 5 MB) i całkowitej liczby par (np. 10 000).
  - [x] Limit rozmiaru obrazka (np. 15 MB) — pomiń zbyt duże.
  - [x] Strumieniowe kopiowanie z licznikiem bajtów zamiast `readBytes()` tam, gdzie się da.
- **Pliki:** `data/PairRepository.kt`
- **Kryteria akceptacji:** Bardzo duży/spuchnięty ZIP nie wywala aplikacji; użytkownik dostaje komunikat.

### SEC-5 — Walidacja typów obrazków przy imporcie
- **Priorytet/Rozmiar:** 🟡 / S · **Status:** ✅ DONE
- **Problem:** Do `images/` trafia dowolny plik; nazwy z bazy mogą wskazywać nie-obrazki.
- **Kroki:**
  - [x] Akceptować tylko rozszerzenia obrazków (`png/jpg/jpeg/webp`) w `images/`.
  - [x] Zignorować pozostałe wpisy.
- **Pliki:** `data/PairRepository.kt`
- **Kryteria akceptacji:** Nie-obrazki nie są zapisywane do `images/`.

---

## M2 — Stabilność i odporność 🟠

### STA-1 — Downsampling bitmap w powiadomieniach
- **Priorytet/Rozmiar:** 🟠 / M · **Status:** ✅ DONE
- **Problem:** `BitmapFactory.decodeFile` bez `inSampleSize`; obrazek z aparatu (kilka tys. px) → ogromny bitmap → OOM lub przekroczenie limitu rozmiaru powiadomienia (obraz się nie pokazuje).
- **Kroki:**
  - [x] Odczyt wymiarów (`inJustDecodeBounds=true`), policzyć `inSampleSize` do docelowej krawędzi ~1024 px.
  - [x] Dekodować z próbkowaniem; obsłużyć `null`.
  - [x] Wydzielić helper `decodeSampledBitmap(file, maxEdge)`.
- **Pliki:** `notifications/NotificationHelper.kt`
- **Kryteria akceptacji:** Duży obrazek wyświetla się w powiadomieniu bez OOM; zużycie pamięci ograniczone.

### STA-2 — Normalizacja obrazka przy imporcie do pamięci aplikacji
- **Priorytet/Rozmiar:** 🟡 / M · **Status:** TODO
- **Problem:** `copyImageFromUri` kopiuje 1:1 — bardzo duże pliki puchną w pamięci wewnętrznej.
- **Kroki:**
  - [ ] Opcjonalnie przeskalować/re-enkodować obrazek do rozsądnego maksimum przy kopiowaniu.
  - [ ] Zachować proporcje; limit krawędzi ~1600 px.
- **Pliki:** `data/PairRepository.kt`
- **Kryteria akceptacji:** Duże zdjęcie zapisuje się jako rozsądnej wielkości plik.

### STA-3 — `recentImmersion` thread-safe
- **Priorytet/Rozmiar:** 🟡 / S · **Status:** ✅ DONE
- **Problem:** Statyczny `LinkedHashSet` mutowany z korutyn IO bez synchronizacji → ryzyko `ConcurrentModificationException`.
- **Kroki:**
  - [x] Owinąć w `Collections.synchronizedSet` lub chronić `Mutex`/`synchronized`.
- **Pliki:** `notifications/NotificationScheduler.kt`
- **Kryteria akceptacji:** Brak wyścigu przy równoległych tickach.

### STA-4 — Bezpieczne otwieranie strumieni (usunąć `!!`)
- **Priorytet/Rozmiar:** 🟡 / S · **Status:** ✅ DONE
- **Problem:** `openInputStream(source)!!` / `openOutputStream(target)!!` mogą rzucić NPE.
- **Kroki:**
  - [x] Zamienić na sprawdzenie null + czytelny błąd/komunikat.
- **Pliki:** `data/PairRepository.kt`
- **Kryteria akceptacji:** Nieotwarty strumień → komunikat, nie crash.

### STA-5 — Audyt guardów w widgetach i odbiornikach
- **Priorytet/Rozmiar:** 🔵 / S · **Status:** ✅ DONE
- **Kroki:**
  - [x] Sprawdzić wszystkie ścieżki `applicationContext as PairOMaticApp` pod kątem wyjątku, gdy proces w nietypowym stanie.
  - [x] Upewnić się, że `goAsync().finish()` zawsze się wykonuje (try/finally — jest, potwierdzić).
- **Pliki:** `widget/*`, `notifications/*Receiver.kt`
- **Kryteria akceptacji:** Brak ścieżek bez `finish()`.

### STA-6 — Zabezpieczenie przed równoległym `postNextTest`
- **Priorytet/Rozmiar:** 🔵 / S · **Status:** ✅ DONE
- **Problem:** Szybkie taps/oceny mogą wywołać kilka `postNextTest` naraz (podwójne powiadomienie).
- **Kroki:**
  - [x] Lekki throttling/idempotencja (np. jeden `Mutex` w schedulerze) lub `setOnlyAlertOnce` przy zamianie.
- **Pliki:** `notifications/NotificationScheduler.kt`
- **Kryteria akceptacji:** Brak podwójnych powiadomień przy szybkich akcjach.

---

## M3 — Wydajność i skalowanie 🟡

### PERF-1 — Ograniczyć pełny `getAllPairs()` na każde zdarzenie
- **Priorytet/Rozmiar:** 🟡 / M · **Status:** TODO
- **Problem:** Każde powiadomienie/tap widgetu ładuje wszystkie 400+ par do pamięci.
- **Kroki:**
  - [ ] Rozważyć krótkotrwały cache listy w schedulerze/widgetach (z inwalidacją przy zmianach).
  - [ ] Zmierzyć realny koszt (przy 400 może nie być potrzeby).
- **Pliki:** `notifications/NotificationScheduler.kt`, `widget/*`
- **Kryteria akceptacji:** Mniej pełnych odczytów bazy przy serii akcji; brak regresji doboru.

### PERF-2 — Losowanie ważone po stronie SQL (opcjonalne)
- **Priorytet/Rozmiar:** 🔵 / L · **Status:** TODO
- **Problem:** Cała pula ładowana do pamięci przy każdym doborze.
- **Kroki:**
  - [ ] Zaprojektować zapytanie doboru (np. losowanie ważone z wagami liczonymi w SQL) — zachować `SelectionEngine` jako fallback/testowalną referencję.
- **Pliki:** `data/db/PairDao.kt`, `domain/SelectionEngine.kt`
- **Kryteria akceptacji:** Dobór działa bez ładowania całej tabeli; testy potwierdzają rozkład.

### PERF-3 — Limit nowych par (`level=null`) wprowadzanych naraz
- **Priorytet/Rozmiar:** 🟡 / S · **Status:** ✅ DONE
- **Problem:** Z `CLAUDE.md` („do rozważenia") — 400 nieznanych par naraz zalewa rotację.
- **Kroki:**
  - [x] Dodać strojalny limit liczby świeżych `null` w puli doboru.
  - [x] Udostępnić jako stałą/ustawienie.
- **Pliki:** `domain/SelectionEngine.kt`, `domain/SelectionConfig.kt`
- **Kryteria akceptacji:** Nowe pary wchodzą stopniowo; test na limit.

### PERF-4 — Statystyki liczone w SQL (COUNT), nie w pamięci
- **Priorytet/Rozmiar:** 🔵 / S · **Status:** ✅ DONE
- **Problem:** `StatsViewModel` liczy `count { ... }` po pełnej liście.
- **Kroki:**
  - [x] Dodać zapytania `COUNT(*)` w DAO dla każdej kategorii (lub jedno zapytanie agregujące).
- **Pliki:** `data/db/PairDao.kt`, `ui/stats/StatsViewModel.kt`
- **Kryteria akceptacji:** Statystyki bez ładowania całej tabeli do pamięci UI.

---

## M4 — Testy i jakość 🟠

### TEST-1 — Testy `SchedulerRules` (godziny ciszy)
- **Priorytet/Rozmiar:** 🟠 / S · **Status:** ✅ DONE
- **Kroki:**
  - [x] Okno przez północ (22:00–07:00), okno w ciągu dnia, `start==end`, brzegi (dokładnie start/end).
- **Pliki:** `test/.../SchedulerRulesTest.kt`
- **Kryteria akceptacji:** Pokrycie wszystkich gałęzi `isWithinQuietHours`.

### TEST-2 — Testy serii/celu (`ProgressStats`)
- **Priorytet/Rozmiar:** 🟠 / M · **Status:** TODO
- **Kroki:**
  - [ ] Wydzielić czystą logikę serii z `SettingsRepository.recordGrade` (funkcja bez DataStore) do przetestowania.
  - [ ] Testy: pierwszy dzień, kolejny dzień (streak+1), przerwa (reset), dwie oceny tego samego dnia, rollover licznika „dziś".
- **Pliki:** `domain/…` (nowy helper), `test/…`
- **Kryteria akceptacji:** Logika serii pokryta testami, niezależna od DataStore.

### TEST-3 — Testy pogrubiania (`pairLetterIndices`)
- **Priorytet/Rozmiar:** 🟡 / S · **Status:** ✅ DONE
- **Kroki:**
  - [x] Duplikaty liter, wielkość liter, brak dopasowania, puste słowo, litery poza kolejnością.
- **Pliki:** `test/.../PairTextTest.kt`
- **Kryteria akceptacji:** Wszystkie przypadki brzegowe zielone.

### TEST-4 — Testy `SelectionEngine` (uzupełnienie)
- **Priorytet/Rozmiar:** 🟡 / M · **Status:** ✅ DONE
- **Kroki:**
  - [x] Cooldown zeruje pulę → `pickNext` null; fallback bez cooldownu zwraca parę.
  - [x] `exclude` opróżnia pulę → ponów bez `exclude`.
  - [x] Wagi (level/recency/hardFlag) wpływają na rozkład (test z ustalonym `Random`).
- **Pliki:** `test/.../SelectionEngineTest.kt`
- **Kryteria akceptacji:** Deterministyczne testy z wstrzykniętym `Random`.

### TEST-5 — Testy parsera CSV
- **Priorytet/Rozmiar:** 🟡 / S · **Status:** ✅ DONE
- **Kroki:**
  - [x] Separator `,` i `;`, cudzysłowy, podwójne cudzysłowy, nagłówek, puste pola.
- **Pliki:** `test/.../CsvParseTest.kt` (może wymagać wydzielenia parsera do testowalnej funkcji)
- **Kryteria akceptacji:** Parser pokryty; edge case'y zielone.

### TEST-6 — Test round-trip eksport→import
- **Priorytet/Rozmiar:** 🔵 / M · **Status:** TODO
- **Kroki:**
  - [ ] Test (instrumentalny lub z fake `ContentResolver`) — eksport ZIP i ponowny import odtwarza pary i statystyki.
- **Pliki:** `androidTest/…` lub `test/…` z abstrakcją IO
- **Kryteria akceptacji:** Dane po round-trip identyczne.

---

## M5 — Refaktor i porządki 🔵

### REF-1 — Scalić logikę „bold" do jednej funkcji
- **Priorytet/Rozmiar:** 🔵 / S · **Status:** ✅ DONE
- **Problem:** Trzy kopie: `boldPairLetters` (Compose), `NotificationHelper.boldedWord`, `PairWidgetProvider.boldWord`.
- **Kroki:**
  - [x] Jedna funkcja bazowa zwracająca indeksy + adaptery `AnnotatedString` i `Spannable`.
- **Pliki:** `ui/components/PairText.kt`, `notifications/NotificationHelper.kt`, `widget/PairWidgetProvider.kt`
- **Kryteria akceptacji:** Jedno źródło prawdy dla pogrubiania.

### REF-2 — Usunąć martwy kod priorytetu powiadomień
- **Priorytet/Rozmiar:** 🔵 / S · **Status:** ✅ DONE
- **Problem:** `setPriority` nieaktywne na `minSdk=26`; sterowanie dźwiękiem robi już `setSilent`.
- **Kroki:**
  - [x] Uprościć `baseBuilder`, usunąć zbędny `setPriority`/komentarze.
- **Pliki:** `notifications/NotificationHelper.kt`
- **Kryteria akceptacji:** Kod prostszy, zachowanie bez zmian.

### REF-3 — Wyodrębnić stałe współdzielone
- **Priorytet/Rozmiar:** 🔵 / S · **Status:** ✅ DONE
- **Kroki:**
  - [x] Wspólny `NO_COOLDOWN` (scheduler + widget), kolory/rozmiary widgetów jako zasoby.
- **Pliki:** `notifications/*`, `widget/*`, `res/values/*`
- **Kryteria akceptacji:** Brak zduplikowanych stałych.

### REF-4 — Jeden helper odmiany liczb (dzień/dni, para/pary/par)
- **Priorytet/Rozmiar:** 🔵 / S · **Status:** ✅ DONE
- **Kroki:**
  - [x] Wydzielić `plural()` i użyć w Stats, widgetach, Snackbarze „Cofnij".
- **Pliki:** `ui/util/…`, miejsca użycia
- **Kryteria akceptacji:** Jedna implementacja odmiany.

### REF-5 — Aktualizacja dokumentacji po zmianach
- **Priorytet/Rozmiar:** 🔵 / S · **Status:** ✅ DONE
- **Kroki:**
  - [x] Zaktualizować `docs/` i `CLAUDE.md` (sekcja komend budowania/testów, decyzje bezpieczeństwa).
- **Pliki:** `docs/*`, `CLAUDE.md`
- **Kryteria akceptacji:** Docs zgodne z kodem.

---

## M6 — UX i drobne 🔵

### UX-1 — Splash/placeholder zamiast pustego ekranu przy starcie
- **Priorytet/Rozmiar:** 🔵 / S · **Status:** TODO
- **Problem:** `MainActivity` pokazuje pusty ekran, dopóki ustawienia się nie wczytają (null state).
- **Kroki:**
  - [ ] Prosty placeholder na gradiencie (logo/spinner) na czas ładowania.
- **Pliki:** `MainActivity.kt`
- **Kryteria akceptacji:** Brak „mignięcia" pustego ekranu.

### UX-2 — Prośba o `POST_NOTIFICATIONS` poza onboardingiem
- **Priorytet/Rozmiar:** 🔵 / S · **Status:** TODO
- **Problem:** Jeśli użytkownik pominie onboarding, uprawnienie nie zostanie poproszone.
- **Kroki:**
  - [ ] Delikatny baner/CTA w aplikacji, gdy brak uprawnienia i powiadomienia włączone.
- **Pliki:** `ui/…`
- **Kryteria akceptacji:** Użytkownik ma drugą szansę na nadanie uprawnienia.

### UX-3 — Reminder: polityka `KEEP` + reschedule tylko przy zmianie godziny
- **Priorytet/Rozmiar:** 🔵 / S · **Status:** TODO
- **Problem:** `enqueueUniquePeriodicWork(UPDATE)` przy każdym starcie resetuje kotwicę 24h.
- **Kroki:**
  - [ ] Użyć `KEEP` przy starcie; `UPDATE` tylko po zmianie godziny/przełącznika.
- **Pliki:** `reminder/ReminderWorker.kt`, `PairOMaticApp.kt`, `ui/settings/SettingsViewModel.kt`
- **Kryteria akceptacji:** Przypomnienie stabilnie odpala się o wybranej porze mimo częstych startów apki.

---

## Uwagi do realizacji

- **Jeden PR = jedno (lub kilka pokrewnych) drobne zadanie.** Ułatwia review i lokalizację ewentualnych błędów kompilacji.
- **M4 (testy) warto wpleść wcześnie** — testy `SelectionEngine`/`SchedulerRules`/`ProgressStats` zabezpieczają zmiany z M1–M3.
- Po każdym scaleniu powstaje nowy build APK (auto-release) — możliwość testów na telefonie.
- Aktualizuj checkboxy i kolumnę **Status** w tym pliku wraz z postępem.
