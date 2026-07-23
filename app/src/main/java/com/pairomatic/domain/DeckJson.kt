package com.pairomatic.domain

import org.json.JSONArray
import org.json.JSONObject

/**
 * Czyste kodowanie/dekodowanie talii do/z formatu `pairs.json` (metadane w eksporcie `.zip`).
 * Wydzielone z `PairRepository`, żeby round-trip eksport→import był testowalny bez Androida/IO.
 */
object DeckJson {

    /** Neutralna reprezentacja pary do (de)serializacji — bez zależności od Room. */
    data class DeckPair(
        val letters: String,
        val word: String,
        val imagePath: String?,
        val level: Int?,
        val lastSeen: Long?,
        val hardFlag: Boolean,
        val reviewFlag: Boolean
    )

    /**
     * Serializuje pary do JSON.
     * @param includeStats gdy false, pomija level/lastSeen/hardFlag/reviewFlag („goła" talia).
     */
    fun encode(pairs: List<DeckPair>, includeStats: Boolean): String {
        val json = JSONArray()
        for (p in pairs) {
            val obj = JSONObject()
                .put("letters", p.letters)
                .put("word", p.word)
                .put("image", p.imagePath ?: JSONObject.NULL)
            if (includeStats) {
                obj.put("level", p.level ?: JSONObject.NULL)
                obj.put("lastSeen", p.lastSeen ?: JSONObject.NULL)
                obj.put("hardFlag", p.hardFlag)
                obj.put("reviewFlag", p.reviewFlag)
            }
            json.put(obj)
        }
        return json.toString(2)
    }

    /**
     * Parsuje JSON talii. Brakujące pola statystyk przyjmują wartości domyślne
     * (level/lastSeen = null, flagi = false), więc „goła" talia wczytuje się poprawnie.
     * @param limit maksymalna liczba wczytanych par (ochrona przed zalaniem).
     */
    fun decode(content: String, limit: Int = Int.MAX_VALUE): List<DeckPair> {
        val array = JSONArray(content)
        val n = minOf(array.length(), limit)
        val out = ArrayList<DeckPair>(n)
        for (i in 0 until n) {
            val obj = array.getJSONObject(i)
            out.add(
                DeckPair(
                    letters = obj.getString("letters"),
                    word = obj.optString("word", ""),
                    imagePath = obj.optString("image", null)?.takeIf { it.isNotBlank() && it != "null" },
                    level = if (obj.isNull("level")) null else obj.optInt("level"),
                    lastSeen = if (obj.isNull("lastSeen")) null else obj.optLong("lastSeen"),
                    hardFlag = obj.optBoolean("hardFlag", false),
                    reviewFlag = obj.optBoolean("reviewFlag", false)
                )
            )
        }
        return out
    }
}
