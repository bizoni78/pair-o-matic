package com.pairomatic.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Pojedyncza para liter wraz ze stanem nauki.
 *
 * Ważna subtelność: [level] == null oznacza „jeszcze nigdy nieoceniona" i musi być
 * odróżnialne od level == 0 („nie znałem"). Analogicznie [lastSeen] == null oznacza
 * „nigdy nieoceniona/nieklinięta".
 */
@Entity(
    tableName = "pairs",
    indices = [Index(value = ["letters"], unique = true)]
)
data class PairEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Para liter, np. "CT" — unikalny klucz biznesowy. */
    val letters: String,
    /** Słowo-obraz, np. "Cytryna". */
    val word: String,
    /** Nazwa pliku obrazka w pamięci wewnętrznej aplikacji (bez ścieżki katalogu). */
    val imagePath: String? = null,
    /** null = nigdy nieoceniona; 0 = nie znałem; 1 = w miarę; 2 = bardzo dobrze. */
    val level: Int? = null,
    /** epoch millis ostatniej oceny/kliknięcia (nie dotyczy immersji); null = nigdy. */
    val lastSeen: Long? = null,
    /** Ręczne oznaczenie „nie wchodzi do głowy". */
    val hardFlag: Boolean = false,
    /** Ręczne oznaczenie „słowo do zmiany" — para, dla której szukamy lepszego słowa. */
    val reviewFlag: Boolean = false
)
