# 5. Funkcje aplikacji

[← Poprzednio: Algorytm doboru](04-algorytm-doboru.md) · [Spis treści](README.md)

---

Poza powiadomieniami (gdzie dzieje się nauka) aplikacja ma **cztery obszary funkcjonalne**:
statystyki, ustawienia, zarządzanie bazą par oraz import/eksport.

---

## 📈 Ekran statystyk

Szybki podgląd stanu Twojej talii. Wszystkie liczniki wyliczają się wprost z bazy par:

| Licznik | Co pokazuje |
|---|---|
| 🟢 Znam bardzo dobrze | liczba par na poziomie „bardzo dobrze" |
| 🟡 Znam w miarę | liczba par na poziomie „w miarę" |
| 🔴 Nie znam | liczba par ocenionych jako „nie znałem" |
| 🚩 „Nie wchodzi do głowy" | liczba par oflagowanych ręcznie |
| ⚪ Jeszcze nieklinięte | liczba par nigdy nieocenionych |

> ℹ️ **Kategorie mogą się przecinać** — np. para może być jednocześnie „nie znałem" **i** oflagowana.
> To zamierzone; liczniki nie muszą sumować się do całej talii.

**Opcjonalnie (miłe, nie wymagane w v1):** pasek postępu, procent opanowania, lista najsłabszych par.

---

## ⚙️ Ekran ustawień

Tu konfigurujesz sposób działania aplikacji:

- **Wybór trybu** — Test albo Immersja (wzajemnie wykluczające).
- **Główny włącznik powiadomień** — wstrzymaj lub wznów całą naukę jednym przełącznikiem.
- **Godziny ciszy** — okno (od–do), w którym powiadomienia się nie pojawiają (patrz niżej).
- **Kadencja immersji** — jak często auto-rotacja pokazuje nową parę (np. 15 / 30 / 60 min).
- **Poziom ważności powiadomień** — cichy (podmiana w miejscu) vs. „heads-up" (wyskakujące).
- *(Opcjonalnie)* strojenie stałych algorytmu doboru.

### 🌙 Godziny ciszy

Konfigurowalne okno (zwykle nocne), w którym **nie pojawiają się żadne powiadomienia** — w obu
trybach. Przy każdej próbie pokazania kolejnej pary aplikacja sprawdza godzinę: jeśli mieści się w
oknie ciszy, powiadomienie jest pomijane. Po zakończeniu okna nauka wznawia się przy najbliższej
okazji.

---

## 🗂️ Zarządzanie bazą par

Przy 400+ parach porządne narzędzia do przeglądania są niezbędne:

- **Lista par** z **wyszukiwaniem i filtrowaniem** — po literach, po poziomie znajomości, po fladze.
- **Dodawanie / edycja / usuwanie** pary — ustawienie słowa, obrazka, przełączenie flagi „trudna".
- **Przypisanie obrazka** — wybierasz plik, aplikacja kopiuje go do swojej pamięci.

---

## 📦 Import i eksport

Cała Twoja talia jest przenośna — możesz ją zarchiwizować lub podzielić się nią z kimś.

### Eksport

- Pakuje wszystko do pliku **`.zip`**: metadane (co jest jaką parą) + folder z obrazkami.
- Lokalizację zapisu wybierasz przez systemowy selektor plików Androida.
- **Opcja:** eksport **ze statystykami czy bez** — możesz udostępnić komuś „gołą" talię (same pary
  i obrazki, bez Twoich postępów) albo pełną kopię zapasową.

### Import

- Wczytuje `.zip`, wypakowuje obrazki do pamięci aplikacji i podpina je do par.
- Przy konfliktach (para o tych samych literach już istnieje) wybierasz strategię:
  - **Scal** — zaktualizuj istniejące pary,
  - **Zastąp wszystko** — wyczyść i wgraj od nowa.

### Opcjonalnie: CSV

Prosty import/eksport samego tekstu (`litery, słowo, nazwa_pliku_obrazka`) — wygodny do masowej
edycji całej talii w arkuszu kalkulacyjnym.

---

[← Poprzednio: Algorytm doboru](04-algorytm-doboru.md) · [Spis treści](README.md) · [Dalej: Wymagania techniczne →](06-wymagania-techniczne.md)
