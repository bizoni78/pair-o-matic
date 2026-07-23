package com.pairomatic.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.room.withTransaction
import com.pairomatic.data.db.AppDatabase
import com.pairomatic.data.db.PairEntity
import com.pairomatic.domain.CsvParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Jedyne źródło prawdy dla par. Obsługuje CRUD, przechowywanie obrazków w pamięci
 * wewnętrznej oraz import/eksport talii do pliku `.zip`.
 */
class PairRepository(
    private val database: AppDatabase,
    context: Context
) {
    private val dao = database.pairDao()
    private val appContext = context.applicationContext
    private val imagesDir: File = File(appContext.filesDir, "images").apply { mkdirs() }

    fun observeAll(): Flow<List<PairEntity>> = dao.observeAll()

    /** Liczniki statystyk liczone w SQL (bez ładowania całej tabeli do pamięci). */
    fun observeStats(): Flow<com.pairomatic.data.db.StatsCounts> = dao.observeStats()

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

    /**
     * Kopiuje wskazany obrazek do pamięci wewnętrznej i zwraca jego nazwę pliku.
     *
     * STA-2: jeśli obrazek jest większy niż [MAX_IMAGE_EDGE_PX], przy kopiowaniu skaluje go
     * (z zachowaniem proporcji) i re-enkoduje, żeby duże zdjęcia nie puchły w pamięci aplikacji.
     * Obrazki mniejsze — lub gdy dekodowanie się nie powiedzie (np. nietypowy format) — kopiowane
     * są 1:1 bez utraty jakości.
     */
    suspend fun copyImageFromUri(uri: Uri): String = withContext(Dispatchers.IO) {
        val mime = appContext.contentResolver.getType(uri)
        val ext = mime?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "img"

        // Krok 1: odczytaj same wymiary (bez alokacji pikseli), żeby zdecydować czy skalować.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        appContext.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        val maxEdge = maxOf(bounds.outWidth, bounds.outHeight)

        // Skalujemy tylko rastrowe obrazki, które faktycznie są za duże.
        if (bounds.outWidth <= 0 || maxEdge <= MAX_IMAGE_EDGE_PX) {
            return@withContext copyRaw(uri, ext)
        }

        val format = when (ext.lowercase()) {
            "jpg", "jpeg" -> Bitmap.CompressFormat.JPEG
            "webp" -> Bitmap.CompressFormat.WEBP
            else -> Bitmap.CompressFormat.PNG   // domyślnie bezstratnie (zachowuje przezroczystość)
        }
        // Rozszerzenie pliku zgodne z formatem zapisu (PNG dla nieznanych/rastrowych bez ext).
        val outExt = when (format) {
            Bitmap.CompressFormat.JPEG -> "jpg"
            Bitmap.CompressFormat.WEBP -> "webp"
            else -> "png"
        }

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = computeInSampleSize(maxEdge, MAX_IMAGE_EDGE_PX)
        }
        val decoded = appContext.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        } ?: return@withContext copyRaw(uri, ext)   // nie udało się zdekodować → kopia 1:1

        val scaled = scaleToMaxEdge(decoded, MAX_IMAGE_EDGE_PX)
        val fileName = "${UUID.randomUUID()}.$outExt"
        File(imagesDir, fileName).outputStream().use { output ->
            scaled.compress(format, JPEG_QUALITY, output)
        }
        if (scaled !== decoded) decoded.recycle()
        scaled.recycle()
        fileName
    }

    /** Kopiuje obrazek bajt w bajt (bez re-enkodowania). */
    private fun copyRaw(uri: Uri, ext: String): String {
        val fileName = "${UUID.randomUUID()}.$ext"
        val input = appContext.contentResolver.openInputStream(uri)
            ?: error("Nie udało się odczytać obrazka")
        input.use { stream ->
            File(imagesDir, fileName).outputStream().use { output -> stream.copyTo(output) }
        }
        return fileName
    }

    /** Największa potęga 2, przy której dłuższa krawędź zejdzie do ~[maxEdge] (dekodowanie zgrubne). */
    private fun computeInSampleSize(sourceMaxEdge: Int, maxEdge: Int): Int {
        var sample = 1
        while (sourceMaxEdge / (sample * 2) >= maxEdge) sample *= 2
        return sample
    }

    /** Dokładne doskalowanie (po zgrubnym inSampleSize) do dłuższej krawędzi [maxEdge]. */
    private fun scaleToMaxEdge(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val longer = maxOf(bitmap.width, bitmap.height)
        if (longer <= maxEdge) return bitmap
        val ratio = maxEdge.toFloat() / longer
        val w = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val h = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
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
        val output = appContext.contentResolver.openOutputStream(target)
            ?: error("Nie udało się otworzyć pliku docelowego")
        output.use { raw ->
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
        val output = appContext.contentResolver.openOutputStream(target)
            ?: error("Nie udało się otworzyć pliku docelowego")
        output.use { raw ->
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
        val input = appContext.contentResolver.openInputStream(source) ?: return@withContext
        input.use { raw ->
            ZipInputStream(raw).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    when {
                        name == "pairs.json" -> pairsJson = readLimited(zip, MAX_META_BYTES)
                        !entry.isDirectory && name.endsWith(".csv", ignoreCase = true) ->
                            pairsCsv = readLimited(zip, MAX_META_BYTES)
                        name.startsWith("images/") && !entry.isDirectory -> {
                            // SEC-1: bierzemy WYŁĄCZNIE nazwę pliku (chroni przed zip-slip: „images/../../x").
                            val safeName = File(name.removePrefix("images/")).name
                            // SEC-5: tylko pliki obrazków.
                            if (safeName.isNotBlank() && isAllowedImage(safeName)) {
                                val target = File(imagesDir, safeName)
                                // SEC-4: limit rozmiaru pojedynczego obrazka.
                                if (isInsideImagesDir(target)) {
                                    copyLimited(zip, target, MAX_IMAGE_BYTES)
                                }
                            }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }

        // Krok 2: sparsuj metadane, a zapis do bazy wykonaj ATOMOWO (SEC-2 — transakcja).
        val jsonContent = pairsJson
        val csvContent = pairsCsv
        when {
            jsonContent != null -> {
                val array = JSONArray(jsonContent)
                val incoming = ArrayList<PairEntity>(minOf(array.length(), MAX_PAIRS))
                for (i in 0 until minOf(array.length(), MAX_PAIRS)) {
                    val obj = array.getJSONObject(i)
                    incoming.add(
                        PairEntity(
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
                database.withTransaction {
                    if (replaceAll) dao.deleteAll()
                    for (inc in incoming) {
                        val existing = dao.getByLetters(inc.letters)
                        if (existing == null) dao.insert(inc)
                        else dao.update(inc.copy(id = existing.id))
                    }
                }
            }
            csvContent != null -> {
                val rows = CsvParser.parse(csvContent).take(MAX_PAIRS)
                if (rows.isNotEmpty()) {
                    database.withTransaction {
                        if (replaceAll) dao.deleteAll()
                        upsertCsvRows(rows)
                    }
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
        val text = appContext.contentResolver.openInputStream(source)?.use { readLimited(it, MAX_META_BYTES) }
            ?: return@withContext
        val rows = CsvParser.parse(text).take(MAX_PAIRS)
        if (rows.isEmpty()) return@withContext
        database.withTransaction {
            if (replaceAll) dao.deleteAll()
            upsertCsvRows(rows)
        }
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

    // ---------------------------------------------------------------------------------------------
    // Bezpieczeństwo importu (SEC-1, SEC-4, SEC-5)
    // ---------------------------------------------------------------------------------------------

    private fun isAllowedImage(name: String): Boolean =
        name.substringAfterLast('.', "").lowercase() in ALLOWED_IMAGE_EXT

    /** Sprawdza, że plik faktycznie leży w katalogu obrazków (dodatkowa ochrona przed zip-slip). */
    private fun isInsideImagesDir(file: File): Boolean =
        file.canonicalPath.startsWith(imagesDir.canonicalPath + File.separator)

    /** Kopiuje strumień do pliku z limitem bajtów; po przekroczeniu usuwa plik i zwraca false. */
    private fun copyLimited(input: InputStream, target: File, maxBytes: Long): Boolean {
        var total = 0L
        target.outputStream().use { out ->
            val buf = ByteArray(8192)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                total += n
                if (total > maxBytes) {
                    target.delete()
                    return false
                }
                out.write(buf, 0, n)
            }
        }
        return true
    }

    /** Wczytuje strumień do stringa z limitem bajtów; zwraca null, gdy przekroczono limit. */
    private fun readLimited(input: InputStream, maxBytes: Int): String? {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        var total = 0
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            total += n
            if (total > maxBytes) return null
            out.write(buf, 0, n)
        }
        return out.toString(Charsets.UTF_8.name())
    }

    companion object {
        private const val MAX_PAIRS = 10_000
        private const val MAX_META_BYTES = 8 * 1024 * 1024       // 8 MB (pairs.json / csv)
        private const val MAX_IMAGE_BYTES = 15L * 1024 * 1024    // 15 MB na obrazek
        private const val MAX_IMAGE_EDGE_PX = 1600               // STA-2: limit dłuższej krawędzi
        private const val JPEG_QUALITY = 90                      // jakość re-enkodowania (JPEG/WEBP)
        private val ALLOWED_IMAGE_EXT = setOf("png", "jpg", "jpeg", "webp")
    }
}
