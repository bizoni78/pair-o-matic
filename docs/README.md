# 📚 Dokumentacja — Letter Pairs Trainer

Witaj w dokumentacji projektu **Letter Pairs Trainer** — aplikacji na Androida do treningu
skojarzeń *letter pairs* metodą powiadomień.

> 💡 **Dla kogo jest ta dokumentacja?**
> Te pliki są napisane **dla ludzi** — do czytania, zrozumienia produktu i planowania.
> Jeśli szukasz zwięzłego kontekstu technicznego dla Claude Code, zajrzyj do [`CLAUDE.md`](../CLAUDE.md)
> w katalogu głównym.

---

## 🧭 Spis treści

| Rozdział | Opis |
|---|---|
| [1. Przegląd produktu](01-przeglad-produktu.md) | Czym jest aplikacja, dla kogo i jaki problem rozwiązuje |
| [2. Tryby nauki](02-tryby-nauki.md) | Tryb testu vs. tryb immersji — jak działają i czym się różnią |
| [3. Model danych](03-model-danych.md) | Jak reprezentowana jest pojedyncza para liter |
| [4. Algorytm doboru pary](04-algorytm-doboru.md) | Jak aplikacja decyduje, którą parę pokazać następną |
| [5. Funkcje aplikacji](05-funkcje-aplikacji.md) | Ekrany: statystyki, ustawienia, zarządzanie bazą, import/eksport |
| [6. Wymagania techniczne](06-wymagania-techniczne.md) | Android, uprawnienia, ograniczenia, architektura |
| [7. Plan rozwoju](07-plan-rozwoju.md) | Kolejność prac, zakres v1 i otwarte kwestie |
| [🛠️ Roadmapa poprawek](ROADMAP.md) | Lista zadań z audytu kodu (bezpieczeństwo, stabilność, wydajność, testy) do wdrożenia |

---

## ⚡ W skrócie

**Letter pairs** to technika pamięciowa, w której każdej parze liter (np. `CT`) przypisujesz
słowo-obraz (np. *Cytryna*). Wytrenowane skojarzenia pozwalają później błyskawicznie kodować
karty, liczby czy dowolne ciągi znaków.

Ta aplikacja **trenuje te skojarzenia w tle Twojego dnia** — głównie przez powiadomienia
pojawiające się przy odblokowywaniu telefonu. Zamiast wygospodarowywać czas na osobne sesje,
utrwalasz pary w krótkich mikro-momentach, a aplikacja pilnuje, żeby częściej wracały te,
które sprawiają Ci trudność.

### Trzy rzeczy, które warto zapamiętać

1. 🔔 **Nauka dzieje się w powiadomieniach** — aplikację otwierasz głównie do zarządzania i statystyk.
2. 🎯 **Trudniejsze pary wracają częściej** — dzięki losowaniu ważonemu (nie sztywnym fiszkom).
3. 📴 **W pełni offline i prywatnie** — brak kont, chmury i synchronizacji.

---

## 📌 Status projektu

**Wersja robocza specyfikacji (v1).** Aplikacja jest na etapie planowania — dokumentacja opisuje
*co* zbudować, zanim powstanie kod. Nazwa "Letter Pairs Trainer" jest robocza.
