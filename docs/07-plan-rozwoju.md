# 7. Plan rozwoju

[← Poprzednio: Wymagania techniczne](06-wymagania-techniczne.md) · [Spis treści](README.md)

---

## 🗺️ Sugerowana kolejność prac

Każdy krok buduje na poprzednim i można go osobno przetestować, zanim ruszysz dalej:

| # | Etap | Dlaczego w tej kolejności |
|:---:|---|---|
| 1 | 🗄️ **Baza par + podstawowy import** | Bez danych nie ma czego pokazywać — najpierw sposób na wgranie 400+ par |
| 2 | 📝 **Ekran listy/edycji par + import/eksport `.zip`** | Zarządzanie talią i przenośność danych |
| 3 | ⚖️ **Silnik doboru (losowanie ważone)** | Czysta, przetestowana funkcja — serce logiki |
| 4 | 🎯 **Tryb testu (powiadomienia)** | Główna pętla nauki: pokaż `CT` → rozwiń → oceń → następna |
| 5 | 📈 **Ekran statystyk** | Podgląd postępów (5 liczników) |
| 6 | 🌊 **Tryb immersji + auto-rotacja** | Drugi tryb nauki, na timerze |
| 7 | ⚙️ **Ustawienia** | Wybór trybu, godziny ciszy, kadencja, główny przełącznik |
| 8 | 🔋 **Onboarding baterii + zgoda na powiadomienia** | Ostatnie szlify, by nauka działała niezawodnie |

---

## ✅ Zakres wersji 1

Aplikacja **jednoużytkownikowa, w pełni offline**:

- 🔔 Nauka przez powiadomienia w dwóch trybach (test / immersja).
- 🗂️ Zarządzanie bazą 400+ par z wyszukiwaniem i filtrowaniem.
- ⚖️ Inteligentny dobór par (losowanie ważone).
- 📈 Statystyki bieżącego stanu talii.
- 📦 Import/eksport talii (`.zip`, opcjonalnie CSV).
- 🌙 Godziny ciszy i główny włącznik.

---

## 🚫 Poza zakresem v1 (świadome „nie")

Te rzeczy **celowo** odkładamy — nie są potrzebne do działającego produktu:

| Czego **nie** robimy w v1 | Dlaczego |
|---|---|
| ☁️ Synchronizacja w chmurze, konta, wiele urządzeń | v1 jest prywatna i offline |
| 🏪 Publikacja w Google Play | dystrybucja przez sideload |
| ✏️ Generowanie liter na obrazkach | obrazki są gotowe, dostarczone z zewnątrz |
| 📉 Wykresy trendów w czasie | możliwe później (wymaga logu zdarzeń) |
| 📲 Wybudzanie ekranu „na żądanie" | niemożliwe i niepotrzebne — powiadomienie czeka na ekranie blokady |

---

## ❓ Otwarte kwestie do potwierdzenia

Przed niektórymi decyzjami warto się umówić co do szczegółów:

- [ ] **Docelowa wersja Androida / model urządzenia** — wpływa na minimalną wersję i szczegóły uprawnień.
- [ ] **Wartości startowe algorytmu doboru** — czy przyjmujemy sugerowane (patrz [rozdział 4](04-algorytm-doboru.md)).
- [ ] **Eksport domyślnie ze statystykami czy bez?**
- [ ] **Limit nowych par wprowadzanych naraz** — czy go wprowadzać, by nie zrzucić od razu wszystkich 400.
- [ ] **Nazwa aplikacji** — obecnie robocza: „Letter Pairs Trainer".

---

[← Poprzednio: Wymagania techniczne](06-wymagania-techniczne.md) · [Spis treści](README.md)
