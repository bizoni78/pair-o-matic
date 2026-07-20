package com.pairomatic.data

import android.content.Context
import android.net.Uri
import com.pairomatic.data.db.PairDao
import com.pairomatic.data.db.PairEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Jedyne źródło prawdy dla par. Obsługuje CRUD, przechowywanie obrazków w pamięci
 * wewnętrznej oraz import/eksport talii do pliku `.zip`.
 */
class PairRepository(
    private val dao: PairDao,
    context: Context
) {
    private val appContext = context.applicationContext
    private val imagesDir: File = File(appContext.filesDir, "images").apply { mkdirs() }

    fun observeAll(): Flow<List<PairEntity>> = dao.observeAll()

    suspend fun getAllPairs(): List<PairEntity> = withContext(Dispatchers.IO) { dao.getAll() }

    suspend fun getById(id: Long): PairEntity? = dao.getById(id)

    val imagesDirectory: File get() = imagesDir

    suspend fun upsert(pair: PairEntity): Long = withContext(Dispatchers.IO) {
        if (pair.id == 0L) dao.insert(pair) else {
            dao.update(pair)
            pair.id
        }
    }

    suspend fun delete(pair: PairEntity) = withContext(Dispatchers.IO) {
        pair.imagePath?.let { File(imagesDir, it).delete() }
        dao.delete(pair)
    }

    suspend fun grade(id: Long, level: Int, lastSeen: Long) = withContext(Dispatchers.IO) {
        dao.grade(id, level, lastSeen)
    }

    /** Ustawia/zdejmuje flagę „słowo do zmiany" dla danej pary. */
    suspend fun setReviewFlag(id: Long, flag: Boolean) = withContext(Dispatchers.IO) {
        dao.setReviewFlag(id, flag)
    }

    /** Ustawia/zdejmuje flagę „nie wchodzi do głowy" dla danej pary. */
    suspend fun setHardFlag(id: Long, flag: Boolean) = withContext(Dispatchers.IO) {
        dao.setHardFlag(id, flag)
    }

    /** Szybka edycja samego słowa danej pary. */
    suspend fun updateWord(id: Long, word: String) = withContext(Dispatchers.IO) {
        dao.updateWord(id, word)
    }

    /** Masowo ustawia flagę „słowo do zmiany" dla wielu par. */
    suspend fun setReviewFlagMany(ids: List<Long>, flag: Boolean) = withContext(Dispatchers.IO) {
        if (ids.isNotEmpty()) dao.setReviewFlagMany(ids, flag)
    }

    /** Masowo ustawia flagę „nie wchodzi do głowy" dla wielu par. */
    suspend fun setHardFlagMany(ids: List<Long>, flag: Boolean) = withContext(Dispatchers.IO) {
        if (ids.isNotEmpty()) dao.setHardFlagMany(ids, flag)
    }

    /** Masowo usuwa pary wraz z ich plikami obrazków. */
    suspend fun deleteMany(pairs: List<PairEntity>) = withContext(Dispatchers.IO) {
        if (pairs.isEmpty()) return@withContext
        pairs.forEach { p -> p.imagePath?.let { File(imagesDir, it).delete() } }
        dao.deleteByIds(pairs.map { it.id })
    }

    /**
     * Usuwa tylko wiersze par (BEZ kasowania plików obrazków) — na potrzeby „Cofnij".
     * Osierocone pliki można potem posprzątać przez [deleteOrphanImages].
     */
    suspend fun deleteRowsOnly(pairs: List<PairEntity>) = withContext(Dispatchers.IO) {
        if (pairs.isNotEmpty()) dao.deleteByIds(pairs.map { it.id })
    }

    /** Przywraca wcześniej usunięte pary (z zachowaniem id i ścieżek obrazków). */
    suspend fun restorePairs(pairs: List<PairEntity>) = withContext(Dispatchers.IO) {
        pairs.forEach { runCatching { dao.insert(it) } }
    }

    /** Liczy pliki obrazków w pamięci, do których nie odwołuje się żadna para. */
    suspend fun countOrphanImages(): Int = withContext(Dispatchers.IO) {
        orphanImageFiles().size
    }

    /** Usuwa osierocone pliki obrazków i zwraca ich liczbę. */
    suspend fun deleteOrphanImages(): Int = withContext(Dispatchers.IO) {
        val orphans = orphanImageFiles()
        orphans.forEach { it.delete() }
        orphans.size
    }

    /** Pliki obrazków w pamięci, do których nie odwołuje się żadna para. */
    private suspend fun orphanImageFiles(): List<File> {
        val used = dao.getAll().mapNotNull { it.imagePath }.toHashSet()
        return (imagesDir.listFiles()?.toList() ?: emptyList())
            .filter { it.isFile && it.name !in used }
    }

    fun imageFile(fileName: String): File = File(imagesDir, fileName)

    /** Kopiuje wskazany obrazek do pamięci wewnętrznej i zwraca jego nazwę pliku. */
    suspend fun copyImageFromUri(uri: Uri): String = withContext(Dispatchers.IO) {
        val ext = appContext.contentResolver.getType(uri)
            ?.substringAfterLast('/')
            ?.takeIf { it.isNotBlank() } ?: "img"
        val fileName = "${UUID.randomUUID()}.$ext"
        appContext.contentResolver.openInputStream(uri)!!.use { input ->
            File(imagesDir, fileName).outputStream().use { output -> input.copyTo(output) }
        }
        fileName
    }

    // ---------------------------------------------------------------------------------------------
    // Import / eksport
    // ---------------------------------------------------------------------------------------------

    /**
     * Eksportuje talię do pliku `.zip` (metadane `pairs.json` + folder `images/`).
     * @param includeStats gdy false, pomija level/lastSeen/hardFlag (udostępnianie „gołej" talii).
     */
    suspend fun exportToZip(target: Uri, includeStats: Boolean) = withContext(Dispatchers.IO) {
        val pairs = dao.getAll()
        appContext.contentResolver.openOutputStream(target)!!.use { raw ->
            ZipOutputStream(raw).use { zip ->
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
                zip.putNextEntry(ZipEntry("pairs.json"))
                zip.write(json.toString(2).toByteArray())
                zip.closeEntry()

                for (p in pairs) {
                    val name = p.imagePath ?: continue
                    val file = File(imagesDir, name)
                    if (!file.exists()) continue
                    zip.putNextEntry(ZipEntry("images/$name"))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
    }

    /**
     * Eksportuje pary do pliku `.csv` (`litery,slowo,obrazek`) — do edycji w arkuszu.
     * CSV zawiera tylko tekst (bez obrazków i statystyk); spina się z importem CSV.
     */
    suspend fun exportToCsv(target: Uri) = withContext(Dispatchers.IO) {
        val pairs = dao.getAll()
        appContext.contentResolver.openOutputStream(target)!!.use { raw ->
            raw.bufferedWriter(Charsets.UTF_8).use { w ->
                w.append("litery,slowo,obrazek\n")
                for (p in pairs) {
                    w.append(csvField(p.letters)).append(',')
                        .append(csvField(p.word)).append(',')
                        .append(csvField(p.imagePath ?: "")).append('\n')
                }
            }
        }
    }

    private fun csvField(value: String): String =
        if (value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }

    /**
     * Importuje talię z pliku `.zip`.
     * @param replaceAll true = wyczyść bazę przed importem; false = scal po kluczu `letters`.
     */
    suspend fun importFromZip(source: Uri, replaceAll: Boolean) = withContext(Dispatchers.IO) {
        var pairsJson: String? = null
        var pairsCsv: String? = null
        // Krok 1: wypakuj obrazki i wczytaj metadane (pairs.json ALBO dowolny plik .csv).
        appContext.contentResolver.openInputStream(source)!!.use { raw ->
            ZipInputStream(raw).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    when {
                        name == "pairs.json" -> pairsJson = zip.readBytes().decodeToString()
                        !entry.isDirectory && name.endsWith(".csv", ignoreCase = true) ->
                            pairsCsv = zip.readBytes().decodeToString()
                        name.startsWith("images/") && !entry.isDirectory -> {
                            val fileName = name.removePrefix("images/")
                            if (fileName.isNotBlank()) {
                                File(imagesDir, fileName).outputStream().use { zip.copyTo(it) }
                            }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }

        // Krok 2: zapisz pary — preferuj JSON, w przeciwnym razie CSV. (kopie do val dla smart-castu)
        val jsonContent = pairsJson
        val csvContent = pairsCsv
        when {
            jsonContent != null -> {
                if (replaceAll) dao.deleteAll()
                val array = JSONArray(jsonContent)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val letters = obj.getString("letters")
                    val incoming = PairEntity(
                        letters = letters,
                        word = obj.optString("word", ""),
                        imagePath = obj.optString("image", null)?.takeIf { it.isNotBlank() && it != "null" },
                        level = if (obj.isNull("level")) null else obj.optInt("level"),
                        lastSeen = if (obj.isNull("lastSeen")) null else obj.optLong("lastSeen"),
                        hardFlag = obj.optBoolean("hardFlag", false),
                        reviewFlag = obj.optBoolean("reviewFlag", false)
                    )
                    val existing = dao.getByLetters(letters)
                    if (existing == null) dao.insert(incoming)
                    else dao.update(incoming.copy(id = existing.id))
                }
            }
            csvContent != null -> {
                val rows = parseCsv(csvContent)
                if (rows.isNotEmpty()) {
                    if (replaceAll) dao.deleteAll()
                    upsertCsvRows(rows)
                }
            }
        }
    }

    /**
     * Importuje pary z pliku CSV (`litery, słowo, nazwa_pliku_obrazka`).
     * Separator: przecinek lub średnik (wykrywany automatycznie). Wiersz nagłówka pomijany.
     * CSV nie zawiera obrazków ani statystyk — ustawia tylko litery, słowo i (opcjonalnie) nazwę pliku.
     *
     * @param replaceAll true = wyczyść bazę przed importem; false = scal po kluczu `letters`
     *                   (zachowując istniejące statystyki i obrazek, jeśli CSV go nie podaje).
     */
    suspend fun importFromCsv(source: Uri, replaceAll: Boolean) = withContext(Dispatchers.IO) {
        val text = appContext.contentResolver.openInputStream(source)!!.use {
            it.readBytes().decodeToString()
        }
        val rows = parseCsv(text)
        if (rows.isEmpty()) return@withContext
        if (replaceAll) dao.deleteAll()
        upsertCsvRows(rows)
    }

    /**
     * Zapisuje pary z wierszy CSV (`litery, słowo, nazwa_pliku_obrazka`). Pomija nagłówek.
     * Nie czyści bazy — o to dba wywołujący. Zawsze sprawdza `getByLetters`, więc obsługuje
     * duplikaty liter w samym pliku. Scalanie zachowuje istniejące słowo/obrazek, gdy pole puste.
     */
    private suspend fun upsertCsvRows(rows: List<List<String>>) {
        for (cols in rows) {
            val letters = cols.getOrNull(0)?.trim().orEmpty()
            if (letters.isEmpty()) continue
            if (letters.equals("letters", ignoreCase = true) || letters.equals("litery", ignoreCase = true)) continue

            val word = cols.getOrNull(1)?.trim().orEmpty()
            val image = cols.getOrNull(2)?.trim()?.takeIf { it.isNotEmpty() }

            val existing = dao.getByLetters(letters)
            if (existing == null) {
                dao.insert(PairEntity(letters = letters, word = word, imagePath = image))
            } else {
                dao.update(
                    existing.copy(
                        word = if (word.isNotEmpty()) word else existing.word,
                        imagePath = image ?: existing.imagePath
                    )
                )
            }
        }
    }

    /** Dzieli tekst CSV na wiersze i kolumny (świadomy cudzysłowów, separator , lub ;). */
    private fun parseCsv(text: String): List<List<String>> {
        val delimiter = if (text.count { it == ';' } > text.count { it == ',' }) ';' else ','
        val result = mutableListOf<List<String>>()
        for (raw in text.split("\n")) {
            val line = raw.trimEnd('\r')
            if (line.isBlank()) continue
            result.add(splitCsvLine(line, delimiter))
        }
        return result
    }

    private fun splitCsvLine(line: String, delimiter: Char): List<String> {
        val fields = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"'); i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == delimiter && !inQuotes -> { fields.add(sb.toString()); sb.setLength(0) }
                else -> sb.append(c)
            }
            i++
        }
        fields.add(sb.toString())
        return fields
    }
}
