# 4. Algorytm doboru pary

[← Poprzednio: Model danych](03-model-danych.md) · [Spis treści](README.md)

---

Skąd aplikacja wie, **którą parę pokazać następną**? Nie z góry ustalonego harmonogramu jak w
klasycznych fiszkach, lecz przez **losowanie ważone**. Każda para dostaje „wagę" — im wyższa, tym
większa szansa, że wypadnie następna. To podejście świetnie pasuje do ambientowego, nieregularnego
pokazywania i naturalnie realizuje zasadę **„trudne i oflagowane — częściej"**.

---

## ⚖️ Z czego składa się waga pary

Waga to iloczyn czterech czynników. Jeśli którykolwiek „wyzeruje" parę, po prostu nie wejdzie ona do
najbliższego losowania.

```
                    ┌─────────────┐
   cooldown  ──────▶│             │
   poziom    ──────▶│   WAGA  =   │──────▶  szansa wylosowania
   świeżość  ──────▶│  iloczyn    │
   flaga     ──────▶│             │
                    └─────────────┘
```

### 1. 🧊 Cooldown — „chwila przerwy"

Para pokazana przed chwilą **tymczasowo wypada z puli**, żeby nie wróciła dwa razy pod rząd.
Domyślnie przez **30 minut** po ostatnim pokazaniu jej waga wynosi zero.

### 2. 📊 Poziom znajomości — „im gorzej znana, tym częściej"

Serce mechanizmu. Pary słabiej opanowane dostają większą wagę:

| Poziom | Waga bazowa | Jak często |
|---|:---:|---|
| 🔴 nie znałem (`0`) | **10** | najczęściej |
| ⚪ nowa (nieoceniona) | **8** | często — wchodzą do rotacji |
| 🟡 znam w miarę (`1`) | **4** | co jakiś czas |
| 🟢 znam bardzo dobrze (`2`) | **1** | rzadko, ale **nie nigdy** |

> 💡 Nawet dobrze znane pary pojawiają się sporadycznie — po to, żeby nie „wypadły z głowy".

### 3. 🕰️ Świeżość — „im dawniej widziana, tym większa waga"

Im więcej czasu minęło od ostatniego pokazania pary, tym bardziej rośnie jej waga (ale wzrost jest
ograniczony z góry, np. do ~24 godzin). Dzięki temu pary, których dawno nie widziałeś, wracają do
gry.

### 4. 🚩 Flaga „nie wchodzi do głowy"

Jeśli ręcznie oznaczysz parę jako trudną, jej waga jest dodatkowo **mnożona** (domyślnie ×3). To
Twój „ręczny bat" na uparte skojarzenia.

---

## 🎲 Jak wygląda losowanie

```
1. Odrzuć pary w cooldownie (waga = 0).
2. Policz wagę każdej pozostałej pary
   (poziom × świeżość × flaga).
3. Wylosuj jedną parę — z prawdopodobieństwem
   proporcjonalnym do jej wagi.
```

**Efekt w praktyce:** pary „nie znałem" i oflagowane dominują w rotacji, „bardzo dobrze" pojawiają
się od czasu do czasu dla utrwalenia, a świeżo pokazana para nie wraca natychmiast.

---

## 🎛️ Wartości startowe (strojalne)

| Parametr | Domyślnie | Rola |
|---|:---:|---|
| Waga: nowa | 8 | nowe pary wchodzą do rotacji, ale nie zalewają jej naraz |
| Waga: nie znałem | 10 | najwyższy priorytet |
| Waga: w miarę | 4 | średni priorytet |
| Waga: bardzo dobrze | 1 | rzadkie utrwalanie |
| Mnożnik flagi | ×3 | wzmocnienie par trudnych |
| Cooldown | 30 min | brak natychmiastowych powtórek |
| Limit świeżości | ~24 h | górna granica wzrostu wagi z czasem |

Te wartości to sensowny punkt startowy — będzie można je stroić w ustawieniach.

> 🧭 **Do rozważenia:** limit liczby *nowych* par wprowadzanych naraz, żeby nie zrzucić od razu
> wszystkich 400 nieznanych par na głowę użytkownika.

---

[← Poprzednio: Model danych](03-model-danych.md) · [Spis treści](README.md) · [Dalej: Funkcje aplikacji →](05-funkcje-aplikacji.md)
