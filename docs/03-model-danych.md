# 3. Model danych

[← Poprzednio: Tryby nauki](02-tryby-nauki.md) · [Spis treści](README.md)

---

Cała aplikacja kręci się wokół jednej encji: **pary liter**. Poniżej opis w formie przyjaznej do
czytania — szczegóły techniczne (typy, Room) znajdziesz w [`CLAUDE.md`](../CLAUDE.md).

---

## 🧩 Para (`Pair`)

Każda para to jeden rekord w bazie z następującymi cechami:

| Cecha | Co oznacza | Przykład |
|---|---|---|
| **Identyfikator** | wewnętrzny numer rekordu | `42` |
| **Litery** | sama para — unikalny klucz, po nim rozpoznajemy parę | `"CT"` |
| **Słowo** | słowo-obraz przypisane do pary | `"Cytryna"` |
| **Obrazek** | ścieżka do pliku obrazka w pamięci aplikacji (może być pusta) | `ct.png` |
| **Poziom** | jak dobrze znasz parę (patrz niżej) | `2` |
| **Ostatnio widziana** | kiedy ostatnio ją **oceniłeś/kliknąłeś** | `2026-07-16 14:30` |
| **Flaga „trudna"** | ręczne oznaczenie „nie wchodzi do głowy" | ✅ / ❌ |

---

## 📊 Poziom znajomości

To najważniejsze pole dla algorytmu doboru. Ma cztery możliwe stany:

| Wartość | Znaczenie |
|:---:|---|
| *(pusty)* | **jeszcze nigdy nieoceniona** — nowa para, nietknięta |
| `0` | 🔴 **nie znałem** |
| `1` | 🟡 **znam w miarę** |
| `2` | 🟢 **znam bardzo dobrze** |

> ⚠️ **Kluczowa subtelność:** stan „jeszcze nieoceniona" (pusty) to **coś innego** niż „nie znałem"
> (`0`). Nowa para, której nigdy nie widziałeś, to nie to samo co para, którą oceniłeś negatywnie.
> Aplikacja starannie te dwa stany rozróżnia — inaczej liczniki statystyk by się posklejały.

---

## 🖼️ Jak przechowywane są obrazki

- Obrazki to **pliki w pamięci wewnętrznej aplikacji**.
- W bazie danych trzymana jest **tylko ścieżka/nazwa pliku**, nie sam obraz.
- Obrazki są dostarczane z zewnątrz (mogą już mieć wrysowane litery) — aplikacja ich nie generuje.

---

## ⏱️ Kiedy aktualizuje się „ostatnio widziana"

To pole zmienia się **tylko w trybie testu**, przy ocenie lub kliknięciu. **Tryb immersji go nie
dotyka** — bierna ekspozycja celowo nie wpływa na statystyki ani na harmonogram doboru.

---

## 🔮 Co z historią w czasie?

W wersji v1 aplikacja przechowuje **tylko bieżący stan** każdej pary — wszystkie statystyki liczą
się wprost z tabeli par. Pełna historia ocen w czasie (do wykresów trendów) to pomysł na przyszłość:
wystarczyłoby dodać osobny log zdarzeń. **W v1 nie jest potrzebny.**

---

[← Poprzednio: Tryby nauki](02-tryby-nauki.md) · [Spis treści](README.md) · [Dalej: Algorytm doboru →](04-algorytm-doboru.md)
