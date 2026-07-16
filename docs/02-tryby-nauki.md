# 2. Tryby nauki

[← Poprzednio: Przegląd produktu](01-przeglad-produktu.md) · [Spis treści](README.md)

---

Aplikacja oferuje **dwa wzajemnie wykluczające się tryby** powiadomień. W danym momencie aktywny
jest tylko jeden — wybierasz go w ustawieniach.

| | 🎯 Tryb testu | 🌊 Tryb immersji |
|---|---|---|
| **Filozofia** | aktywne przypominanie (*active recall*) | bierna ekspozycja (*„patrzę i przyswajam"*) |
| **Co widać od razu** | tylko litery, np. `CT` | od razu obrazek |
| **Ocena** | 3 przyciski oceny | brak |
| **Statystyki** | zapisywane | **nie** zapisywane |
| **Rotacja** | zdarzeniowa (po Twojej reakcji) | automatyczna, na timerze |
| **Domyślny** | ✅ tak | — |

---

## 🎯 Tryb testu (z oceną) — domyślny

To serce aplikacji. Zamiast biernie patrzeć, **aktywnie przypominasz sobie** skojarzenie, zanim
sprawdzisz odpowiedź. Ten wysiłek przypomnienia jest dokładnie tym, co utrwala pamięć.

### Jak wygląda krok po kroku

1. **Powiadomienie zwinięte** pokazuje **wyłącznie parę liter**, np. `CT` — bez obrazka, bez słowa.
   To jest test: próbujesz sobie przypomnieć, jaki obraz kryje się pod tą parą.
2. **Rozwijasz powiadomienie** — pojawia się słowo (*Cytryna*), obrazek oraz **trzy przyciski oceny**.
3. **Oceniasz**, jak Ci poszło. Twoja ocena wpływa na to, jak często ta para wróci.
4. Po ocenie (lub odrzuceniu) — po krótkim opóźnieniu — wskakuje **kolejna para**.

> ⚙️ **Bez timera, bez serwisu w tle.** Nowa para pojawia się **dopiero wtedy, gdy zareagujesz** na
> bieżącą (ocenisz lub odrzucisz swipe'em). Tryb jest w pełni zdarzeniowy — nic nie „tyka" w tle.

### Trzy poziomy oceny

| Przycisk | Znaczenie | Efekt na powtórki |
|---|---|---|
| 🟢 **Znam bardzo dobrze** | opanowane | prawie w ogóle nie powtarzane |
| 🟡 **Znam w miarę** | średnio | powtarzane co jakiś czas |
| 🔴 **Nie znałem** | problem | powtarzane często |

**Co się dzieje przy interakcji:**

- **Kliknięcie oceny** → zapisuje poziom + znacznik czasu → pokazuje kolejną parę.
- **Swipe (odrzucenie bez oceny)** → traktowane jak „dalej": pokazuje kolejną parę, ale **nie
  zmienia** oceny odrzuconej pary. Przydatne, gdy nie chcesz teraz oceniać.

---

## 🌊 Tryb immersji (bez oceny)

Tryb do **biernego „nasiąkania"** materiałem. Nie testuje Cię — po prostu regularnie pokazuje
obrazki, żeby skojarzenia utrwalały się przez samą ekspozycję.

### Charakterystyka

- Powiadomienie pokazuje **od razu obrazek** (który zawiera już rysunek i litery), **bez przycisków**.
- **Nie zbiera żadnych statystyk** — nie zmienia ani poziomu znajomości, ani czasu ostatniego pokazania.
- **Jedyny tryb z automatyczną rotacją na timerze.** Nowa para pojawia się sama co ustalony czas
  (np. ~15 min lub rzadziej — konfigurowalne).
- Żeby ta sama para nie wracała dwa razy pod rząd, tryb pamięta **ostatnio pokazane** (lista
  trzymana tylko w pamięci, bez zapisu do bazy).

> 🧘 **Kiedy używać immersji?** Gdy chcesz „opłukać się" materiałem bez wysiłku oceniania — np.
> przy wprowadzaniu nowej partii par albo dla samego utrzymania kontaktu z talią.

---

## 🤔 Który tryb wybrać?

- **Chcesz realnie się uczyć i mierzyć postęp?** → 🎯 **Tryb testu**. Active recall + statystyki to
  najskuteczniejsza kombinacja.
- **Chcesz tylko biernie utrwalać, bez klikania?** → 🌊 **Tryb immersji**. Wygodny, ale nie mierzy
  postępów.

---

[← Poprzednio: Przegląd produktu](01-przeglad-produktu.md) · [Spis treści](README.md) · [Dalej: Model danych →](03-model-danych.md)
