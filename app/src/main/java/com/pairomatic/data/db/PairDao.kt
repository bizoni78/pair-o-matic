package com.pairomatic.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PairDao {

    @Query("SELECT * FROM pairs ORDER BY letters ASC")
    fun observeAll(): Flow<List<PairEntity>>

    /** Liczniki statystyk jednym zapytaniem agregującym (bez ładowania całej tabeli do pamięci). */
    @Query(
        """
        SELECT
            COUNT(*) AS total,
            COALESCE(SUM(CASE WHEN level = 2 THEN 1 ELSE 0 END), 0) AS veryWell,
            COALESCE(SUM(CASE WHEN level = 1 THEN 1 ELSE 0 END), 0) AS soso,
            COALESCE(SUM(CASE WHEN level = 0 THEN 1 ELSE 0 END), 0) AS dontKnow,
            COALESCE(SUM(CASE WHEN hardFlag = 1 THEN 1 ELSE 0 END), 0) AS hard,
            COALESCE(SUM(CASE WHEN level IS NULL THEN 1 ELSE 0 END), 0) AS neverGraded,
            COALESCE(SUM(CASE WHEN word = '' THEN 1 ELSE 0 END), 0) AS noWord,
            COALESCE(SUM(CASE WHEN imagePath IS NULL OR imagePath = '' THEN 1 ELSE 0 END), 0) AS noImage,
            COALESCE(SUM(CASE WHEN reviewFlag = 1 THEN 1 ELSE 0 END), 0) AS review
        FROM pairs
        """
    )
    fun observeStats(): Flow<StatsCounts>

    @Query("SELECT * FROM pairs")
    suspend fun getAll(): List<PairEntity>

    /**
     * PERF-2: kandydaci do doboru z wagą policzoną w SQL (bez ładowania całej tabeli).
     * Waga = poziom × świeżość × flaga „trudna" — dokładnie jak w [com.pairomatic.domain.SelectionEngine].
     * Pomija pary w cooldownie oraz nadmiarowe nowe pary (level IS NULL) ponad `newLimit` (najstarsze po id).
     * Wykluczenia „ostatnio pokazanych" nakłada warstwa wyżej (mała lista → prościej i taniej w Kotlinie).
     */
    @Query(
        """
        SELECT id AS id,
            (CASE
                WHEN level IS NULL THEN :wNull
                WHEN level = 0 THEN :w0
                WHEN level = 1 THEN :w1
                WHEN level = 2 THEN :w2
                ELSE :wNull END)
            * (1.0 + MIN(
                CASE WHEN lastSeen IS NULL THEN :capHours
                     ELSE MAX((:now - lastSeen) / 3600000.0, 0.0) END,
                :capHours))
            * (CASE WHEN hardFlag = 1 THEN :hardBoost ELSE 1.0 END) AS weight
        FROM pairs
        WHERE (lastSeen IS NULL OR (:now - lastSeen) >= :cooldownMillis)
          AND (level IS NOT NULL
               OR id IN (SELECT id FROM pairs WHERE level IS NULL ORDER BY id ASC LIMIT :newLimit))
        """
    )
    suspend fun selectionCandidates(
        now: Long,
        cooldownMillis: Long,
        wNull: Double,
        w0: Double,
        w1: Double,
        w2: Double,
        capHours: Double,
        hardBoost: Double,
        newLimit: Int
    ): List<PairWeight>

    @Query("SELECT * FROM pairs WHERE id = :id")
    suspend fun getById(id: Long): PairEntity?

    @Query("SELECT * FROM pairs WHERE letters = :letters LIMIT 1")
    suspend fun getByLetters(letters: String): PairEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(pair: PairEntity): Long

    @Update
    suspend fun update(pair: PairEntity)

    @Delete
    suspend fun delete(pair: PairEntity)

    @Query("DELETE FROM pairs")
    suspend fun deleteAll()

    /** Zapisuje ocenę (poziom + czas) bez naruszania pozostałych pól. */
    @Query("UPDATE pairs SET level = :level, lastSeen = :lastSeen WHERE id = :id")
    suspend fun grade(id: Long, level: Int, lastSeen: Long)

    /** Przełącza flagę „słowo do zmiany" bez naruszania pozostałych pól. */
    @Query("UPDATE pairs SET reviewFlag = :flag WHERE id = :id")
    suspend fun setReviewFlag(id: Long, flag: Boolean)

    /** Przełącza flagę „nie wchodzi do głowy" bez naruszania pozostałych pól. */
    @Query("UPDATE pairs SET hardFlag = :flag WHERE id = :id")
    suspend fun setHardFlag(id: Long, flag: Boolean)

    /** Szybka edycja samego słowa (bez naruszania statystyk i obrazka). */
    @Query("UPDATE pairs SET word = :word WHERE id = :id")
    suspend fun updateWord(id: Long, word: String)

    /** Masowe ustawienie flagi „słowo do zmiany" dla wielu par. */
    @Query("UPDATE pairs SET reviewFlag = :flag WHERE id IN (:ids)")
    suspend fun setReviewFlagMany(ids: List<Long>, flag: Boolean)

    /** Masowe ustawienie flagi „nie wchodzi do głowy" dla wielu par. */
    @Query("UPDATE pairs SET hardFlag = :flag WHERE id IN (:ids)")
    suspend fun setHardFlagMany(ids: List<Long>, flag: Boolean)

    /** Masowe usunięcie par po id (obrazki kasuje repozytorium). */
    @Query("DELETE FROM pairs WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}
