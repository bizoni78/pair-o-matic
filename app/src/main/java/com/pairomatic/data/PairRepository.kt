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
     * Importuje talię z pliku `.zip`.
     * @param replaceAll true = wyczyść bazę przed importem; false = scal po kluczu `letters`.
     */
    suspend fun importFromZip(source: Uri, replaceAll: Boolean) = withContext(Dispatchers.IO) {
        var pairsJson: String? = null
        // Krok 1: wypakuj obrazki i wczytaj metadane.
        appContext.contentResolver.openInputStream(source)!!.use { raw ->
            ZipInputStream(raw).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    when {
                        name == "pairs.json" -> pairsJson = zip.readBytes().decodeToString()
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
        val content = pairsJson ?: return@withContext
        if (replaceAll) dao.deleteAll()

        // Krok 2: zapisz pary.
        val array = JSONArray(content)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val letters = obj.getString("letters")
            val incoming = PairEntity(
                letters = letters,
                word = obj.optString("word", ""),
                imagePath = obj.optString("image", null)?.takeIf { it.isNotBlank() && it != "null" },
                level = if (obj.isNull("level")) null else obj.optInt("level"),
                lastSeen = if (obj.isNull("lastSeen")) null else obj.optLong("lastSeen"),
                hardFlag = obj.optBoolean("hardFlag", false)
            )
            val existing = if (replaceAll) null else dao.getByLetters(letters)
            if (existing == null) {
                dao.insert(incoming)
            } else {
                dao.update(incoming.copy(id = existing.id))
            }
        }
    }
}
