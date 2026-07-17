<div align="center">

# 🧠 Letter Pairs Trainer

**Trenuj skojarzenia _letter pairs_ w tle codziennego używania telefonu — głównie przez powiadomienia.**

[![Android CI](https://github.com/bizoni78/pair-o-matic/actions/workflows/android.yml/badge.svg)](https://github.com/bizoni78/pair-o-matic/actions/workflows/android.yml)
![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4)
![Offline](https://img.shields.io/badge/tryb-100%25%20offline-success)

</div>

---

## ✨ O projekcie

**Letter pairs** to technika pamięciowa, w której każdej **parze liter** (np. `CT`) przypisujesz
jedno wyraziste **słowo-obraz** (np. _Cytryna_). Wytrenowane skojarzenia pozwalają później błyskawicznie
kodować karty, liczby czy dowolne ciągi znaków.

Problem w tym, że opanowanie 400+ takich skojarzeń wymaga setek powtórek. **Letter Pairs Trainer**
rozwiązuje to, przemycając naukę w tło Twojego dnia: przy okazji odblokowywania telefonu widzisz na
ekranie blokady kolejną parę i utrwalasz ją w kilkusekundowej mikro-sesji. Aplikacja śledzi, które pary
masz opanowane, a które sprawiają trudność — i **częściej pokazuje te słabsze**.

> 📴 W pełni **offline i prywatnie** — bez kont, chmury i synchronizacji.

---

## 🎯 Dwa tryby nauki

Aplikacja oferuje dwa wzajemnie wykluczające się tryby powiadomień (wybór w ustawieniach):

| | 🎯 **Tryb testu** _(domyślny)_ | 🌊 **Tryb immersji** |
|---|---|---|
| **Filozofia** | aktywne przypominanie (_active recall_) | bierna ekspozycja |
| **Zwinięte** | tylko litery, np. `CT` — to jest test | od razu obrazek |
| **Rozwinięte** | słowo + obrazek + 3 przyciski oceny | — (bez przycisków) |
| **Statystyki** | zapisywane | nie zapisywane |
| **Rotacja** | zdarzeniowa (po Twojej reakcji) | automatyczna, na timerze |

W trybie testu oceniasz każdą parę w skali **„nie znałem" / „w miarę" / „bardzo dobrze"**, a ocena
wpływa na to, jak często para wróci.

---

## 🚀 Funkcje

- 🔔 **Nauka przez powiadomienia** — dwa tryby (test / immersja), bez potrzeby otwierania aplikacji.
- ⚖️ **Inteligentny dobór par** — losowanie ważone: trudniejsze i oflagowane pary wracają częściej.
- 🗂️ **Zarządzanie bazą 400+ par** — lista z wyszukiwaniem i filtrowaniem, pełny CRUD, obrazki.
- 📈 **Statystyki** — bieżący stan opanowania talii w pięciu licznikach.
- 🌙 **Godziny ciszy** — okno (np. nocne), w którym powiadomienia się nie pojawiają.
- 📦 **Import / eksport** — przenośna talia w pliku `.zip` (metadane + obrazki), z/bez statystyk.
- 🎛️ **Ustawienia** — tryb, główny przełącznik, kadencja immersji, poziom ważności powiadomień.

---

## ⚙️ Jak działa dobór pary

Zamiast sztywnego harmonogramu (jak w klasycznych fiszkach), aplikacja używa **losowania ważonego**.
Każda para dostaje wagę — im wyższa, tym większa szansa, że pojawi się następna:

```
waga(para) = poziom_znajomości × świeżość × flaga_„trudna"     (para w cooldownie → waga 0)
```

- **Poziom znajomości** — „nie znałem" waży najwięcej, „bardzo dobrze" najmniej (ale nie zero).
- **Świeżość** — im dawniej widziana, tym większa waga (ograniczona z góry).
- **Flaga „nie wchodzi do głowy"** — ręczny mnożnik dla upartych par.
- **Cooldown** — świeżo pokazana para chwilowo wypada z puli.

Szczegóły i wartości domyślne: [`docs/04-algorytm-doboru.md`](docs/04-algorytm-doboru.md).

---

## 🛠️ Stos technologiczny

| Warstwa | Technologia |
|---|---|
| Język / UI | **Kotlin** · **Jetpack Compose** (Material 3) |
| Architektura | warstwy `data` / `domain` / `notifications` / `immersion` / `ui` |
| Baza danych | **Room** (SQLite) |
| Ustawienia | **DataStore** Preferences |
| Timer immersji | **WorkManager** |
| Obrazki | **Coil** |
| Build | **Gradle** (version catalog) · **AGP** · **KSP** |
| CI | **GitHub Actions** (kompilacja + testy jednostkowe) |

**Wymagania:** `minSdk 26` (Android 8.0) · `targetSdk 35` · kompilacja Java 17.

---

## 🏗️ Budowanie

```bash
# 1. Sklonuj repozytorium
git clone https://github.com/bizoni78/pair-o-matic.git
cd pair-o-matic

# 2. Otwórz w Android Studio (File → Open) i pozwól na Gradle Sync,
#    albo zbuduj z linii poleceń:
./gradlew assembleDebug        # zbuduj debugowy APK
./gradlew testDebugUnitTest    # uruchom testy jednostkowe
```

Gotowy plik `app-debug.apk` znajdziesz w `app/build/outputs/apk/debug/`.
Każdy push i pull request jest automatycznie budowany i testowany przez [CI](../../actions).

---

## 📁 Struktura projektu

```
app/src/main/java/com/pairomatic/
├── data/            # Room (encja Pair, DAO, baza), repozytorium, ustawienia (DataStore)
├── domain/          # SelectionEngine (dobór ważony), reguły planowania (godziny ciszy)
├── notifications/   # budowa powiadomień, odbiorniki ocen i odrzuceń
├── immersion/       # ImmersionWorker (WorkManager)
└── ui/              # ekrany Compose: pary, statystyki, ustawienia + nawigacja
```

---

## 📚 Dokumentacja

- 📖 **Dla ludzi** — przyjazny opis produktu, tryby, algorytm, funkcje: [`docs/`](docs/README.md)
- 🤖 **Dla Claude Code** — zwięzły kontekst techniczny: [`CLAUDE.md`](CLAUDE.md)

---

## 🗺️ Status

**Wersja v1 — w budowie.** Fundament aplikacji (baza, dobór par, powiadomienia, ekrany) jest gotowy.
Kolejność prac i otwarte kwestie: [`docs/07-plan-rozwoju.md`](docs/07-plan-rozwoju.md).

---

<div align="center">
<sub>Projekt prywatny · dystrybucja przez sideload · nazwa robocza „Letter Pairs Trainer"</sub>
</div>
