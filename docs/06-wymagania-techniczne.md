# 6. Wymagania techniczne

[← Poprzednio: Funkcje aplikacji](05-funkcje-aplikacji.md) · [Spis treści](README.md)

---

Ten rozdział to przyjazny przegląd decyzji technicznych. Poziom „dla implementacji" (nazwy klas,
API Androida) znajdziesz w [`CLAUDE.md`](../CLAUDE.md).

---

## 📱 Platforma i wersje Androida

| Parametr | Wartość | Uwaga |
|---|---|---|
| Platforma | Android, aplikacja natywna | Kotlin + AndroidX |
| Minimalna wersja | Android 8.0 (`minSdk 26`) | *(do potwierdzenia z urządzeniem)* |
| Docelowa wersja | najnowsza stabilna | |
| Kluczowy próg | Android 13+ | wymaga zgody na powiadomienia w trakcie działania |

---

## 🚚 Dystrybucja: sideload

Wersja v1 jest instalowana **prywatnie, przez sideload** (bez Google Play). To ważna decyzja, bo:

- ✅ **nie obowiązują ograniczenia Google Play** (polityki, typy usług w tle),
- ⚠️ w zamian **użytkownik ręcznie** wyłącza optymalizację baterii dla aplikacji.

Publikacja w Google Play to ewentualność na przyszłość, poza zakresem v1.

---

## 🔐 Uprawnienia i ograniczenia

| Zagadnienie | Jak to rozwiązujemy |
|---|---|
| 🔔 **Zgoda na powiadomienia** (Android 13+) | prośba przy pierwszym uruchomieniu |
| ⏰ **Timer trybu immersji** | mechanizm systemowy do zadań cyklicznych (min. 15 min) — bez dodatkowych uprawnień |
| 🔋 **„Zabójcy tła"** (Xiaomi, Huawei, Samsung, OPPO…) | onboarding prowadzący przez wyłączenie optymalizacji baterii i autostartu |
| 🧵 **Usługa pierwszoplanowa (foreground service)** | **niepotrzebna** — patrz niżej |
| 🖼️ **Przechowywanie obrazków** | pamięć wewnętrzna aplikacji; import/eksport przez systemowy selektor plików |

### Dlaczego nie potrzebujemy usługi w tle?

Bo **tryb testu jest zdarzeniowy** — kolejna para pojawia się dopiero, gdy zareagujesz na obecną
(ocena/swipe). Nic nie musi „tykać" w tle. Jedyny element czasowy w całej aplikacji to timer trybu
immersji, który obsługuje standardowy systemowy planer zadań. To duże uproszczenie i oszczędność
baterii.

---

## 🏗️ Architektura (w zarysie)

Propozycja podziału odpowiedzialności (nie sztywny wymóg):

```
┌───────────────────────────────────────────────┐
│                     UI                         │
│  lista/edycja par · statystyki · ustawienia    │
│              · import/eksport                  │
└───────────────────────────────────────────────┘
                      │
┌───────────────────────────────────────────────┐
│                Warstwa domenowa                │
│  „silnik doboru" (czysta, testowalna funkcja)  │
│  reguły planowania (godziny ciszy, przełącznik)│
└───────────────────────────────────────────────┘
                      │
┌─────────────────────────┐   ┌──────────────────┐
│    Baza par (Room)      │   │   Powiadomienia   │
│   400+ rekordów         │   │  budowa + obsługa │
│                         │   │  ocen i odrzuceń  │
└─────────────────────────┘   └──────────────────┘
```

**Kluczowa zasada projektowa:** logika wyboru kolejnej pary to **czysta funkcja** — łatwa do
pokrycia testami jednostkowymi, bez zależności od Androida. To najważniejszy element do
przetestowania.

---

## 💾 Skala i wydajność

Baza liczy **400+ par**. Dla lokalnej bazy SQLite to trywialne obciążenie — nie ma tu problemu
wydajnościowego, ale interfejs (lista, wyszukiwanie, filtrowanie) musi być zaprojektowany z myślą o
wygodnym przeglądaniu takiej liczby pozycji.

---

## 🔒 Prywatność i kopie zapasowe

Aplikacja jest **w pełni offline** — brak kont, chmury i synchronizacji. Zgodnie z tym założeniem
**Android Auto Backup jest wyłączony** (`android:allowBackup="false"` w `AndroidManifest.xml`),
żeby baza par, statystyki i ustawienia (DataStore) **nie trafiały do Google Drive**. Kopie zapasowe
robi się świadomie i lokalnie — przez eksport talii do `.zip`/`.csv` (SAF), a aktualizacje aplikacji
zachowują dane dzięki stałemu kluczowi podpisu.

---

[← Poprzednio: Funkcje aplikacji](05-funkcje-aplikacji.md) · [Spis treści](README.md) · [Dalej: Plan rozwoju →](07-plan-rozwoju.md)
